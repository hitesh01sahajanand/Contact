package com.example.contactmanager.viewmodels

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
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
                loadAllContacts()
            }, 1000)
        }
    }

    init {
        context.contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            observer
        )
    }

    override fun onCleared() {
        super.onCleared()
        context.contentResolver.unregisterContentObserver(observer)
    }

    private var _allContactList = MutableLiveData<List<ContactListItem>>()
    val allContactList: LiveData<List<ContactListItem>> = _allContactList

    var currentSelectedAccount: String = "All Accounts"

    fun loadContacts() {
        when (currentSelectedAccount) {
            "All Accounts" -> loadAllContacts()
            "Device Only" -> getContactsByDevice()
            else -> getContactsByAccountWithHeaders(currentSelectedAccount)
        }
    }

    fun loadAllContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getContactsByAccountWithHeaders()
            _allContactList.postValue(data)
        }
    }

    fun getContactsByAccountWithHeaders(accountName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getContactsByGoogleAccount(accountName)
            _allContactList.postValue(data)
        }
    }

    fun getContactsByDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getContactsByDevice()
            _allContactList.postValue(data)
        }
    }

}