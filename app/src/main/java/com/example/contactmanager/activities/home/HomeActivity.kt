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
        Manifest.permission.WRITE_CALL_LOG
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isFromPermissionRequest = true
        if (permissions.all { it.value }) {
            checkPermissions(showCustomDialog = false)
        } else {
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
                isFromPermissionRequest = false
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT)
                    .show()
                checkPermissions(showCustomDialog = true)
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
        checkPermissions(showCustomDialog = true)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If permissions are missing, remove any restored fragments to prevent them from
        // initializing ViewModels that might access ContentProviders and cause crashes.
        if (!PermissionManager.hasPermissions(this)) {
            supportFragmentManager.fragments.forEach { fragment ->
                supportFragmentManager.beginTransaction().remove(fragment).commitNow()
            }
        }

        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // initView() is now called from checkPermissions() after all permissions are granted
    }

    override fun onResume() {
        super.onResume()
        if (!isFromPermissionRequest) {
            checkPermissions(showCustomDialog = true)
        }
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
            binding.llContainer.visibility = View.INVISIBLE
            if (showCustomDialog) {
                permissionDialog = PermissionManager.openPermissionDialog(this) {
                    isFromPermissionRequest = true
                    requestPermissionLauncher.launch(contactPermissions)
                }
            } else {
                // If we are here, we are likely in a sequence, but standard permissions are still missing.
                // To be safe, we show the dialog instead of auto-launching to avoid system dialog loops.
                permissionDialog = PermissionManager.openPermissionDialog(this) {
                    isFromPermissionRequest = true
                    requestPermissionLauncher.launch(contactPermissions)
                }
            }
        } else if (!PermissionManager.hasOverlayPermission(this)) {
            binding.llContainer.visibility = View.INVISIBLE
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
        } else {
            binding.llContainer.visibility = View.VISIBLE
            initView()
        }
    }


    private fun initView() {
        if (isViewInitialized) return
        isViewInitialized = true

        binding.onClickHandler = this

        setupFragments()
        handleBackPress()
    }

    private fun setupFragments() {

        favoritesFragment = FavoritesFragment()
        recentsFragment = RecentsFragment()
        contactsFragment = ContactsFragment()
        keypadFragment = KeypadFragment()

        val transaction = supportFragmentManager.beginTransaction()

        transaction.add(binding.llContainer.id, recentsFragment)
        activeFragment = recentsFragment
        updateTabUI(binding.llRecents)
        transaction.commit()

        Handler(Looper.getMainLooper()).postDelayed({
            val lazyTransaction = supportFragmentManager.beginTransaction()
            if (activeFragment != recentsFragment) lazyTransaction.add(
                binding.llContainer.id,
                recentsFragment
            ).hide(recentsFragment)
            if (activeFragment != contactsFragment) lazyTransaction.add(
                binding.llContainer.id,
                contactsFragment
            ).hide(contactsFragment)
            if (activeFragment != favoritesFragment) lazyTransaction.add(
                binding.llContainer.id,
                favoritesFragment
            ).hide(favoritesFragment)
            if (activeFragment != keypadFragment) lazyTransaction.add(
                binding.llContainer.id,
                keypadFragment
            ).hide(keypadFragment)
            lazyTransaction.commitAllowingStateLoss()
        }, 500)
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finishAffinity()
    }

}