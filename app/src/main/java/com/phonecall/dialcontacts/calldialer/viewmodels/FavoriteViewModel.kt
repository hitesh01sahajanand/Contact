package com.phonecall.dialcontacts.calldialer.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonecall.dialcontacts.calldialer.models.ContactModel
import com.phonecall.dialcontacts.calldialer.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val repository: FavoriteRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val handler = Handler(Looper.getMainLooper())

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({
                getAllFavoriteContact()
            }, 1000)
        }
    }

    init {
        registerObserver()
    }

    private var isObserverRegistered = false

    private fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun registerObserver() {
        if (isObserverRegistered) return
        // Guard: only register if READ_CONTACTS is granted to avoid SecurityException
        if (!hasContactPermission()) return
        try {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer
            )
            isObserverRegistered = true
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isObserverRegistered) {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    private var _allFavoriteContacts = MutableLiveData<ArrayList<ContactModel>>()
    val allFavoriteContacts: LiveData<ArrayList<ContactModel>> = _allFavoriteContacts
    fun getAllFavoriteContact() {
        // Skip silently if permission not yet granted
        if (!hasContactPermission()) return
        registerObserver()
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getAllFavoriteContacts()
            _allFavoriteContacts.postValue(data)
        }
    }

    fun addToFavoriteUnFavorite(contactId: String, makeFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                repository.addToFavoriteUnFavorite(contactId, makeFavorite)
            }
        }
    }

    fun updateFavoriteStatus(contacts: List<ContactModel>) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                repository.updateFavoriteStatusBatch(contacts)
                kotlinx.coroutines.delay(500) // Give DB time to settle
                getAllFavoriteContact()
            }
        }
    }

}