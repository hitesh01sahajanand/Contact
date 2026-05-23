package com.phonecall.dialcontacts.calldialer.activities.splash

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSUtilitis
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.home.HomeActivity
import com.phonecall.dialcontacts.calldialer.activities.language.LanguageActivity
import com.phonecall.dialcontacts.calldialer.activities.permissions.PermissionActivity
import com.phonecall.dialcontacts.calldialer.databinding.ActivityMainBinding
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.SharedPreferenceManager
import com.phonecall.dialcontacts.calldialer.utils.ThemeManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseSplashActivity() {
    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->
            proceedToNext()
        }

    override fun initActivity() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyAppTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()

        // Fetch FCM Token immediately on startup
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener { task: Task<String?>? ->
                if (task!!.isSuccessful) {
                    val token = task.getResult()
                    Log.d("FCM_TOKEN", "Startup FCM Token: $token")
                } else {
                    Log.e("FCM_TOKEN", "Startup FCM Token fetch failed", task.exception)
                }
            }

        ADSMainClass.updateConsecutiveStreak(this)
        ADSMainClass.scheduleInstallDayWorker(this)

        if (intent.getBooleanExtra("from_overlay_notification", false)) {
            SharedPreferenceManager.putBoolean(this, Constance.OVERLAY_PERMISSION_SKIP, false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = window.insetsController
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.navigationBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun initView() {
        binding.lavSplashLogo.setAnimation(R.raw.splash_logo)
        binding.lavSplashLogo.addAnimatorListener(object :
            android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}

            override fun onAnimationEnd(animation: android.animation.Animator) {
                checkNotificationPermission()
            }

            override fun onAnimationCancel(animation: android.animation.Animator) {}

            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
        binding.lavSplashLogo.playAnimation()
    }

    private fun checkNotificationPermission() {

        val permissions = mutableListOf<String>()

        // Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Call Permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CALL_PHONE)
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            proceedToNext()
        }
    }

    private fun proceedToNext() {
        val isLogIN = SharedPreferenceManager.getBoolean(this, Constance.IS_LOG_IN, false)
        val options = ActivityOptions.makeCustomAnimation(
            this,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        if (ADSMainClass.getSplashToLanguage()) {
            if (!ADSMainClass.getLanguageScreen()) {
                if (!isLogIN) {
                    ADSUtilitis.trackScreen(this@SplashActivity, "Splash_TO_Language")
                    startActivity(Intent(this@SplashActivity, LanguageActivity::class.java))
                    finish()
                }else{
                    ADSUtilitis.trackScreen(this@SplashActivity, "Splash_TO_Main")
                    startActivity(Intent(this, HomeActivity::class.java), options.toBundle())
                    finish()
                }

            } else {
                if (!isLogIN) {
                    startActivity(Intent(this, PermissionActivity::class.java))
                    finish()
                } else {
                    ADSUtilitis.trackScreen(this@SplashActivity, "Splash_TO_Main")
                    startActivity(Intent(this, HomeActivity::class.java), options.toBundle())
                    finish()
                }
            }
        } else {
            if (!isLogIN) {
                startActivity(Intent(this, PermissionActivity::class.java))
                finish()
            } else {
                ADSUtilitis.trackScreen(this@SplashActivity, "Splash_TO_Main")
                startActivity(Intent(this, HomeActivity::class.java), options.toBundle())
                finish()
            }
        }
    }
}