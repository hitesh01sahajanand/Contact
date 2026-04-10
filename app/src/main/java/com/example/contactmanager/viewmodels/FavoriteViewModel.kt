package com.example.contactmanager.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.models.ContactModel
import com.example.contactmanager.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val repository: FavoriteRepository
) : ViewModel() {

   /* private var _contacts = MutableLiveData<List<ContactModel>>()
    val contacts: LiveData<List<ContactModel>> = _contacts*/

    private var _allFavoriteContacts = MutableLiveData<ArrayList<ContactModel>>()
    val allFavoriteContacts: LiveData<ArrayList<ContactModel>> = _allFavoriteContacts

    /*fun loadContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getContacts()
            _contacts.postValue(data)
        }
    }*/

    fun getAllFavoriteContact(){
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getAllFavoriteContacts()
            _allFavoriteContacts.postValue(data)
        }
    }

    fun addToFavoriteUnFavorite(contactId: String, makeFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addToFavoriteUnFavorite(contactId, makeFavorite)
        }
    }

}