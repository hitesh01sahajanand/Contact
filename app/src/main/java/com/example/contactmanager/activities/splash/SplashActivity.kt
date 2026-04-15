package com.example.contactmanager.activities.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.contactmanager.R
import com.example.contactmanager.activities.home.HomeActivity
import com.example.contactmanager.activities.permissions.PermissionActivity
import com.example.contactmanager.databinding.ActivityMainBinding
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.SharedPreferenceManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

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
        Handler(mainLooper).postDelayed({
            val isLogIN = SharedPreferenceManager.getBoolean(this, Constance.IS_LOG_IN)

            if (!isLogIN) {
                startActivity(Intent(this, PermissionActivity::class.java))
                finish()
            } else {
                startActivity(Intent(this, HomeActivity::class.java))
                finishAffinity()
            }
        }, 1000)

    }
}