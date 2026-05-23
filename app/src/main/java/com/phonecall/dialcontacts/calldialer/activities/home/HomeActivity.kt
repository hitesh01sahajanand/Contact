package com.phonecall.dialcontacts.calldialer.activities.home

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.MobileAds
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSBannerSmall
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSInterDisplayClick
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass.EXIT_SCREEN_NATIVE
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSNativeDisplay
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSUtilitis
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSAppManage
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.databinding.ActivityHomeBinding
import com.phonecall.dialcontacts.calldialer.fragments.contacts.ContactsFragment
import com.phonecall.dialcontacts.calldialer.fragments.favorites.FavoritesFragment
import com.phonecall.dialcontacts.calldialer.fragments.keypad.KeypadFragment
import com.phonecall.dialcontacts.calldialer.fragments.recents.RecentsFragment
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.DailyNotificationUtils
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.utils.PermissionManager
import com.phonecall.dialcontacts.calldialer.utils.SharedPreferenceManager
import com.phonecall.dialcontacts.calldialer.utils.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var favoritesFragment: FavoritesFragment
    private lateinit var recentsFragment: RecentsFragment
    private lateinit var contactsFragment: ContactsFragment
    private lateinit var keypadFragment: KeypadFragment
    private lateinit var activeFragment: Fragment

    private var doubleBackToExitPressedOnce = false
    private var selectedTab: View? = null
    private var isViewInitialized = false
    private var isFromPermissionRequest = false
    private var permissionDialog: Dialog? = null
    private lateinit var appUpdateManager: AppUpdateManager
    private val APP_UPDATE_REQUEST_CODE = 1001

    private val contactPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isFromPermissionRequest = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationGranted = permissions[Manifest.permission.POST_NOTIFICATIONS]
            if (notificationGranted != null && !notificationGranted) {
                SharedPreferenceManager.putBoolean(
                    this,
                    Constance.NOTIFICATION_PERMISSION_SKIP,
                    true
                )
            }
        }

        val requiredPermissions = contactPermissions.filter {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it != Manifest.permission.POST_NOTIFICATIONS
            } else {
                true
            }
        }

        if (!requiredPermissions.all {
                permissions[it] == true || ContextCompat.checkSelfPermission(
                    this,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            // Check if any of the denied permissions are permanently denied (user clicked "Don't ask again")
            val isPermanentlyDenied = requiredPermissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(this, it)
            }

            if (isPermanentlyDenied) {
                Toast.makeText(
                    this,
                    getString(R.string.permissions_are_required),
                    Toast.LENGTH_LONG
                ).show()
                openAppSettings()
                isFromPermissionRequest = true
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT)
                    .show()
                isFromPermissionRequest = false
            }
        }
    }

    private fun openAppSettings() {
        ADSAppManage.isAppOpenBlocked = true
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isFromPermissionRequest = true
        SharedPreferenceManager.putBoolean(this, Constance.OVERLAY_PERMISSION_SKIP, true)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyAppTheme(this)
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Common.hideSystemUI(this)
        appUpdateManager = AppUpdateManagerFactory.create(this)
        if (ADSMainClass.shouldShowAppUpdate()) {
            checkForUpdates()
        }

        try {
            DailyNotificationUtils.scheduleDailyNotification(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        initView()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions(showCustomDialog = !isFromPermissionRequest)
        isFromPermissionRequest = false
    }

    override fun onStop() {
        super.onStop()
        permissionDialog?.dismiss()
    }

    private fun checkForUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val updateType =
                if (ADSMainClass.getInAppUpdateType().equals("Immediate", ignoreCase = true)) {
                    AppUpdateType.IMMEDIATE
                } else {
                    AppUpdateType.FLEXIBLE
                }

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(updateType)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateType,
                        this,
                        APP_UPDATE_REQUEST_CODE
                    )
                    ADSMainClass.updateAppUpdateShowCount()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
//            showUpdateCompletedSnackbar()
        }
    }

    /*private fun showUpdateCompletedSnackbar() {
        Snackbar.make(
            findViewById(R.id.drawerLayout),
            "An update has just been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("RESTART") { appUpdateManager.completeUpdate() }
            show()
        }
    }*/

    fun exitApp(activity: Activity) {
        val massageBox = Dialog(this)
        massageBox.requestWindowFeature(1)
        massageBox.window!!.setLayout(-1, -2)
        massageBox.window!!.setBackgroundDrawable(0.toDrawable())

        val inflater = massageBox.layoutInflater
        val customView: View = inflater.inflate(R.layout.rate_d, null)
        massageBox.setContentView(customView)
        massageBox.window?.setGravity(Gravity.BOTTOM)
        massageBox.window?.setLayout(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        ADSNativeDisplay.loadAdmobNativeAdBig(
            ADSMainClass.getStringValue(EXIT_SCREEN_NATIVE),
            massageBox.findViewById(R.id.flNativeSmallPlaceholder),
            massageBox.findViewById(R.id.shimmer_container_small),
            "big",
            this
        )

        massageBox.window?.attributes?.windowAnimations = R.style.ExitDialogAnimation
        massageBox.setCancelable(true)
        massageBox.setCanceledOnTouchOutside(
            true
        )
        val tvDone = massageBox.findViewById<View?>(R.id.okay) as TextView
        tvDone.setOnClickListener {
            ADSMainClass.IS_AD_SHOWING = 0
            activity.finishAffinity()
        }
        massageBox.show()
    }

    private fun checkPermissions(showCustomDialog: Boolean) {
        permissionDialog?.dismiss()
        val missingPermissions = contactPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.filter {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && it == Manifest.permission.POST_NOTIFICATIONS) {
                !SharedPreferenceManager.getBoolean(this, Constance.NOTIFICATION_PERMISSION_SKIP)
            } else {
                true
            }
        }

        if (missingPermissions.isNotEmpty()) {
            if (showCustomDialog) {
                permissionDialog = PermissionManager.openPermissionDialog(this) {
                    isFromPermissionRequest = true
                    requestPermissionLauncher.launch(contactPermissions)
                }
            } else {
                permissionDialog = PermissionManager.openPermissionDialog(this) {
                    isFromPermissionRequest = true
                    requestPermissionLauncher.launch(contactPermissions)
                }
            }
        } else if (!PermissionManager.hasOverlayPermission(this) && !SharedPreferenceManager.getBoolean(
                this,
                Constance.OVERLAY_PERMISSION_SKIP
            )
        ) {

            if (showCustomDialog) {
                permissionDialog = PermissionManager.openPermissionDialog(this) {
                    isFromPermissionRequest = true
                    SharedPreferenceManager.putBoolean(
                        this,
                        Constance.OVERLAY_PERMISSION_SKIP,
                        true
                    )
                    ADSAppManage.isAppOpenBlocked = true
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:$packageName".toUri()
                    )
                    overlayPermissionLauncher.launch(intent)
                }
            } else {
                // Just allowed Contacts/Call Log: go directly to Overlay settings for a seamless experience
                isFromPermissionRequest = true
                SharedPreferenceManager.putBoolean(this, Constance.OVERLAY_PERMISSION_SKIP, true)
                ADSAppManage.isAppOpenBlocked = true
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri()
                )
                overlayPermissionLauncher.launch(intent)
            }
        }

        if (PermissionManager.hasContactPermissions(this)) {
            initView()
        }
    }


    private fun initView() {
        if (isViewInitialized) return
        isViewInitialized = true

        binding.onClickHandler = this

        setupFragments()
        handleBackPress()
        handleIntent(intent)
        initialize(this)
        loadAds()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isViewInitialized) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent?) {
        val openTab = intent?.getStringExtra("open_tab")
        when (openTab) {
            "recents" -> {
                if (::recentsFragment.isInitialized) {
                    switchFragments(recentsFragment)
                    updateTabUI(binding.llRecents)
                }
            }

            "contacts" -> {
                if (::contactsFragment.isInitialized) {
                    switchFragments(contactsFragment)
                    updateTabUI(binding.llContacts)
                }
            }
        }
    }

    private fun setupFragments() {
        val fragmentManager = supportFragmentManager

        // Try to find existing fragments by tag to handle activity recreation correctly
        val existingRecents = fragmentManager.findFragmentByTag("recents") as? RecentsFragment
        val existingContacts = fragmentManager.findFragmentByTag("contacts") as? ContactsFragment
        val existingFavorites = fragmentManager.findFragmentByTag("favorites") as? FavoritesFragment
        val existingKeypad = fragmentManager.findFragmentByTag("keypad") as? KeypadFragment

        recentsFragment = existingRecents ?: RecentsFragment()
        contactsFragment = existingContacts ?: ContactsFragment()
        favoritesFragment = existingFavorites ?: FavoritesFragment()
        keypadFragment = existingKeypad ?: KeypadFragment()

        val transaction = fragmentManager.beginTransaction()

        // Add fragments if they are not already in the FragmentManager
        if (!recentsFragment.isAdded) transaction.add(
            binding.llContainer.id,
            recentsFragment,
            "recents"
        )
        if (!contactsFragment.isAdded) transaction.add(
            binding.llContainer.id,
            contactsFragment,
            "contacts"
        ).hide(contactsFragment)
        if (!favoritesFragment.isAdded) transaction.add(
            binding.llContainer.id,
            favoritesFragment,
            "favorites"
        ).hide(favoritesFragment)
        if (!keypadFragment.isAdded) transaction.add(
            binding.llContainer.id,
            keypadFragment,
            "keypad"
        ).hide(keypadFragment)

        // Determine which fragment should be active
        // If we are recreating, try to find the one that is not hidden
        val restoredActive = when {
            existingRecents != null && !existingRecents.isHidden -> existingRecents
            existingContacts != null && !existingContacts.isHidden -> existingContacts
            existingFavorites != null && !existingFavorites.isHidden -> existingFavorites
            existingKeypad != null && !existingKeypad.isHidden -> existingKeypad
            else -> recentsFragment
        }

        activeFragment = restoredActive
        transaction.show(activeFragment)

        // Ensure the correct tab is highlighted
        val selectedTab = when (activeFragment) {
            favoritesFragment -> binding.llFavorite
            contactsFragment -> binding.llContacts
            keypadFragment -> binding.llKeypad
            else -> binding.llRecents
        }

        transaction.commitNow()
        updateTabUI(selectedTab)
    }


    private fun updateTabUI(selected: View) {

        val tabs = listOf(
            binding.llFavorite,
            binding.llRecents,
            binding.llContacts,
            binding.llKeypad
        )

        tabs.forEach { tab ->

            val textView = tab.getChildAt(1) as? TextView
            val imageView = tab.getChildAt(0) as? ImageView

            if (tab == selected) {
//                tab.alpha = 1f
                textView?.setTextColor(getColor(R.color.main_color))
                imageView?.let {
                    ImageViewCompat.setImageTintList(
                        it,
                        ColorStateList.valueOf(getColor(R.color.main_color))
                    )
                }

            } else {
//                tab.alpha = 0.5f
                textView?.setTextColor(getColor(R.color.grey_color))
                imageView?.let {
                    ImageViewCompat.setImageTintList(
                        it,
                        ColorStateList.valueOf(getColor(R.color.grey_color))
                    )
                }
            }
        }

        selectedTab = selected
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        if (selectedTab == view) return

        when (view.id) {
            binding.llFavorite.id -> {
                switchFragments(favoritesFragment)
                updateTabUI(binding.llFavorite)
            }

            binding.llRecents.id -> {
                switchFragments(recentsFragment)
                updateTabUI(binding.llRecents)
            }

            binding.llContacts.id -> {
                switchFragments(contactsFragment)
                updateTabUI(binding.llContacts)
            }

            binding.llKeypad.id -> {
                switchFragments(keypadFragment)
                updateTabUI(binding.llKeypad)
            }
        }
    }

    private fun switchFragments(target: Fragment) {
        if (activeFragment == target) return

        when (target) {
            is RecentsFragment -> target.clearSearch()
            is ContactsFragment -> target.clearSearch()
            is FavoritesFragment -> target.clearSearch()
        }

        supportFragmentManager.beginTransaction().hide(activeFragment).show(target).commit()
        activeFragment = target
    }

    private fun loadAds() {

        if (ADSMainClass.getHomeScreenBottomShow()) {
            if (ADSMainClass.getHomeScreenAdsType().equals("native")) {
                ADSNativeDisplay.loadAdmobNativeAdBig(
                    ADSMainClass.getStringValue(ADSMainClass.HOME_SCREEN_NATIVE),
                    findViewById(R.id.flNativeSmallPlaceholder),
                    findViewById(R.id.shimmer_container_banner),
                    "small",
                    this
                )
            } else {
                ADSBannerSmall.loadAdMobBanner(
                    ADSMainClass.getStringValue(ADSMainClass.HOME_SCREEN_BANNER),
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

        if (!ADSMainClass.getAdsTypeManage()
                .equals("Load") && ADSUtilitis.IsNetworkConnected(this@HomeActivity)
        ) {
            ADSInterDisplayClick.AdmobInterstitialAd(
                this@HomeActivity,
                ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME)
            )
        }
    }

    fun initialize(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(context)
        }
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                if (activeFragment !is RecentsFragment) {
                    switchFragments(recentsFragment)
                    updateTabUI(binding.llRecents)

                } else {
                    if (doubleBackToExitPressedOnce) {
                        finish()
                        return
                    }

                    doubleBackToExitPressedOnce = true

                    if (ADSMainClass.getExitAds()) {
                        exitApp(this@HomeActivity)
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        doubleBackToExitPressedOnce = false
                    }, 2000)
                }
            }
        })
    }


}