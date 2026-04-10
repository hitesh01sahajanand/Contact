package com.example.contactmanager.utils

import android.telecom.Call
import android.telecom.VideoProfile

object CallManager {
    var currentCall: Call? = null
    var phoneNumber: String? = null

    fun answer() {
        currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun disconnect() {
        currentCall?.disconnect()
    }
}