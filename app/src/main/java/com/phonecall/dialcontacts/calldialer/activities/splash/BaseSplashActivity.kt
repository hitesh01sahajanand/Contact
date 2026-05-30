package com.phonecall.dialcontacts.calldialer.activities.splash

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.addLoggingBehavior
import com.facebook.FacebookSdk.fullyInitialize
import com.facebook.FacebookSdk.sdkInitialize
import com.facebook.FacebookSdk.setAutoInitEnabled
import com.facebook.FacebookSdk.setAutoLogAppEventsEnabled
import com.facebook.FacebookSdk.setClientToken
import com.facebook.FacebookSdk.setIsDebugEnabled
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSAppManage
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSAppStarting
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass.SHOW_FULL_SCREEN_INTENT_PERMISSION
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSUtilitis
import com.phonecall.dialcontacts.calldialer.Advertisement.MyApplication
import com.phonecall.dialcontacts.calldialer.BuildConfig
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.utils.ManegeUtilsView
import com.phonecall.dialcontacts.calldialer.utils.ManegeUtilsView.ssfsfsfsf
import com.posthog.PostHog
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

abstract class
BaseSplashActivity : AppCompatActivity() {
    lateinit var decrypted: String
    var iv_firebaseRemoteConfig: FirebaseRemoteConfig? = null
    private lateinit var version: String


    var resoAudience_Category: MutableList<Int> = ArrayList()
    var resoContent_Category: MutableList<Int> = ArrayList()
    var resosetCountryInterestList: MutableList<String> = ArrayList()

    abstract fun initActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LOW_PROFILE
                )
//        setContentView(R.layout.activity_main)
//        try {
//            Check_Install_Referrer()
//        } catch (e: Exception) {
//        }
        FirebaseApp.initializeApp(this)
        ADSAppManage.FastStart = true
        ADSAppStarting.ADSIsShowing = false
        ADSAppManage.AppStartingScreenOpen = false


//         val original = "_AppVersion_"
//        val encrypted = AESUtil.encrypt(original)


//        Log.d("ddddd", "Encrypted: $encrypted")
//        Log.d("ddddd", "Decrypted: $decrypted")

//        val result = versionName?.replace(".", "_")
//        val versionCode = BuildConfig.VERSION_CODE
//        ManegeUtilsView.isUtilsManege(this)
//        val decrypted = AESUtil.decrypt("8iMtOPuhnN4
//        PAAstYlIAnw==" ?: "")

        //             RQUIAOSLDKCMXJSU
//        val encrypt =  fe("RQUIAOSLDKCMXJSU","appVersionName")
        ManegeUtilsView.isUtilsManege(this)
        val versionName: String =
            packageManager.getPackageInfo(packageName, 0).versionCode.toString()

        decrypted = ssfsfsfsf + versionName
//        Log.e("TAG", "onCreate: $encrypt  & $decrypted", )
//
//        version = decrypted + result
//
//
//
//        Log.d("ddddd", "versionName: $versionName")
//        Log.d("ddddd", "result: $result")
//        Log.d("ddddd", "result:decrypted  $decrypted")
//        Log.d("ddddd", "result:version $version")


        showing_to_all_data()
        //Key Hash
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures!!) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT))
            }
        } catch (e: PackageManager.NameNotFoundException) {
        } catch (e: NoSuchAlgorithmException) {
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun showing_to_all_data() {
        if (!ADSUtilitis.IsNetworkConnected(this)) {
            Handler(Looper.getMainLooper()).postDelayed({
                app_next_screen_showing()
            }, 500)
            return
        }
        iv_firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings =
            FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build()
        iv_firebaseRemoteConfig!!.setConfigSettingsAsync(configSettings)

        iv_firebaseRemoteConfig!!.setDefaultsAsync(R.xml.default_config)

        iv_firebaseRemoteConfig!!.fetchAndActivate()
            .addOnCompleteListener(this, OnCompleteListener { task ->
                if (task.isSuccessful) {
                    try {
                        val jsonobject: JSONObject =
                            JSONArray(iv_firebaseRemoteConfig!!.getString(decrypted)).getJSONObject(
                                0
                            )
                        val admob_native_btn_color =
                            jsonobject.getString("Admob_Native_ButtonColor")
                        val admob_native_btn_text_color =
                            jsonobject.getString("Admob_Native_ButtonTextColor")
                        val ads_type_load = jsonobject.getString("Ads_Loading_Types")
                        val application_id = jsonobject.getString("ApplicationId")
                        val appliction_token = jsonobject.getString("ApplicationToken")
                        val bottom_ads_type = jsonobject.getString("Bottom_AdTypes")
                        val privacy_policy = jsonobject.getString("App_PrivacyPolicy")
                        val splash_ads_type = jsonobject.getString("Splash_Ad_LoadType")
                        val ads_click = jsonobject.getLong("ADSClick")
                        val back_click = jsonobject.getLong("BacksClick")
                        val native_by_page = jsonobject.getLong("Native_ByPage")
                        val ads_blocker = jsonobject.getBoolean("AdsBlocks")
                        val coming_soon = jsonobject.getBoolean("Coming_Soon")
                        val exit_ads = jsonobject.getBoolean("ExitDialog")
                        val firebaseanalytics = jsonobject.getBoolean("Firebaseanalytics")
                        val update_coming_soon = jsonobject.getBoolean("Update_Coming_Soon")


                        val screen = jsonobject.getJSONObject("screen")

                        // Splash Screen
                        val splash_screen = screen.getJSONObject("splash_screen")
                        val splah_app_open_ads_show =
                            splash_screen.getBoolean("splash_app_open_ads_show")
                        val splash_app_open_after_count =
                            splash_screen.getInt("splash_app_open_after_count")
                        val app_open_show_in_background =
                            splash_screen.getBoolean("app_open_show_in_background")
                        val app_open_daily_show_count =
                            splash_screen.getInt("app_open_daily_show_count")
                        val app_open_id = splash_screen.getString("app_open_id")
                        ADSMainClass.setSplashAppOpenShow(splah_app_open_ads_show)
                        ADSMainClass.setSplashAppOpenAfterCount(splash_app_open_after_count)
                        ADSMainClass.setAppOpenBackgroundShow(app_open_show_in_background)
                        ADSMainClass.setAppOpenAdDailyLimit(app_open_daily_show_count)

                        // Language Screen
                        val language_screen = screen.getJSONObject("language_screen")
                        val splash_to_language = language_screen.getBoolean("splash_to_language")
                        val language_screen_bottom_ad_show =
                            language_screen.getBoolean("language_screen_bottom_ad_show")
                        val language_ads_type = language_screen.getString("language_ads_type")
                        val language_inter_ads_show =
                            language_screen.getBoolean("language_inter_ads_show")
                        val language_banner_id = language_screen.getString("language_banner_id")
                        val language_native_id = language_screen.getString("language_native_id")
                        ADSMainClass.setSplashToLanguage(splash_to_language)
                        ADSMainClass.setLanguageScreenBottomAdShow(language_screen_bottom_ad_show)
                        ADSMainClass.setLanguageAdsType(language_ads_type)
                        ADSMainClass.setLanguageInterAdsShow(language_inter_ads_show)

                        // Permission Screen
                        val permission_screen = screen.getJSONObject("permission_screen")
//                        val permission_small_ads_show = permission_screen.getBoolean("permission_bottom_ads_show")
                        val permission_ads_type = permission_screen.getString("permission_ads_type")
                        val permission_native_id =
                            permission_screen.getString("permission_native_id")
                        val permission_banner_id =
                            permission_screen.getString("permission_banner_id")
//                        ADSMainClass.setPermissionSmallAdsShow(permission_small_ads_show)
                        ADSMainClass.setPermissionAdsType(permission_ads_type)

                        // Home Screen
                        val home_screen = screen.getJSONObject("home_screen")
                        val home_screen_bottom_show =
                            home_screen.getBoolean("home_screen_bottom_show")
                        val home_screen_ads_type = home_screen.getString("home_screen_ads_type")
                        val home_banner_id = home_screen.getString("home_banner_id")
                        val home_native_id = home_screen.getString("home_native_id")
//                        val home_screen_native1 = home_screen.getString("home_screen_native1")
//                        val home_screen_native2 = home_screen.getString("home_screen_native2")
                        ADSMainClass.setHomeScreenBottomShow(home_screen_bottom_show)
                        ADSMainClass.setHomeScreenAdsType(home_screen_ads_type)

                        // Add Contact Details Screen
                        val contact_details_screen = screen.getJSONObject("contact_details_screen")
                        val contact_details_screen_bottom_ads_show =
                            contact_details_screen.getBoolean("contact_details_screen_bottom_ads_show")
                        val contact_details_screen_ads_type =
                            contact_details_screen.getString("contact_details_screen_ads_type")
                        val contact_details_screen_banner_id =
                            contact_details_screen.getString("contact_details_screen_banner_id")
                        val contact_details_screen_native_id =
                            contact_details_screen.getString("contact_details_screen_native_id")
                        ADSMainClass.setContactDetailSmallAdsShow(
                            contact_details_screen_bottom_ads_show
                        )
                        ADSMainClass.setContactDetailAdsType(contact_details_screen_ads_type)

                        // Setting Screen
                        val setting_screen = screen.getJSONObject("setting_screen")
                        val setting_bottom_ads_show =
                            setting_screen.getBoolean("setting_bottom_ads_show")
                        val setting_ads_type = setting_screen.getString("setting_ads_type")
                        val setting_banner_id = setting_screen.getString("setting_banner_id")
                        val setting_native_id = setting_screen.getString("setting_native_id")
                        ADSMainClass.setSettingBottomAdsShow(setting_bottom_ads_show)
                        ADSMainClass.setSettingAdsType(setting_ads_type)


                        val callend_screen = screen.getJSONObject("callend_screen")

                        val is_callend_show = callend_screen.getBoolean("is_callend_show")
                        val is_callend_bottom_ad_show =
                            callend_screen.getBoolean("is_callend_bottom_ad_show")
                        val callend_bottom_ads_type =
                            callend_screen.getString("callend_bottom_ads_type")
                        val callend_banner_ad_id = callend_screen.getString("callend_banner_ad_id")
                        val callend_native_ad_id = callend_screen.getString("callend_native_ad_id")
                        val notification_install_days =
                            callend_screen.getInt("notification_install_days")
                        val notification_call_install_days =
                            callend_screen.getInt("notification_call_install_days")
                        val notification_call_overlay_install_days =
                            callend_screen.getInt("notification_call_overlay_install_days")
                        val country_get_with_ip = callend_screen.getBoolean("country_get_with_ip")

                        ADSMainClass.setIsShowCallEnd(is_callend_show)
                        ADSMainClass.setCallEndBottomAdsShow(is_callend_bottom_ad_show)
                        ADSMainClass.setCallEndBottomAdsType(callend_bottom_ads_type)
                        ADSMainClass.setNotificationInstallDays(notification_install_days)
                        ADSMainClass.setNotificationCallInstallDays(notification_call_install_days)
                        ADSMainClass.setNotificationCallOverlayInstallDays(
                            notification_call_overlay_install_days
                        )
                        val all_allow_notif =
                            callend_screen.optBoolean(ADSMainClass.ALL_ALLOW_PERMISSION_SHOW_FB_NOTIFICATION)
                        ADSMainClass.setAllAllowPermissionShowFbNotification(all_allow_notif)
                        val close_button_show_on_full_native_ads =
                            callend_screen.getBoolean(ADSMainClass.CLOSE_BUTTON_SHOW_ON_FULL_NATIVE_ADS)
                        ADSMainClass.setCloseButtonShowOnFullNativeAds(
                            close_button_show_on_full_native_ads
                        )
                        ADSMainClass.setCountryGetWithIp(country_get_with_ip)

                        val notification_country_list = ArrayList<String>()
                        val notification_country_array =
                            callend_screen.getJSONArray("notification_country")
                        for (i in 0 until notification_country_array.length()) {
                            notification_country_list.add(notification_country_array.getString(i))
                        }
                        ADSMainClass.setNotificationCountries(notification_country_list)

                        val notification_call_country_list = ArrayList<String>()
                        val notification_call_country_array =
                            callend_screen.getJSONArray("notification_call_country")
                        for (i in 0 until notification_call_country_array.length()) {
                            notification_call_country_list.add(
                                notification_call_country_array.getString(
                                    i
                                )
                            )
                        }
                        ADSMainClass.setNotificationCallCountries(notification_call_country_list)

                        val notification_call_overlay_country_list = ArrayList<String>()
                        val notification_call_overlay_country_array =
                            callend_screen.getJSONArray("notification_call_overlay_country")
                        for (i in 0 until notification_call_overlay_country_array.length()) {
                            notification_call_overlay_country_list.add(
                                notification_call_overlay_country_array.getString(i)
                            )
                        }
                        ADSMainClass.setNotificationCallOverlayCountries(
                            notification_call_overlay_country_list
                        )


                        val call_end_bacK_inter =
                            callend_screen.getJSONObject("call_end_bacK_inter")
                        val call_end_inter_ads_show =
                            call_end_bacK_inter.getBoolean("call_end_inter_ads_show")
                        val notification_screen_back_ads_show =
                            call_end_bacK_inter.getBoolean("notification_screen_back_ads_show")
                        val call_end_inter_ads_type =
                            call_end_bacK_inter.getString("call_end_inter_ads_type")
                        val call_end_inter_day_count = call_end_bacK_inter.getLong("call_end_inter_day_count")
                        val callend_again_open_count = call_end_bacK_inter.getLong("callend_again_open_count")
                        val call_end_inter_active_total_show_count =
                            call_end_bacK_inter.getLong("call_end_inter_active_total_show_count")
                        val callend_inter_ad_id =
                            call_end_bacK_inter.getString("callend_inter_ad_id")


                        val call_end_inter_ads_show_country = ArrayList<String>()
                        try {
                            val countryArray =
                                call_end_bacK_inter.getJSONArray("call_end_inter_ads_show_country")
                            for (i in 0 until countryArray.length()) {
                                call_end_inter_ads_show_country.add(countryArray.getString(i))
                            }
                        } catch (e: Exception) {
                            // Parameter might be missing
                        }
                        ADSMainClass.setCallEndAdCountries(call_end_inter_ads_show_country)
                        ADSMainClass.setCallEndInterAdsShow(call_end_inter_ads_show)
                        ADSMainClass.setNotificationScreenBackAdsShow(
                            notification_screen_back_ads_show
                        )
                        ADSMainClass.setCallEndInterAdsType(call_end_inter_ads_type)
                        ADSMainClass.setCallEndInterDayCount(
                            Math.toIntExact(
                                call_end_inter_day_count
                            )
                        )
                        ADSMainClass.setCallEndAgainOpenCount(
                            Math.toIntExact(
                                callend_again_open_count
                            )
                        )

                        ADSMainClass.setCallEndInterShowCount(
                            Math.toIntExact(
                                call_end_inter_active_total_show_count
                            )
                        )


                        val other_screen = screen.getJSONObject("other_screen")
                        val inter_first_time = other_screen.getString("inter_first_time")
                        val inter_second_time = other_screen.getString("inter_second_time")
                        val other_native_id = other_screen.getString("other_native_id")
                        val other_banner_id = other_screen.getString("other_banner_id")
                        val other_bottom_ads_type = other_screen.getString("other_bottom_ads_type")
                        val other_bottom_ads_show = other_screen.getBoolean("other_bottom_ads_show")
                        val inter_ads_show = other_screen.getBoolean("inter_ads_show")
                        val inter_ads_show_on_back =
                            other_screen.getBoolean("inter_ads_show_on_back")
                        val exit_native_id = other_screen.getString("exit_native_id")
                        ADSMainClass.setInterAdsShow(inter_ads_show)
                        ADSMainClass.setInterAdsOnBackShow(inter_ads_show_on_back)
                        ADSMainClass.setOtherAdsType(other_bottom_ads_type)
                        ADSMainClass.setOtherAdsShow(other_bottom_ads_show)


                        val quick_message_screen = screen.getJSONObject("quick_message_screen")
                        val full_Intent_screen_close_time =
                            quick_message_screen.getInt(ADSMainClass.FULL_INTENT_SCREEN_CLOSE_TIME)
                        val message_callend_show =
                            quick_message_screen.getBoolean(ADSMainClass.MESSAGE_CALLEND_SHOW)
                        val message_callend_ad_show =
                            quick_message_screen.getBoolean(ADSMainClass.MESSAGE_CALLEND_AD_SHOW)
                        val show_full_screen_intent_permission = quick_message_screen.optBoolean(
                            SHOW_FULL_SCREEN_INTENT_PERMISSION,
                            true
                        )
                        val quick_ads_type =
                            quick_message_screen.optString(ADSMainClass.QUICK_ADS_TYPE)
                        val quick_banner_id =
                            quick_message_screen.optString(ADSMainClass.QUICK_BANNER_ID)
                        val quick_native_id =
                            quick_message_screen.optString(ADSMainClass.QUICK_NATIVE_ID)

                        val update_app = screen.getJSONObject("update_app")
                        val in_app_update_show = update_app.getBoolean("in_app_update_show")
                        val in_app_update_type = update_app.getString("in_app_update_type")
                        val in_appp_dailog_daily_show_count =
                            update_app.getInt("in_appp_dailog_daily_show_count")

                        ADSMainClass.setInAppUpdateShow(in_app_update_show)
                        ADSMainClass.setInAppUpdateType(in_app_update_type)
                        ADSMainClass.setInApppDialogDailyShowCount(in_appp_dailog_daily_show_count)

                        ADSMainClass.setIntTIMEValue(
                            ADSMainClass.FULL_INTENT_SCREEN_CLOSE_TIME,
                            full_Intent_screen_close_time
                        )
                        ADSMainClass.setMessageCallEndShow(message_callend_show)
                        ADSMainClass.setMessageCallEndADShow(message_callend_ad_show)
                        ADSMainClass.setBooleanValue(
                            SHOW_FULL_SCREEN_INTENT_PERMISSION,
                            show_full_screen_intent_permission
                        )
                        ADSMainClass.setQuickAdsType(quick_ads_type)

                        val overlay_notification_show = try {
                            jsonobject.getBoolean("overlay_permission_notification_show")
                        } catch (e: Exception) {
                            false
                        }
                        ADSMainClass.setOverlayPermissionNotificationShow(overlay_notification_show)

                        val overlay_notification_days = try {
                            jsonobject.getInt("overlay_permission_notification_days_show_count")
                        } catch (e: Exception) {
                            0
                        }
                        ADSMainClass.setOverlayPermissionNotificationDaysShowCount(
                            overlay_notification_days
                        )


                        ADSMainClass.setStringValue(ADSMainClass.PERMISSION_SCREEN_NATIVE, permission_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.PERMISSION_SCREEN_BANNER, permission_banner_id)

                        ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_NATIVE, language_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_BANNER, language_banner_id)

                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE, home_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_BANNER, home_banner_id)
                        /*ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE1, home_screen_native1)
                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE2, home_screen_native2)*/

                        ADSMainClass.setStringValue(ADSMainClass.CONTACT_DETAIL_SCREEN_NATIVE, contact_details_screen_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.CONTACT_DETAIL_SCREEN_BANNER, contact_details_screen_banner_id)

                        ADSMainClass.setStringValue(ADSMainClass.SETTING_SCREEN_NATIVE, setting_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.SETTING_SCREEN_BANNER, setting_banner_id)

                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_Native, callend_native_ad_id)
                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_BANNER, callend_banner_ad_id)
                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_Inter, callend_inter_ad_id)

                        ADSMainClass.setStringValue(ADSMainClass.QUICK_BANNER_ID, quick_banner_id)
                        ADSMainClass.setStringValue(ADSMainClass.QUICK_NATIVE_ID, quick_native_id)

                        ADSMainClass.setStringValue(ADSMainClass.APP_OPEN_ID, app_open_id)
                        ADSMainClass.setStringValue(ADSMainClass.INTER_FIRST_TIME, inter_first_time)
                        ADSMainClass.setStringValue(ADSMainClass.INTER_SECOND_TIME, inter_second_time)
                        ADSMainClass.setStringValue(ADSMainClass.OTHER_SCREEN_NATIVE, other_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.OTHER_SCREEN_BANNER, other_banner_id)
                        ADSMainClass.setStringValue(ADSMainClass.EXIT_SCREEN_NATIVE, exit_native_id)


                        /*ADSMainClass.setStringValue(
                            ADSMainClass.LANGUAGE_SCREEN_NATIVE,
                            "ca-app-pub-3940256099942544/2247696110"
                        )

                        ADSMainClass.setStringValue(
                            ADSMainClass.EXIT_SCREEN_NATIVE,
                            "ca-app-pub-3940256099942544/2247696110"
                        )

                        ADSMainClass.setStringValue(
                            ADSMainClass.LANGUAGE_SCREEN_BANNER,
                            "ca-app-pub-3940256099942544/9214589741"
                        )

                        ADSMainClass.setStringValue(
                            ADSMainClass.PERMISSION_SCREEN_NATIVE,
                            "ca-app-pub-3940256099942544/2247696110"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.PERMISSION_SCREEN_BANNER,
                            "ca-app-pub-3940256099942544/9214589741"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.HOME_SCREEN_NATIVE1,
                            "ca-app-pub-3940256099942544/2247696110"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.HOME_SCREEN_NATIVE2,
                            "ca-app-pub-3940256099942544/2247696110"
                        )

                        ADSMainClass.setStringValue(
                            ADSMainClass.HOME_SCREEN_NATIVE,
                            "ca-app-pub-3940256099942544/2247696110"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.HOME_SCREEN_BANNER,
                            "ca-app-pub-3940256099942544/9214589741"
                        )

                        ADSMainClass.setStringValue(
                            ADSMainClass.CONTACT_DETAIL_SCREEN_NATIVE,
                            "ca-app-pub-3940256099942544/2247696110"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.CONTACT_DETAIL_SCREEN_BANNER,
                            "ca-app-pub-3940256099942544/9214589741"
                        )

                        ADSMainClass.setStringValue(
                            ADSMainClass.SETTING_SCREEN_NATIVE,
                            "ca-app-pub-3940256099942544/2247696110"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.SETTING_SCREEN_BANNER,
                            "ca-app-pub-3940256099942544/9214589741"
                        )

                        ADSMainClass.setStringValue(
                            ADSMainClass.CALL_END_Native,
                            "ca-app-pub-3940256099942544/2247696110"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.CALL_END_BANNER,
                            "ca-app-pub-3940256099942544/9214589741"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.CALL_END_Inter,
                            "ca-app-pub-3940256099942544/1033173712"
                        )

                        ADSMainClass.setStringValue(
                            ADSMainClass.QUICK_BANNER_ID,
                            "ca-app-pub-3940256099942544/9214589741"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.QUICK_NATIVE_ID,
                            "ca-app-pub-3940256099942544/2247696110"
                        )

                        ADSMainClass.setStringValue(
                            ADSMainClass.APP_OPEN_ID,
                            "ca-app-pub-3940256099942544/9257395921"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.INTER_FIRST_TIME,
                            "ca-app-pub-3940256099942544/1033173712"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.INTER_SECOND_TIME,
                            "ca-app-pub-3940256099942544/1033173712"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.OTHER_SCREEN_NATIVE,
                            "ca-app-pub-3940256099942544/2247696110"
                        )
                        ADSMainClass.setStringValue(
                            ADSMainClass.OTHER_SCREEN_BANNER,
                            "ca-app-pub-3940256099942544/9214589741"
                        )*/




                        ADSMainClass.setExitAds(exit_ads)
                        ADSMainClass.setComingSoon(coming_soon)
                        ADSMainClass.setPrivacyPolicy(privacy_policy)
                        FirebaseAnalytics.getInstance(this@BaseSplashActivity)
                            .setAnalyticsCollectionEnabled(firebaseanalytics)
                        ADSMainClass.setSplashADType(splash_ads_type)
                        ADSMainClass.setAds_Free(ads_blocker)
                        ADSMainClass.setAdsTypeManage(ads_type_load)
                        ADSMainClass.setBannerTypes(bottom_ads_type)
                        ADSMainClass.setNativeButtonColor(admob_native_btn_color)
                        ADSMainClass.setNativeButtonTextColor(admob_native_btn_text_color)
                        ADSMainClass.setAdsClick(Math.toIntExact(ads_click))
                        ADSMainClass.setAdsBackClick(Math.toIntExact(back_click))
                        ADSMainClass.setNativeByPage(Math.toIntExact(native_by_page))


                        if (!application_id.isEmpty() && !appliction_token.isEmpty()) {
                            SetApplication(application_id, appliction_token)
                        }

                        if (update_coming_soon) {
                            ADSMainClass.setComingSoon(true)
                            /*startActivity(
                                Intent(
                                    this@BaseSplashActivity,
                                    CommingActivity::class.java
                                )
                            )*/
                            finish()
                            return@OnCompleteListener
                        }
                        if (ADSMainClass.getComingSoon()) {
                            app_next_screen_showing()
                        } else {
                            app_open_showing()
                            MyApplication.FastStart = false
                        }
                    } catch (e: java.lang.Exception) {
                        Log.d("TAG", "showing_to_all_data: " + e.message.toString())
                        e.printStackTrace()
                    }
                } else {
                    app_next_screen_showing()
                }
            })
    }

    private fun app_open_showing() {

        if (!ADSMainClass.shouldShowSplashAd()) {
            app_next_screen_showing()
            return
        }

        if (ADSMainClass.getAds_Free()) {
            app_next_screen_showing()
            return
        }

        if (ADSMainClass.getSplashADType() == "Appopen") {
            ADSAppStarting.AdsSplashAppStartingDisplay(this@BaseSplashActivity, {
                MyApplication.FastStart = true
                app_next_screen_showing()
            }
            )

        } else {
            MyApplication.FastStart = true
            app_next_screen_showing()
        }
    }

    private fun SetApplication(application_id: String, token: String) {
        FacebookSdk.setApplicationId(application_id)
        setClientToken(token)
        sdkInitialize(this@BaseSplashActivity)
//        FacebookSdk.sdkInitialize(applicationContext)
//        AppEventsLogger.activateApp(this)
        setAutoInitEnabled(true)
        fullyInitialize()
        setAutoLogAppEventsEnabled(true)

        if (BuildConfig.DEBUG) {
            setIsDebugEnabled(true)
        }
        addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        val logger = AppEventsLogger.newLogger(this@BaseSplashActivity)
        logger.applicationId
    }

    private fun app_next_screen_showing() {
        PostHog.capture(event = "splashOpen")
        ADSAppManage.AppStartingScreenOpen = true
        initActivity()
    }
}
