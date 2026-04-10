package com.example.contactmanager.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.telecom.Call
import android.telecom.InCallService
import com.example.contactmanager.ApplicationClass
import com.example.contactmanager.R
import com.example.contactmanager.activities.call.CallActivity
import com.example.contactmanager.models.CallObjModel
import com.example.contactmanager.utils.CallListenerService
import com.example.contactmanager.utils.Constance
import com.jeremyliao.liveeventbus.LiveEventBus

class InCallMainService : InCallService() {

    var call: Call? = null
    var callService: InCallMainService? = null
    var colorCallNotificationListenerService: CallListenerService? = null
    private var incomingNumber: String? = null
//    private var preference: Preference? = null

    val callbackService = CallbackService(this)

    private var isLedOn = false

    companion object {
        private const val SYSTEM_CALL_CHANNEL_ID = "system_call_channel_silent_v2"
        private const val ONGOING_CALL_NOTIFICATION_ID = 1001
        private const val INCOMING_NOTIFY_ID = 4242
        private const val OUTGOING_NOTIFY_ID = 4243
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            try {
                if (message.what == 1010109) {
                    val intent = Intent(applicationContext, CallActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP

                    intent.putExtra("show_relaunch", false)
                    intent.putExtra("incomingNumber", incomingNumber)
                    intent.putExtra("isNew", true)

                    startActivity(intent)

                    /* if (message.arg1 == 1 && calls.size > 1) {
                         LiveEventBus.get(ConstantUtils.UPDATE_CALL_LIST)
                             .postDelay(call, 100L)
                     }*/
                }

                /*if (message.what == 269488144 && isXiaomiFamily()) {
                    colorCallNotificationListenerService?.createIncomingNotification(message.obj as NotificationViewModel)
                }*/

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    inner class CallbackService(val service: InCallMainService) : Call.Callback() {

        override fun onChildrenChanged(call: Call, list: MutableList<Call>) {
            LiveEventBus
                .get<Call>(Constance.UPDATE_CALL_TO_CONFERENCE)
                .post(call)
        }

        override fun onParentChanged(call: Call, parent: Call?) {
            parent?.let {
                LiveEventBus.get<Call>(Constance.UPDATE_CALL_TO_CONFERENCE).post(it)
            }
        }

        override fun onStateChanged(call: Call, state: Int) {
            LiveEventBus
                .get<CallObjModel>(Constance.UPDATE_CALL_STATE)
                .post(CallObjModel(call, state))

            (applicationContext as ApplicationClass).appCall = call
            stopLED()

            when (state) {
                Call.STATE_ACTIVE -> showOngoingCallNotification(call)

                Call.STATE_DISCONNECTED,
                Call.STATE_DISCONNECTING -> {
                    stopOngoingCallNotification()
                    cancelLegacyCallNotifications()

                    /*SimpleCallScreeningService.getCallerIdPopup()?.apply {
                        close()
                        SimpleCallScreeningService.setCallerIdPopup(null)
                    }*/
                }
            }
        }
    }

    private fun isXiaomiFamily(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        val brand = Build.BRAND?.lowercase() ?: ""

        return manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                manufacturer.contains("poco") ||
                brand.contains("xiaomi") ||
                brand.contains("redmi") ||
                brand.contains("poco")
    }

    private fun stopOngoingCallNotification() {
        try {
            stopForeground(true)
        } catch (_: Exception) {
        }
    }

    private fun cancelLegacyCallNotifications() {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.cancel(INCOMING_NOTIFY_ID)
            nm?.cancel(OUTGOING_NOTIFY_ID)
        } catch (_: Exception) {
        }
    }

    private fun showOngoingCallNotification(call: Call?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (call?.details?.handle == null) return

        val number = call.details.handle.schemeSpecificPart

        val intent = Intent(this, CallActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

        intent.putExtra("incomingNumber", number)

        val pendingIntent = PendingIntent.getActivity(
            this, 4, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val person = Person.Builder()
            .setName(number ?: "Call")
            .build()

        val hangupIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, CallActionService::class.java).setAction("HANGUP"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val notification = Notification.Builder(this, SYSTEM_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_all_call)
            .setStyle(Notification.CallStyle.forOngoingCall(person, hangupIntent))
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(ONGOING_CALL_NOTIFICATION_ID, notification)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        this.call = call
        (applicationContext as ApplicationClass).appCall = call

        call.registerCallback(callbackService)

        incomingNumber = call.details?.handle?.schemeSpecificPart

        if (call.state == Call.STATE_RINGING) {
            handler.sendMessage(Message().apply {
                what = 1010109
                arg1 = -1
            })
        }

        if (call.state == Call.STATE_ACTIVE) {
            showOngoingCallNotification(call)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callbackService)

        stopOngoingCallNotification()
        cancelLegacyCallNotifications()

        /*SimpleCallScreeningService.getCallerIdPopup()?.apply {
            close()
            SimpleCallScreeningService.setCallerIdPopup(null)
        }*/
    }

    override fun onCreate() {
        super.onCreate()
        callService = this
//        preference = Preference(this)

        (applicationContext as ApplicationClass).inCallService = this
        colorCallNotificationListenerService = CallListenerService(applicationContext)
    }

    fun starLED() {
        /*if (!isLedOn && PreferenceUtils.getInstance()
                .getBoolean(ConstantUtils.LED_FLASH)
        ) {
            FlashLightUtils.get(this).blink(800, -1)
            isLedOn = true
        }*/
    }

    fun stopLED() {
        /*if (isLedOn) {
            FlashLightUtils.get(this).stopBlinking()
            FlashLightUtils.get(this).blink(800, 1)
            isLedOn = false
        }*/
    }
}