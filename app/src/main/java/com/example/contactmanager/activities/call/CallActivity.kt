package com.example.contactmanager.activities.call

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.telecom.Call
import android.telecom.CallAudioState
import android.util.Log
import android.view.KeyEvent
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
import com.example.contactmanager.adapters.ConferenceParticipantsAdapter
import com.example.contactmanager.databinding.ActivityCallBinding
import com.example.contactmanager.databinding.ConferenceManagerBottomSheetBinding
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.NewCallManager
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.isConference
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.media.ToneGenerator
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.contactmanager.models.QuickResponseModel
import com.example.contactmanager.viewmodels.QuickResponseViewModel
import com.example.contactmanager.repository.TagRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class CallActivity : AppCompatActivity(), OnClickHandler {
    @Inject
    lateinit var tagRepository: TagRepository
    private lateinit var binding: ActivityCallBinding
    private var mProximityWakeLock: WakeLock? = null
    private var isMoreExpanded = false
    private val quickResponseViewModel: QuickResponseViewModel by viewModels()
    private var quickMessages = emptyList<QuickResponseModel>()
    private var toneGenerator: ToneGenerator? = null

    // Local audio state — toggled immediately on click so UI is instant
    private var isMuted = false
    private var isSpeakerOn = false

    private val tagMap = mutableMapOf<String, String?>()

    private val keyMap = mapOf(
        R.id.linear1 to '1',
        R.id.linear2 to '2',
        R.id.linear3 to '3',
        R.id.linear4 to '4',
        R.id.linear5 to '5',
        R.id.linear6 to '6',
        R.id.linear7 to '7',
        R.id.linear8 to '8',
        R.id.linear9 to '9',
        R.id.linear10 to '*',
        R.id.linear11 to '0',
        R.id.linear12 to '#'
    )

    private val toneMap = mapOf(
        R.id.linear1 to ToneGenerator.TONE_DTMF_1,
        R.id.linear2 to ToneGenerator.TONE_DTMF_2,
        R.id.linear3 to ToneGenerator.TONE_DTMF_3,
        R.id.linear4 to ToneGenerator.TONE_DTMF_4,
        R.id.linear5 to ToneGenerator.TONE_DTMF_5,
        R.id.linear6 to ToneGenerator.TONE_DTMF_6,
        R.id.linear7 to ToneGenerator.TONE_DTMF_7,
        R.id.linear8 to ToneGenerator.TONE_DTMF_8,
        R.id.linear9 to ToneGenerator.TONE_DTMF_9,
        R.id.linear10 to ToneGenerator.TONE_DTMF_S,
        R.id.linear11 to ToneGenerator.TONE_DTMF_0,
        R.id.linear12 to ToneGenerator.TONE_DTMF_P
    )

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

        lifecycleScope.launch {
            quickResponseViewModel.initializeDefaultMessages()
            quickResponseViewModel.messages.collectLatest {
                quickMessages = it
            }
        }

        initView()
    }

    private fun initView() {
        binding.onClickHandler = this
        binding.inIncomingLayout.onClickHandler = this
        binding.inOutgoingCallLayout.onClickHandler = this

        makeFullScreenImmersive()

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 80)
        } catch (e: Exception) {
            Log.e("CallActivity", "Exception while creating ToneGenerator: $e")
        }

        binding.inOutgoingCallLayout.edtDisplayNumber.apply {
            showSoftInputOnFocus = false
            isFocusable = true
            isFocusableInTouchMode = true
        }

        setupDialPad()

        updateUI()
    }

    override fun onStart() {
        super.onStart()
        NewCallManager.addListener(callListener)
        NewCallManager.isCallActivityVisible = true
        NewCallManager.notifyListeners()
    }

    override fun onStop() {
        super.onStop()
        NewCallManager.removeListener(callListener)
        NewCallManager.isCallActivityVisible = false
        NewCallManager.notifyListeners()
    }

    override fun onPause() {
        super.onPause()
        removeProximitySensor()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeProximitySensor()
        toneGenerator?.release()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val state = NewCallManager.getState()

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (state == Call.STATE_RINGING) {
                        // Increase ringer volume
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_RING,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI
                        )
                    } else {
                        // Increase in-call voice volume
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_VOICE_CALL,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                    return true
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (state == Call.STATE_RINGING) {
                        // Decrease ringer volume (also mutes/silences ringtone)
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_RING,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI
                        )
                    } else {
                        // Decrease in-call voice volume
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_VOICE_CALL,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
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
                NewCallManager.inCallService?.callAudioState?.let { state ->
                    // Sync local state from system truth
                    isSpeakerOn = state.route == CallAudioState.ROUTE_SPEAKER
                    isMuted = state.isMuted
                    applyAudioUI()
                }
            }
        }

        override fun onMuteChanged(muteState: Boolean) {
            runOnUiThread {
                NewCallManager.inCallService?.callAudioState?.let { state ->
                    // Sync local state from system truth
                    isMuted = state.isMuted
                    isSpeakerOn = state.route == CallAudioState.ROUTE_SPEAKER
                    applyAudioUI()
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
//        updateHoldUI(call)
//        NewCallManager.inCallService?.callAudioState?.let { updateAudioUI(it) }

        val phoneState = NewCallManager.getPhoneState()
        val isConference = call.isConference()

        // Toggle Row 1 Visibility
        binding.inOutgoingCallLayout.llRow1.isVisible = isMoreExpanded

        // Hold Banner logic
        if (phoneState is NewCallManager.TwoCalls) {
            binding.inOutgoingCallLayout.llHoldNumber.isVisible = true
            val holdCall = phoneState.onHold
            val holdNumber = holdCall.details.handle?.schemeSpecificPart ?: "Unknown"
            val holdName = getDisplayName(holdNumber, holdCall.details.callerDisplayName)
            binding.inOutgoingCallLayout.tvHoldNumber.text =
                "$holdName - ${getString(R.string.hold)}"
        } else {
            binding.inOutgoingCallLayout.llHoldNumber.isVisible = false
        }

        // Dynamic Button Visibility based on State
        when {
            isConference -> {
                binding.inOutgoingCallLayout.llHold.isVisible = true
                binding.inOutgoingCallLayout.llVideoCall.isVisible = true
                binding.inOutgoingCallLayout.llAddCall.isVisible = true
                binding.inOutgoingCallLayout.llSwap.isVisible = false
                binding.inOutgoingCallLayout.llMerge.isVisible = false
                binding.inOutgoingCallLayout.llManage.isVisible = true
            }

            phoneState is NewCallManager.TwoCalls -> {
                binding.inOutgoingCallLayout.llHold.isVisible = false
                binding.inOutgoingCallLayout.llVideoCall.isVisible = true
                binding.inOutgoingCallLayout.llAddCall.isVisible = true
                binding.inOutgoingCallLayout.llSwap.isVisible = true
                binding.inOutgoingCallLayout.llMerge.isVisible = true
                binding.inOutgoingCallLayout.llManage.isVisible = false
            }

            else -> {
                binding.inOutgoingCallLayout.llHold.isVisible = true
                binding.inOutgoingCallLayout.llVideoCall.isVisible = true
                binding.inOutgoingCallLayout.llAddCall.isVisible = true
                binding.inOutgoingCallLayout.llSwap.visibility = View.INVISIBLE
//                binding.inOutgoingCallLayout.llSwap.isVisible = false
                binding.inOutgoingCallLayout.llMerge.isVisible = false
                binding.inOutgoingCallLayout.llManage.isVisible = false
            }
        }

        binding.inOutgoingCallLayout.llAddCall.isEnabled = NewCallManager.canAddCall()
        binding.inOutgoingCallLayout.llAddCall.alpha =
            if (NewCallManager.canAddCall()) 1.0f else 0.5f

        val state = NewCallManager.getState()
        var number = call.details.handle?.schemeSpecificPart ?: "Unknown"
        var name = getDisplayName(number, call.details.callerDisplayName)

        if (call.isConference()) {
            name = "Conference Call"
            val participants =
                NewCallManager.getConferenceCalls().joinToString(", ") { conferenceCall ->
                    val handleNumber =
                        conferenceCall.details.handle?.schemeSpecificPart ?: "Unknown"
                    getDisplayName(
                        handleNumber,
                        conferenceCall.details.callerDisplayName
                    )
                }
            number = participants.ifEmpty { "Multiple Participants" }
        }

        val accountHandle = call.details.accountHandle
        val subId = accountHandle?.id?.toIntOrNull() ?: -1
        val simLabel = Common.getSimLabel(this, subId)

        binding.inIncomingLayout.tvIncomingSimNumber.text = simLabel
        binding.inIncomingLayout.tvIncomingSimNumber.isVisible = simLabel.isNotEmpty()

        binding.inOutgoingCallLayout.tvOutgoingSimNumber.text = simLabel
        binding.inOutgoingCallLayout.tvOutgoingSimNumber.isVisible = simLabel.isNotEmpty()

        when (state) {
            Call.STATE_RINGING -> {
                Log.e("TAG", "updateUI: gggg $number $name")
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

            Call.STATE_DISCONNECTING -> {
                binding.inOutgoingCallLayout.chronometer.stop()
            }

            Call.STATE_DISCONNECTED -> {
                binding.inOutgoingCallLayout.chronometer.stop()
                if (NewCallManager.getPhoneState() is NewCallManager.NoCall) {
                    finish()
                }
            }
        }
        updateProximitySensor()
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.inIncomingLayout.llRemindMe.id -> {
                val call = NewCallManager.getPrimaryCall()
                val number = call?.details?.handle?.schemeSpecificPart ?: "Unknown"
                val name = getDisplayName(number, call?.details?.callerDisplayName)

                Common.showRemindMeDialog(this, name, number, onReminderSet = {
                    NewCallManager.reject()
                })
            }

            binding.inIncomingLayout.llMessage.id -> {
                Common.showQuickMessageDialog(this, quickMessages, onItemClick = { messages ->
                    if (messages.isNotBlank()) {
                        sendSMSMessage(messages)
                    }
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
                intent.putExtra(Constance.IS_DIALER, true)
                startActivity(intent)
            }

            binding.inOutgoingCallLayout.llMerge.id -> {
                NewCallManager.merge()
            }

            binding.inOutgoingCallLayout.llSwap.id -> {
                NewCallManager.swap()
            }

            binding.inOutgoingCallLayout.llManage.id -> {
                showConferenceManager()
            }

            binding.inOutgoingCallLayout.llHold.id -> {
                NewCallManager.toggleHold()
            }

            binding.inOutgoingCallLayout.llVideoCall.id -> {
                onVideoCallClicked()
            }

            binding.inOutgoingCallLayout.llMore.id -> {
                isMoreExpanded = !isMoreExpanded
                updateUI()
            }

            binding.inOutgoingCallLayout.llKeypad.id -> {
                if (binding.inOutgoingCallLayout.llAllButtons.isVisible) {
                    binding.inOutgoingCallLayout.llAllButtons.isVisible = false
                    binding.inOutgoingCallLayout.ivRejectCall.isVisible = false
                    binding.inOutgoingCallLayout.keyboard.isVisible = true
                } else {
                    binding.inOutgoingCallLayout.keyboard.isVisible = false
                    binding.inOutgoingCallLayout.ivRejectCall.isVisible = true
                    binding.inOutgoingCallLayout.llAllButtons.isVisible = true
                }
            }

            binding.inOutgoingCallLayout.llClose.id -> {
                if (binding.inOutgoingCallLayout.llAllButtons.isVisible) {
                    binding.inOutgoingCallLayout.llAllButtons.isVisible = false
                    binding.inOutgoingCallLayout.ivRejectCall.isVisible = false
                    binding.inOutgoingCallLayout.keyboard.isVisible = true
                } else {
                    binding.inOutgoingCallLayout.keyboard.isVisible = false
                    binding.inOutgoingCallLayout.ivRejectCall.isVisible = true
                    binding.inOutgoingCallLayout.llAllButtons.isVisible = true
                }
            }


            binding.inOutgoingCallLayout.llSpeaker.id -> {
                val service = NewCallManager.inCallService ?: return
                // Toggle local state immediately — don't read callAudioState (it's still old)
                isSpeakerOn = !isSpeakerOn
                service.setAudioRoute(
                    if (isSpeakerOn)
                        CallAudioState.ROUTE_SPEAKER             // 📢 Loudspeaker
                    else
                        CallAudioState.ROUTE_WIRED_OR_EARPIECE   // 📞 Earpiece
                )
                applyAudioUI()
            }

            binding.inOutgoingCallLayout.llMute.id -> {
                val service = NewCallManager.inCallService ?: return
                // Toggle local state immediately — don't read callAudioState (it's still old)
                isMuted = !isMuted
                service.setMuted(isMuted)
                applyAudioUI()
            }

            binding.inOutgoingCallLayout.ivRejectCall.id -> {
                NewCallManager.reject()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDialPad() {
        val buttons = listOf(
            binding.inOutgoingCallLayout.linear1,
            binding.inOutgoingCallLayout.linear2,
            binding.inOutgoingCallLayout.linear3,
            binding.inOutgoingCallLayout.linear4,
            binding.inOutgoingCallLayout.linear5,
            binding.inOutgoingCallLayout.linear6,
            binding.inOutgoingCallLayout.linear7,
            binding.inOutgoingCallLayout.linear8,
            binding.inOutgoingCallLayout.linear9,
            binding.inOutgoingCallLayout.linear10,
            binding.inOutgoingCallLayout.linear11,
            binding.inOutgoingCallLayout.linear12
        )

        buttons.forEach { view ->
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        handleDialPadTouchDown(v.id)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handleDialPadTouchUp(v.id)
                        v.performClick()
                    }
                }
                false
            }
        }
    }

    private fun handleDialPadTouchDown(viewId: Int) {
        val charValue = keyMap[viewId] ?: return
        val call = NewCallManager.getPrimaryCall()
        
        call?.playDtmfTone(charValue)

        val isDialPadSound = com.example.contactmanager.utils.SharedPreferenceManager.getBoolean(this, Constance.DIAL_PAD_SOUND, false)
        if (isDialPadSound) {
            toneMap[viewId]?.let { tone ->
                toneGenerator?.startTone(tone, 150)
            }
        }

        binding.inOutgoingCallLayout.edtDisplayNumber.append(charValue.toString())
    }

    private fun handleDialPadTouchUp(viewId: Int) {
        val call = NewCallManager.getPrimaryCall()
        call?.stopDtmfTone()
    }

    fun onVideoCallClicked() {
        val call = NewCallManager.getPrimaryCall()
        val currentCall = call ?: return

        if (currentCall.isConference()) return

        val number = currentCall.details.handle.schemeSpecificPart

        Common.showVideoAppChooser(this, number) {
            disconnectAndCall { }
        }
    }

    private fun disconnectAndCall(action: () -> Unit) {
        val call = NewCallManager.getPrimaryCall()
        call?.disconnect()
        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            action()
        }
    }


    private fun sendSMSMessage(msg: String) {
        try {
            val call = NewCallManager.getPrimaryCall() ?: return
            val number = call.details.handle?.schemeSpecificPart ?: return

            // Open the SMS app immediately so the user can see the message.
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "smsto:$number".toUri()
                    putExtra("sms_body", msg)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Reject the call AFTER the SMS app has started and the transition is complete.
            // This prevents race conditions in the TelecomManager on certain devices where 
            // the rejection signal is dropped if the activity is finishing/transitioning simultaneously.
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                NewCallManager.reject()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Apply mute/speaker UI using local state variables [isMuted] and [isSpeakerOn].
     * These are toggled immediately on click so there's no async delay on the first tap.
     * System callbacks (onAudioStateChanged / onMuteChanged) sync the local vars back.
     */
    private fun applyAudioUI() {
        val iconActiveColor  = ContextCompat.getColor(this, R.color.white)
        val iconInactiveColor = ContextCompat.getColor(this, R.color.black_color)
        val cardActiveColor  = ContextCompat.getColor(this, R.color.grey_color)
        val cardInactiveColor = ContextCompat.getColor(this, R.color.bg_color)

        // 🔊 Speaker — gray card + white icon when ON
        binding.inOutgoingCallLayout.ivSpeaker.setColorFilter(
            if (isSpeakerOn) iconActiveColor else iconInactiveColor
        )
        binding.inOutgoingCallLayout.cvSpeaker.setCardBackgroundColor(
            if (isSpeakerOn) cardActiveColor else cardInactiveColor
        )

        // 🎤 Mute — gray card + white icon when muted
        binding.inOutgoingCallLayout.ivMute.setColorFilter(
            if (isMuted) iconActiveColor else iconInactiveColor
        )
        binding.inOutgoingCallLayout.cvMute.setCardBackgroundColor(
            if (isMuted) cardActiveColor else cardInactiveColor
        )
        updateProximitySensor()
    }


    private fun updateHoldUI(call: Call) {
        val isOnHold = NewCallManager.getState() == Call.STATE_HOLDING

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
        updateProximitySensor()
    }


    private fun getDisplayName(number: String, callerDisplayName: String?): String {
        val contactName = Common.getContactName(this, number)
        if (contactName != number) {
            return contactName
        }

        val tag = tagMap[number]
        if (!tag.isNullOrBlank()) {
            return tag
        }

        if (!tagMap.containsKey(number) && number != "Unknown") {
            tagMap[number] = null // Mark as fetching
            lifecycleScope.launch {
                val fetchedTag = tagRepository.getTag(number)
                tagMap[number] = fetchedTag
                if (!fetchedTag.isNullOrBlank()) {
                    updateUI()
                }
            }
        }

        if (!callerDisplayName.isNullOrBlank() && callerDisplayName != number) {
            return callerDisplayName
        }
        return number
    }

    private fun updateProximitySensor() {
        val state = NewCallManager.getState()
        val audioRoute =
            NewCallManager.inCallService?.callAudioState?.route ?: CallAudioState.ROUTE_EARPIECE

        // Check for both ROUTE_EARPIECE and the legacy/combined ROUTE_WIRED_OR_EARPIECE
        val isEarpiece = audioRoute == CallAudioState.ROUTE_EARPIECE ||
                audioRoute == CallAudioState.ROUTE_WIRED_OR_EARPIECE

        // However, if it's explicitly SPEKAER or BLUETOOTH, we definitely don't want proximity
        val isSpeaker = audioRoute == CallAudioState.ROUTE_SPEAKER
        val isBluetooth = audioRoute == CallAudioState.ROUTE_BLUETOOTH

        val isVideo = NewCallManager.getPrimaryCall()?.details?.videoState?.let {
            it != android.telecom.VideoProfile.STATE_AUDIO_ONLY
        } ?: false

        Log.d(
            "CallActivity",
            "updateProximitySensor: state=$state, route=$audioRoute, isEarpiece=$isEarpiece, isSpeaker=$isSpeaker, isBluetooth=$isBluetooth, isVideo=$isVideo"
        )

        val shouldActivate =
            (state == Call.STATE_ACTIVE || state == Call.STATE_DIALING || state == Call.STATE_CONNECTING)
                    && isEarpiece && !isSpeaker && !isBluetooth && !isVideo

        if (shouldActivate) {
            startProximitySensor()
        } else {
            removeProximitySensor()
        }
    }

    fun removeProximitySensor() {
        try {
            mProximityWakeLock?.let { wakeLock ->
                if (wakeLock.isHeld) {
                    Log.d("CallActivity", "releasing proximity wake lock")
                    wakeLock.release()
                }
            }
            mProximityWakeLock = null
            // Re-enable screen on if needed
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            Log.e("CallActivity", "Error releasing proximity wake lock", e)
        }
    }

    fun startProximitySensor() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
            if (powerManager == null) {
                Log.e("CallActivity", "PowerManager is null")
                return
            }

            if (mProximityWakeLock == null) {
                val isSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)
                } else {
                    true // Assume supported for legacy
                }

                if (isSupported) {
                    mProximityWakeLock = powerManager.newWakeLock(
                        PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                        "ContactManager:ProximityWakeLock"
                    )
                    Log.d("CallActivity", "Created proximity wake lock")
                } else {
                    Log.w(
                        "CallActivity",
                        "Proximity screen off wake lock NOT supported on this device"
                    )
                }
            }

            mProximityWakeLock?.let { wakeLock ->
                if (!wakeLock.isHeld) {
                    Log.d("CallActivity", "acquiring proximity wake lock")
                    wakeLock.acquire(30 * 60 * 1000L /* 30 minutes */)
                    // Some devices require clearing this flag for proximity screen off to work
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        } catch (e: Exception) {
            Log.e("CallActivity", "Error acquiring proximity wake lock", e)
        }
    }

    private fun showConferenceManager() {
        val participants = NewCallManager.getConferenceCalls()
        if (participants.isEmpty()) return

        val bottomSheet = BottomSheetDialog(this)
        val dialogBinding = ConferenceManagerBottomSheetBinding.inflate(
            layoutInflater
        )
        bottomSheet.setContentView(dialogBinding.root)

        val adapter = ConferenceParticipantsAdapter(participants) { participant ->
            participant.disconnect()
            bottomSheet.dismiss()
        }
        dialogBinding.rvParticipants.adapter = adapter
        bottomSheet.show()
    }
}