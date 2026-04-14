package com.example.contactmanager.activities.call

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.example.contactmanager.ApplicationClass
import com.example.contactmanager.R
import com.example.contactmanager.databinding.ActivityCallBinding
import com.example.contactmanager.utils.NewCallManager
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.getStateCompat
import com.example.contactmanager.viewmodels.CallViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityCallBinding
    private val viewModel: CallViewModel by viewModels()

    val messages = listOf(
        "Can't talk right now",
        "Call you later",
        "In a meeting",
        "Busy, text me"
    )

    private var mProximityWakeLock: WakeLock? = null

    companion object {
        fun getStartIntent(context: Context, needSelectSIM: Boolean = false): Intent {
            val openAppIntent = Intent(context, CallActivity::class.java)
//            openAppIntent.putExtra(NEED_SELECT_SIM, needSelectSIM)
            //Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT --removed it, it can cause a full screen ringing instead of notifications
            openAppIntent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            return openAppIntent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call)
        initView()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    private fun initView() {

        binding.onClickHandler = this
        binding.inIncomingLayout.onClickHandler = this
        binding.inOutgoingCallLayout.onClickHandler = this

        makeFullScreenImmersive()

        val isFromNotification = intent.getBooleanExtra("fromNotification", false)
        val isNew = intent.getBooleanExtra("isNew", false)

        if (isFromNotification || isNew) {
//            setUI()
        }

//        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        NewCallManager.addListener(callListener)
    }

    override fun onStop() {
        super.onStop()
        NewCallManager.removeListener(callListener)
    }

    override fun onPause() {
        super.onPause()
        removeProximitySensor()
    }

    private val callListener = object : NewCallManager.CallManagerListener {
        override fun onStateChanged() {
            updateUI()
        }

        override fun onPrimaryCallChanged(call: Call) {
            updateUI()
        }

        override fun onAudioStateChanged() {}
        override fun onMuteChanged(isMuted: Boolean) {}
    }

    private fun updateUI() {
        val call = NewCallManager.getPrimaryCall() ?: return
        val state = call.getStateCompat()
        val number = call.details.handle?.schemeSpecificPart ?: "Unknown"
        val name = call.details.callerDisplayName ?: number
        when (state) {
            Call.STATE_RINGING -> {
//                showIncomingUI()
                binding.inIncomingLayout.tvNumberName.text = name
                binding.inIncomingLayout.tvCalling.text = "Incoming Call"

                binding.inIncomingLayout.root.isVisible = true
                binding.inOutgoingCallLayout.root.isVisible = false

                Toast.makeText(
                    this,
                    "Incoming Call ${call.details.callerDisplayName}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            Call.STATE_DIALING, Call.STATE_CONNECTING -> {
//                showOutgoingUI()

                binding.inOutgoingCallLayout.root.isVisible = true
                binding.inIncomingLayout.root.isVisible = false

                binding.inOutgoingCallLayout.tvNumberName.text = name
                binding.inOutgoingCallLayout.tvNumber.text = number
                Toast.makeText(
                    this,
                    "Outgoing ${call.details.callerDisplayName}",
                    Toast.LENGTH_SHORT
                ).show()

            }

            Call.STATE_ACTIVE -> {
//                showOngoingUI()
                Toast.makeText(
                    this,
                    "OnGoing Call ${call.details.callerDisplayName}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            Call.STATE_HOLDING -> {
//                showHoldUI()
                Toast.makeText(
                    this,
                    "ON Hold Call ${call.details.callerDisplayName}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            Call.STATE_DISCONNECTED -> {
                finish()
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {

            binding.inIncomingLayout.llRemindMe.id -> {

                /*  val number = (application as ApplicationClass).appCall
                      ?.details?.handle?.schemeSpecificPart

                  Toast.makeText(this, "Reminder set for $number", Toast.LENGTH_SHORT).show()

                  Handler(Looper.getMainLooper()).postDelayed({
                      Toast.makeText(this, "Call back $number", Toast.LENGTH_LONG).show()
                  }, 10 * 60 * 1000) // 10 min*/

            }

            binding.inIncomingLayout.llMessage.id -> {

                /*  val number = (application as ApplicationClass).appCall
                      ?.details?.handle?.schemeSpecificPart

                  val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                      data = "smsto:$number".toUri()
                      putExtra("sms_body", messages[0]) // default
                  }
                  startActivity(smsIntent)*/
            }

            binding.inIncomingLayout.llCallDecline.id -> {
                /*  val call = (application as ApplicationClass).appCall
                  call?.reject(false, null)
                  CallManager.updateCallList(emptyList())
                  finish()*/
                NewCallManager.reject()
                finish()
            }

            binding.inIncomingLayout.llCallAccept.id -> {
                /* val call = (application as ApplicationClass).appCall
                 call?.answer(call.details.videoState)*/
                NewCallManager.accept()
            }

            binding.inOutgoingCallLayout.llAddCall.id -> {

            }

            binding.inOutgoingCallLayout.llHold.id -> {

            }

            binding.inOutgoingCallLayout.llBluetooth.id -> {

            }

            binding.inOutgoingCallLayout.llSpeaker.id -> {

            }

            binding.inOutgoingCallLayout.llMute.id -> {

            }

            binding.inOutgoingCallLayout.ivRejectCall.id -> {
                /*val call = (application as ApplicationClass).appCall
                call?.disconnect()
                finish()*/
                NewCallManager.reject()
                finish()
            }

            /*binding.ll.id -> {
                CallManager.disconnect()
                finish()
            }*/
        }
    }

    private fun observeViewModel() {

        viewModel.callState.observe(this) { (call, state) ->

            val number = call.details.handle?.schemeSpecificPart ?: "Unknown"
            val name = call.details.callerDisplayName ?: number

            when (state) {

                Call.STATE_RINGING -> {
                    // ✅ Incoming call

                    binding.inIncomingLayout.tvNumberName.text = name
                    binding.inIncomingLayout.tvCalling.text = "Incoming Call"

                    binding.inIncomingLayout.root.isVisible = true
                    binding.inOutgoingCallLayout.root.isVisible = false

                    Toast.makeText(
                        this,
                        "Incoming Call ${call.details.callerDisplayName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                Call.STATE_CONNECTING,
                Call.STATE_DIALING -> {
                    // ✅ Outgoing call
                    binding.inOutgoingCallLayout.root.isVisible = true
                    binding.inIncomingLayout.root.isVisible = false

                    binding.inOutgoingCallLayout.tvNumberName.text = name
                    binding.inOutgoingCallLayout.tvNumber.text = number
                    Toast.makeText(
                        this,
                        "Outgoing ${call.details.callerDisplayName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                Call.STATE_ACTIVE -> {
                    // ✅ Call connected (incoming ya outgoing dono ho sakta hai)
                    startProximitySensor()
                    binding.inOutgoingCallLayout.root.isVisible = true
                    binding.inIncomingLayout.root.isVisible = false
                    binding.inOutgoingCallLayout.tvNumberName.text = name
                    binding.inOutgoingCallLayout.tvNumber.text = number

                    Toast.makeText(this, "Active Call", Toast.LENGTH_SHORT).show()
                }

                Call.STATE_DISCONNECTING -> {
                    removeProximitySensor()
                    finish()
                }

                Call.STATE_DISCONNECTED -> {
                    removeProximitySensor()
                    (application as ApplicationClass).appCall = null
                    finish()
                    Toast.makeText(this, "End Call", Toast.LENGTH_SHORT).show()
                }


            }

        }

        /*viewModel.callList.observe(this) { list ->
            if (list.isEmpty()) {
                removeProximitySensor()
                finish()
                return@observe
            }

            val call = list.first()
            val state = call.state

            val name = call.details.handle?.schemeSpecificPart ?: "Unknown"

            when (state) {
                Call.STATE_RINGING -> {
                    binding.inIncomingLayout.root.isVisible = true
                    binding.inOutgoingCallLayout.root.isVisible = false
                }

                Call.STATE_DIALING,
                Call.STATE_CONNECTING,
                Call.STATE_ACTIVE -> {
                    binding.inIncomingLayout.root.isVisible = false
                    binding.inOutgoingCallLayout.root.isVisible = true
                }
            }
        }*/
    }


    private fun toggleSpeaker() {
        val service = (application as ApplicationClass).inCallService ?: return
        val state = service.callAudioState

        if (state.route != CallAudioState.ROUTE_SPEAKER) {
            service.setAudioRoute(CallAudioState.ROUTE_SPEAKER)
        } else {
            service.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
        }
    }

    private fun toggleHold(call: Call) {
        if (call.state == Call.STATE_HOLDING) {
            call.unhold()
        } else {
            call.hold()
        }
    }

    private fun showDialer(call: Call) {
        /* val dialer = DialerNumberControl(this, binding.swapContactName)
         if (!dialer.isVisible) {
             dialer.showDialer(call)
         }*/
    }

    private fun handleVideoCall(call: Call) {

        val number = call.details.handle.schemeSpecificPart
        val DUO = "com.google.android.apps.tachyon"

        try {
            val intent = Intent().apply {
                action = "com.google.android.apps.tachyon.action.DIAL"
                setPackage(DUO)
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)

        } catch (e: Exception) {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAudioUI(state: CallAudioState) {

        // Speaker
        val isSpeaker = state.route == CallAudioState.ROUTE_SPEAKER
        /*binding.inOutgoingCallLayout.ivSpeaker.setBackgroundResource(
            if (isSpeaker) R.drawable.call_vector_bg else 0
        )

        // Mute
        binding.inOutgoingCallLayout.ivMute.setBackgroundResource(
            if (state.isMuted) R.drawable.call_vector_bg else 0
        )*/
    }

    private fun openQuickReplyDialog(call: Call) {

        /*val prefs = getSharedPreferences("QuickResponsePrefs", Context.MODE_PRIVATE)
        val list = prefs.getStringSet("quick_responses", emptySet())?.toList() ?: emptyList()

        val arr = list.toTypedArray()

        DialogViewManege.openMsgDialog(this, arr) { pos ->

            if (pos == -100) {
                declineCall(call)
                sendSMS(call, "")
            } else {
                declineCall(call, arr[pos])
            }
        }*/
    }

    private fun openReminderDialog(call: Call) {

        /*val times = longArrayOf(5 * 60 * 1000, 10 * 60 * 1000, 30 * 60 * 1000)

        DialogViewManege.openReminderDialog(
            this,
            arrayOf("In 5 minute", "In 10 minute", "In 30 minute")
        ) { pos ->

            val intent = Intent(this, ReminderBroadCastReceive::class.java)
            intent.putExtra("reminderNumber", call.details.handle.schemeSpecificPart)

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_MUTABLE
            )

            val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val delay = if (pos == -100) 60 * 60 * 1000 else times[pos]

            alarm.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delay,
                pendingIntent
            )

            declineCall(call)
        }*/
    }

    private fun sendSMS(call: Call, msg: String) {
        try {
            val number = call.details.handle.schemeSpecificPart

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$number")
                putExtra("sms_body", msg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun makeFullScreenImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            window.insetsController?.let { controller ->
                controller.hide(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                )

                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.let {
                it.hide(WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        val sdk = Build.VERSION.SDK_INT

        // Lock portrait (except API 26 due to known issues)
        if (sdk != Build.VERSION_CODES.O) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Allow drawing behind system bars
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        // Always keep screen ON (valid, not deprecated)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // ✅ Modern APIs
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)

        } else {
            // ✅ Fallback for old devices only
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                receiver,
                IntentFilter(Constance.CALL_DISCONNECTED),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(receiver, IntentFilter(Constance.CALL_DISCONNECTED))
        }

        val call = (application as ApplicationClass).appCall

        if (call == null) {
            finish()
        }*/
    }


    fun removeProximitySensor() {
        try {
            mProximityWakeLock?.let { wakeLock ->
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
            mProximityWakeLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startProximitySensor() {
        try {
            val powerManager = applicationContext.getSystemService(POWER_SERVICE) as? PowerManager

            if (mProximityWakeLock == null && powerManager != null) {
                mProximityWakeLock =
                    powerManager.newWakeLock(32, "color:Salut_ddd")
            }

            mProximityWakeLock?.let { wakeLock ->
                if (!wakeLock.isHeld) {
                    wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getNumberFromCall(call: Call?): String? {
        if (call == null || call.details == null) {
            return null
        }
        val handle = call.details.handle ?: return null
        return handle.schemeSpecificPart
    }
}