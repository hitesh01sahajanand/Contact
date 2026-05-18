package com.phonecall.dialcontacts.calldialer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_dial_table")
data class SpeedDialModel(
    @PrimaryKey
    val slot: Int,
    val contactName: String,
    val contactNumber: String,
    val photoUri: String? = null
)
