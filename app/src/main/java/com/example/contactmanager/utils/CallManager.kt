package com.example.contactmanager.utils

import android.telecom.Call
import android.telecom.VideoProfile
import androidx.lifecycle.MutableLiveData

object CallManager {
    val callState = MutableLiveData<Pair<Call, Int>>()

    val callList = MutableLiveData<List<Call>>()

    fun updateCall(call: Call, state: Int) {
        callState.postValue(call to state)
    }

    fun updateCallList(list: List<Call>) {
        callList.postValue(list)
    }
}