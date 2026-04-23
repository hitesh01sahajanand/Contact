package com.example.contactmanager.viewmodels

import android.content.Context
import android.database.ContentObserver
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.telephony.PhoneNumberUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.models.CallHistoryListItems
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.repository.BlockRepository
import com.example.contactmanager.repository.RecentRepository
import com.example.contactmanager.repository.TagRepository
import com.example.contactmanager.utils.Common
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.flow.first

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val repository: RecentRepository,
    private val tagRepository: TagRepository,
    private val blockRepository: BlockRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val handler = Handler(Looper.getMainLooper())
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({
                loadAllRecentsHistory(0, 1000)
            }, 1000)
        }
    }

    init {
        context.contentResolver.registerContentObserver(
            CallLog.Calls.CONTENT_URI,
            true,
            observer
        )
        observeBlockedNumbers()
    }

    private fun observeBlockedNumbers() {
        viewModelScope.launch {
            blockRepository.getAllBlockedNumbers().collect {
                entriesMutex.withLock {
                    if (allRawEntries.isNotEmpty()) {
                        val processedData = processRawCallLogs(allRawEntries)
                        _allRecentCallHistory.postValue(processedData)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        context.contentResolver.unregisterContentObserver(observer)
    }

    private var currentOffset = 0
    private var isLastPage = false
    private var isLoading = false
    private val entriesMutex = Mutex()
    private val allRawEntries = ArrayList<CallLogEntry>()

    private var _allRecentCallHistory = MutableLiveData<ArrayList<CallHistoryListItems>>()
    val allRecentCallHistory: LiveData<ArrayList<CallHistoryListItems>> = _allRecentCallHistory

    private var _isNextPageLoading = MutableLiveData<Boolean>()
    val isNextPageLoading: LiveData<Boolean> = _isNextPageLoading

    private val colorList = listOf(
        "#2173C2".toColorInt(),
        "#FFB950".toColorInt(),
        "#AEB33C".toColorInt(),
        "#FF6082".toColorInt(),
        "#60CB6B".toColorInt()
    )

    fun loadAllRecentsHistory(offset: Int, limit: Int) {
        if (isLoading) return
        isLoading = true
        
        if (offset == 0) {
            currentOffset = 0
            isLastPage = false
        }

        if (offset > 0) {
            _isNextPageLoading.postValue(true)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val rawData = repository.getCallHistory(offset, limit)
            
            if (rawData.isEmpty()) {
                isLastPage = true
                isLoading = false
                _isNextPageLoading.postValue(false)
                if (offset == 0) {
                    _allRecentCallHistory.postValue(ArrayList())
                }
                return@launch
            }

            val processedData = entriesMutex.withLock {
                if (offset == 0) {
                    allRawEntries.clear()
                }
                allRawEntries.addAll(rawData)
                currentOffset += rawData.size

                processRawCallLogs(allRawEntries)
            }
            
            _allRecentCallHistory.postValue(processedData)
            
            isLoading = false
            _isNextPageLoading.postValue(false)
            
            if (rawData.size < limit) {
                isLastPage = true
            }
        }
    }

    fun loadNextPage() {
        if (!isLoading && !isLastPage) {
            loadAllRecentsHistory(currentOffset, 1000)
        }
    }

    fun saveTag(phoneNumber: String, tagName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.saveTag(phoneNumber, tagName)
            // Reload history to show the tag
            loadAllRecentsHistory(0, currentOffset.coerceAtLeast(1000))
        }
    }

    fun blockNumber(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blockRepository.blockNumber(phoneNumber)
        }
    }

    fun unblockNumber(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blockRepository.unblockNumber(phoneNumber)
        }
    }

    private suspend fun processRawCallLogs(
        rawList: List<CallLogEntry>
    ): ArrayList<CallHistoryListItems> = withContext(Dispatchers.Default) {

        if (rawList.isEmpty()) return@withContext ArrayList()

        // Create deep copies to avoid side effects on the source list and thread safety issues.
        // This ensures that allRawEntries remains "raw" and we don't have race conditions on shared objects.
        val list = rawList.map { it.copy(callIds = it.callIds.toMutableList()) }

        val processedList = ArrayList<CallHistoryListItems>()
        var lastEntry: CallLogEntry? = null
        var lastDateCategory = ""

        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayStart = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStart = calendar.timeInMillis

        val todayStr = "Today"
        val yesterdayStr = "Yesterday"

        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()

        // Pre-fetch tags and blocked numbers once
        val allTags = tagRepository.getAllTags().first()
        val allBlocked = blockRepository.getAllBlockedNumbers().first()

        // Create lookup maps for performance
        val tagMap = allTags.associate { Common.cleanNumber(it.phoneNumber) to it.tagName }
        val blockedNumbersSet = allBlocked.map { Common.cleanNumber(it.phoneNumber) }.toSet()

        for (entry in list) {
            val entryNum = entry.stringNumber
            if (entryNum.isNullOrEmpty()) continue

            val cleaned = Common.cleanNumber(entryNum)
            val colorIndex = (cleaned.hashCode() and Int.MAX_VALUE) % colorList.size
            entry.intColor = colorList[colorIndex]

            val entryDate = entry.dateData
            val entryTime = entryDate?.time ?: 0L

            // Lookup tag and blocked status from memory cache instead of DB
            if (entry.stringCallName.isNullOrEmpty()) {
                val tag = tagMap[cleaned]
                if (!tag.isNullOrEmpty()) {
                    entry.stringCallName = tag
                }
            }
            
            // Check if blocked in custom DB
            entry.isBlocked = blockedNumbersSet.contains(cleaned) || 
                             allBlocked.any { b -> PhoneNumberUtils.compare(b.phoneNumber, entryNum) }

            val currentCategory = when {
                entryTime >= todayStart -> todayStr
                entryTime >= yesterdayStart -> yesterdayStr
                entryDate != null -> dateFormat.format(entryDate)
                else -> ""
            }

            var isDuplicate = false
            lastEntry?.let { last ->
                val lastDate = last.dateData
                if (entryDate != null && lastDate != null) {
                    cal1.time = entryDate
                    cal2.time = lastDate
                    
                    val sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                                 cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

                    if (sameDay) {
                        val isSameNumber = PhoneNumberUtils.compare(entryNum, last.stringNumber)
                        val isSameName = !entry.stringCallName.isNullOrEmpty() && 
                                        entry.stringCallName == last.stringCallName

                        if (isSameNumber || isSameName) {
                            isDuplicate = true
                        }
                    }
                }
            }

            if (isDuplicate) {
                lastEntry?.apply {
                    callCount += 1
                    callIds.addAll(entry.callIds)
                }
            } else {
                if (currentCategory != lastDateCategory) {
                    processedList.add(CallHistoryListItems.Header(currentCategory))
                    lastDateCategory = currentCategory
                }
                entry.callCount = 1
                processedList.add(CallHistoryListItems.Contact(entry))
                lastEntry = entry
            }
        }
        processedList
    }
}