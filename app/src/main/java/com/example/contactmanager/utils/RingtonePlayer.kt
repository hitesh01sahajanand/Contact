package com.example.contactmanager.utils

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Handles playing the ringtone and vibration for incoming calls.
 * Call [startRinging] when an incoming call arrives and [stopRinging] when it ends.
 */
class RingtonePlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun startRinging(number: String? = null) {
        stopRinging() // Safety: stop any previous playback

        startVibration()
        startRingtone(number)
    }

    private fun startRingtone(number: String?) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // Don't play if device is on silent
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
                Log.d("RingtonePlayer", "Ringer is silent — skipping ringtone")
                return
            }

            val contactRingtoneUri = if (!number.isNullOrEmpty()) {
                Common.getContactRingtoneUri(context, number)
            } else null

            val ringtoneUri = contactRingtoneUri ?: try {
                RingtoneManager.getActualDefaultRingtoneUri(
                    context, RingtoneManager.TYPE_RINGTONE
                )
            } catch (e: SecurityException) {
                Log.e("TAG", "startRingtone: ${e.message}")
                null
            } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val mp = MediaPlayer()

            // Link MediaPlayer volume directly to the STREAM_RING stream so
            // hardware volume keys (via CallActivity.dispatchKeyEvent) automatically
            // adjust playback without any manual setVolume() calls.
            @Suppress("DEPRECATION")
            mp.setAudioStreamType(AudioManager.STREAM_RING)

            try {
                // Try FileDescriptor first — works for content:// URIs (custom files)
                val pfd = context.contentResolver.openFileDescriptor(ringtoneUri, "r")
                if (pfd != null) {
                    pfd.use { mp.setDataSource(it.fileDescriptor) }
                } else {
                    mp.setDataSource(context, ringtoneUri)
                }
            } catch (e: Exception) {
                Log.e("TAG", "startRingtone: ${e.message}")
                // Fallback: directly set data source with context
                mp.setDataSource(context, ringtoneUri)
            }

            mp.isLooping = true
            mp.prepare()
            mp.start()
            mediaPlayer = mp
            Log.d("RingtonePlayer", "Ringtone started: $ringtoneUri")

        } catch (e: Exception) {
            Log.e("RingtonePlayer", "Failed to play ringtone", e)
        }
    }

    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 1000, 1000) // wait 0ms, vibrate 1s, wait 1s

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibrator = manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("RingtonePlayer", "Failed to start vibration", e)
        }
    }

    fun stopRinging() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("RingtonePlayer", "Error stopping ringtone", e)
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("RingtonePlayer", "Error stopping vibration", e)
        }
    }
}
