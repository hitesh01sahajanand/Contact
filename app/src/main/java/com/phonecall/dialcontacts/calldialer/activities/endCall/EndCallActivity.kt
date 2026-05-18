package com.phonecall.dialcontacts.calldialer.activities.endCall

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.newContact.NewContactActivity
import com.phonecall.dialcontacts.calldialer.adapters.CallEndTabAdapter
import com.phonecall.dialcontacts.calldialer.databinding.ActivityEndCallBinding
import com.phonecall.dialcontacts.calldialer.repository.TagRepository
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.utils.PermissionManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.material.tabs.TabLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.phonecall.dialcontacts.calldialer.utils.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class EndCallActivity : AppCompatActivity(), OnClickHandler {

    @Inject
    lateinit var tagRepository: TagRepository
    lateinit var binding: ActivityEndCallBinding
    var mobileNumber: String? = null
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var callType: String = CALL_TYPE_ENDED
    private var isUserAskPermission = false
    private var adView: AdView? = null

    companion object {
        const val EXTRA_MOBILE_NUMBER = "mobile_number"
        const val EXTRA_START_TIME = "StartTime"
        const val EXTRA_END_TIME = "EndTime"
        const val EXTRA_CALL_TYPE = "CallType"
        const val CALL_TYPE_ENDED = "ended"
        const val CALL_TYPE_MISSED = "missed"
        const val CALL_TYPE_REJECTED = "rejected"
        const val CALL_TYPE_INCOMING = "incoming"
        const val CALL_TYPE_OUTGOING = "outgoing"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Show over lockscreen - calling these before super.onCreate can be more reliable on some OEMs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        ThemeManager.applyAppTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_end_call)
        binding.onClickHandler = this
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initData(intent)
        setupTabs()
        initView()
        setupAds()
        setupAnalytics()
        checkOverlayPermission()
        setupBackPress()

        // Cancel any pending end-call notification
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(1001)
    }

    private fun initData(intent: Intent) {
        mobileNumber = intent.getStringExtra(EXTRA_MOBILE_NUMBER)
        startTime = intent.getLongExtra(EXTRA_START_TIME, 0)
        endTime = intent.getLongExtra(EXTRA_END_TIME, 0)
        callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: CALL_TYPE_ENDED
    }

    private fun setupTabs() {
        val icons = arrayOf(
            R.drawable.icon_img_call_first_icon,
            R.drawable.icon_img_call_second_icon,
            R.drawable.icon_img_call_four_icon,
            R.drawable.icon_img_call_third_icon
        )

        binding.wsfwTabslayout.apply {
            removeAllTabs()
            icons.forEach {
                addTab(newTab().setIcon(it))
            }
            tabGravity = TabLayout.GRAVITY_FILL
        }

        binding.wsfwViewpager.adapter =
            CallEndTabAdapter(supportFragmentManager, binding.wsfwTabslayout.tabCount)
        binding.wsfwViewpager.addOnPageChangeListener(
            TabLayout.TabLayoutOnPageChangeListener(
                binding.wsfwTabslayout
            )
        )

        binding.wsfwTabslayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.wsfwViewpager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun initView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        // Set Contact Details — async: contact name → tag → number → "Unknown"
        val number = mobileNumber
        if (number.isNullOrEmpty()) {
            binding.tvTitle.text = getString(R.string.unknown)
            binding.ivContactPhoto.visibility = View.GONE
            binding.tvFirstName.visibility = View.VISIBLE
            binding.tvFirstName.text = "?"
            binding.llAddContact.visibility = View.GONE
        } else {
            val contact = Common.getContactByNumber(this, number)
            if (contact != null) {
                // Step 1 — system contact found
                binding.llAddContact.visibility = View.GONE
                val displayName = contact.displayName?.takeIf { it.isNotBlank() } ?: number
                binding.tvTitle.text = displayName
                if (!contact.userThumbnail.isNullOrEmpty()) {
                    binding.ivContactPhoto.visibility = View.VISIBLE
//                    binding.tvFirstName.visibility = View.GONE
                    binding.ivUser.visibility = View.GONE
                    Glide.with(this).load(contact.userThumbnail)
                        .apply(RequestOptions.bitmapTransform(CircleCrop()))
                        .error(R.drawable.ic_contact_profile).into(binding.ivContactPhoto)
                } else {
                    binding.ivContactPhoto.visibility = View.GONE
                    binding.tvFirstName.visibility = View.VISIBLE
                    val initials = displayName.trim().split(" ").filter { it.isNotEmpty() }.take(2)
                        .joinToString("") { it.first().uppercase() }
                    binding.tvFirstName.text = initials
                }
            } else {
                // Step 2 — no system contact; check Room DB for a tag
                binding.llAddContact.visibility = View.VISIBLE
                // Show number immediately as a placeholder while we query
                binding.tvTitle.text = number
                binding.ivContactPhoto.visibility = View.GONE
                binding.tvFirstName.text = number.trim().take(1)

                lifecycleScope.launch {
                    val tag = withContext(Dispatchers.IO) { tagRepository.getTag(number) }
                    if (tag.isNullOrBlank()) {
                        binding.tvFirstName.visibility = View.GONE
                        binding.ivUser.visibility = View.VISIBLE
                    } else {
                        binding.ivUser.visibility = View.GONE
                        binding.tvFirstName.visibility = View.VISIBLE
                    }
                    val resolvedName = when {
                        !tag.isNullOrBlank() -> tag          // Step 2 — tag found
                        number.isNotBlank() -> number       // Step 3 — raw number
                        else -> getString(R.string.unknown) // Step 4 — Unknown
                    }
                    binding.tvTitle.text = resolvedName
                    binding.tvFirstName.text = resolvedName.trim().take(1).uppercase()

                }
            }
        }

        // Set Time
        binding.tvTime.text =
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Calendar.getInstance().time)

        // Set Status Label based on call type
        binding.tvCallType.text = when (callType) {
            CALL_TYPE_INCOMING -> getString(R.string.incoming_call)
            CALL_TYPE_OUTGOING -> getString(R.string.outgoing_calls)
            CALL_TYPE_MISSED -> getString(R.string.missed_call)
            CALL_TYPE_REJECTED -> getString(R.string.call_declined)
            else -> ""
        }


        val durationMs = endTime - startTime
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        binding.tvDuration.text = if (hours > 0) {
            String.format(
                Locale.getDefault(),
                getString(R.string.duration_02d_02d_02d),
                hours,
                minutes,
                seconds
            )
        } else {
            String.format(
                Locale.getDefault(), getString(R.string.duration_02d_02d), minutes, seconds
            )
        }
    }

    private fun setupAds() {
        binding.linearBannerShimmer.visibility = View.VISIBLE
        adView = AdView(this)
//        ADSBanner.CallEndBannerRandomId()
        adView?.adUnitId = ADSMainClass.getStringValue(ADSMainClass.CALL_END_ADAPTIVE_BANNER)
        val adSize = getAdaptiveAdSize()
        adView?.setAdSize(adSize)
        binding.adContainer.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView?.loadAd(adRequest)

        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        adView?.onPaidEventListener = OnPaidEventListener { adValue ->
            val revenue = adValue.valueMicros / 1_000_000.0
            val currency = adValue.currencyCode
            val adRevenueParams = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_PLATFORM, "Google Ad Manager")
                putString(FirebaseAnalytics.Param.CURRENCY, currency)
                putDouble(FirebaseAnalytics.Param.VALUE, revenue)
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, adRevenueParams)
        }

        adView?.adListener = object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                binding.linearBannerShimmer.visibility = View.GONE
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                binding.linearBannerShimmer.visibility = View.GONE
            }
        }
    }

    private fun getAdaptiveAdSize(): AdSize {
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val density = outMetrics.density
        val adWidth = (outMetrics.widthPixels / density).toInt()
        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(this, adWidth)
    }

    private fun setupAnalytics() {
        FirebaseApp.initializeApp(this)
        val countCallEnd = ADSMainClass.getCallEndShowEvent() + 1
        ADSMainClass.setCallEndShowEvent(countCallEnd)

        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()

        when (countCallEnd) {
            1 -> if (!ADSMainClass.getCallEndShow1()) {
                bundle.putBoolean("OneTimeCallEndShow", true)
                firebaseAnalytics.logEvent("OneTimeCallEndShow", bundle)
                ADSMainClass.setCallEndShow1(true)
            }

            5 -> if (!ADSMainClass.getCallEndShow5()) {
                bundle.putBoolean("FiveTimeCallEndShow", true)
                firebaseAnalytics.logEvent("FiveTimeCallEndShow", bundle)
                ADSMainClass.setCallEndShow5(true)
            }

            10 -> if (!ADSMainClass.getCallEndShow10()) {
                bundle.putBoolean("TenTimeCallEndShow", true)
                firebaseAnalytics.logEvent("TenTimeCallEndShow", bundle)
                ADSMainClass.setCallEndShow10(true)
            }

            50 -> if (!ADSMainClass.getCallEndShow50()) {
                bundle.putBoolean("FiftyTimeCallEndShow", true)
                firebaseAnalytics.logEvent("FiftyTimeCallEndShow", bundle)
                ADSMainClass.setCallEndShow50(true)
            }

            100 -> if (!ADSMainClass.getCallEndShow100()) {
                bundle.putBoolean("HundredTimeCallEndShow", true)
                firebaseAnalytics.logEvent("HundredTimeCallEndShow", bundle)
                ADSMainClass.setCallEndShow100(true)
            }
        }
    }

    private fun checkOverlayPermission() {
        if (!isUserAskPermission && !PermissionManager.hasOverlayPermission(this)) {
            Toast.makeText(
                this,
                getString(R.string.please_accept_overlay_permission), Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = "package:$packageName".toUri()
            }
            startActivity(intent)
            isUserAskPermission = true
        }
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        when (view.id) {
            binding.llCall.id -> {
                mobileNumber?.let { number ->
                    Common.actionCall(number, this)
                }
                finishAndRemoveTask()
            }

            binding.ivCall.id -> {
                mobileNumber?.let { number ->
                    Common.actionCall(number, this)
                }
                finishAndRemoveTask()
            }

            binding.llAddContact.id -> {
                val intent = Intent(this, NewContactActivity::class.java).apply {
                    putExtra(Constance.NUMBER, mobileNumber)
                }
                startActivity(intent)
                finishAndRemoveTask()
            }

            binding.llSms.id -> {
                val intent = Intent(Intent.ACTION_SENDTO, "smsto:$mobileNumber".toUri())
                startActivity(intent)
                finishAndRemoveTask()
            }

            binding.llContact.id -> {
                Common.openHomeActivity(this, "contacts")
            }
        }
    }

    override fun onResume() {
        super.onResume()
//        sendFirebaseEvent("EndCallActivity", "EndCallActivity")
    }

    private fun sendFirebaseEvent(screenName: String, screenClass: String) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        initData(intent)
        initView()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndRemoveTask()
            }
        })
    }

}