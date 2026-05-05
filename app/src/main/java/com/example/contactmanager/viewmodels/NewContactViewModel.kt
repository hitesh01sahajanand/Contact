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

    private var _googleAccount: MutableLiveData<List<Triple<String, String, String>>> = MutableLiveData()
    val googleAccount: LiveData<List<Triple<String, String, String>>> = _googleAccount

    private var _contactEmail: MutableLiveData<String> = MutableLiveData()
    val contactEmail: LiveData<String> = _contactEmail

    private var _savedContactMassage: MutableLiveData<String> = MutableLiveData()
    val savedContactMassage: LiveData<String> = _savedContactMassage

    fun getGoogleAccounts() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getGoogleAccounts()
            _googleAccount.postValue(data)
        }
    }

    fun fetchContactEmail(contactId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val email = repository.getContactEmail(contactId)
            _contactEmail.postValue(email ?: "")
        }
    }

    private var _contactAccountInfo: MutableLiveData<Pair<String?, String?>> = MutableLiveData()
    val contactAccountInfo: LiveData<Pair<String?, String?>> = _contactAccountInfo

    fun fetchContactAccountName(contactId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val accountInfo = repository.getContactAccountName(contactId)
            _contactAccountInfo.postValue(accountInfo)
        }
    }

    private var _newContactId: MutableLiveData<String?> = MutableLiveData()
    val newContactId: LiveData<String?> = _newContactId

    fun saveOrUpdateContact(
        name: String,
        number: String,
        email: String?,
        selectedImageUri: Uri?,
        accountModel: AccountModel,
        isContactSaved: Boolean,
        contactId: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveOrUpdateContact(
                name = name,
                number = number,
                email = email,
                selectedImageUri = selectedImageUri,
                accountModel = accountModel,
                isContactSaved = isContactSaved,
                contactId = contactId,
                onCallBack = { msg, newId ->
                    _savedContactMassage.postValue(msg)
                    _newContactId.postValue(newId)
                }
            )
        }
    }
}