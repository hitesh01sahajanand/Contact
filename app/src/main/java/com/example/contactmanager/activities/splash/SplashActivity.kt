package com.example.contactmanager.activities.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.contactmanager.R
import com.example.contactmanager.activities.home.HomeActivity
import com.example.contactmanager.activities.permissions.PermissionActivity
import com.example.contactmanager.databinding.ActivityMainBinding
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.PermissionManager
import com.example.contactmanager.utils.SendData
import com.example.contactmanager.utils.SharedPreferenceManager
import com.example.contactmanager.viewmodels.RecentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: RecentViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
    }

    private fun initView() {

        val isPermissionGranted = PermissionManager.hasPermissions(this)
        if (isPermissionGranted) {

            viewModel.loadAllRecentsHistory(0, 1000)
            viewModel.allRecentCallHistory.observe(this) { recentList ->
                if (recentList.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        SendData.allRecentCallHistory.postValue(recentList)
                    }

                    SendData.isFirstTime = true
                    goToNextScreen(100)
                } else {
                    goToNextScreen(3000)
                }
            }
        } else {
            goToNextScreen(3000)
        }

    }

    fun goToNextScreen(time: Long) {
        Handler(mainLooper).postDelayed({
            val isLogIN = SharedPreferenceManager.getBoolean(this, Constance.IS_LOG_IN)

            if (!isLogIN) {
                startActivity(Intent(this, PermissionActivity::class.java))
                finish()
            } else {
                startActivity(Intent(this, HomeActivity::class.java))
                finishAffinity()
            }
        }, time)
    }
}