package com.phonecall.dialcontacts.calldialer.activities.call

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.telecom.Call
import android.telecom.CallAudioState
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.home.HomeActivity
import com.phonecall.dialcontacts.calldialer.adapters.ConferenceParticipantsAdapter
import com.phonecall.dialcontacts.calldialer.databinding.ActivityCallBinding
import com.phonecall.dialcontacts.calldialer.databinding.ConferenceManagerBottomSheetBinding
import com.phonecall.dialcontacts.calldialer.models.QuickResponseModel
import com.phonecall.dialcontacts.calldialer.repository.TagRepository
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.NewCallManager
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.utils.isConference
import com.phonecall.dialcontacts.calldialer.viewmodels.QuickResponseViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    // Local audio / button state — toggled immediately on click so UI is instant
    private var isMuted = false
    private var isSpeakerOn = false
    private var isOnHold = false
    private var isKeypadOpen = false

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
        // Do NOT call enableEdgeToEdge() here — it conflicts with fullscreen/immersive mode
        // and causes the status bar to remain visible with white icons.
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.inOutgoingCallLayout.nestedScrollview) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(
                view.paddingLeft,
                statusBarInsets.top,
                view.paddingRight,
                view.paddingBottom
            )

            insets
        }

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
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
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
                    applyButtonUI()
                }
            }
        }

        override fun onMuteChanged(muteState: Boolean) {
            runOnUiThread {
                NewCallManager.inCallService?.callAudioState?.let { state ->
                    // Sync local state from system truth
                    isMuted = state.isMuted
                    isSpeakerOn = state.route == CallAudioState.ROUTE_SPEAKER
                    applyButtonUI()
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
            val holdNumber =
                holdCall.details.handle?.schemeSpecificPart ?: getString(R.string.unknown)
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
                binding.inOutgoingCallLayout.llMerge.isVisible = false
                binding.inOutgoingCallLayout.llManage.isVisible = false
            }
        }

        binding.inOutgoingCallLayout.llAddCall.isEnabled = NewCallManager.canAddCall()
        binding.inOutgoingCallLayout.llAddCall.alpha =
            if (NewCallManager.canAddCall()) 1.0f else 0.5f

        val state = NewCallManager.getState()
        var number = call.details.handle?.schemeSpecificPart ?: getString(R.string.unknown)
        var name = getDisplayName(number, call.details.callerDisplayName)

        if (call.isConference()) {
            name = getString(R.string.conference_call)
            val participants =
                NewCallManager.getConferenceCalls().joinToString(", ") { conferenceCall ->
                    val handleNumber =
                        conferenceCall.details.handle?.schemeSpecificPart
                            ?: getString(R.string.unknown)
                    getDisplayName(
                        handleNumber,
                        conferenceCall.details.callerDisplayName
                    )
                }
            number = participants.ifEmpty { getString(R.string.multiple_participants) }
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
                // Resume from hold — clear hold state
                isOnHold = false
                applyButtonUI()
            }

            Call.STATE_HOLDING -> {
                binding.inOutgoingCallLayout.chronometer.stop()
                binding.inOutgoingCallLayout.chronometer.text = getString(R.string.on_hold)

                binding.inOutgoingCallLayout.root.isVisible = true
                binding.inIncomingLayout.root.isVisible = false
                binding.inOutgoingCallLayout.tvNumberName.text = name
                binding.inOutgoingCallLayout.tvNumber.text = number
                isOnHold = true
                applyButtonUI()
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
        if (!isValidClick()) return
        when (view.id) {
            binding.inIncomingLayout.llRemindMe.id -> {
                val call = NewCallManager.getPrimaryCall()
                val number =
                    call?.details?.handle?.schemeSpecificPart ?: getString(R.string.unknown)
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
                applyButtonUI()
            }

            binding.inOutgoingCallLayout.llKeypad.id -> {
                isKeypadOpen = !isKeypadOpen
                if (isKeypadOpen) {
                    binding.inOutgoingCallLayout.llAllButtons.isVisible = false
                    binding.inOutgoingCallLayout.ivRejectCall.isVisible = false
                    binding.inOutgoingCallLayout.keyboard.isVisible = true
                } else {
                    binding.inOutgoingCallLayout.keyboard.isVisible = false
                    binding.inOutgoingCallLayout.ivRejectCall.isVisible = true
                    binding.inOutgoingCallLayout.llAllButtons.isVisible = true
                }
                applyButtonUI()
            }

            binding.inOutgoingCallLayout.llClose.id -> {
                isKeypadOpen = false
                binding.inOutgoingCallLayout.keyboard.isVisible = false
                binding.inOutgoingCallLayout.ivRejectCall.isVisible = true
                binding.inOutgoingCallLayout.llAllButtons.isVisible = true
                applyButtonUI()
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
                applyButtonUI()
            }

            binding.inOutgoingCallLayout.llMute.id -> {
                val service = NewCallManager.inCallService ?: return
                // Toggle local state immediately — don't read callAudioState (it's still old)
                isMuted = !isMuted
                service.setMuted(isMuted)
                applyButtonUI()
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

        val isDialPadSound =
            com.phonecall.dialcontacts.calldialer.utils.SharedPreferenceManager.getBoolean(
                this,
                Constance.DIAL_PAD_SOUND,
                false
            )
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
     * Apply toggle UI for ALL buttons in ll_all_buttons.
     * Active state  → grey card background + white icon tint.
     * Inactive state → normal bg_color card + black icon tint.
     *
     * State variables:
     *   [isMuted]        — microphone muted
     *   [isSpeakerOn]    — loudspeaker active
     *   [isOnHold]       — call on hold
     *   [isKeypadOpen]   — in-call keypad visible
     *   [isMoreExpanded] — row-1 buttons expanded
     */
    private fun applyButtonUI() {
        val iconActive = ContextCompat.getColor(this, R.color.white)
        val iconInactive = ContextCompat.getColor(this, R.color.black_color)
        val cardActive = ContextCompat.getColor(this, R.color.grey_color)
        val cardInactive = ContextCompat.getColor(this, R.color.bg_color)

        fun applyButton(
            card: com.google.android.material.card.MaterialCardView,
            icon: android.widget.ImageView,
            isActive: Boolean
        ) {
            card.setCardBackgroundColor(if (isActive) cardActive else cardInactive)
            icon.setColorFilter(if (isActive) iconActive else iconInactive)
        }

        // 🎤 Mute
        applyButton(
            binding.inOutgoingCallLayout.cvMute,
            binding.inOutgoingCallLayout.ivMute,
            isMuted
        )

        // 🔊 Speaker
        applyButton(
            binding.inOutgoingCallLayout.cvSpeaker,
            binding.inOutgoingCallLayout.ivSpeaker,
            isSpeakerOn
        )

        // ⏸ Hold
        applyButton(
            binding.inOutgoingCallLayout.cvHold,
            binding.inOutgoingCallLayout.ivHold,
            isOnHold
        )

        // ⌨ Keypad — active while keypad is open
        binding.inOutgoingCallLayout.cvKeypad.setCardBackgroundColor(
            if (isKeypadOpen) cardActive else cardInactive
        )
        // Keypad icon does not have a dedicated ImageView id, colour the card only


        applyButton(
            binding.inOutgoingCallLayout.cvMore,
            binding.inOutgoingCallLayout.ivMore,
            isMoreExpanded
        )
        // ⋯ More — active while row-1 is expanded

        updateProximitySensor()
    }

    fun makeFullScreenImmersive() {
        val sdk = Build.VERSION.SDK_INT

        // Hide ONLY the navigation bar — keep status bar visible so time & details show
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Make navigation bar transparent so no gap appears
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Lock portrait (except API 26 due to known issues)
        if (sdk != Build.VERSION_CODES.O) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Keep screen ON during call
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Show over lock screen
        if (sdk >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
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
        makeFullScreenImmersive()
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
                } else {
                    Log.w(
                        "CallActivity",
                        "Proximity screen off wake lock NOT supported on this device"
                    )
                }
            }

            mProximityWakeLock?.let { wakeLock ->
                if (!wakeLock.isHeld) {
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