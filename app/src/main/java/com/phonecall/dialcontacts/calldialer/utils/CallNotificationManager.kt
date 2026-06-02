package com.phonecall.dialcontacts.calldialer.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.telecom.Call
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.call.CallActivity
import com.phonecall.dialcontacts.calldialer.receivers.CallActionReceiver

class CallNotificationManager(private val context: Context) {

    private data class CallDisplayInfo(
        val title: String,
        val subtitle: String,
        val showTitle: Boolean
    )

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CALL_CHANNEL_ID = "call_channel"
        const val MISSED_CALL_CHANNEL_ID = "missed_call_channel"
        const val END_CALL_CHANNEL_ID = "end_call_channel"
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

            val endCallChannel = NotificationChannel(
                END_CALL_CHANNEL_ID,
                "Call Ended",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows notifications when a call ends"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(callChannel)
            notificationManager.createNotificationChannel(missedCallChannel)
            notificationManager.createNotificationChannel(endCallChannel)
        }
    }

    @SuppressLint("RemoteViewLayout", "FullScreenIntentPolicy")
    fun setupNotification(lowPriority: Boolean = false, tag: String? = null): Notification {
        val call = NewCallManager.getPrimaryCall() ?: return buildEmptyNotification()

        val state = NewCallManager.getState()
        val displayInfo = getCallDisplayInfo(call, tag)
        val finalName = displayInfo.title
        val number = displayInfo.subtitle

        val isIncoming = state == Call.STATE_RINGING

        // Collapsed notification view (shown in notification shade)
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_view)
        if (displayInfo.showTitle) {
            remoteViews.setViewVisibility(R.id.pop_name, View.VISIBLE)
            remoteViews.setTextViewText(R.id.pop_name, displayInfo.title)
        } else {
            remoteViews.setViewVisibility(R.id.pop_name, View.GONE)
        }
        remoteViews.setTextViewText(R.id.pop_number, number)
        remoteViews.setImageViewBitmap(R.id.pop_image, Common.generateAvatar(finalName))

        // Heads-up view — separate instance to avoid shared-state issues on MIUI/POCO
        val headsUpViews = RemoteViews(context.packageName, R.layout.notification_view)
        if (displayInfo.showTitle) {
            headsUpViews.setViewVisibility(R.id.pop_name, View.VISIBLE)
            headsUpViews.setTextViewText(R.id.pop_name, displayInfo.title)
        } else {
            headsUpViews.setViewVisibility(R.id.pop_name, View.GONE)
        }
        headsUpViews.setTextViewText(R.id.pop_number, number)
        headsUpViews.setImageViewBitmap(R.id.pop_image, Common.generateAvatar(finalName))

        // Accept Action
        val acceptIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = "ANSWER"
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context, 0, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.pop_accept, acceptPendingIntent)
        headsUpViews.setOnClickPendingIntent(R.id.pop_accept, acceptPendingIntent)

        // Decline Action
        val declineIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = "DECLINE"
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context, 1, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.pop_decline, declinePendingIntent)
        headsUpViews.setOnClickPendingIntent(R.id.pop_decline, declinePendingIntent)

        // Show/hide accept+decline buttons based on call state
        if (isIncoming) {
            remoteViews.setViewVisibility(R.id.pop_accept, View.VISIBLE)
            remoteViews.setViewVisibility(R.id.pop_decline, View.VISIBLE)
            headsUpViews.setViewVisibility(R.id.pop_accept, View.VISIBLE)
            headsUpViews.setViewVisibility(R.id.pop_decline, View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.pop_accept, View.GONE)
            remoteViews.setViewVisibility(R.id.pop_decline, View.GONE)
            headsUpViews.setViewVisibility(R.id.pop_accept, View.GONE)
            headsUpViews.setViewVisibility(R.id.pop_decline, View.GONE)
        }

        // Fullscreen Intent (for lock-screen / incoming call)
        val activityIntent = CallActivity.getStartIntent(context)
        val activityPendingIntent = PendingIntent.getActivity(
            context, 2, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determine priority:
        //   isIncoming && NOT lowPriority  → HIGH  (heads-up + full-screen)
        //   everything else                → LOW   (silent ongoing)
        val priority = if (isIncoming && !lowPriority) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_LOW
        }

        val builder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_call)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.notification_call
                )
            )
            .setColor(ContextCompat.getColor(context, R.color.main_color))
            // Use separate RemoteViews instances — fixes MIUI/POCO clipping bug
            .setCustomContentView(remoteViews)
            .setCustomHeadsUpContentView(headsUpViews)
            // DO NOT use DecoratedCustomViewStyle — it wraps a second chrome layer
            // around custom views on MIUI/POCO causing the UI to be cut off.
            .setContentTitle(finalName)
            .setContentText(
                when {
                    isIncoming -> context.getString(R.string.incoming_call)
                    call.isConference() -> number
                    else -> context.getString(R.string.calling)
                }
            )
            .setPriority(priority)
            .setVisibility(
                if (isIncoming) NotificationCompat.VISIBILITY_PUBLIC
                else NotificationCompat.VISIBILITY_PRIVATE
            )
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(activityPendingIntent)
            .setOnlyAlertOnce(true)

        if (isIncoming && !lowPriority) {
            builder.setFullScreenIntent(activityPendingIntent, true)
        }

        val notification = builder.build()
        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
        return notification
    }

    private fun buildEmptyNotification(): Notification {
        return NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_call)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.notification_call
                )
            )
            .setColor(ContextCompat.getColor(context, R.color.main_color))
            .setContentTitle(context.getString(R.string.call_ended))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun cancelNotification() {
        notificationManager.cancel(CALL_NOTIFICATION_ID)
    }

    fun showMissedCallNotification(number: String, name: String?) {
        // Priority: contact name > tag (name param) > no label
        val fetchedContactName = Common.getContactName(context, number)
        val contactName = if (fetchedContactName == number) "" else fetchedContactName

        val displayName = contactName.takeIf { it.isNotEmpty() } ?: name?.takeIf { it.isNotEmpty() }

        val contentText = if (displayName != null) "$displayName: $number" else number

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
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.notification_call
                )
            )
            .setColor(ContextCompat.getColor(context, R.color.red))
            .setContentTitle(context.getString(R.string.missed_call))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(activityPendingIntent)

        notificationManager.notify(MISSED_CALL_NOTIFICATION_ID + number.hashCode(), builder.build())
    }

    private fun getCallDisplayInfo(call: Call, tag: String?): CallDisplayInfo {
        if (call.isConference()) {
            val participants = NewCallManager.getConferenceCalls().joinToString(", ") { conferenceCall ->
                val handleNumber = conferenceCall.details.handle?.schemeSpecificPart
                    ?: context.getString(R.string.unknown)
                getParticipantDisplayName(handleNumber, conferenceCall.details.callerDisplayName)
            }
            return CallDisplayInfo(
                title = context.getString(R.string.conference_call),
                subtitle = participants.ifEmpty {
                    context.getString(R.string.multiple_participants)
                },
                showTitle = true
            )
        }

        val number = call.details.handle?.schemeSpecificPart ?: context.getString(R.string.unknown)
        val contactName = Common.getContactName(context, number)
        val name = when {
            contactName != number -> contactName
            !tag.isNullOrBlank() -> tag
            !call.details.callerDisplayName.isNullOrBlank() &&
                call.details.callerDisplayName != number -> call.details.callerDisplayName
            else -> ""
        }

        return CallDisplayInfo(
            title = name.ifBlank { number },
            subtitle = number,
            showTitle = name.isNotBlank()
        )
    }

    private fun getParticipantDisplayName(number: String, callerDisplayName: String?): String {
        val contactName = Common.getContactName(context, number)
        if (contactName != number) {
            return contactName
        }
        if (!callerDisplayName.isNullOrBlank() && callerDisplayName != number) {
            return callerDisplayName
        }
        return number
    }
}
