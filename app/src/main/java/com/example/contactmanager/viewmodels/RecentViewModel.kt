package com.example.contactmanager.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.repository.RecentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val repository: RecentRepository
) : ViewModel() {

    private var _allRecentCallHistory = MutableLiveData<ArrayList<CallLogEntry>>()
    val allRecentCallHistory: LiveData<ArrayList<CallLogEntry>> = _allRecentCallHistory

    /* private var _allMessedCall = MutableLiveData<List<CallLogEntry>>()
     val allMessedCall: LiveData<List<CallLogEntry>> = _allMessedCall*/

    fun loadAllRecentsHistory(offset: Int, limit: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getCallHistory(offset, limit)
            _allRecentCallHistory.postValue(data)
        }
    }

    /* fun loadMissedCalls() {
         viewModelScope.launch(Dispatchers.IO) {
             val data = repository.getMissedCalls()
             _allMessedCall.postValue(data)
         }
     }*/

}