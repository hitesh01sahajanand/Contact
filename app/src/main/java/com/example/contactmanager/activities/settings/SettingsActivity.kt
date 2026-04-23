package com.example.contactmanager.activities.settings

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.contactmanager.activities.home.HomeActivity
import androidx.databinding.DataBindingUtil
import com.example.contactmanager.R
import com.example.contactmanager.activities.blockNumbers.BlockNumbersActivity
import com.example.contactmanager.databinding.ActivitySettingsBinding
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.ThemeManager
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager.isDefaultDialer
import com.example.contactmanager.utils.SharedPreferenceManager

class SettingsActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
    }

    private val defaultDialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            when (result.resultCode) {
                RESULT_OK -> {
                    binding.llShowConfirmationDialog.visibility = View.VISIBLE
                    binding.llCallerIdDisable.visibility = View.GONE
                }

                RESULT_CANCELED -> {
                    binding.llCallerIdDisable.visibility = View.VISIBLE
                    binding.llShowConfirmationDialog.visibility = View.GONE
                }

            }

        }

    private fun initView() {
        binding.onClickHandler = this

        if (isDefaultDialer(this)) {
            binding.llShowConfirmationDialog.visibility = View.VISIBLE
            binding.llCallerIdDisable.visibility = View.GONE
        } else {
            binding.llCallerIdDisable.visibility = View.VISIBLE
            binding.llShowConfirmationDialog.visibility = View.GONE
        }

        val isConfirmDialog =
            SharedPreferenceManager.getBoolean(this, Constance.CONFIRM_DIALOG, false)
        binding.switchConfirmDialog.isChecked = isConfirmDialog

        binding.switchConfirmDialog.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:$packageName".toUri()
                    )
                    startActivity(intent)
                    // Optional: reset switch if permission not granted, but usually we just let them go to settings
                }
                SharedPreferenceManager.putBoolean(this, Constance.CONFIRM_DIALOG, true)
            } else {
                SharedPreferenceManager.putBoolean(this, Constance.CONFIRM_DIALOG, false)
            }
        }

        val appTheme = SharedPreferenceManager.getString(this, Constance.APP_THEME)
        if (appTheme.isNotEmpty()) {
            binding.tvThemeType.text = appTheme
        } else {
            binding.tvThemeType.text = getString(R.string.set_default)
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
            Log.e("TAG", "openDefaultAppDialog: ${e.message}")
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.ivBack.id -> {
                onBackPressedDispatcher.onBackPressed()
            }

            binding.llCallerIdDisable.id -> {
                openDefaultAppDialog(this)
            }

            binding.llAppTheme.id -> {
                Common.showAppThemeBottomSheet(this, onItemClick = { theme ->
                    val currentTheme = SharedPreferenceManager.getString(this, Constance.APP_THEME)

                    if (theme == currentTheme) return@showAppThemeBottomSheet

                    SharedPreferenceManager.putString(this, Constance.APP_THEME, theme)
                    ThemeManager.applyAppTheme(this)

                    val intent = Intent(this, HomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                })
            }

            binding.llLanguage.id -> {

            }

            binding.llBlockNumber.id -> {
                startActivity(Intent(this, BlockNumbersActivity::class.java))
            }


            binding.llSpeedDial.id -> {

            }

            binding.llQuickResponse.id -> {

            }

            binding.llSoundVibration.id -> {

            }

            binding.llMergeDuplicate.id -> {

            }

            binding.llSimPref.id -> {

            }

            binding.llChangeRingtone.id -> {

            }

            binding.llShare.id -> {

            }

            binding.llRateUs.id -> {

            }

            binding.llPrivacyPolicy.id -> {

            }
        }
    }
}