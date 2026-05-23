package com.phonecall.dialcontacts.calldialer.receivers

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.endCall.EndCallActivity
import com.phonecall.dialcontacts.calldialer.callEndUtils.CallEndLaunchHelper
import com.phonecall.dialcontacts.calldialer.callEndUtils.CallEndPendingLaunch
import java.util.Date

class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PhoneStateReceiver"
        private const val PREFS_NAME = "PhoneStatePrefs"
        private const val KEY_LAST_STATE = "lastState"
        private const val KEY_SAVED_NUMBER = "savedNumber"
        private const val KEY_IS_INCOMING = "isIncoming"
        private const val KEY_CALL_START_TIME = "callStartTime"
        private const val KEY_IDLE_HANDLED = "idleHandled"

        // Separate notification channel owned by this receiver
        private const val RECEIVER_CHANNEL_ID = "receiver_end_call_channel"
        private const val NOTIFY_ID = 9912
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val action = intent.action
        Log.d(TAG, "onReceive action=$action")

        when (action) {
            Intent.ACTION_USER_PRESENT -> {
                Log.d(TAG, "onReceive: USER_PRESENT, launching pending call end")
                CallEndPendingLaunch.tryLaunchAndClear(context)
            }

            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d(TAG, "PHONE_STATE state=$stateStr number=$number")

                if (!number.isNullOrEmpty()) {
                    prefs.edit().putString(KEY_SAVED_NUMBER, number).apply()
                }

                val state = when (stateStr) {
                    TelephonyManager.EXTRA_STATE_IDLE    -> TelephonyManager.CALL_STATE_IDLE
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
                    TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
                    else -> return
                }
                handleStateChange(context, state, prefs)
            }

            Intent.ACTION_NEW_OUTGOING_CALL -> {
                val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                Log.d(TAG, "NEW_OUTGOING_CALL number=$number")
                prefs.edit().apply {
                    putString(KEY_SAVED_NUMBER, number)
                    putBoolean(KEY_IS_INCOMING, false)
                    putLong(KEY_CALL_START_TIME, System.currentTimeMillis())
                    putInt(KEY_LAST_STATE, TelephonyManager.CALL_STATE_OFFHOOK)
                    putBoolean(KEY_IDLE_HANDLED, false)
                    apply()
                }
            }
        }
    }

    private fun handleStateChange(
        context: Context,
        state: Int,
        prefs: android.content.SharedPreferences
    ) {
        val lastState = prefs.getInt(KEY_LAST_STATE, TelephonyManager.CALL_STATE_IDLE)

        if (state == TelephonyManager.CALL_STATE_IDLE) {
            // Guard: don't fire twice for the same call end
            val alreadyHandled = prefs.getBoolean(KEY_IDLE_HANDLED, false)
            if (alreadyHandled) {
                Log.d(TAG, "IDLE already handled – skipping")
                return
            }
            // If the process was killed and missed RINGING/OFFHOOK, callStartTime will be 0.
            // Still try to show EndCallActivity using the call log as a fallback for the number.
            val hasCallData = prefs.getLong(KEY_CALL_START_TIME, 0L) > 0L
            if (!hasCallData && lastState == TelephonyManager.CALL_STATE_IDLE) {
                Log.d(TAG, "No call data and lastState=IDLE – skipping")
                return
            }
        } else {
            // For non-IDLE states, skip duplicate transitions
            if (lastState == state) return
        }

        prefs.edit().putInt(KEY_LAST_STATE, state).apply()

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                prefs.edit().apply {
                    putBoolean(KEY_IS_INCOMING, true)
                    putLong(KEY_CALL_START_TIME, System.currentTimeMillis())
                    putBoolean(KEY_IDLE_HANDLED, false)
                    apply()
                }
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    // Outgoing call
                    prefs.edit().apply {
                        putBoolean(KEY_IS_INCOMING, false)
                        putLong(KEY_CALL_START_TIME, System.currentTimeMillis())
                        putBoolean(KEY_IDLE_HANDLED, false)
                        apply()
                    }
                }
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                val endTime       = System.currentTimeMillis()
                val isIncoming    = prefs.getBoolean(KEY_IS_INCOMING, false)
                val callStartTime = prefs.getLong(KEY_CALL_START_TIME, 0L)
                val savedNumber   = prefs.getString(KEY_SAVED_NUMBER, null)

                // Try to get more accurate type from call log
                val logType = getLastCallTypeFromLog(context)
                val callType = when (logType) {
                    CallLog.Calls.INCOMING_TYPE -> EndCallActivity.CALL_TYPE_INCOMING
                    CallLog.Calls.OUTGOING_TYPE -> EndCallActivity.CALL_TYPE_OUTGOING
                    CallLog.Calls.MISSED_TYPE -> EndCallActivity.CALL_TYPE_MISSED
                    CallLog.Calls.REJECTED_TYPE -> EndCallActivity.CALL_TYPE_REJECTED
                    else -> {
                        // Fallback logic
                        when {
                            lastState == TelephonyManager.CALL_STATE_RINGING -> EndCallActivity.CALL_TYPE_MISSED
                            isIncoming -> EndCallActivity.CALL_TYPE_INCOMING
                            else -> EndCallActivity.CALL_TYPE_OUTGOING
                        }
                    }
                }

                // Mark handled BEFORE launching so a second broadcast can't fire again
                prefs.edit().putBoolean(KEY_IDLE_HANDLED, true).apply()

                Log.e(TAG, "handleStateChange: fgdgdgdggdgg", )

//                launchEndCallActivity(context, savedNumber, callStartTime, endTime, callType)
                CallEndLaunchHelper.openAfterCallEnded(context, savedNumber, Date(callStartTime), Date(endTime), callType)

                // Reset
                prefs.edit().apply {
                    remove(KEY_SAVED_NUMBER)
                    remove(KEY_IS_INCOMING)
                    remove(KEY_CALL_START_TIME)
                    putInt(KEY_LAST_STATE, TelephonyManager.CALL_STATE_IDLE)
                    apply()
                }
            }
        }
    }

    private fun launchEndCallActivity(
        context: Context,
        number: String?,
        startTime: Long,
        endTime: Long,
        callType: String
    ) {
        val finalNumber = number?.takeIf { it.isNotEmpty() }
            ?: getLastCallNumberFromLog(context)
            ?: ""

        val intent = Intent(context, EndCallActivity::class.java).apply {
            putExtra(EndCallActivity.EXTRA_MOBILE_NUMBER, finalNumber)
            putExtra(EndCallActivity.EXTRA_START_TIME, startTime)
            putExtra(EndCallActivity.EXTRA_END_TIME, endTime)
            putExtra(EndCallActivity.EXTRA_CALL_TYPE, callType)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, NOTIFY_ID, intent, piFlags
        )

        // ── Step 1: Full-screen notification ────────────────────────────────────
        // This is the PRIMARY mechanism for Background & Killed state on Android 10+.
        // Android pops the activity via fullScreenIntent even when the app is not running.
        showEndCallNotification(context, finalNumber, callType, fullScreenPendingIntent)

        // ── Step 2: Direct startActivity ────────────────────────────────────────
        // On Android 10+ (API 29+), startActivity() from a BroadcastReceiver is blocked
        // UNLESS one of these is true:
        //   a) The app is in the foreground
        //   b) The app has SYSTEM_ALERT_WINDOW (canDrawOverlays) permission  ← KEY FIX
        //   c) Android version < 10
        // Your app already declares SYSTEM_ALERT_WINDOW — so once user grants it,
        // startActivity() works from background on ALL Android versions including 13.
        val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true
        }

        if (canDrawOverlays || isAppInForeground(context)) {
            try {
                context.startActivity(intent)
                Log.d(TAG, "startActivity launched (canDrawOverlays=$canDrawOverlays)")
            } catch (e: Exception) {
                Log.e(TAG, "startActivity failed: ${e.message}")
            }
        } else {
            // Overlay permission not granted and app is in background.
            // The full-screen notification above will handle it — user taps to open.
            Log.w(TAG, "No overlay permission & app in background — relying on full-screen notification")
        }
    }

    /**
     * Posts a maximum-priority full-screen notification.
     * On Android 10+ this is the only reliable way to pop an Activity from background/killed.
     */
    private fun showEndCallNotification(
        context: Context,
        number: String,
        callType: String,
        pendingIntent: PendingIntent
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create / update channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RECEIVER_CHANNEL_ID,
                context.getString(R.string.call_ended),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                enableLights(true)
            }
            nm.createNotificationChannel(channel)
        }

        // Android 14+: warn if permission not granted (user may have denied it)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!nm.canUseFullScreenIntent()) {
                Log.w(TAG, "USE_FULL_SCREEN_INTENT not granted – activity will open via heads-up tap only")
            }
        }

        val contentText = if (callType == EndCallActivity.CALL_TYPE_MISSED)
            "Missed call from $number"
        else
            "Call with $number ended"

        val notification = NotificationCompat.Builder(context, RECEIVER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setDefaults(Notification.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, false)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFY_ID, notification)
    }

    /** Returns true if this app's process is currently in the foreground. */
    private fun isAppInForeground(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.runningAppProcesses?.any { proc ->
            proc.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
            proc.processName == context.packageName
        } == true
    }

    private fun getLastCallNumberFromLog(context: Context): String? {
        if (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_CALL_LOG
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return null

        return context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER),
            null, null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst())
                cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
            else null
        }
    }

    private fun getLastCallTypeFromLog(context: Context): Int {
        if (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_CALL_LOG
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return -1

        return context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.TYPE),
            null, null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst())
                cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE))
            else -1
        } ?: -1
    }
}
