package com.phonecall.dialcontacts.calldialer.activities.permissions

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSBannerSmall
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSNativeDisplay
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.home.HomeActivity
import com.phonecall.dialcontacts.calldialer.activities.language.LanguageActivity
import com.phonecall.dialcontacts.calldialer.databinding.ActivityPermissionBinding
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.utils.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlin.jvm.java

@AndroidEntryPoint
class PermissionActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityPermissionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyAppTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_permission)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Common.hideSystemUI(this)
        intView()
        loadAds()
    }

    private val defaultDialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            when (result.resultCode) {
                RESULT_OK -> {
                    goNextActivity()
                }

                RESULT_CANCELED -> {
                    goNextActivity()
                }
            }
        }

    private fun intView() {
        binding.onClickHandler = this

        binding.lottiPermission.setAnimation(R.raw.permission_light)
        binding.lottiPermission.playAnimation()

        binding.lottiPermissionBtn.setAnimation(R.raw.permission_btn)
        binding.lottiPermissionBtn.playAnimation()

        manageTextViews()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goNextActivity()
            }
        })

    }

    fun manageTextViews() {

        val text = getString(R.string.we_don_t_collect_personal)
        val spannable = SpannableString(text)

        val termsStart = text.indexOf("Terms of Service")
        val termsEnd = termsStart + "Terms of Service".length

        val termsClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(widget.context, "Terms clicked", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@PermissionActivity, R.color.main_color)
                ds.isUnderlineText = true
            }
        }

        spannable.setSpan(termsClickable, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val privacyStart = text.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length

        val privacyClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {

            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@PermissionActivity, R.color.main_color)
                ds.isUnderlineText = true
            }
        }

        spannable.setSpan(
            privacyClickable,
            privacyStart,
            privacyEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvPrivacyPolicy.text = spannable
        binding.tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()
        binding.tvPrivacyPolicy.highlightColor = Color.TRANSPARENT
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        when (view.id) {
            binding.lottiPermissionBtn.id -> {
                openDefaultAppDialog(this)
            }

            binding.tvCancel.id -> {
                goNextActivity()
            }
        }
    }

    private fun loadAds() {
        if (ADSMainClass.getPermissionSmallAdsShow()) {
            if (ADSMainClass.getPermissionAdsType().equals("native")) {
                ADSNativeDisplay.loadAdmobNativeAdBig(
                    ADSMainClass.getStringValue(ADSMainClass.PERMISSION_SCREEN_NATIVE),
                    findViewById(R.id.flNativeSmallPlaceholder),
                    findViewById(R.id.shimmer_container_banner),
                    "small",
                    this
                )
            } else {
                ADSBannerSmall.loadAdMobBanner(
                    ADSMainClass.getStringValue(ADSMainClass.PERMISSION_SCREEN_BANNER),
                    findViewById(R.id.flBannerSmallPlaceholder),
                    findViewById(R.id.shimmer_container_banner),
                    this
                )
            }
        } else {
            findViewById<View>(R.id.shimmer_container_banner).visibility = View.GONE
            findViewById<View>(R.id.flNativeSmallPlaceholder).visibility = View.GONE
            findViewById<View>(R.id.flBannerSmallPlaceholder).visibility = View.GONE
        }
    }

    fun openDefaultAppDialog(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val roleManager = context.getSystemService(ROLE_SERVICE) as RoleManager
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                defaultDialerLauncher.launch(intent)
            } else {
                val telecomManager =
                    context.getSystemService(TELECOM_SERVICE) as TelecomManager
                if (context.packageName != telecomManager.defaultDialerPackage) {
                    val intent = Intent("android.telecom.action.CHANGE_DEFAULT_DIALER").apply {
                        putExtra(
                            "android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME",
                            context.packageName
                        )
                    }
                    defaultDialerLauncher.launch(intent)
                }
            }
        } catch (e: Exception) {
            Log.e("TAG", "openDefaultAppDialog:${e.message}")
        }
    }

    fun goNextActivity() {
        val isFromLanguage = intent.getBooleanExtra("language", false)
        if (isFromLanguage) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this, LanguageActivity::class.java))
            finish()
        }
    }
}