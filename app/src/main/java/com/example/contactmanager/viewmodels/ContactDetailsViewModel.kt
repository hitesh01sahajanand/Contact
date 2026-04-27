package com.example.contactmanager.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.repository.ContactDetailsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactDetailsViewModel @Inject constructor(private val repository: ContactDetailsRepository) :
    ViewModel() {
    private val _contactHistory: MutableLiveData<List<CallLogEntry>> = MutableLiveData()
    val contactHistory: LiveData<List<CallLogEntry>> = _contactHistory

    private val _contactData: MutableLiveData<CallLogEntry?> = MutableLiveData()
    val contactData: LiveData<CallLogEntry?> = _contactData


    fun getNumberToHistory(number: String, offset: Int, limit: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getCallHistoryForNumber(number, offset, limit)
            _contactHistory.postValue(data)
        }
    }

    fun getUpdatedContact(contactId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(300) 
            val data = repository.getUpdatedContact(contactId)
            _contactData.postValue(data)
        }
    }

    fun deleteCallHistoryForNumber(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCallHistoryForNumber(number)
        }
    }
}