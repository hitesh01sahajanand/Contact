package com.example.contactmanager.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.models.AccountModel
import com.example.contactmanager.repository.NewContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewContactViewModel @Inject constructor(
    private val repository: NewContactRepository
) : ViewModel() {

    private var _googleAccount: MutableLiveData<List<Pair<String, String>>> = MutableLiveData()
    val googleAccount: LiveData<List<Pair<String, String>>> = _googleAccount

    private var _savedContactMassage: MutableLiveData<String> = MutableLiveData()
    val savedContactMassage: LiveData<String> = _savedContactMassage

    fun getGoogleAccounts() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getGoogleAccounts()
            _googleAccount.postValue(data)
        }
    }

    fun saveContact(
        firstName: String,
        lastName: String,
        phoneList: List<Pair<String, String>>,
        emailList: List<Pair<String, String>>,
        selectedImageUri: Uri?,
        accountModel: AccountModel,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveContact(
                firstName,
                lastName,
                phoneList,
                emailList,
                selectedImageUri,
                accountModel,
                onCallBack = { msg ->
                    _savedContactMassage.postValue(msg)
                }
            )
        }
    }
}