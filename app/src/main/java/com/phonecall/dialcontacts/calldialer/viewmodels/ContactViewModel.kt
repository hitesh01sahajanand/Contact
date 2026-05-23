package com.phonecall.dialcontacts.calldialer.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.models.ContactListItem
import com.phonecall.dialcontacts.calldialer.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val repository: ContactRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val handler = Handler(Looper.getMainLooper())
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({
                loadContacts(showLoader = false)
            }, 300)
        }
    }

    init {
        registerObserver()
    }

    private var isObserverRegistered = false

    private fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun registerObserver() {
        if (isObserverRegistered) return
        // Guard: only register if READ_CONTACTS is granted to avoid SecurityException
        if (!hasContactPermission()) return
        try {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer
            )
            isObserverRegistered = true
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isObserverRegistered) {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    private var _allContactList = MutableLiveData<List<ContactListItem>>()
    val allContactList: LiveData<List<ContactListItem>> = _allContactList

    private var _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var _accountCounts = MutableLiveData<Map<String, Int>>()
    val accountCounts: LiveData<Map<String, Int>> = _accountCounts

    var currentSelectedAccount: String = ACCOUNT_ALL

    fun loadContacts(showLoader: Boolean = true) {
        // Skip silently if permission not yet granted
        if (!hasContactPermission()) return
        registerObserver()
        when (currentSelectedAccount) {
            ACCOUNT_ALL -> loadAllContacts(showLoader)
            ACCOUNT_DEVICE -> getContactsByDevice(showLoader)
            else -> getContactsByAccountWithHeaders(currentSelectedAccount, showLoader)
        }
    }

    companion object {
        const val ACCOUNT_ALL = "All Accounts"
        const val ACCOUNT_DEVICE = "Device Only"
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadAllContacts(showLoader: Boolean = true) {
        if (showLoader) _isLoading.postValue(true)
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = repository.getContactsByAccountWithHeaders()
                _allContactList.postValue(data)
            } finally {
                if (showLoader) _isLoading.postValue(false)
            }
        }
    }

    fun getContactsByAccountWithHeaders(accountName: String, showLoader: Boolean = true) {
        if (showLoader) _isLoading.postValue(true)
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = repository.getContactsByGoogleAccount(accountName)
                _allContactList.postValue(data)
            } finally {
                if (showLoader) _isLoading.postValue(false)
            }
        }
    }

    fun getContactsByDevice(showLoader: Boolean = true) {
        if (showLoader) _isLoading.postValue(true)
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = repository.getContactsByDevice()
                _allContactList.postValue(data)
            } finally {
                if (showLoader) _isLoading.postValue(false)
            }
        }
    }

    fun fetchAccountCounts(isMerge: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val counts = repository.getAccountContactCounts(isMerge)
            _accountCounts.postValue(counts)
        }
    }
}