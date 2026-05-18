package com.phonecall.dialcontacts.calldialer.repository

import com.phonecall.dialcontacts.calldialer.database.SpeedDialDao
import com.phonecall.dialcontacts.calldialer.models.SpeedDialModel
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
