package com.example.contactmanager.activities.settings

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.contactmanager.R
import com.example.contactmanager.databinding.ActivitySettingsBinding
import com.example.contactmanager.utils.OnClickHandler

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

    private fun initView() {
        binding.onClickHandler = this
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.ivBack.id -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}