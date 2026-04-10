package com.example.contactmanager.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.contactmanager.models.CallLogEntry

object SendData {
    var contactDetails: CallLogEntry? = null

    var allRecentCallHistory: MutableLiveData<ArrayList<CallLogEntry>> = MutableLiveData()
    var isFirstTime = false

}