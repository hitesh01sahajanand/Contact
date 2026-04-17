package com.example.contactmanager.activities.home

import android.app.ComponentCaller
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager.isDefaultDialer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var favoritesFragment: FavoritesFragment
    private lateinit var recentsFragment: RecentsFragment
    private lateinit var contactsFragment: ContactsFragment
    private lateinit var keypadFragment: KeypadFragment
    private lateinit var activeFragment: Fragment

    private var doubleBackToExitPressedOnce = false
    private var isDefaultDialerApp = false
    private var selectedTab: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
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

    private fun initView() {
        binding.onClickHandler = this
        isDefaultDialerApp = isDefaultDialer(this)

        setupFragments()
        handleBackPress()
    }

    private fun setupFragments() {

        val isDialer = intent.getBooleanExtra(Constance.IS_DIALER, false)

        favoritesFragment = FavoritesFragment()
        recentsFragment = RecentsFragment()
        contactsFragment = ContactsFragment()
        keypadFragment = KeypadFragment()

        val transaction = supportFragmentManager.beginTransaction()

        transaction.add(binding.llContainer.id, recentsFragment).hide(recentsFragment)
        transaction.add(binding.llContainer.id, contactsFragment).hide(contactsFragment)
        transaction.add(binding.llContainer.id, favoritesFragment).hide(favoritesFragment)
        transaction.add(binding.llContainer.id, keypadFragment).hide(keypadFragment)

        if (isDefaultDialerApp) {
            transaction.show(recentsFragment)
            activeFragment = recentsFragment
            updateTabUI(binding.llRecents)
        } else {
            transaction.show(keypadFragment)
            activeFragment = keypadFragment
            updateTabUI(binding.llKeypad)
        }

        if (isDialer) {
            transaction.show(keypadFragment)
            activeFragment = keypadFragment
            updateTabUI(binding.llKeypad)
        }

        transaction.commit()
    }

    private fun updateTabUI(selected: View) {

        val tabs = listOf(
            binding.llFavorite,
            binding.llRecents,
            binding.llContacts,
            binding.llKeypad,
//            binding.llSettings
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
        isDefaultDialerApp = isDefaultDialer(this)

        if (!isDefaultDialerApp && view.id != binding.llKeypad.id) {
            Toast.makeText(this, "Set as default app first", Toast.LENGTH_SHORT).show()
            return
        }

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
                        "Press back again to exit",
                        Toast.LENGTH_SHORT
                    ).show()

                    Handler(Looper.getMainLooper()).postDelayed({
                        doubleBackToExitPressedOnce = false
                    }, 2000)
                }
            }
        })
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)

        Log.e("TAG", "onActivityResult: $requestCode")
        if (requestCode == 100) {

        }
    }

}