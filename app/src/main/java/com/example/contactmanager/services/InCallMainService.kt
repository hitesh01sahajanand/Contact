package com.example.contactmanager.services

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import android.provider.Settings
import com.example.contactmanager.ApplicationClass
import com.example.contactmanager.activities.call.CallActivity
import com.example.contactmanager.utils.CallNotificationManager
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.FlashLightUtils
import com.example.contactmanager.utils.NewCallManager
import com.example.contactmanager.utils.SharedPreferenceManager
import com.example.contactmanager.utils.isOutgoing

import com.example.contactmanager.repository.BlockRepository
import com.example.contactmanager.utils.RingtonePlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InCallMainService : InCallService(), NewCallManager.CallManagerListener {
    
    @Inject
    lateinit var blockRepository: BlockRepository
    
    private lateinit var callNotificationManager: CallNotificationManager
    private lateinit var ringtonePlayer: RingtonePlayer

    override fun onCreate() {
        super.onCreate()
        callNotificationManager = CallNotificationManager(this)
        ringtonePlayer = RingtonePlayer(this)
        NewCallManager.inCallService = this
        NewCallManager.addListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtonePlayer.stopRinging()
        NewCallManager.removeListener(this)
    }

    override fun onStateChanged() {
        refreshNotification()
    }

    override fun onAudioStateChanged() {}
    override fun onPrimaryCallChanged(call: Call) {
        (applicationContext as ApplicationClass).appCall = call
        refreshNotification()
    }
    override fun onMuteChanged(isMuted: Boolean) {
        refreshNotification()
    }

    private fun refreshNotification() {
        val call = NewCallManager.getPrimaryCall()
        if (call == null) {
            stopForeground(true)
            callNotificationManager.cancelNotification()
            return
        }

        val state = NewCallManager.getState()
        val isIncomingRinging = state == Call.STATE_RINGING
        val isVisible = NewCallManager.isCallActivityVisible
        
        // High priority only for incoming ringing calls that are not visible
        val lowPriority = !(isIncomingRinging && !isVisible)
        
        val notification = callNotificationManager.setupNotification(lowPriority)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    CallNotificationManager.CALL_NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            } else {
                startForeground(CallNotificationManager.CALL_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val callListener = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            // Stop ringing as soon as state leaves RINGING
            if (state != Call.STATE_RINGING) {
                ringtonePlayer.stopRinging()
                FlashLightUtils.getInstance(this@InCallMainService).stopBlinking()
            }
            if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                stopForeground(true)
                callNotificationManager.cancelNotification()
            } else {
                refreshNotification()
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        val number = call.details?.handle?.schemeSpecificPart ?: ""
        val isIncoming = call.state == Call.STATE_RINGING
        
        Log.d("InCallMainService", "onCallAdded: number=$number, isIncoming=$isIncoming")

        CoroutineScope(Dispatchers.Main).launch {
            if (isIncoming && number.isNotEmpty()) {
                val blocked = blockRepository.isBlocked(number)
                Log.d("InCallMainService", "Checking block for $number: $blocked")
                if (blocked) {
                    Log.d("InCallMainService", "Disconnecting blocked call from $number")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        call.disconnect()
                    } else {
                        call.reject(false, null)
                    }
                    return@launch
                }
            }

            // Proceed with normal logic only if NOT blocked
            (applicationContext as ApplicationClass).appCall = call
            NewCallManager.inCallService = this@InCallMainService
            NewCallManager.onCallAdded(call)
            call.registerCallback(callListener)

            val isOutgoing = call.isOutgoing()
            val state = call.state
            val isIncomingRinging = state == Call.STATE_RINGING

            // Start ringtone & vibration for incoming ringing calls
            if (isIncomingRinging) {
                ringtonePlayer.startRinging()
            }

            if (isIncomingRinging && SharedPreferenceManager.getBoolean(this@InCallMainService, Constance.CALL_FLASH, false)) {
                FlashLightUtils.getInstance(this@InCallMainService).startBlinking()
            }

            // High priority for incoming call, low for outgoing/ongoing
            val lowPriority = !isIncomingRinging
            val notification = callNotificationManager.setupNotification(lowPriority)
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        CallNotificationManager.CALL_NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                    )
                } else {
                    startForeground(CallNotificationManager.CALL_NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Try to start activity
            if (isOutgoing || isIncomingRinging) {
                try {
                    val intent = CallActivity.getStartIntent(this@InCallMainService)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)

                    // Show Popup if enabled
                    if (isIncomingRinging && SharedPreferenceManager.getBoolean(this@InCallMainService, Constance.CONFIRM_DIALOG)) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || Settings.canDrawOverlays(this@InCallMainService)) {
                            val contact = Common.getContactByNumber(this@InCallMainService, number)
                            val name = contact?.displayName ?: number
                            val imageUrl = contact?.userThumbnail ?: ""
                            val time = Common.formatTime(System.currentTimeMillis())

                            Common.showDialerPopUp(this@InCallMainService, name, number, time, imageUrl) {
                                // Callback
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        ringtonePlayer.stopRinging()
        FlashLightUtils.getInstance(this).stopBlinking()
        call.unregisterCallback(callListener)

        // Check for missed call
        val disconnectCauseCode = call.details?.disconnectCause?.code
        if (call.state == Call.STATE_RINGING || disconnectCauseCode == android.telecom.DisconnectCause.MISSED) {
            val number = call.details?.handle?.schemeSpecificPart ?: "Unknown"
            val name = call.details?.callerDisplayName

            callNotificationManager.showMissedCallNotification(number, name)
        }

        val wasPrimaryCall = call == NewCallManager.getPrimaryCall()
        if (wasPrimaryCall) {
            (applicationContext as ApplicationClass).appCall = null
        }
        NewCallManager.onCallRemoved(call)
        
        if (NewCallManager.getPhoneState() == NewCallManager.NoCall) {
            stopForeground(true)
            callNotificationManager.cancelNotification()
        } else {
            refreshNotification()
            if (wasPrimaryCall) {
                try {
                    val intent = CallActivity.getStartIntent(this)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /*var call: Call? = null
    var callService: InCallMainService? = null
//    var colorCallNotificationListenerService: CallListenerService? = null
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

                    if (message.arg1 == 1 && calls.size > 1) {
                        CallManager.updateCallList(calls)

                    }
                }

                if (message.what == 269488144 && isXiaomiFamily()) {
//                    colorCallNotificationListenerService?.createIncomingNotification(message.obj as NotificationViewModel)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    inner class CallbackService(val service: InCallMainService) : Call.Callback() {

        override fun onChildrenChanged(call: Call, list: MutableList<Call>) {
            CallManager.updateCallList(service.calls)
        }

        override fun onParentChanged(call: Call, parent: Call?) {
            parent?.let {
                CallManager.updateCallList(service.calls)
            }
        }

        override fun onStateChanged(call: Call, state: Int) {

            val app = applicationContext as ApplicationClass

            app.appCall = call

            CallManager.updateCall(call, state)
            CallManager.updateCallList(service.calls)

            when (state) {

                Call.STATE_RINGING -> {
                    // incoming
                }

                Call.STATE_DIALING,
                Call.STATE_CONNECTING -> {
                    // outgoing
                }

                Call.STATE_ACTIVE -> {
                    // active
                }

                // 🔥 FIX HERE
                Call.STATE_DISCONNECTING -> {
                    // 🔥 immediately close UI
                    CallManager.updateCallList(emptyList())
                }

                Call.STATE_DISCONNECTED -> {
                    app.appCall = null
                    CallManager.updateCallList(emptyList())
                }
            }

            stopLED()
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
        Log.e("TAG", "onCallAdded: $calls")


        this.call = call

        val app = applicationContext as ApplicationClass
        app.appCall = call

        call.registerCallback(callbackService)

        val state = call.state

        // ✅ IMPORTANT: update LiveData
        CallManager.updateCall(call, state)
        CallManager.updateCallList(calls)

        if (state == Call.STATE_RINGING) {
            sendCallActivityMessage(-1)
            return
        }

        if (state == Call.STATE_DIALING || state == Call.STATE_CONNECTING) {
            sendCallActivityMessage(1)
        }
    }

    private fun sendCallActivityMessage(arg: Int) {
        handler.sendMessage(Message().apply {
            what = 1010109
            arg1 = arg
        })
    }

    private fun createIncomingNotification(callModel: CallingModel) {

        val notificationModel = NotificationViewModel().apply {
            this.callModel = callModel

            val number = callModel.call?.details?.handle?.schemeSpecificPart

            phoneNumberOfCall = number
//            nameFromCall = PhoneBookUtils.searchDisplayName(this@InCallMainService, number)
//            imageOfUserCall = Utility.getImageOfUserCall(callModel, this@InCallMainService)
        }

        val message = Message().apply {
            obj = notificationModel
            what = 269488144
        }

        handler.sendMessage(message)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callbackService)

        val app = applicationContext as ApplicationClass

        if (app.appCall == call) {
            app.appCall = null
        }

        if (app.newCall == call) {
            app.newCall = null
        }


        CallManager.updateCallList(emptyList())

        stopOngoingCallNotification()
        cancelLegacyCallNotifications()

        *//*SimpleCallScreeningService.getCallerIdPopup()?.apply {
            close()
            SimpleCallScreeningService.setCallerIdPopup(null)
        }*//*
    }

    override fun onCreate() {
        super.onCreate()
        callService = this
//        preference = Preference(this)

        (applicationContext as ApplicationClass).inCallService = this
//        colorCallNotificationListenerService = CallListenerService(applicationContext)
    }

    fun starLED() {
        *//*if (!isLedOn && PreferenceUtils.getInstance()
                .getBoolean(ConstantUtils.LED_FLASH)
        ) {
            FlashLightUtils.get(this).blink(800, -1)
            isLedOn = true
        }*//*
    }

    fun stopLED() {
        *//*if (isLedOn) {
            FlashLightUtils.get(this).stopBlinking()
            FlashLightUtils.get(this).blink(800, 1)
            isLedOn = false
        }*//*
    }*/
}