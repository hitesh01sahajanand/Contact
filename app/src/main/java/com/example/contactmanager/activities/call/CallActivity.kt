package com.example.contactmanager.activities.call

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.telecom.Call
import android.telecom.CallAudioState
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.example.contactmanager.R
import com.example.contactmanager.activities.home.HomeActivity
import com.example.contactmanager.databinding.ActivityCallBinding
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.NewCallManager
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.getStateCompat
import com.example.contactmanager.utils.isConference

class CallActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityCallBinding
    private var mProximityWakeLock: WakeLock? = null

    companion object {
        fun getStartIntent(context: Context): Intent {
            val openAppIntent = Intent(context, CallActivity::class.java)
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

    private fun initView() {
        binding.onClickHandler = this
        binding.inIncomingLayout.onClickHandler = this
        binding.inOutgoingCallLayout.onClickHandler = this

        makeFullScreenImmersive()

        updateUI()
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

        override fun onAudioStateChanged() {
            runOnUiThread {
                NewCallManager.inCallService?.callAudioState?.let {
                    updateAudioUI(it)
                }
            }
        }

        override fun onMuteChanged(isMuted: Boolean) {
            runOnUiThread {
                NewCallManager.inCallService?.callAudioState?.let {
                    updateAudioUI(it)
                }
            }
        }
    }

    private fun updateUI() {
        val call = NewCallManager.getPrimaryCall()
        if (call == null) {
            // Support explicit manual testing launch from Recent Fragment
            val isNew = intent.getBooleanExtra("isNew", false)
            if (isNew) {
                // Dummy UI rendering for testing since there's no live call
                binding.inIncomingLayout.root.isVisible = false
                binding.inOutgoingCallLayout.root.isVisible = true
                binding.inIncomingLayout.tvNumberName.text = "Test Caller"
                binding.inIncomingLayout.tvCalling.text = "Incoming Call"
                return
            }

            removeProximitySensor()
            finish()
            return
        }

        // Keep visual buttons synced
        updateHoldUI(call)
        NewCallManager.inCallService?.callAudioState?.let { updateAudioUI(it) }

        val phoneState = NewCallManager.getPhoneState()
        binding.inOutgoingCallLayout.llConference.isVisible = phoneState is NewCallManager.TwoCalls
        binding.inOutgoingCallLayout.llAddCall.isVisible = NewCallManager.canAddCall()
        binding.inOutgoingCallLayout.llAddCall.alpha = if (NewCallManager.canAddCall()) 1.0f else 0.5f

        // Let the system handle capability-based merging
        binding.inOutgoingCallLayout.llMerge.isEnabled = true
        binding.inOutgoingCallLayout.llMerge.alpha = 1.0f
        binding.inOutgoingCallLayout.llSwap.isEnabled = true
        binding.inOutgoingCallLayout.llSwap.alpha = 1.0f

        val state = call.getStateCompat()
        var number = call.details.handle?.schemeSpecificPart ?: "Unknown"
        var name = call.details.callerDisplayName ?: number

        if (call.isConference()) {
            name = "Conference Call"
            val participants =
                NewCallManager.getConferenceCalls().joinToString(", ") { conferenceCall ->
                    val handleNumber =
                        conferenceCall.details.handle?.schemeSpecificPart ?: "Unknown"
                    conferenceCall.details.callerDisplayName ?: handleNumber
                }
            number = participants.ifEmpty { "Multiple Participants" }
        }

        when (state) {
            Call.STATE_RINGING -> {
                binding.inIncomingLayout.tvNumberName.text = name
                binding.inIncomingLayout.tvCalling.text = getString(R.string.incoming_call)

                binding.inIncomingLayout.root.isVisible = true
                binding.inOutgoingCallLayout.root.isVisible = false
            }

            Call.STATE_DIALING, Call.STATE_CONNECTING -> {
                binding.inOutgoingCallLayout.root.isVisible = true
                binding.inIncomingLayout.root.isVisible = false

                binding.inOutgoingCallLayout.tvNumberName.text = name
                binding.inOutgoingCallLayout.tvNumber.text = number
                binding.inOutgoingCallLayout.chronometer.stop()
                binding.inOutgoingCallLayout.chronometer.text = getString(R.string.calling_)
            }

            Call.STATE_ACTIVE -> {
                startProximitySensor()
                binding.inOutgoingCallLayout.root.isVisible = true
                binding.inIncomingLayout.root.isVisible = false

                binding.inOutgoingCallLayout.tvNumberName.text = name
                binding.inOutgoingCallLayout.tvNumber.text = number

                val connectTimeMillis = call.details.connectTimeMillis
                if (connectTimeMillis > 0) {
                    val durationMillis = System.currentTimeMillis() - connectTimeMillis
                    binding.inOutgoingCallLayout.chronometer.base =
                        android.os.SystemClock.elapsedRealtime() - durationMillis
                } else {
                    binding.inOutgoingCallLayout.chronometer.base =
                        android.os.SystemClock.elapsedRealtime()
                }
                binding.inOutgoingCallLayout.chronometer.start()
            }

            Call.STATE_HOLDING -> {
                binding.inOutgoingCallLayout.chronometer.stop()
                binding.inOutgoingCallLayout.chronometer.text = getString(R.string.on_hold)

                binding.inOutgoingCallLayout.root.isVisible = true
                binding.inIncomingLayout.root.isVisible = false
                binding.inOutgoingCallLayout.tvNumberName.text = name
                binding.inOutgoingCallLayout.tvNumber.text = number
            }

            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                binding.inOutgoingCallLayout.chronometer.stop()
                removeProximitySensor()
                finish()
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.inIncomingLayout.llRemindMe.id -> {
                Common.showRemindMeDialog(this, onReminderSet = {
                    Toast.makeText(this, getString(R.string.remind_me), Toast.LENGTH_SHORT).show()
                })

            }

            binding.inIncomingLayout.llMessage.id -> {
                Common.showMessageDialog(this, onItemClick = { messages ->
                    sendSMSMessage(messages)
                    NewCallManager.reject()
                })
            }

            binding.inIncomingLayout.llCallDecline.id -> {
                NewCallManager.reject()
            }

            binding.inIncomingLayout.llCallAccept.id -> {
                NewCallManager.accept()
            }

            binding.inOutgoingCallLayout.llAddCall.id -> {
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra(Constance.IS_Dialer, true)
                startActivity(intent)
            }

            binding.inOutgoingCallLayout.llMerge.id -> {
                NewCallManager.merge()
            }

            binding.inOutgoingCallLayout.llSwap.id -> {
                NewCallManager.swap()
            }

            binding.inOutgoingCallLayout.llHold.id -> {
                NewCallManager.toggleHold()
            }

            binding.inOutgoingCallLayout.llBluetooth.id -> {
                val service = NewCallManager.inCallService ?: return
                val isBluetooth = service.callAudioState.route == CallAudioState.ROUTE_BLUETOOTH

                service.setAudioRoute(
                    if (isBluetooth)
                        CallAudioState.ROUTE_WIRED_OR_EARPIECE
                    else
                        CallAudioState.ROUTE_BLUETOOTH
                )
            }

            binding.inOutgoingCallLayout.llSpeaker.id -> {
                val service = NewCallManager.inCallService ?: return

                val isSpeaker = service.callAudioState.route == CallAudioState.ROUTE_SPEAKER

                service.setAudioRoute(
                    if (isSpeaker)
                        CallAudioState.ROUTE_WIRED_OR_EARPIECE   // 📞 Earpiece
                    else
                        CallAudioState.ROUTE_SPEAKER             // 📢 Loudspeaker
                )
            }

            binding.inOutgoingCallLayout.llMute.id -> {
                val service = NewCallManager.inCallService ?: return
                service.setMuted(!service.callAudioState.isMuted)
            }

            binding.inOutgoingCallLayout.ivRejectCall.id -> {
                NewCallManager.reject()
            }
        }
    }


    private fun sendSMSMessage(msg: String) {
        try {
            val call = NewCallManager.getPrimaryCall() ?: return
            val number = call.details.handle.schemeSpecificPart

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "smsto:$number".toUri()
                putExtra("sms_body", msg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateAudioUI(state: CallAudioState) {

        val iconActiveColor = ContextCompat.getColor(this, R.color.white)
        val iconInactiveColor = ContextCompat.getColor(this, R.color.black_color)

        val cardActiveColor = ContextCompat.getColor(this, R.color.black_color)
        val cardInactiveColor = ContextCompat.getColor(this, R.color.bg_color)

        val route = state.route

        val isSpeaker = route == CallAudioState.ROUTE_SPEAKER
        val isBluetooth = route == CallAudioState.ROUTE_BLUETOOTH
        val isEarpiece = route == CallAudioState.ROUTE_WIRED_OR_EARPIECE

        val isMuted = state.isMuted

        // 🔊 Speaker
        binding.inOutgoingCallLayout.ivSpeaker.setColorFilter(
            if (isSpeaker) iconActiveColor else iconInactiveColor
        )
        binding.inOutgoingCallLayout.cvSpeaker.setCardBackgroundColor(
            if (isSpeaker) cardActiveColor else cardInactiveColor
        )

        // 🎧 Bluetooth
        binding.inOutgoingCallLayout.ivBluetooth.setColorFilter(
            if (isBluetooth) iconActiveColor else iconInactiveColor
        )
        binding.inOutgoingCallLayout.cvBluetooth.setCardBackgroundColor(
            if (isBluetooth) cardActiveColor else cardInactiveColor
        )

        // 🎤 Mute
        binding.inOutgoingCallLayout.ivMute.setColorFilter(
            if (isMuted) iconActiveColor else iconInactiveColor
        )
        binding.inOutgoingCallLayout.cvMute.setCardBackgroundColor(
            if (isMuted) cardActiveColor else cardInactiveColor
        )
    }


    private fun updateHoldUI(call: Call) {
        val isOnHold = call.getStateCompat() == Call.STATE_HOLDING

        val iconActiveColor = ContextCompat.getColor(this, R.color.white)
        val iconInactiveColor = ContextCompat.getColor(this, R.color.black_color)

        val cardActiveColor = ContextCompat.getColor(this, R.color.black_color)
        val cardInactiveColor = ContextCompat.getColor(this, R.color.bg_color)

        binding.inOutgoingCallLayout.ivHold.setColorFilter(
            if (isOnHold) iconActiveColor else iconInactiveColor
        )
        binding.inOutgoingCallLayout.cvHold.setCardBackgroundColor(
            if (isOnHold) cardActiveColor else cardInactiveColor
        )
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
        val call = NewCallManager.getPrimaryCall()
        val isNew = intent.getBooleanExtra("isNew", false)
        if (call == null && !isNew) {
            finish()
        }
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
}