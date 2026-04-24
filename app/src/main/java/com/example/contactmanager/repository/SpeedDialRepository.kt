package com.example.contactmanager.repository

import com.example.contactmanager.database.SpeedDialDao
import com.example.contactmanager.models.SpeedDialModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SpeedDialRepository @Inject constructor(
    private val speedDialDao: SpeedDialDao
) {
    fun getAllSpeedDials(): Flow<List<SpeedDialModel>> = speedDialDao.getAllSpeedDials()

    suspend fun insertSpeedDial(speedDial: SpeedDialModel) {
        speedDialDao.insertSpeedDial(speedDial)
    }

    suspend fun deleteSpeedDial(slot: Int) {
        speedDialDao.deleteSpeedDial(slot)
    }

    suspend fun getSpeedDialBySlot(slot: Int): SpeedDialModel? {
        return speedDialDao.getSpeedDialBySlot(slot)
    }
}
