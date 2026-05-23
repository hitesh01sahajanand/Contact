package com.phonecall.dialcontacts.calldialer.activities.settings

import android.Manifest
import android.accounts.AccountManager
import android.app.Dialog
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSBannerSmall
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSNativeDisplay
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSAppManage
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.blockNumbers.BlockNumbersActivity
import com.phonecall.dialcontacts.calldialer.activities.language.LanguageActivity
import com.phonecall.dialcontacts.calldialer.activities.quickResponse.QuickResponseActivity
import com.phonecall.dialcontacts.calldialer.activities.setRingtone.SetRingtoneActivity
import com.phonecall.dialcontacts.calldialer.activities.speedDial.SpeedDialActivity
import com.phonecall.dialcontacts.calldialer.adapters.AvailableAccountsAdapter
import com.phonecall.dialcontacts.calldialer.databinding.ActivitySettingsBinding
import com.phonecall.dialcontacts.calldialer.databinding.ExportContactDialogBinding
import com.phonecall.dialcontacts.calldialer.models.AvailableAccountModel
import com.phonecall.dialcontacts.calldialer.models.VCardContact
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.utils.PermissionManager
import com.phonecall.dialcontacts.calldialer.utils.PermissionManager.isDefaultDialer
import com.phonecall.dialcontacts.calldialer.utils.SharedPreferenceManager
import com.phonecall.dialcontacts.calldialer.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class SettingsActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivitySettingsBinding
    private var importFileUri: Uri? = null
    private var availableAccounts = listOf<AvailableAccountModel>()

    override fun onResume() {
        super.onResume()
        displayCurrentRingtone()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyAppTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Common.hideSystemUI(this)

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

    private val manageWriteSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.System.canWrite(this)) {
            startActivity(Intent(this, SetRingtoneActivity::class.java))
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
                    PermissionManager.openPermissionDialog(this, onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:$packageName".toUri()
                        )
                        startActivity(intent)

                    })
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
        displayCurrentRingtone()
        loadAds()
    }

    private fun loadAds() {
        if (ADSMainClass.getSettingBottomAdsShow()) {
            if (ADSMainClass.getSettingAdsType().equals("native")) {
                ADSNativeDisplay.loadAdmobNativeAdBig(
                    ADSMainClass.getStringValue(ADSMainClass.SETTING_SCREEN_NATIVE),
                    findViewById(R.id.flNativeSmallPlaceholder),
                    findViewById(R.id.shimmer_container_banner),
                    "small",
                    this
                )
            } else {
                ADSBannerSmall.loadAdMobBanner(
                    ADSMainClass.getStringValue(ADSMainClass.SETTING_SCREEN_BANNER),
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

    private fun displayCurrentRingtone() {
        try {
            val uri =
                RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(this, uri)
                val name = ringtone?.getTitle(this)
                binding.tvRingtoneName.text = name?.takeIf { it.isNotBlank() } ?: "Default"
            } else {
                binding.tvRingtoneName.text = "None"
            }
        } catch (e: Exception) {
            Log.e("Settings", "displayCurrentRingtone: ${e.message}")
            binding.tvRingtoneName.text = "Default"
        }
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
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
                    recreate()
                })
            }

            binding.llLanguage.id -> {
                val intent = Intent(this, LanguageActivity::class.java)
                intent.putExtra("language", true)
                startActivity(intent)
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
                ADSAppManage.isAppOpenBlocked = true
                val intent = try {
                    Intent(Settings.ACTION_SOUND_SETTINGS)
                } catch (_: Exception) {
                    Intent(Settings.ACTION_SETTINGS)
                }
                startActivity(intent)
            }

            binding.llSimPref.id -> {
                Common.ensureDefaultDialer(this, onProceed = {
                    Common.selectSimDialog(context = this, onItemClick = { subId ->
                        SharedPreferenceManager.putInt(this, Constance.SIM_PREFERENCE, subId)
                        updateSimPrefUI()
                    })
                })
            }

            binding.llChangeRingtone.id -> {
                if (Settings.System.canWrite(this)) {
                    startActivity(Intent(this, SetRingtoneActivity::class.java))
                } else {
                    ADSAppManage.isAppOpenBlocked = true
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = "package:$packageName".toUri()
                    manageWriteSettingsLauncher.launch(intent)
                    Toast.makeText(
                        this,
                        getString(R.string.please_allow_modify_system_settings_to_change_ringtone),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            binding.llExportContact.id -> {
                val permissionsToRequest = mutableListOf(
                    Manifest.permission.READ_CONTACTS
                )
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                // READ_PHONE_STATE is optional but recommended for SIM account names
                val phoneStateGranted = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
                if (!phoneStateGranted) {
                    permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
                }

                val ungrantedPermissions = permissionsToRequest.filter {
                    ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                }

                if (ungrantedPermissions.isEmpty() || (ungrantedPermissions.size == 1 && ungrantedPermissions[0] == Manifest.permission.READ_PHONE_STATE)) {
                    showExportContactsDialog()
                } else {
                    exportContactsPermissionLauncher.launch(ungrantedPermissions.toTypedArray())
                }
            }

            binding.llImportContact.id -> {
                ADSAppManage.isAppOpenBlocked = true
                importFileLauncher.launch("*/*")
            }

            binding.llShare.id -> {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                        val shareMessage = buildString {
                            appendLine("Let me recommend you this application")
                            appendLine()
                            append("https://play.google.com/store/apps/details?id=$packageName")
                        }
                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Choose one"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            binding.llRateUs.id -> {
                try {
                    ADSAppManage.isAppOpenBlocked = true
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$packageName")
                        )
                    )
                } catch (_: ActivityNotFoundException) {
                    ADSAppManage.isAppOpenBlocked = true
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                        )
                    )
                }
            }

            binding.llPrivacyPolicy.id -> {

            }
        }
    }

    private fun getAvailableAccounts(includeEmpty: Boolean = false): List<AvailableAccountModel> {
        val accountsMap = mutableMapOf<String, AvailableAccountModel>()
        val resolver = contentResolver

        if (includeEmpty) {
            // 1. Add Device / Phone account
            accountsMap["device"] = AvailableAccountModel("", "", getString(R.string.device), 0)

            // 2. Add SIM accounts using SubscriptionManager
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val subscriptionManager =
                    getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                subscriptionManager?.activeSubscriptionInfoList?.forEachIndexed { index, info ->
                    val simName = "SIM ${index + 1}"
                    val key = "sim_${info.subscriptionId}"
                    accountsMap[key] =
                        AvailableAccountModel(info.displayName.toString(), "sim", simName, 0)
                }
            }

            // 3. Add Accounts from AccountManager (Google, etc.)
            val accountManager = AccountManager.get(this)
            accountManager.accounts.forEach { account ->
                val type = account.type
                val name = account.name
                val isGoogle = type == "com.google"
                val isWhatsApp = type.contains("whatsapp", ignoreCase = true)
                val isTelegram = type.contains("telegram", ignoreCase = true)

                if (isGoogle || isWhatsApp || isTelegram) {
                    val key = "${name}_${type}"
                    val displayName = when {
                        isGoogle -> name
                        isWhatsApp -> "WhatsApp ($name)"
                        isTelegram -> "Telegram ($name)"
                        else -> name
                    }
                    accountsMap[key] = AvailableAccountModel(name, type, displayName, 0)
                }
            }
        }

        try {
            resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.RawContacts.ACCOUNT_NAME,
                    ContactsContract.RawContacts.ACCOUNT_TYPE
                ),
                "${ContactsContract.RawContacts.DELETED} = 0",
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0) ?: ""
                    val type = cursor.getString(1) ?: ""

                    val isGoogle = type == "com.google"
                    val isWhatsApp = type.contains("whatsapp", ignoreCase = true)
                    val isTelegram = type.contains("telegram", ignoreCase = true)
                    val isEmail = name.contains("@") && type.contains("exchange", ignoreCase = true)
                    val isSim = type.contains("sim", ignoreCase = true) || type.contains(
                        "adn",
                        ignoreCase = true
                    )

                    val isDevice = !isGoogle && !isWhatsApp && !isTelegram && !isEmail && !isSim

                    if (isDevice) {
                        val deviceModel = accountsMap.getOrPut("device") {
                            AvailableAccountModel("", "", getString(R.string.device), 0)
                        }
                        deviceModel.count++
                    } else if (isSim) {
                        val key = if (name.isNotEmpty()) "sim_$name" else "sim_$type"
                        val simModel = accountsMap.getOrPut(key) {
                            // If we don't have a mapping, just call it SIM
                            // But try to find an index if possible
                            val simIndex = accountsMap.values.count { it.accountType == "sim" } + 1
                            AvailableAccountModel(
                                name,
                                type,
                                "SIM $simIndex",
                                0
                            )
                        }
                        simModel.count++
                    } else {
                        val key = "${name}_${type}"
                        val model = accountsMap.getOrPut(key) {
                            val displayName = when {
                                isGoogle -> name
                                isWhatsApp -> "WhatsApp"
                                isTelegram -> "Telegram"
                                else -> name.ifEmpty { type }
                            }
                            AvailableAccountModel(name, type, displayName.trim(), 0)
                        }
                        model.count++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sortedAccounts = accountsMap.values.sortedWith(compareBy { account ->
            val type = account.accountType.lowercase()
            val name = account.accountName.lowercase()
            when {
                // Device/Phone: empty type and name
                type.isEmpty() && name.isEmpty() -> 0
                // SIM: type contains "sim" or "adn"
                type.contains("sim") || type.contains("adn") -> 1
                // Others: Email, Google, WhatsApp, etc.
                else -> 2
            }
        })

        return sortedAccounts.toMutableList()
    }

    private fun showExportContactsDialog() {
        val dialog = Dialog(this)
        val accountBinding = ExportContactDialogBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(accountBinding.root)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.setCancelable(false)

        val margin = (15 * resources.displayMetrics.density).toInt()
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        dialog.window?.setLayout(
            screenWidth - (margin * 3),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Show loader while fetching accounts
        accountBinding.llExportContact.visibility = View.GONE
        accountBinding.llLoader.visibility = View.VISIBLE
        accountBinding.llExportSavedLocation.visibility = View.GONE

        val availableAccountsAdapter = AvailableAccountsAdapter()
        accountBinding.rvAvailableAccounts.adapter = availableAccountsAdapter
        accountBinding.rvAvailableAccounts.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val accounts = getAvailableAccounts()
            withContext(Dispatchers.Main) {
                if (accounts.isEmpty()) {
                    dialog.dismiss()
                    Toast.makeText(
                        this@SettingsActivity,
                        getString(R.string.no_accounts_found),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    availableAccounts = accounts
                    availableAccountsAdapter.addAll(accounts)
                    accountBinding.llLoader.visibility = View.GONE
                    accountBinding.llExportContact.visibility = View.VISIBLE
                }
            }
        }

        val appName = getString(R.string.app_name).replace(" ", "_")
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val defaultName = "${appName}_${dateFormat.format(java.util.Date())}.vcf"
        accountBinding.edtFileName.setText(defaultName)
        accountBinding.edtFileName.setSelection(defaultName.length - 4)

        accountBinding.cvCancel.setOnClickListener {
            dialog.dismiss()
        }

        accountBinding.ivClose.setOnClickListener {
            dialog.dismiss()
        }

        accountBinding.cvExport.setOnClickListener {
            val selected = availableAccountsAdapter.getSelectedAccounts()
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_accounts_selected), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val fileName = accountBinding.edtFileName.text.toString().trim()
            if (fileName.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_file_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalFileName =
                if (fileName.endsWith(".vcf", ignoreCase = true)) fileName else "$fileName.vcf"

            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, finalFileName)

            if (file.exists()) {
                Toast.makeText(
                    this,
                    getString(R.string.this_name_is_already_stored_please_choose_another),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Show loader within the same dialog
            accountBinding.llExportContact.visibility = View.GONE
            accountBinding.llLoader.visibility = View.VISIBLE

            exportSelectedAccounts(selected, finalFileName, accountBinding, dialog)
        }

        accountBinding.cvOkay.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun exportSelectedAccounts(
        selectedAccounts: List<AvailableAccountModel>,
        finalFileName: String,
        accountBinding: ExportContactDialogBinding,
        dialog: Dialog
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val validContactIds = mutableSetOf<Long>()
                val resolver = contentResolver

                resolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(
                        ContactsContract.RawContacts.CONTACT_ID,
                        ContactsContract.RawContacts.ACCOUNT_NAME,
                        ContactsContract.RawContacts.ACCOUNT_TYPE
                    ),
                    "${ContactsContract.RawContacts.DELETED} = 0",
                    null,
                    null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        if (cursor.isNull(0)) continue
                        val contactId = cursor.getLong(0)
                        val name = cursor.getString(1) ?: ""
                        val type = cursor.getString(2) ?: ""

                        val isGoogle = type == "com.google"
                        val isWhatsApp = type.contains("whatsapp", ignoreCase = true)
                        val isTelegram = type.contains("telegram", ignoreCase = true)
                        val isEmail =
                            name.contains("@") && type.contains("exchange", ignoreCase = true)
                        val isSim = type.contains("sim", ignoreCase = true)

                        val isDevice = !isGoogle && !isWhatsApp && !isTelegram && !isEmail && !isSim

                        val matches = selectedAccounts.any { account ->
                            if (account.accountName.isEmpty() && account.accountType.isEmpty()) {
                                isDevice
                            } else {
                                account.accountName == name && account.accountType == type
                            }
                        }
                        if (matches) {
                            validContactIds.add(contactId)
                        }
                    }
                }

                Log.d("Export", "Valid contact IDs: ${validContactIds.size}")

                val lookupKeys = mutableSetOf<String>()
                if (validContactIds.isNotEmpty()) {
                    resolver.query(
                        ContactsContract.Contacts.CONTENT_URI,
                        arrayOf(
                            ContactsContract.Contacts.LOOKUP_KEY,
                            ContactsContract.Contacts._ID
                        ),
                        "${ContactsContract.Contacts._ID} IN (${validContactIds.joinToString(",")})",
                        null,
                        null
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val key = cursor.getString(0)
                            if (!key.isNullOrEmpty()) {
                                lookupKeys.add(key)
                            }
                        }
                    }
                }

                Log.d("Export", "Found ${lookupKeys.size} lookup keys for export")

                if (lookupKeys.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.no_contacts_found_to_export),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    val created = downloadsDir.mkdirs()
                    Log.d("Export", "Downloads directory created: $created")
                }

                val file = File(downloadsDir, finalFileName)
                var bytesWritten = 0L

                FileOutputStream(file).use { output ->
                    for (key in lookupKeys) {
                        val uri = Uri.withAppendedPath(
                            ContactsContract.Contacts.CONTENT_VCARD_URI,
                            key
                        )
                        try {
                            resolver.openAssetFileDescriptor(uri, "r")?.createInputStream()
                                ?.use { input ->
                                    val copied = input.copyTo(output)
                                    bytesWritten += copied
                                }
                        } catch (e: Exception) {
                            Log.e("Export", "Error reading vCard for key $key", e)
                        }
                    }
                    output.flush()
                }

                Log.d("Export", "Bytes written to file: $bytesWritten")

                if (bytesWritten == 0L) {
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.no_contacts_found_to_export),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (file.exists()) file.delete()
                    return@launch
                }

                MediaScannerConnection.scanFile(
                    this@SettingsActivity,
                    arrayOf(file.absolutePath),
                    arrayOf("text/vcard"),
                    null
                )

                // Also broadcast intent for older versions
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = Uri.fromFile(file)
                    sendBroadcast(mediaScanIntent)
                }

                withContext(Dispatchers.Main) {
                    accountBinding.llLoader.visibility = View.GONE
                    accountBinding.llExportSavedLocation.visibility = View.VISIBLE
                    accountBinding.tvExportSavedLocation.text =
                        getString(R.string.contacts_successfully_saved_to, file.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    Toast.makeText(
                        this@SettingsActivity,
                        getString(R.string.error_exporting_contacts, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private val exportContactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readContactsGranted = (permissions[Manifest.permission.READ_CONTACTS]
                ?: ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_CONTACTS
                )) == PackageManager.PERMISSION_GRANTED

            val writeStorageGranted = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                val writeGranted = (permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                    ?: ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )) == PackageManager.PERMISSION_GRANTED

                val readGranted = (permissions[Manifest.permission.READ_EXTERNAL_STORAGE]
                    ?: ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )) == PackageManager.PERMISSION_GRANTED

                writeGranted && readGranted
            } else true

            if (readContactsGranted && writeStorageGranted) {
                showExportContactsDialog()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permissions_required_to_export_contacts), Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

    private val importFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                importFileUri = uri
                val permissionsToRequest = mutableListOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.READ_PHONE_STATE
                )
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                val ungrantedPermissions = permissionsToRequest.filter {
                    ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                }
                if (ungrantedPermissions.isEmpty()) {
                    showImportAccountSelectionDialog(uri)
                } else {
                    importContactsPermissionLauncher.launch(ungrantedPermissions.toTypedArray())
                }
            }
        }

    private val importContactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readContactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true
            val writeContactsGranted = permissions[Manifest.permission.WRITE_CONTACTS] == true
            val readPhoneStateGranted = permissions[Manifest.permission.READ_PHONE_STATE] == true
            val readStorageGranted = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            } else true

            if (readContactsGranted && writeContactsGranted && readPhoneStateGranted && readStorageGranted) {
                importFileUri?.let { showImportAccountSelectionDialog(it) }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permissions_required_to_import_contacts), Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

    private fun showImportAccountSelectionDialog(uri: Uri) {
        val dialog = Dialog(this)
        val accountBinding = ExportContactDialogBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(accountBinding.root)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.setCancelable(false)

        val margin = (15 * resources.displayMetrics.density).toInt()
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        dialog.window?.setLayout(
            screenWidth - (margin * 3),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Show loader while fetching accounts
        accountBinding.llExportContact.visibility = View.GONE
        accountBinding.llLoader.visibility = View.VISIBLE
        accountBinding.llExportSavedLocation.visibility = View.GONE

        val availableAccountsAdapter = AvailableAccountsAdapter()
        accountBinding.rvAvailableAccounts.adapter = availableAccountsAdapter
        accountBinding.rvAvailableAccounts.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val accounts = getAvailableAccounts(includeEmpty = true)
            withContext(Dispatchers.Main) {
                if (accounts.isEmpty()) {
                    dialog.dismiss()
                    Toast.makeText(
                        this@SettingsActivity,
                        getString(R.string.no_accounts_found),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    availableAccountsAdapter.addAll(accounts)
                    accountBinding.llLoader.visibility = View.GONE
                    accountBinding.llExportContact.visibility = View.VISIBLE
                }
            }
        }

        // Adjust for import
        accountBinding.tvTitle.text = getString(R.string.select_account_for_import)
        accountBinding.tvDone.text = getString(R.string.import_)
        accountBinding.edtFileName.visibility = View.GONE

        accountBinding.cvCancel.setOnClickListener {
            dialog.dismiss()
        }

        accountBinding.ivClose.setOnClickListener {
            dialog.dismiss()
        }

        accountBinding.cvExport.setOnClickListener {
            val selected = availableAccountsAdapter.getSelectedAccounts()
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_accounts_selected), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // For import, we use the first selected account
            val selectedAccount = selected[0]

            // Switch to loader
            accountBinding.llExportContact.visibility = View.GONE
            accountBinding.llLoader.visibility = View.VISIBLE
            accountBinding.tvExport.text = getString(R.string.importing_contacts)

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val contacts = parseVCard(uri)
                    if (contacts.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            dialog.dismiss()
                            Toast.makeText(
                                this@SettingsActivity,
                                getString(R.string.no_contacts_found_in_file),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }

                    saveContactsToAccount(contacts, selectedAccount)

                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.contacts_imported_successfully, contacts.size),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.error_importing_contacts, e.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun parseVCard(fileUri: Uri): List<VCardContact> {
        val contacts = mutableListOf<VCardContact>()
        try {
            contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { reader ->
                var inVCard = false
                var currentContact: VCardContact? = null

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    var l = line!!.trim()

                    while (l.endsWith("=") && l.contains("QUOTED-PRINTABLE", ignoreCase = true)) {
                        l = l.dropLast(1)
                        val nextLine = reader.readLine() ?: break
                        l += nextLine.trim()
                    }

                    if (l == "BEGIN:VCARD") {
                        inVCard = true
                        currentContact = VCardContact()
                    } else if (l == "END:VCARD") {
                        if (inVCard && currentContact != null) {
                            if (currentContact.name.isNotEmpty() || currentContact.phones.isNotEmpty() || currentContact.emails.isNotEmpty()) {
                                contacts.add(currentContact)
                            }
                        }
                        inVCard = false
                        currentContact = null
                    } else if (inVCard && currentContact != null) {
                        val upperL = l.uppercase()
                        if (upperL.startsWith("FN:") || upperL.startsWith("FN;")) {
                            val value = l.substringAfter(":", "")
                            if (value.isNotEmpty()) currentContact.name =
                                decodeQuotedPrintable(value)
                        } else if (upperL.startsWith("N:") || upperL.startsWith("N;")) {
                            if (currentContact.name.isEmpty()) {
                                val value = l.substringAfter(":", "")
                                val parts = value.split(";")
                                val last = decodeQuotedPrintable(parts.getOrNull(0) ?: "")
                                val first = decodeQuotedPrintable(parts.getOrNull(1) ?: "")
                                currentContact.name = "$first $last".trim()
                            }
                        } else if (upperL.startsWith("TEL:") || upperL.startsWith("TEL;")) {
                            val value = l.substringAfter(":", "")
                            if (value.isNotEmpty()) currentContact.phones.add(
                                decodeQuotedPrintable(
                                    value
                                )
                            )
                        } else if (upperL.startsWith("EMAIL:") || upperL.startsWith("EMAIL;")) {
                            val value = l.substringAfter(":", "")
                            if (value.isNotEmpty()) currentContact.emails.add(
                                decodeQuotedPrintable(
                                    value
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contacts
    }

    private fun decodeQuotedPrintable(text: String): String {
        return text.replace(Regex("=([0-9A-F]{2})", RegexOption.IGNORE_CASE)) { matchResult ->
            try {
                matchResult.groupValues[1].toInt(16).toChar().toString()
            } catch (e: Exception) {
                Log.e("TAG", "decodeQuotedPrintable: ${e.message}")
                matchResult.value
            }
        }
    }

    private suspend fun saveContactsToAccount(
        contacts: List<VCardContact>,
        account: AvailableAccountModel
    ) {
        withContext(Dispatchers.IO) {
            val ops = ArrayList<ContentProviderOperation>()
            val accountName = account.accountName.ifEmpty { null }
            val accountType = account.accountType.ifEmpty { null }

            for (contact in contacts) {
                val rawContactInsertIndex = ops.size

                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                        .build()
                )

                if (contact.name.isNotEmpty()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(
                                ContactsContract.Data.RAW_CONTACT_ID,
                                rawContactInsertIndex
                            )
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                                contact.name
                            )
                            .build()
                    )
                }

                for (phone in contact.phones) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(
                                ContactsContract.Data.RAW_CONTACT_ID,
                                rawContactInsertIndex
                            )
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                            )
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                            .withValue(
                                ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                            )
                            .build()
                    )
                }

                for (email in contact.emails) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(
                                ContactsContract.Data.RAW_CONTACT_ID,
                                rawContactInsertIndex
                            )
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                            )
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                            .withValue(
                                ContactsContract.CommonDataKinds.Email.TYPE,
                                ContactsContract.CommonDataKinds.Email.TYPE_WORK
                            )
                            .build()
                    )
                }

                if (ops.size >= 300) {
                    try {
                        contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    ops.clear()
                }
            }

            if (ops.isNotEmpty()) {
                try {
                    contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
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
                    binding.tvSimPref.text = selectedSim.displayName
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
}