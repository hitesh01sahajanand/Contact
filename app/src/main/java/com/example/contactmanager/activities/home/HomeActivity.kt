package com.example.contactmanager.activities.home

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.contactmanager.R
import com.example.contactmanager.databinding.ActivityHomeBinding
import com.example.contactmanager.fragments.contacts.ContactsFragment
import com.example.contactmanager.fragments.favorites.FavoritesFragment
import com.example.contactmanager.fragments.keypad.KeypadFragment
import com.example.contactmanager.fragments.recents.RecentsFragment
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.net.toUri
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.SharedPreferenceManager
import com.example.contactmanager.utils.ThemeManager

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

    private val contactPermissions = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isFromPermissionRequest = true
        if (!permissions.all { it.value }) {
            // Check if any of the denied permissions are permanently denied (user clicked "Don't ask again")
            val isPermanentlyDenied = contactPermissions.any {
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

    private fun checkPermissions(showCustomDialog: Boolean) {
        permissionDialog?.dismiss()
        val missingPermissions = contactPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
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
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:$packageName".toUri()
                    )
                    overlayPermissionLauncher.launch(intent)
                }
            } else {
                // Just allowed Contacts/Call Log: go directly to Overlay settings for a seamless experience
                isFromPermissionRequest = true
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

            /* binding.llSettings.id -> {
                 switchFragments(settingsFragment)
                 updateTabUI(binding.llSettings)
             }*/
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
                    Toast.makeText(
                        this@HomeActivity,
                        getString(R.string.press_back_again_to_exit),
                        Toast.LENGTH_SHORT
                    ).show()

                    Handler(Looper.getMainLooper()).postDelayed({
                        doubleBackToExitPressedOnce = false
                    }, 2000)
                }
            }
        })
    }


}