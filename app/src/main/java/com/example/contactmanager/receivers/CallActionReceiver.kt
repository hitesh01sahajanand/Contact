package com.example.contactmanager.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.VideoProfile
import com.example.contactmanager.utils.NewCallManager

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val call = NewCallManager.getPrimaryCall() ?: return

        when (intent?.action) {
            "ANSWER" -> {
                call.answer(VideoProfile.STATE_AUDIO_ONLY)
            }
            "DECLINE", "HANGUP" -> {
                call.disconnect()
            }
        }
    }
}
