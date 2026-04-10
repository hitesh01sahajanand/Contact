package com.example.contactmanager.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.models.ContactListItem
import com.example.contactmanager.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val repository: ContactRepository
) : ViewModel() {

    private var _allContactList = MutableLiveData<List<ContactListItem>>()
    val allContactList: LiveData<List<ContactListItem>> = _allContactList

    private var _googleAccountList: MutableLiveData<List<ContactListItem>> = MutableLiveData()
    val googleAccountList: LiveData<List<ContactListItem>> = _googleAccountList

    fun loadAllContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getContactsByAccountWithHeaders()
            _allContactList.postValue(data)
        }
    }

    fun getContactsByAccountWithHeaders(accountName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getContactsByGoogleAccount(accountName)
            _googleAccountList.postValue(data)
        }
    }

}