package com.example.contactmanager.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.models.BlockModel
import com.example.contactmanager.repository.BlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockViewModel @Inject constructor(
    private val blockRepository: BlockRepository
) : ViewModel() {

    val allBlockedNumbers: Flow<List<BlockModel>> = blockRepository.getAllBlockedNumbers()

    fun blockNumber(number: String) {
        viewModelScope.launch {
            blockRepository.blockNumber(number)
        }
    }

    fun unblockNumber(number: String) {
        viewModelScope.launch {
            blockRepository.unblockNumber(number)
        }
    }
}
