package com.phonecall.dialcontacts.calldialer.viewmodels

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonecall.dialcontacts.calldialer.models.QuickResponseModel
import com.phonecall.dialcontacts.calldialer.repository.QuickResponseRepository
import com.phonecall.dialcontacts.calldialer.utils.QuickResponseDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuickResponseViewModel @Inject constructor(
    private val repository: QuickResponseRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val localeKey = MutableStateFlow(currentLocaleKey())

    val messages: StateFlow<List<QuickResponseModel>> = combine(
        repository.getAllMessages(),
        localeKey
    ) { storedMessages, _ ->
        QuickResponseDefaults.buildDisplayList(context, storedMessages)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshForLocaleChange() {
        localeKey.value = currentLocaleKey()
    }

    fun insertMessage(message: String) {
        viewModelScope.launch {
            repository.insertMessage(message)
        }
    }

    fun deleteMessageById(id: Int) {
        if (id <= 0) return
        viewModelScope.launch {
            repository.deleteMessageById(id)
        }
    }

    private fun currentLocaleKey(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) {
            "default"
        } else {
            locales[0]?.toLanguageTag() ?: "default"
        }
    }
}
