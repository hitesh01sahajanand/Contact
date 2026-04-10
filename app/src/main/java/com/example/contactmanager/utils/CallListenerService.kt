package com.example.contactmanager.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.telecom.Call
import com.example.contactmanager.activities.call.CallActivity
import com.example.contactmanager.models.CallingModel

class CallListenerService(private var context: Context) {

    companion object {
        const val COLOR_ACTION_ACCEPT_CALL = "com.callerscreen.CallerId.action.ACCEPT_CALL"
        const val COLOR_ACTION_CALLBACK = "com.callerscreen.CallerId.action.CALLBACK"
        const val COLOR_ACTION_DECLINE_CALL = "com.callerscreen.CallerId.action.DECLINE_CALL"
        const val COLOR_ACTION_DELETE = "com.callerscreen.CallerId.action.DELETE"
        const val COLOR_ACTION_HANG_UP_CALL = "com.callerscreen.CallerId.action.HANG_UP_CALL"
        const val COLOR_ACTION_MESSAGE = "com.callerscreen.CallerId.action.MESSAGE"
        const val COLOR_ACTION_MISSED_CALL = "com.callerscreen.CallerId.action.MISSED_CALL"
        const val COLOR_REMINDER_ACTION_CALLBACK = "com.callerscreen.CallerId.action.CALLBACK_REMINDER"
        const val COLOR_REMINDER_ACTION_MESSAGE = "com.callerscreen.CallerId.action.MESSAGE_REMINDER"

        const val REMINDER_CALL_NOTIFY_ID = 4245
        private const val INCOMING_NOTIFY_ID = 4242
        private const val MISSED_CALL_NOTIFY_ID = 4244
        private const val OUTGOING_NOTIFY_ID = 4243

        /*fun getParentCallIntent(noAnimation: Boolean): Intent {
            val intent = Intent(MyAdAppController.conl_getMainApps(), CallActivity::class.java)
            if (noAnimation) intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            return intent
        }*/
    }

//    private val preference = Preference(context)

    var callModel: CallingModel? = null
//    var contactHistoryBean: ContactHistoryModel? = null

//    var timerHandler: Handler? = null
//    var timerHandler2: Handler? = null

//    private var remoteViews: RemoteViews? = null

    // ---------------- Incoming ----------------

    /*fun createIncomingNotification(model: NotificationViewModel) {
        callModel = model.callModel

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(INCOMING_NOTIFY_ID, IncomingCallNotViewer(context, model).build())

        checkAfterSometimeForNotification()
        checkAfterSometimeForMissedCallNotification()
    }*/

    // ---------------- Outgoing ----------------

    /*private fun createOutgoing(
        notificationModel: NotificationUtilsModel,
        callModel: CallingModel,
        isConference: Boolean
    ) {
        removeIncomingNotification()
        this.callModel = callModel

        val pendingIntent = pendingIntentParentCall(false, true, isConference)

        val channelId = context.getString(R.string.default_outgoing_notification_channel_id)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder =
            if (Build.VERSION.SDK_INT >= 26) {
                if (nm.getNotificationChannel(channelId) == null) {
                    val channel = NotificationChannel(
                        channelId,
                        "Active Calls",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    nm.createNotificationChannel(channel)
                }
                NotificationCompat.Builder(context, channelId)
            } else {
                NotificationCompat.Builder(context)
            }

        builder.setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.notification_call)
            .setContentTitle(notificationModel.titleName)
            .setContentText(notificationModel.desc)
            .setLargeIcon(notificationModel.bitmap)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)

        val hangIntent = Intent(context, NotificationDataService::class.java)
            .setAction(COLOR_ACTION_HANG_UP_CALL)

        val pi = PendingIntent.getService(
            context,
            0,
            hangIntent,
            if (Build.VERSION.SDK_INT >= 23)
                PendingIntent.FLAG_MUTABLE
            else PendingIntent.FLAG_IMMUTABLE
        )

        builder.addAction(0, "Hang Up", pi)

        nm.notify(OUTGOING_NOTIFY_ID, builder.build())

        checkAfterSometimeForNotification()
        checkAfterSometimeForMissedCallNotification()
    }*/

    // ---------------- Missed Call ----------------

    /*fun createMissedCall(contact: ContactHistoryModel) {
        contactHistoryBean = contact
        removeIncomingNotification()

        val count = (preference.getInteger("missed_count") ?: 0) + 1
        preference.setInteger("missed_count", count)

        val channelId = context.getString(R.string.default_missedcall_notification_channel_id)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder =
            if (Build.VERSION.SDK_INT >= 26) {
                if (nm.getNotificationChannel(channelId) == null) {
                    val channel = NotificationChannel(
                        channelId,
                        "Missed Calls",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                    nm.createNotificationChannel(channel)
                }
                NotificationCompat.Builder(context, channelId)
            } else {
                NotificationCompat.Builder(context)
            }

        val intent = Intent(context, MainActivity::class.java)
            .setAction(COLOR_ACTION_MISSED_CALL)
            .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)

        val pi = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        builder.setContentIntent(pi)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setAutoCancel(true)
            .setNumber(count)
            .setCategory(NotificationCompat.CATEGORY_CALL)

        if (count > 1) {
            builder.setContentTitle("Missed Calls")
            builder.setContentText("$count Missed calls")
        } else {
            builder.setContentTitle("Missed Call")
            builder.setContentText(contact.displayNameForNotification ?: contact.phoneNumber)
        }

        nm.notify(MISSED_CALL_NOTIFY_ID, builder.build())

        Handler(Looper.getMainLooper()).postDelayed({
            removeMissedCallNotification()
        }, 500)
    }*/

    // ---------------- Timer Logic ----------------

    /*fun checkAfterSometimeForNotification() {
        timerHandler = Handler(Looper.getMainLooper())
        timerHandler?.postDelayed({
            timerHandler = null

            val calls = (context as MyAdAppController).In_CallService.calls
            if (calls.isEmpty() || isCallEnded(calls[0])) {
                removeNotification()
            } else {
                checkAfterSometimeForNotification()
            }
        }, 3000)
    }*/

    /*fun checkAfterSometimeForMissedCallNotification() {
        timerHandler2 = Handler(Looper.getMainLooper())
        timerHandler2?.postDelayed({
            timerHandler2 = null

            val calls = (context as MyAdAppController).In_CallService.calls
            if (calls.isEmpty() || isCallEnded(calls[0])) {
                removeMissedCallNotification()
            } else {
                checkAfterSometimeForMissedCallNotification()
            }
        }, 500)
    }*/

    /*private fun isCallEnded(call: Call): Boolean {
        return call.state == Call.STATE_DISCONNECTED ||
                call.state == Call.STATE_DISCONNECTING
    }*/

    // ---------------- Pending Intent ----------------

    /*fun pendingIntentParentCall(
        noAnim: Boolean,
        fromNotification: Boolean,
        isConference: Boolean
    ): PendingIntent {

        val intent = getParentCallIntent(noAnim)

        if (fromNotification) {
            intent.putExtra("fromNotification", true)

            callModel?.let {
                try {
                    val number =
                        if (!it.call.details.hasProperty(1) || !isConference)
                            it.call.details.handle.schemeSpecificPart
                        else null

                    intent.putExtra("incomingNumber", number)

                } catch (e: Exception) {
                    intent.putExtra("incomingNumber", null as String?)
                }
            }
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= 23)
                PendingIntent.FLAG_MUTABLE
            else PendingIntent.FLAG_IMMUTABLE
        )
    }*/

    // ---------------- Remove Notifications ----------------

    /*fun removeIncomingNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(INCOMING_NOTIFY_ID)
        callModel = null
        remoteViews = null
    }*/

    /*fun removeNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(INCOMING_NOTIFY_ID)
        nm.cancel(OUTGOING_NOTIFY_ID)
        callModel = null
        remoteViews = null
    }*/

    /*fun removeMissedCallNotification() {
        try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecom.cancelMissedCallsNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }*/

    fun removeMissedNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(MISSED_CALL_NOTIFY_ID)
    }

    // ---------------- Utility ----------------

    fun getBaseTime(call: Call?): Long {
        return if (call?.details != null) {
            SystemClock.elapsedRealtime() -
                    (System.currentTimeMillis() - call.details.connectTimeMillis)
        } else 0L
    }

    /*fun updateNotification(
        notificationModel: NotificationUtilsModel,
        callModel: CallingModel?,
        isConference: Boolean
    ) {
        removeNotification()

        this.callModel = callModel ?: return

        val call = callModel.call
        if (call == null ||
            call.state == Call.STATE_DISCONNECTED ||
            call.state == Call.STATE_DISCONNECTING
        ) {
            removeNotification()
        } else {
            // createOutgoing(notificationModel, callModel, isConference)
        }
    }*/
}