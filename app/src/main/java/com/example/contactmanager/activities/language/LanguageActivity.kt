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
        LanguageModel(R.drawable.ic_flag_english, "English", "English", "United States", "en"),
        LanguageModel(R.drawable.ic_flag_india, "हिंदी", "Hindi", "India", "hi"),
        LanguageModel(R.drawable.ic_flag_french, "Français", "French", "France", "fr"),
        LanguageModel(R.drawable.ic_flag_japanese, "日本語", "Japanese", "Japan", "ja"),
        LanguageModel(R.drawable.ic_flag_spanish, "Español", "Spanish", "Spain", "es"),
        LanguageModel(R.drawable.ic_flag_korean, "한국어", "Korean", "South Korea", "ko"),
        LanguageModel(R.drawable.ic_flag_italian, "Italiano", "Italian", "Italy", "it"),
        LanguageModel(R.drawable.ic_flag_russian, "Русский", "Russian", "Russia", "ru"),
        LanguageModel(R.drawable.ic_flag_german, "Deutsch", "German", "Germany", "de"),
        LanguageModel(R.drawable.ic_flag_nepali, "नेपाली", "Nepali", "Nepal", "ne"),
        LanguageModel(R.drawable.ic_flag_chinese, "中文", "Chinese", "China", "zh"),
        LanguageModel(R.drawable.ic_flag_thai, "ไทย", "Thai", "Thailand", "th"),
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

        val currentLangCode = LanguageManager.getCurrentLanguage()
        languageList.forEach {
            it.isSelected = it.code == currentLangCode
        }
        if (languageList.none { it.isSelected }) {
            languageList.firstOrNull()?.isSelected = true
        }

        adapter = LanguageAdapter(languageList)
        binding.rvLanguages.adapter = adapter
        binding.rvLanguages.layoutManager = LinearLayoutManager(this)

    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.cvDone.id -> {
                val selectedLanguage = adapter.getSelectedLanguage()

                LanguageManager.setLanguage(selectedLanguage.code)

                SharedPreferenceManager.putBoolean(this, Constance.IS_LOG_IN, true)

                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity()
            }
        }
    }
}