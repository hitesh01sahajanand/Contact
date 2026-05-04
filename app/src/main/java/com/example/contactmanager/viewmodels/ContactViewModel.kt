package com.example.contactmanager.viewmodels

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.models.ContactListItem
import com.example.contactmanager.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val repository: ContactRepository,
    @param: ApplicationContext private val context: Context
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
        try {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        context.contentResolver.unregisterContentObserver(observer)
    }

    private var _allContactList = MutableLiveData<List<ContactListItem>>()
    val allContactList: LiveData<List<ContactListItem>> = _allContactList

    private var _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var _accountCounts = MutableLiveData<Map<String, Int>>()
    val accountCounts: LiveData<Map<String, Int>> = _accountCounts

    var currentSelectedAccount: String = "All Accounts"

    fun loadContacts(showLoader: Boolean = true) {
        when (currentSelectedAccount) {
            "All Accounts" -> loadAllContacts(showLoader)
            "Device Only" -> getContactsByDevice(showLoader)
            else -> getContactsByAccountWithHeaders(currentSelectedAccount, showLoader)
        }
    }

    fun loadAllContacts(showLoader: Boolean = true) {
        if (showLoader) _isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getContactsByAccountWithHeaders()
            _allContactList.postValue(data)
            if (showLoader) _isLoading.postValue(false)
        }
    }

    fun getContactsByAccountWithHeaders(accountName: String, showLoader: Boolean = true) {
        if (showLoader) _isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getContactsByGoogleAccount(accountName)
            _allContactList.postValue(data)
            if (showLoader) _isLoading.postValue(false)
        }
    }

    fun getContactsByDevice(showLoader: Boolean = true) {
        if (showLoader) _isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getContactsByDevice()
            _allContactList.postValue(data)
            if (showLoader) _isLoading.postValue(false)
        }
    }
    /*fun getContactCount(account: String): String {
        val list = when (account) {
            "All Accounts" -> repository.getContactsByAccountWithHeaders()
            "Device Only" -> repository.getContactsByDevice()
            else -> repository.getContactsByGoogleAccount(account)
        }
        return list.filterIsInstance<ContactListItem.Contact>().size.toString()
    }*/

    fun fetchAccountCounts(isMerge: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val counts = repository.getAccountContactCounts(isMerge)
            _accountCounts.postValue(counts)
        }
    }
}