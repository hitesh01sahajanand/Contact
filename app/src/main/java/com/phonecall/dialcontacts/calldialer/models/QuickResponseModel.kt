package com.phonecall.dialcontacts.calldialer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_response_table")
data class QuickResponseModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val message: String
)
