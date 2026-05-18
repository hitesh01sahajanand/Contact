package com.phonecall.dialcontacts.calldialer.activities.language

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSBannerSmall
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSNativeDisplay
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.home.HomeActivity
import com.phonecall.dialcontacts.calldialer.adapters.LanguageAdapter
import com.phonecall.dialcontacts.calldialer.databinding.ActivityLanguageBinding
import com.phonecall.dialcontacts.calldialer.models.LanguageModel
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.LanguageManager
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.utils.SharedPreferenceManager
import com.phonecall.dialcontacts.calldialer.utils.ThemeManager
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
        ThemeManager.applyAppTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_language)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Common.hideSystemUI(this)
        initView()
    }

    private fun initView() {
        binding.onClickHandler = this

        val currentLangCode = LanguageManager.getCurrentLanguage()
        if (!currentLangCode.isNullOrEmpty()) {
            languageList.forEach {
                it.isSelected = it.code == currentLangCode
            }
        }

        adapter = LanguageAdapter(languageList)
        binding.rvLanguages.adapter = adapter
        binding.rvLanguages.layoutManager = LinearLayoutManager(this)

        if (ADSMainClass.getLanguageScreenBottomAdShow()) {
            if (ADSMainClass.getLanguageAdsType().equals("native")) {
                ADSNativeDisplay.loadAdmobNativeAdBig(
                    ADSMainClass.getStringValue(ADSMainClass.LANGUAGE_SCREEN_NATIVE),
                    findViewById(R.id.flNativeSmallPlaceholder),
                    findViewById(R.id.shimmer_container_small),
                    "big",
                    this
                )
            } else {
                ADSBannerSmall.loadAdMobBanner(
                    ADSMainClass.getStringValue(ADSMainClass.LANGUAGE_SCREEN_BANNER),
                    findViewById(R.id.flBannerSmallPlaceholder),
                    findViewById(R.id.shimmer_container_small),
                    this
                )
            }
        } else {
            findViewById<View>(R.id.shimmer_container_small).visibility = View.GONE
            findViewById<View>(R.id.flNativeSmallPlaceholder).visibility = View.GONE
            findViewById<View>(R.id.flBannerSmallPlaceholder).visibility = View.GONE
        }
//    }

//        Common.loadBottomAds(
//            this, ADSMainClass.LANGUAGE_SCREEN_NATIVE, ADSMainClass.LANGUAGE_SCREEN_BANNER
//        )

    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        when (view.id) {
            binding.cvDone.id -> {
                val selectedLanguage = if (adapter.getSelectedPosition() != -1) {
                    adapter.getSelectedLanguage()
                } else {
                    null
                }

                if (selectedLanguage == null) {
                    // Optionally show a toast or message
                    Toast.makeText(
                        this, getString(R.string.please_select_language), Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val currentLangCode = LanguageManager.getCurrentLanguage()
                val isAlreadyLoggedIn =
                    SharedPreferenceManager.getBoolean(this, Constance.IS_LOG_IN)

                if (selectedLanguage.code == currentLangCode && isAlreadyLoggedIn) {
                    finish()
                    return
                }

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