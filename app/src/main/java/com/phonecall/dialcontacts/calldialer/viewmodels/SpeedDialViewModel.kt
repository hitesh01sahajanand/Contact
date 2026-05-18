package com.phonecall.dialcontacts.calldialer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonecall.dialcontacts.calldialer.models.SpeedDialModel
import com.phonecall.dialcontacts.calldialer.repository.SpeedDialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpeedDialViewModel @Inject constructor(
    private val repository: SpeedDialRepository
) : ViewModel() {

    val speedDialList: StateFlow<List<SpeedDialModel>> = repository.getAllSpeedDials()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun insertSpeedDial(slot: Int, name: String, number: String, photoUri: String? = null) {
        viewModelScope.launch {
            repository.insertSpeedDial(SpeedDialModel(slot, name, number, photoUri))
        }
    }

    fun deleteSpeedDial(slot: Int) {
        viewModelScope.launch {
            repository.deleteSpeedDial(slot)
        }
    }
    
    suspend fun getSpeedDialBySlot(slot: Int): SpeedDialModel? {
        return repository.getSpeedDialBySlot(slot)
    }
}
