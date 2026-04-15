package com.example.contactmanager.viewmodels

import android.graphics.Color
import android.telephony.PhoneNumberUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.models.CallHistoryListItems
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.repository.RecentRepository
import com.example.contactmanager.utils.Common
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val repository: RecentRepository
) : ViewModel() {

    private var _allRecentCallHistory = MutableLiveData<ArrayList<CallHistoryListItems>>()
    val allRecentCallHistory: LiveData<ArrayList<CallHistoryListItems>> = _allRecentCallHistory

    private val colorList = listOf(
        Color.parseColor("#2173C2"),
        Color.parseColor("#FFB950"),
        Color.parseColor("#AEB33C"),
        Color.parseColor("#FF6082"),
        Color.parseColor("#60CB6B")
    )

    fun loadAllRecentsHistory(offset: Int, limit: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val rawData = repository.getCallHistory(offset, limit)
            val processedData = processRawCallLogs(rawData)
            _allRecentCallHistory.postValue(processedData)
        }
    }

    private suspend fun processRawCallLogs(
        list: ArrayList<CallLogEntry>
    ): ArrayList<CallHistoryListItems> = withContext(Dispatchers.Default) {

        if (list.isEmpty()) return@withContext ArrayList()

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

        for (entry in list) {
            val entryNum = entry.stringNumber
            if (entryNum.isNullOrEmpty()) continue

            val cleaned = Common.cleanNumber(entryNum)
            val colorIndex = (cleaned.hashCode() and Int.MAX_VALUE) % colorList.size
            entry.intColor = colorList[colorIndex]

            val entryDate = entry.dateData
            val entryTime = entryDate?.time ?: 0L

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