package com.example.contactmanager.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telecom.VideoProfile
import com.example.contactmanager.utils.NewCallManager

class CallActionService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val call = NewCallManager.getPrimaryCall() ?: return START_NOT_STICKY

        when (intent?.action) {
            "ANSWER" -> {
                call.answer(VideoProfile.STATE_AUDIO_ONLY)
            }

            "DECLINE", "HANGUP" -> {
                call.disconnect()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}