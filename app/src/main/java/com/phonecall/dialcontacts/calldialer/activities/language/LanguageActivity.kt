package com.phonecall.dialcontacts.calldialer.activities.language

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSBannerSmall
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSInterDisplay
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSInterDisplayClick
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass.INTER_FIRST_TIME
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSNativeDisplay
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSUtilitis
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.home.HomeActivity
import com.phonecall.dialcontacts.calldialer.activities.permissions.PermissionActivity
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
    private var showIcon = false
    private var pendingLanguageToApply: String? = null
    private var shouldFinishOnResume = false
    private var pendingNavigation: (() -> Unit)? = null

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

        if (ADSMainClass.getLanguageScreenBottomAdShow()) {
            Log.e("TAG", "onCreate: 1111" )
            if (ADSMainClass.getLanguageAdsType().equals("native")) {

                Log.e("TAG", "onCreate: 222" )
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

        val currentLangCode = LanguageManager.getCurrentLanguage()
        val isAlreadyLoggedIn = SharedPreferenceManager.getBoolean(this, Constance.IS_LOG_IN)

        if (!currentLangCode.isNullOrEmpty()) {
            languageList.forEach {
                it.isSelected = it.code == currentLangCode
            }
        } else if (isAlreadyLoggedIn) {
            languageList.forEach {
                it.isSelected = it.code == "en"
            }
        }

        if (ADSMainClass.getLanguageInterAdsShow() && !ADSMainClass.getAdsTypeManage()
                .equals("Load") && ADSUtilitis.IsNetworkConnected(this@LanguageActivity)
        ) {
            ADSInterDisplay.AdmobInterstitialAd(
                this,
                ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME)
            )
        }

        adapter = LanguageAdapter(languageList)
        binding.rvLanguages.adapter = adapter
        binding.rvLanguages.layoutManager = LinearLayoutManager(this)

        showIcon = intent.getBooleanExtra("language", false)

        if (showIcon) {
            binding.ivBack.visibility = View.VISIBLE
        } else {
            binding.ivBack.visibility = View.GONE
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        })
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

                SharedPreferenceManager.putBoolean(this, Constance.IS_LOG_IN, true)

                /*val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity()*/

                if (ADSMainClass.getLanguageInterAdsShow()) {
                    ADSInterDisplay.ADSInterstitialShowing(
                        this@LanguageActivity,
                        ADSMainClass.getStringValue(INTER_FIRST_TIME),
                        { msg ->
                            Log.e("TAG", "onClick: 11111 " + msg)
                            pendingLanguageToApply = selectedLanguage.code
                            if (showIcon) {
                                Log.e("TAG", "onClick: 2222 ")
                                shouldFinishOnResume = true
                            } else {
                                Log.e("TAG", "onClick: 3333 ")
                                pendingNavigation = {
                                    if (ADSMainClass.getSplashToLanguage()) {
                                        if (!ADSMainClass.getLanguageScreen()) {
                                            Log.e("TAG", "onClick: 44444 ")
                                            val intent = Intent(this@LanguageActivity, PermissionActivity::class.java)
                                            intent.putExtra("language", true)
                                            startActivity(intent)
                                            finish()
                                        }
                                    } else {
                                        Log.e("TAG", "onClick: 55555 ")
                                        ADSMainClass.setLanguageScreen(true)
                                        val firebaseAnalytics = FirebaseAnalytics.getInstance(this@LanguageActivity)
                                        val bundle = Bundle()
                                        bundle.putBoolean("RemoveLanguageFromBack", true)
                                        firebaseAnalytics.logEvent("RemoveLanguageFromBack", bundle)
                                        startActivity(Intent(this@LanguageActivity, HomeActivity::class.java))
                                        finish()
                                    }
                                }
                            }
                        })

                } else {
                    Log.e("TAG", "onClick: 66666 ")
                    if (ADSMainClass.getSplashToLanguage()) {
                        if (!ADSMainClass.getLanguageScreen()) {
                            Log.e("TAG", "onClick: 77777 ")
                            val intent = Intent(this, PermissionActivity::class.java)
                            intent.putExtra("language", true)
                            startActivity(intent)
                            finish()
                            LanguageManager.setLanguage(selectedLanguage.code)
                        }
                    } else {
                        Log.e("TAG", "onClick: 8888 ")
                        ADSMainClass.setLanguageScreen(true)
                        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
                        val bundle = Bundle()
                        bundle.putBoolean("RemoveLanguageFromBack", true)
                        firebaseAnalytics.logEvent("RemoveLanguageFromBack", bundle)
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                        LanguageManager.setLanguage(selectedLanguage.code)
                    }
                }
            }

            binding.ivBack.id -> {
                onBack()
            }
        }
    }

    fun onBack() {
        val currentLangCode = LanguageManager.getCurrentLanguage()
        val needsDefaultLang = currentLangCode.isNullOrEmpty()

        if (showIcon) {
            ADSInterDisplayClick.ADSBackDisplayInterstitial(
                this@LanguageActivity,
                ADSMainClass.getStringValue(ADSMainClass.INTER_SECOND_TIME),
                { _ ->
                    if (needsDefaultLang) {
                        pendingLanguageToApply = "en"
                        SharedPreferenceManager.putBoolean(this@LanguageActivity, Constance.IS_LOG_IN, true)
                        shouldFinishOnResume = true
                    } else {
                        finish()
                    }
                })
            return
        }

        if (ADSMainClass.getSplashToLanguage()) {
            if (!ADSMainClass.getLanguageScreen()) {
                val intent = Intent(this, PermissionActivity::class.java)
                intent.putExtra("language", true)
                startActivity(intent)
                finish()
                if (needsDefaultLang) {
                    LanguageManager.setLanguage("en")
                    SharedPreferenceManager.putBoolean(this, Constance.IS_LOG_IN, true)
                }
            }
        } else {
            ADSMainClass.setLanguageScreen(true)
            val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
            val bundle = Bundle()
            bundle.putBoolean("RemoveLanguageFromBack", true)
            firebaseAnalytics.logEvent("RemoveLanguageFromBack", bundle)
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            if (needsDefaultLang) {
                LanguageManager.setLanguage("en")
                SharedPreferenceManager.putBoolean(this, Constance.IS_LOG_IN, true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Common.hideSystemUI(this)

        val langCode = pendingLanguageToApply
        if (langCode != null) {
            pendingLanguageToApply = null
            if (shouldFinishOnResume) {
                shouldFinishOnResume = false
                finish()
                LanguageManager.setLanguage(langCode)
            } else {
                val nav = pendingNavigation
                pendingNavigation = null
                nav?.invoke()
                LanguageManager.setLanguage(langCode)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Dismiss the ad-loading dialog while the window is still valid.
        // This prevents WindowLeaked when the Activity navigates away or is
        // destroyed while the interstitial ad is still loading in the background.
        try {
            ADSUtilitis.MassageBoxFullDismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        // Dismiss BEFORE super.onDestroy() — after super the window is torn down
        // and dismissing would have no effect (or could throw).
        try {
            ADSUtilitis.MassageBoxFullDismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}