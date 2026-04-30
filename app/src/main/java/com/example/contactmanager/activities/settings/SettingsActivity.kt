package com.example.contactmanager.activities.settings

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.contactmanager.R
import com.example.contactmanager.activities.blockNumbers.BlockNumbersActivity
import com.example.contactmanager.activities.home.HomeActivity
import com.example.contactmanager.activities.language.LanguageActivity
import com.example.contactmanager.activities.quickResponse.QuickResponseActivity
import com.example.contactmanager.activities.setRingtone.SetRingtoneActivity
import com.example.contactmanager.activities.speedDial.SpeedDialActivity
import com.example.contactmanager.databinding.ActivitySettingsBinding
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager.isDefaultDialer
import com.example.contactmanager.utils.SharedPreferenceManager
import com.example.contactmanager.utils.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()
                    )
                    startActivity(intent)
                    // Optional: reset switch if permission not granted, but usually we just let them go to settings
                }
                SharedPreferenceManager.putBoolean(this, Constance.CONFIRM_DIALOG, true)
            } else {
                SharedPreferenceManager.putBoolean(this, Constance.CONFIRM_DIALOG, false)
            }
        }

        val isMergeDuplicate =
            SharedPreferenceManager.getBoolean(this, Constance.MERGE_DUPLICATE_CONTACT, false)
        binding.switchMergeDuplicateContact.isChecked = isMergeDuplicate

        binding.switchMergeDuplicateContact.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                SharedPreferenceManager.putBoolean(this, Constance.MERGE_DUPLICATE_CONTACT, true)
            } else {
                SharedPreferenceManager.putBoolean(this, Constance.MERGE_DUPLICATE_CONTACT, false)
            }
        }
        val isDialPadSound =
            SharedPreferenceManager.getBoolean(this, Constance.DIAL_PAD_SOUND, false)
        binding.switchDialPadSound.isChecked = isDialPadSound

        binding.switchDialPadSound.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                SharedPreferenceManager.putBoolean(this, Constance.DIAL_PAD_SOUND, true)
            } else {
                SharedPreferenceManager.putBoolean(this, Constance.DIAL_PAD_SOUND, false)
            }
        }

        val isCallFlash = SharedPreferenceManager.getBoolean(this, Constance.CALL_FLASH, false)
        binding.switchCallFlash.isChecked = isCallFlash

        binding.switchCallFlash.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                SharedPreferenceManager.putBoolean(this, Constance.CALL_FLASH, true)
            } else {
                SharedPreferenceManager.putBoolean(this, Constance.CALL_FLASH, false)
            }
        }

        val isSwipeAction = SharedPreferenceManager.getBoolean(this, Constance.SWIPE_ACTION, false)
        binding.switchSwipeAction.isChecked = isSwipeAction

        binding.switchSwipeAction.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                SharedPreferenceManager.putBoolean(this, Constance.SWIPE_ACTION, true)
            } else {
                SharedPreferenceManager.putBoolean(this, Constance.SWIPE_ACTION, false)
            }
        }

        val appTheme = SharedPreferenceManager.getString(this, Constance.APP_THEME)
        if (appTheme.isNotEmpty()) {
            val name = when (appTheme) {
                getString(R.string.light_mode_app) -> getString(R.string.light_mode_app)
                getString(R.string.dark_mode) -> getString(R.string.dark_mode)
                else -> getString(R.string.set_default)
            }
            binding.tvThemeType.text = name
        } else {
            binding.tvThemeType.text = getString(R.string.set_default)
        }

        updateSimPrefUI()
    }

    private fun updateSimPrefUI() {
        val simPref = SharedPreferenceManager.getInt(this, Constance.SIM_PREFERENCE, -1)
        if (simPref == -1) {
            binding.tvSimPref.text = getString(R.string.ask_every_time)
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val subscriptionManager =
                    getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val activeSimList = subscriptionManager?.activeSubscriptionInfoList
                val selectedSim = activeSimList?.find { it.subscriptionId == simPref }
                if (selectedSim != null) {
                    binding.tvSimPref.text = selectedSim.carrierName
                } else {
                    binding.tvSimPref.text = getString(R.string.ask_every_time)
                    SharedPreferenceManager.putInt(this, Constance.SIM_PREFERENCE, -1)
                }
            } else {
                binding.tvSimPref.text = getString(R.string.ask_every_time)
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
                val telecomManager = context.getSystemService(TELECOM_SERVICE) as TelecomManager
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
                startActivity(Intent(this, LanguageActivity::class.java))
            }

            binding.llBlockNumber.id -> {
                startActivity(Intent(this, BlockNumbersActivity::class.java))
            }


            binding.llSpeedDial.id -> {
                startActivity(Intent(this, SpeedDialActivity::class.java))
            }

            binding.llQuickResponse.id -> {
                startActivity(Intent(this, QuickResponseActivity::class.java))
            }

            binding.llSoundVibration.id -> {
                val intent = try {
                    Intent(Settings.ACTION_SOUND_SETTINGS)
                } catch (_: Exception) {
                    Intent(Settings.ACTION_SETTINGS)
                }
                startActivity(intent)
            }

            binding.llSimPref.id -> {
                if (ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val subscriptionManager =
                        getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                    val activeSimList = subscriptionManager?.activeSubscriptionInfoList

                    if (activeSimList.isNullOrEmpty()) {
                        Toast.makeText(
                            this,
                            getString(R.string.no_sim_cards_found), Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val simNames = Array(activeSimList.size + 1) { i ->
                        if (i == 0) "Always ask"
                        else "SIM $i (${activeSimList[i - 1].carrierName})"
                    }

                    val simPref = SharedPreferenceManager.getInt(this, Constance.SIM_PREFERENCE, -1)
                    var selectedIndex = if (simPref == -1) 0 else {
                        val index = activeSimList.indexOfFirst { it.subscriptionId == simPref }
                        if (index != -1) index + 1 else 0
                    }

                    val builder = MaterialAlertDialogBuilder(this)

                    builder.setTitle(getString(R.string.select_sim))

                    builder.setSingleChoiceItems(simNames, selectedIndex) { _, which ->
                        selectedIndex = which
                    }

                    builder.setPositiveButton(getString(R.string.set)) { dialog, _ ->
                        if (selectedIndex == 0) {
                            SharedPreferenceManager.putInt(this, Constance.SIM_PREFERENCE, -1)
                        } else {
                            val selectedSim = activeSimList[selectedIndex - 1]
                            SharedPreferenceManager.putInt(
                                this,
                                Constance.SIM_PREFERENCE,
                                selectedSim.subscriptionId
                            )
                        }
                        updateSimPrefUI()
                        dialog.dismiss()
                    }

                    builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }

                    builder.show()
                }
            }

            binding.llChangeRingtone.id -> {
                if (Settings.System.canWrite(this)) {
                    startActivity(Intent(this, SetRingtoneActivity::class.java))
                } else {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = "package:$packageName".toUri()
                    startActivity(intent)
                    Toast.makeText(
                        this,
                        getString(R.string.please_allow_modify_system_settings_to_change_ringtone),
                        Toast.LENGTH_LONG
                    ).show()
                }
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