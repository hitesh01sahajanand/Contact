package com.phonecall.dialcontacts.calldialer.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.splash.SplashActivity
import com.phonecall.dialcontacts.calldialer.utils.DailyNotificationUtils

class DailyNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Check if overlay permission is already granted
        if (Settings.canDrawOverlays(context)) {
            // Already have permission, reschedule but skip notification
            DailyNotificationUtils.scheduleDailyNotification(context)
            return
        }

        // Get Firebase Config states and Days Since Install
        val showFlag = ADSMainClass.getOverlayPermissionNotificationShow()
        val targetDay = ADSMainClass.getOverlayPermissionNotificationDaysShowCount()
        val daysSinceInstall = ADSMainClass.getDaysSinceInstall()

        // Check conditions: must be enabled AND days since install must be >= target day from Firebase
        if (showFlag && daysSinceInstall > 0 && daysSinceInstall >= targetDay.toLong()) {
            showNotification(context)
        }

        // Reschedule for the next day
        DailyNotificationUtils.scheduleDailyNotification(context)
    }

    private fun showNotification(context: Context) {
        val channelId = "daily_reminder_channel"
        val notificationId = 1001

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily engagement notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // intent to open SplashActivity
        val splashIntent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_overlay_notification", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            splashIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Allow Display Over Other Apps!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}
