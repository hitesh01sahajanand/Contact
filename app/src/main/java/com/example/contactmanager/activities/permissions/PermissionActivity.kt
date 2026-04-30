package com.example.contactmanager.activities.permissions

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.example.contactmanager.R
import com.example.contactmanager.activities.language.LanguageActivity
import com.example.contactmanager.databinding.ActivityPermissionBinding
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager
import com.example.contactmanager.utils.SharedPreferenceManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityPermissionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_permission)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        intView()
    }


    private val defaultDialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            when (result.resultCode) {
                RESULT_OK -> {
//                    checkOverlayPermission()
                    goNextActivity()
                }

                RESULT_CANCELED -> {
                    goNextActivity()
                }
            }
        }

    private val permissionLauncherCallLog =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val allGranted = permissions.values.all { it }

            if (allGranted) {
                goNextActivity()
            } else {
                val permanentlyDenied = isPermissionPermanentlyDenied()

                if (permanentlyDenied) {
                    showSettingsDialog()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
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

//        binding.llNotification.isVisible = isNotificationPermissionRequired()
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
                Toast.makeText(widget.context, "Privacy clicked", Toast.LENGTH_SHORT).show()
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
        when (view.id) {
            binding.lottiPermissionBtn.id -> {
                /*if (PermissionManager.hasRequiredPermissions(this)) {
                    goNextActivity()
                } else {
                    requestRequiredPermissions()
                }*/
                openDefaultAppDialog(this)
            }

            binding.tvCancel.id -> {
                goNextActivity()
            }
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
            // Handle exception if needed
        }
    }

    fun goNextActivity() {
        startActivity(Intent(this, LanguageActivity::class.java))
        finish()
    }

    fun isNotificationPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val callGranted = permissions[Manifest.permission.CALL_PHONE] ?: false
            val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
            } else true

            if (callGranted && notificationGranted) {
                checkAndRequestPermissions()

            } else {
                val permanentlyDenied = isPermissionPermanentlyDenied()

                if (permanentlyDenied) {
                    showSettingsDialog()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this).setTitle("Permission Required")
            .setMessage("Permission is permanently denied. Please enable it from settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }.setNegativeButton("Cancel", null).show()
    }

    private fun isPermissionPermanentlyDenied(): Boolean {

        val callDenied = !shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE)

        val notificationDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else false

        return callDenied || notificationDenied
    }

    fun requestRequiredPermissions() {

        val permissionsList = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsList.add(Manifest.permission.CALL_PHONE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsList.isNotEmpty()) {
            permissionLauncher.launch(permissionsList.toTypedArray())
        }
    }


    private fun checkAndRequestPermissions() {
        if (!PermissionManager.hasPermissions(this)) {

            val permissionsList = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_CALL_LOG
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsList.add(Manifest.permission.READ_CALL_LOG)
            }

            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsList.add(Manifest.permission.READ_CONTACTS)
            }

            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsList.add(Manifest.permission.WRITE_CONTACTS)
            }


            if (permissionsList.isNotEmpty()) {
                permissionLauncherCallLog.launch(permissionsList.toTypedArray())
            }
        }
    }
}