package com.example.contactmanager.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.contactmanager.R
import com.example.contactmanager.activities.call.CallActivity
import com.example.contactmanager.receivers.CallActionReceiver

class CallNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CALL_CHANNEL_ID = "call_channel"
        const val MISSED_CALL_CHANNEL_ID = "missed_call_channel"
        const val CALL_NOTIFICATION_ID = 1001
        const val MISSED_CALL_NOTIFICATION_ID = 1002
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                "Ongoing/Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows notifications for active or incoming calls"
                setSound(null, null)
                enableVibration(false)
            }

            val missedCallChannel = NotificationChannel(
                MISSED_CALL_CHANNEL_ID,
                "Missed Calls",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows notifications for missed calls"
            }

            notificationManager.createNotificationChannel(callChannel)
            notificationManager.createNotificationChannel(missedCallChannel)
        }
    }

    @SuppressLint("RemoteViewLayout", "FullScreenIntentPolicy")
    fun setupNotification(lowPriority: Boolean = false): Notification {
        val call = NewCallManager.getPrimaryCall() ?: return buildEmptyNotification()

        val state = NewCallManager.getState()
        val number = call.details.handle?.schemeSpecificPart ?: "Unknown"
        val name = Common.getDisplayName(context, number, call.details.callerDisplayName)

        val isIncoming = state == Call.STATE_RINGING

        // Notification View (RemoteViews)
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_view)
        remoteViews.setTextViewText(R.id.pop_name, name)
        remoteViews.setTextViewText(R.id.pop_number, number)
        remoteViews.setImageViewBitmap(R.id.pop_image, Common.generateAvatar(name))

        // Accept Action
        val acceptIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = "ANSWER"
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context, 0, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.pop_accept, acceptPendingIntent)

        // Decline Action
        val declineIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = "DECLINE"
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context, 1, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.pop_decline, declinePendingIntent)

        // Visibility based on state
        if (isIncoming) {
            remoteViews.setViewVisibility(R.id.pop_accept, View.VISIBLE)
            remoteViews.setViewVisibility(R.id.pop_decline, View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.pop_accept, View.GONE)
            remoteViews.setViewVisibility(R.id.pop_decline, View.GONE)
        }

        // Fullscreen Intent
        val activityIntent = CallActivity.getStartIntent(context)
        val activityPendingIntent = PendingIntent.getActivity(
            context, 2, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_all_call)
            .setCustomContentView(remoteViews)
            .setCustomHeadsUpContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentTitle(name)
            .setContentText(number)
            .setPriority(if (lowPriority) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(activityPendingIntent)
            .setOnlyAlertOnce(true)

        if (isIncoming && !lowPriority) {
            builder.setFullScreenIntent(activityPendingIntent, true)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
        }


        val notification = builder.build()
        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
        return notification
    }

    private fun buildEmptyNotification(): Notification {
        return NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_all_call)
            .setContentTitle("Call Ended")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun cancelNotification() {
        notificationManager.cancel(CALL_NOTIFICATION_ID)
    }

    fun showMissedCallNotification(number: String, name: String?) {
        val newName = if (Common.getContactName(context,number).isEmpty()) name else  Common.getContactName(context, number)

        val activityIntent = Intent(context, CallActivity::class.java).apply {
            // Probably should go to Call Log or Home Activity, but keeping it simple
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val activityPendingIntent = PendingIntent.getActivity(
            context, 3, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, MISSED_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_miss_call)
            .setContentTitle("Missed Call")
            .setContentText("$newName: $number")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(activityPendingIntent)

        notificationManager.notify(MISSED_CALL_NOTIFICATION_ID + number.hashCode(), builder.build())
    }
}
