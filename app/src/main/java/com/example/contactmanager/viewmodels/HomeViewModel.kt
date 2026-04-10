package com.example.contactmanager.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : ViewModel() {



    private var _contactName = MutableLiveData<String>()
    val contactName: LiveData<String> = _contactName







    /*private var _isSavedNumber: MutableLiveData<Boolean> = MutableLiveData()
    val isSavedNumber: LiveData<Boolean> = _isSavedNumber*/




    fun getContactName(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = homeRepository.getContactName(phoneNumber)
            _contactName.postValue(data)
        }
    }






    /*fun isSaveContact(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = homeRepository.isNumberSaved(number)
            _isSavedNumber.postValue(data)
        }
    }*/
}