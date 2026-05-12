package com.example.contactmanager.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.database.ReminderDao
import com.example.contactmanager.models.ReminderModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderDao: ReminderDao
) : ViewModel() {

    val allPendingReminders: LiveData<List<ReminderModel>> = reminderDao.getAllPendingReminders()

    fun insertReminder(reminder: ReminderModel) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.insertReminder(reminder)
        }
    }

    fun markAsDone(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.markAsDone(id)
        }
    }
}
