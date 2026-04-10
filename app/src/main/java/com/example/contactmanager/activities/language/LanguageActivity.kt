package com.example.contactmanager.activities.language

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.activities.home.HomeActivity
import com.example.contactmanager.adapters.LanguageAdapter
import com.example.contactmanager.databinding.ActivityLanguageBinding
import com.example.contactmanager.models.LanguageModel
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.LanguageManager
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.SharedPreferenceManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityLanguageBinding
    private lateinit var adapter: LanguageAdapter

    val languageList = mutableListOf(
        LanguageModel("English", "United States", "en", true),
        LanguageModel("Hindi", "India", "hi", false)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_language)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
    }

    private fun initView() {
        binding.onClickHandler = this

        adapter = LanguageAdapter(languageList)
        binding.rvLanguages.adapter = adapter
        binding.rvLanguages.layoutManager = LinearLayoutManager(this)

    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.cvDone.id -> {

                val selectedLanguage = adapter.getSelectedLanguage()

                when (selectedLanguage.code) {
                    "en" -> LanguageManager.setEnglish()
                    "hi" -> LanguageManager.setHindi()
                }

                SharedPreferenceManager.putBoolean(this, Constance.IS_LOG_IN, true)

                startActivity(Intent(this, HomeActivity::class.java))
                finishAffinity()
            }
        }
    }
}