package com.example.contactmanager.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.R
import com.example.contactmanager.models.QuickResponseModel
import com.example.contactmanager.repository.QuickResponseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuickResponseViewModel @Inject constructor(
    private val repository: QuickResponseRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val messages: StateFlow<List<QuickResponseModel>> = repository.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun insertMessage(message: String) {
        viewModelScope.launch {
            repository.insertMessage(message)
        }
    }

    fun deleteMessageById(id: Int) {
        viewModelScope.launch {
            repository.deleteMessageById(id)
        }
    }

    fun initializeDefaultMessages() {
        viewModelScope.launch {
            val defaultMessages = listOf(
                context.getString(R.string.can_t_talk_right_now),
                context.getString(R.string.i_ll_call_you_later),
                context.getString(R.string.i_m_on_my_way),
                context.getString(R.string.can_t_talk_now_call_me_later)
            )
            repository.insertInitialMessages(defaultMessages)
        }
    }
}
