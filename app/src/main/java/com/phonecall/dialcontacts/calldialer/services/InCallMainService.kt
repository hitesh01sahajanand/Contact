package com.phonecall.dialcontacts.calldialer.services

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telecom.Call
import android.telecom.InCallService
import androidx.core.app.ServiceCompat
import com.phonecall.dialcontacts.calldialer.Advertisement.MyApplication
import com.phonecall.dialcontacts.calldialer.activities.call.CallActivity
import com.phonecall.dialcontacts.calldialer.repository.BlockRepository
import com.phonecall.dialcontacts.calldialer.repository.TagRepository
import com.phonecall.dialcontacts.calldialer.utils.CallNotificationManager
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.FlashLightUtils
import com.phonecall.dialcontacts.calldialer.utils.NewCallManager
import com.phonecall.dialcontacts.calldialer.utils.RingtonePlayer
import com.phonecall.dialcontacts.calldialer.utils.SharedPreferenceManager
import com.phonecall.dialcontacts.calldialer.utils.isOutgoing
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class InCallMainService : InCallService(), NewCallManager.CallManagerListener {

    @Inject
    lateinit var blockRepository: BlockRepository

    @Inject
    lateinit var tagRepository: TagRepository

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
//        (applicationContext as MyApplication).appCall = call
        refreshNotification()
    }

    override fun onMuteChanged(isMuted: Boolean) {
        refreshNotification()
    }

    private fun refreshNotification() {
        CoroutineScope(Dispatchers.IO).launch {
            val call = NewCallManager.getPrimaryCall()
            if (call == null) {
                withContext(Dispatchers.Main) {
                    ServiceCompat.stopForeground(
                        this@InCallMainService,
                        ServiceCompat.STOP_FOREGROUND_REMOVE
                    )
                    callNotificationManager.cancelNotification()
                }
                return@launch
            }

            val state = NewCallManager.getState()
            val isIncomingRinging = state == Call.STATE_RINGING
            val isVisible = NewCallManager.isCallActivityVisible

            // High priority only for incoming ringing calls that are not visible
            val lowPriority = !(isIncomingRinging && !isVisible)

            val number = call.details?.handle?.schemeSpecificPart ?: "Unknown"
            val contactName = Common.getContactName(this@InCallMainService, number)
            val tag = if (contactName == number) tagRepository.getTag(number) else null

            withContext(Dispatchers.Main) {
                val notification = callNotificationManager.setupNotification(lowPriority, tag)
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
        }
    }

    // Track call connect time so we can compute duration in EndCallActivity
    private var callConnectTimeMillis: Long = 0L

    private val callListener = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            // Stop ringing as soon as state leaves RINGING
            if (state != Call.STATE_RINGING) {
                ringtonePlayer.stopRinging()
                FlashLightUtils.getInstance(this@InCallMainService).stopBlinking()
            }
            // Record the moment the call becomes active
            if (state == Call.STATE_ACTIVE && callConnectTimeMillis == 0L) {
                callConnectTimeMillis = System.currentTimeMillis()
            }
            if (state == Call.STATE_DISCONNECTED) {
                ServiceCompat.stopForeground(
                    this@InCallMainService,
                    ServiceCompat.STOP_FOREGROUND_REMOVE
                )
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

        CoroutineScope(Dispatchers.Main).launch {
            if (isIncoming && number.isNotEmpty()) {
                val blocked = blockRepository.isBlocked(number)
                if (blocked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        call.disconnect()
                    } else {
                        call.reject(false, null)
                    }
                    return@launch
                }
            }

            // Proceed with normal logic only if NOT blocked
//            (applicationContext as MyApplication).appCall = call
            NewCallManager.inCallService = this@InCallMainService
            NewCallManager.onCallAdded(call)
            call.registerCallback(callListener)

            val isOutgoing = call.isOutgoing()
            val state = call.state
            val isIncomingRinging = state == Call.STATE_RINGING

            // Start ringtone & vibration for incoming ringing calls
            if (isIncomingRinging) {
                ringtonePlayer.startRinging(number)
            }

            if (isIncomingRinging && SharedPreferenceManager.getBoolean(
                    this@InCallMainService,
                    Constance.CALL_FLASH,
                    false
                )
            ) {
                FlashLightUtils.getInstance(this@InCallMainService).startBlinking()
            }

            // High priority for incoming call, low for outgoing/ongoing
            val lowPriority = !isIncomingRinging

            val contactName = Common.getContactName(this@InCallMainService, number)
            val tag = if (contactName == number) {
                withContext(Dispatchers.IO) { tagRepository.getTag(number) }
            } else null

            val notification = callNotificationManager.setupNotification(lowPriority, tag)

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
                    if (isIncomingRinging && SharedPreferenceManager.getBoolean(
                            this@InCallMainService,
                            Constance.CONFIRM_DIALOG
                        )
                    ) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || Settings.canDrawOverlays(
                                this@InCallMainService
                            )
                        ) {
                            val contact = Common.getContactByNumber(this@InCallMainService, number)
                            val name = contact?.displayName ?: number
                            val imageUrl = contact?.userThumbnail ?: ""
                            val time = Common.formatTime(System.currentTimeMillis())

                            Common.showDialerPopUp(
                                this@InCallMainService,
                                name,
                                number,
                                time,
                                imageUrl
                            ) {
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
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        ringtonePlayer.stopRinging()
        FlashLightUtils.getInstance(this).stopBlinking()
        call.unregisterCallback(callListener)

        val disconnectCauseCode = call.details?.disconnectCause?.code

        val isMissed = call.state == Call.STATE_RINGING
                || disconnectCauseCode == android.telecom.DisconnectCause.MISSED


        // Show missed-call notification
        if (isMissed && number.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val contactName = Common.getContactName(this@InCallMainService, number)
                val tag = if (contactName == number) tagRepository.getTag(number) else null
                withContext(Dispatchers.Main) {
                    callNotificationManager.showMissedCallNotification(number, tag)
                }
            }
        }

        val wasPrimaryCall = call == NewCallManager.getPrimaryCall()
        if (wasPrimaryCall) {
//            (applicationContext as MyApplication).appCall = null
        }
        NewCallManager.onCallRemoved(call)

        if (NewCallManager.getPhoneState() == NewCallManager.NoCall) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
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
}