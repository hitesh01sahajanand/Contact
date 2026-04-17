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
                onCallBack = { msg ->
                    _savedContactMassage.postValue(msg)
                }
            )
        }
    }
}