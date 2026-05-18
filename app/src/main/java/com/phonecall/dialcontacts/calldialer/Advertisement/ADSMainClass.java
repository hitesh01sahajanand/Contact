package com.phonecall.dialcontacts.calldialer.Advertisement;

import android.content.Context;
import android.content.SharedPreferences;

import android.telephony.TelephonyManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ADSMainClass {

    private static final String Ads_Free = "Ads_Free";
    private static final String AdsAdmobInterstitialID = "AdsAdmobInterstitialID";
    private static final String AdsAdmobNativeID = "AdsAdmobNativeID";
    private static final String AdsAdmobNativeIdTwo = "AdsAdmobNativeIdTwo";
    private static final String AdsAdmobBannerId = "AdsAdmobBannerId";
    private static final String Admob_call_end_BannerId = "Admob_call_end_BannerId";
    private static final String AppStartingId = "AppStartingId";
    private static final String NativeByPage = "NativeByPage";
    private static final String AdsDisplayType = "AdsDisplayType";
    private static final String BannerTypes = "BannerTypes";
    private static final String NativeButtonColor = "NativeButtonColor";
    private static final String NativeButtonTextColor = "NativeButtonTextColor";
    private static final String FBNativeId = "FBNativeId";
    private static final String FB_Call_End_Native_Id = "FB_Call_End_Native_Id";
    private static final String FB_Call_End_Banner_Id = "FB_Call_End_Banner_Id";
    private static final String AdsFBInterstitialID = "AdsFBInterstitialID";
    private static final String AdsClick = "AdsClick";
    private static final String AdsBackClick = "AdsBackClick";
    private static final String AdsOneByOneIDS = "AdsOneByOneIDS";
    private static final String AdsTypeManage = "AdsTypeManage";
    private static final String FBNativeBannerID = "FBNativeBannerID";
    private static final String SplashADType = "SplashADType";
    private static final String ExitAds = "ExitAds";
    private static final String PrivacyPolicy = "PrivacyPolicy";
    private static final String ComingSoon = "ComingSoon";
    private static final String ScreenShow = "ScreenShow";
    public static String AdsInterstitialType = "AdsInterstitialType";
    public static String Refferel = "Refferel";

    public static String NavigationShow = "NavigationShow";

    public static String CallEndShow5 = "CallEndShow5";
    public static String CallEndShow100 = "CallEndShow100";
    public static String CallEndShowEvent = "CallEndShowEvent";
    public static String CallRetention7Day = "CallRetention7Day";
    public static String CallRetention3Day = "CallRetention3Day";
    public static String CallRetention1Day = "CallRetention1Day";
    public static String CallEndShow50 = "CallEndShow50";
    public static String CallEndShow10 = "CallEndShow10";
    public static String CallEndShow1 = "CallEndShow1";
    public static String AdmobCallEndNativeList = "AdmobCallEndNativeList";
    public static String Admob_CallEnd_Native_Unit_Id = "Admob_CallEnd_Native_Unit_Id";
    public static String AlternateType = "AlternateType";

    public static final String LANGUAGE_CODE = "language_code";
    public static final String LANGUAGE_SCREEN = "language_screen";
    public static final String LANGUAGE_SCREEN_AVAILABLE = "language_screen_available";

    public static final String PRIVACY_URL = "privacy_url";
    public static String lehfv_HomeRetention1Day = "Simple_HomeRetention1Day";
    public static String lehfv_HomeRetention3Day = "Simple_HomeRetention3Day";
    public static String lehfv_HomeRetention7Day = "Simple_HomeRetention7Day";

    public static String SHOW_ADS_AFTER_LANGUAGE = "show_ads_after_language";
//    public static Boolean IS_LANGUAGE_SCREEN_SHOW = false;

    public static int IS_AD_SHOWING = 0;
    public static String INMOBI_ACCOUNT_ID = "e04644a5ab15457c85258a41f87541a6";
    public static String INMOBI_INTER_AD_ID = "inmobi_inter_ad_id";
    public static String FULL_INTENT_SCREEN_CLOSE_TIME = "full_Intent_screen_close_time";
    public static String MESSAGE_CALLEND_SHOW = "message_callend_show";
    public static String MESSAGE_CALLEND_AD_SHOW = "message_callend_ad_show";


    public static String LANGUAGE_SCREEN_BANNER = "language_screen_banner";
    public static String LANGUAGE_SCREEN_NATIVE = "language_screen_native";
    public static String PERMISSION_SCREEN_BANNER = "permission_screen_banner";
    public static String PERMISSION_SCREEN_NATIVE = "permission_screen_native";
    public static String HOME_SCREEN_BANNER = "home_screen_banner";
    public static String HOME_SCREEN_NATIVE = "home_screen_native";

    public static String CONTACT_DETAIL_SCREEN_BANNER = "contact_detail_screen_banner";
    public static String CONTACT_DETAIL_SCREEN_NATIVE = "contact_detail_screen_native";

    public static String SETTING_SCREEN_BANNER = "setting_screen_banner";
    public static String SETTING_SCREEN_NATIVE = "setting_screen_native";
    public static String OTHER_SCREEN_NATIVE = "other_screen_native";
    public static String OTHER_SCREEN_BANNER = "other_screen_banner";

    public static String CALL_END_BANNER = "callend_banner_ad_id";
    public static String CALL_END_Native = "callend_native_ad_id";
    public static String CALL_END_Inter = "callend_inter_ad_id";
    //Test
    public static String APP_OPEN_ID = "app_open_id";
    public static String INTER_FIRST_TIME = "inter_first_time";
    public static String INTER_SECOND_TIME = "inter_second_time";
    //    public static String INTER_CALLEND_AD = "inter_callend_ad";
    public static String CALL_END_ADAPTIVE_BANNER = "call_end_adaptive_banner";
    public static String HOME_SCREEN_NATIVE1 = "home_screen_native1";
    public static String HOME_SCREEN_NATIVE2 = "home_screen_native2";
    //    public static String CHAT_BOX_BANNER = "chat_box_banner";
//    public static String OTHER_BANNER = "other_banner";
//    public static String PERMISSION_SCREEN_BANNER = "permission_screen_banner";
    public static String EXIT_SCREEN_NATIVE = "ExitDialog";
    public static String SHOW_FULL_SCREEN_INTENT_PERMISSION = "show_full_screen_intent_permission";
    public static String QUICK_ADS_TYPE = "quick_ads_type";
    public static String QUICK_BANNER_ID = "quick_banner_id";
    public static String QUICK_NATIVE_ID = "quick_native_id";
    public static String EXIT_NATIVE_ID = "exit_native_id";
    public static String OVERLAY_PERMISSION_NOTIFICATION_SHOW = "overlay_permission_notification_show";
    public static String OVERLAY_PERMISSION_NOTIFICATION_DAYS_SHOW_COUNT = "overlay_permission_notification_days_show_count";
    public static String LAST_OPEN_DATE = "last_open_date";
    public static String CONSECUTIVE_DAYS_COUNT = "consecutive_days_count";
    public static String NOTIFICATION_INSTALL_DAYS = "notification_install_days";
    public static String NOTIFICATION_CALL_INSTALL_DAYS = "notification_call_install_days";
    public static String NOTIFICATION_CALL_OVERLAY_INSTALL_DAYS = "notification_call_overlay_install_days";
    public static String NOTIFICATION_COUNTRY = "notification_country";
    public static String NOTIFICATION_CALL_COUNTRY = "notification_call_country";
    public static String NOTIFICATION_CALL_OVERLAY_COUNTRY = "notification_call_overlay_country";

    public static String ALL_ALLOW_PERMISSION_SHOW_FB_NOTIFICATION = "All_allow_permission_show_fb_notification";
    public static String CLOSE_BUTTON_SHOW_ON_FULL_NATIVE_ADS = "close_button_show_on_full_native_ads";

    /**
     * Firebase: true = show overlay/full screen intent permission, false = skip it. Default true.
     */
    public static boolean getShowFullScreenIntentPermission() {
        return ADSPrefManage().getBoolean(SHOW_FULL_SCREEN_INTENT_PERMISSION, true);
    }

    public static String FULL_CALL_END_TOTAL_COUNT = "full_call_end_total_count";
    public static String FULL_CALL_END_INTER_SHOW_COUNT = "full_call_end_inter_show_count";
    public static String FULL_CALL_END_INTER_SHOW = "full_call_end_inter_show";
    public static String FULL_CALL_END_AD_TYPE = "full_call_end_ad_type";


    private static final String SplashAppOpenShow = "SplashAppOpenShow";
    private static final String SplashAppOpenAfterCount = "SplashAppOpenAfterCount";
    private static final String SplashAppOpenVisitCount = "SplashAppOpenVisitCount";

    private static final String AppOpenBackgroundShow = "AppOpenBackgroundShow";

    public static String call_end_last_ad_date = "call_end_last_ad_date";
    public static String call_end_ad_count_today = "call_end_ad_count_today";
    public static String update_last_show_date = "update_last_show_date";
    public static String update_show_count_today = "update_show_count_today";

    public static String IsShowCallEnd = "is_call_end_show";
    public static String CALL_END_INTER_DAY_COUNT = "call_end_inter_day_count";
    public static String CALL_END_INTER_SHOW_COUNT = "call_end_inter_show_count";
    private static final String InterAdsShow = "InterAdsShow";
    private static final String InterAdsOnBackShow = "InterAdsOnBackShow";
    public static String APP_INSTALL_DATE = "app_install_date";

    private static final String SplashToLanguage = "SplashToLanguage";
    private static final String LanguageScreenBottomAdShow = "LanguageScreenBottomAdShow";
    private static final String LanguageInterAdsShow = "LanguageInterAdsShow";
    private static final String LanguageAdsType = "LanguageAdsType";
    private static final String HomeScreenAdsType = "HomeScreenAdsType";
    private static final String PermissionSmallAdsShow = "PermissionSmallAdsShow";
    private static final String PermissionAdsType = "PermissionAdsType";
    private static final String HomeScreenBottomShow = "HomeScreenBottomShow";

    private static final String ChatSmallAdsShow = "ChatSmallAdsShow";
    private static final String SettingBottomAdsShow = "SettingBottomAdsShow";
    private static final String ChatAdsType = "ChatAdsType";
    private static final String SettingAdsType = "SettingAdsType";
    private static final String QuickAdsType = "QuickAdsType";
    public static String CallEndBottomAdsShow = "CallEndBottomAdsShow";
    public static String CallEndInterAdsShow = "CallEndInterAdsShow";
    public static String CallEndBottomAdsType = "CallEndBottomAdsType";

    public static String OtherAdsType = "OtherAdsType";
    public static String OtherAdsShow = "OtherAdsShow";
    public static String CALL_END_INTER_ADS_SHOW_COUNTRIES = "call_end_inter_ads_show_country";
    public static String CALL_END_INTER_ADS_TYPE = "call_end_inter_ads_type";
    public static String NOTIFICATION_SCREEN_BACK_ADS_SHOW = "notification_screen_back_ads_show";
    public static String IP_COUNTRY_NAME = "ip_country_name";
    public static String COUNTRY_GET_WITH_IP = "country_get_with_ip";

    public static String getOtherAdsType() {
        return ADSPrefManage().getString(OtherAdsType, "native");
    }

    public static void setOtherAdsType(String value) {
        ADSPrefManage().edit().putString(OtherAdsType, value).apply();
    }

    public static boolean getOtherAdsShow() {
        return ADSPrefManage().getBoolean(OtherAdsShow, false);
    }

    public static void setOtherAdsShow(Boolean value) {
        ADSPrefManage().edit().putBoolean(OtherAdsShow, value).apply();
    }

    public static boolean getLanguageScreenBottomAdShow() {
        return ADSPrefManage().getBoolean(LanguageScreenBottomAdShow, true);
    }

    public static void setLanguageScreenBottomAdShow(boolean value) {
        ADSPrefManage().edit().putBoolean(LanguageScreenBottomAdShow, value).apply();
    }

    public static boolean getLanguageInterAdsShow() {
        return ADSPrefManage().getBoolean(LanguageInterAdsShow, false);
    }

    public static void setLanguageInterAdsShow(boolean value) {
        ADSPrefManage().edit().putBoolean(LanguageInterAdsShow, value).apply();
    }


    public static String getLanguageAdsType() {
        return ADSPrefManage().getString(LanguageAdsType, "native");
    }

    public static void setLanguageAdsType(String value) {
        ADSPrefManage().edit().putString(LanguageAdsType, value).apply();
    }

    public static boolean getSplashToLanguage() {
        return ADSPrefManage().getBoolean(SplashToLanguage, true);
    }

    public static void setSplashToLanguage(boolean value) {
        ADSPrefManage().edit().putBoolean(SplashToLanguage, value).apply();
    }

    public static boolean getPermissionSmallAdsShow() {
        return ADSPrefManage().getBoolean(PermissionSmallAdsShow, true);
    }

    public static void setPermissionSmallAdsShow(boolean value) {
        ADSPrefManage().edit().putBoolean(PermissionSmallAdsShow, value).apply();
    }


    public static String getPermissionAdsType() {
        return ADSPrefManage().getString(PermissionAdsType, "native");
    }

    public static void setPermissionAdsType(String value) {
        ADSPrefManage().edit().putString(PermissionAdsType, value).apply();
    }

    public static boolean getHomeScreenBottomShow() {
        return ADSPrefManage().getBoolean(HomeScreenBottomShow, true);
    }

    public static void setHomeScreenBottomShow(boolean value) {
        ADSPrefManage().edit().putBoolean(HomeScreenBottomShow, value).apply();
    }

    public static String getHomeScreenAdsType() {
        return ADSPrefManage().getString(HomeScreenAdsType, "native");
    }

    public static void setHomeScreenAdsType(String value) {
        ADSPrefManage().edit().putString(HomeScreenAdsType, value).apply();
    }

    public static boolean getContactDetailSmallAdsShow() {
        return ADSPrefManage().getBoolean(ChatSmallAdsShow, true);
    }

    public static void setContactDetailSmallAdsShow(boolean value) {
        ADSPrefManage().edit().putBoolean(ChatSmallAdsShow, value).apply();
    }

    public static String getContactDetailAdsType() {
        return ADSPrefManage().getString(ChatAdsType, "native");
    }

    public static void setContactDetailAdsType(String value) {
        ADSPrefManage().edit().putString(ChatAdsType, value).apply();
    }

    public static boolean getSettingBottomAdsShow() {
        return ADSPrefManage().getBoolean(SettingBottomAdsShow, true);
    }

    public static void setSettingBottomAdsShow(boolean value) {
        ADSPrefManage().edit().putBoolean(SettingBottomAdsShow, value).apply();
    }

    public static String getSettingAdsType() {
        return ADSPrefManage().getString(SettingAdsType, "native");
    }

    public static void setSettingAdsType(String value) {
        ADSPrefManage().edit().putString(SettingAdsType, value).apply();
    }

    public static String getQuickAdsType() {
        return ADSPrefManage().getString(QuickAdsType, "native");
    }

    public static void setQuickAdsType(String value) {
        ADSPrefManage().edit().putString(QuickAdsType, value).apply();
    }

    public static boolean getCallEndBottomAdsShow() {
        return ADSPrefManage().getBoolean(CallEndBottomAdsShow, false);
    }

    public static void setCallEndBottomAdsShow(boolean value) {
        ADSPrefManage().edit().putBoolean(CallEndBottomAdsShow, value).apply();
    }

    public static boolean getCallEndInterAdsShow() {
        return ADSPrefManage().getBoolean(CallEndInterAdsShow, false);
    }

    public static void setCallEndInterAdsShow(boolean value) {
        ADSPrefManage().edit().putBoolean(CallEndInterAdsShow, value).apply();
    }

    public static String getCallEndBottomAdsType() {
        return ADSPrefManage().getString(CallEndBottomAdsType, "native");
    }

    public static void setCallEndBottomAdsType(String value) {
        ADSPrefManage().edit().putString(CallEndBottomAdsType, value).apply();
    }

    public static String getCallEndInterAdsType() {
        return ADSPrefManage().getString(CALL_END_INTER_ADS_TYPE, "inter");
    }

    public static void setCallEndInterAdsType(String value) {
        ADSPrefManage().edit().putString(CALL_END_INTER_ADS_TYPE, value).apply();
    }

    public static boolean getNotificationScreenBackAdsShow() {
        return ADSPrefManage().getBoolean(NOTIFICATION_SCREEN_BACK_ADS_SHOW, false);
    }

    public static void setNotificationScreenBackAdsShow(boolean value) {
        ADSPrefManage().edit().putBoolean(NOTIFICATION_SCREEN_BACK_ADS_SHOW, value).apply();
    }

    public static void setCloseButtonShowOnFullNativeAds(boolean value) {
        ADSPrefManage().edit().putBoolean(CLOSE_BUTTON_SHOW_ON_FULL_NATIVE_ADS, value).apply();
    }

    public static boolean getCloseButtonShowOnFullNativeAds() {
        return ADSPrefManage().getBoolean(CLOSE_BUTTON_SHOW_ON_FULL_NATIVE_ADS, false);
    }

    public static String getIpCountryName() {
        return ADSPrefManage().getString(IP_COUNTRY_NAME, "");
    }

    public static void setIpCountryName(String value) {
        ADSPrefManage().edit().putString(IP_COUNTRY_NAME, value).apply();
    }

    public static boolean getCountryGetWithIp() {
        return ADSPrefManage().getBoolean(COUNTRY_GET_WITH_IP, false);
    }

    public static void setCountryGetWithIp(boolean value) {
        ADSPrefManage().edit().putBoolean(COUNTRY_GET_WITH_IP, value).apply();
    }

    public static boolean getInAppUpdateShow() {
        return ADSPrefManage().getBoolean("in_app_update_show", false);
    }

    public static void setInAppUpdateShow(boolean value) {
        ADSPrefManage().edit().putBoolean("in_app_update_show", value).apply();
    }

    public static String getInAppUpdateType() {
        return ADSPrefManage().getString("in_app_update_type", "Flexible");
    }

    public static void setInAppUpdateType(String value) {
        ADSPrefManage().edit().putString("in_app_update_type", value).apply();
    }

    public static int getInApppDialogDailyShowCount() {
        return ADSPrefManage().getInt("in_appp_dailog_daily_show_count", 0);
    }

    public static void setInApppDialogDailyShowCount(int value) {
        ADSPrefManage().edit().putInt("in_appp_dailog_daily_show_count", value).apply();
    }

    public static void setUpdateLastShowDate(String value) {
        ADSPrefManage().edit().putString(update_last_show_date, value).apply();
    }

    public static String getUpdateLastShowDate() {
        return ADSPrefManage().getString(update_last_show_date, "");
    }

    public static void setUpdateShowCountToday(int value) {
        ADSPrefManage().edit().putInt(update_show_count_today, value).apply();
    }

    public static int getUpdateShowCountToday() {
        return ADSPrefManage().getInt(update_show_count_today, 0);
    }

    public static boolean shouldShowAppUpdate() {
        if (!getInAppUpdateShow()) return false;

        int maxCount = getInApppDialogDailyShowCount();

        String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String lastDate = getUpdateLastShowDate();

        if (todayDate.equals(lastDate)) {
            return getUpdateShowCountToday() < maxCount;
        }

        return true;
    }

    public static void updateAppUpdateShowCount() {
        String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String lastDate = getUpdateLastShowDate();

        if (todayDate.equals(lastDate)) {
            setUpdateShowCountToday(getUpdateShowCountToday() + 1);
        } else {
            setUpdateLastShowDate(todayDate);
            setUpdateShowCountToday(1);
        }
    }

    public static boolean getInterAdsShow() {
        return ADSPrefManage().getBoolean(InterAdsShow, false);
    }

    public static void setInterAdsShow(boolean value) {
        ADSPrefManage().edit().putBoolean(InterAdsShow, value).apply();
    }

    public static boolean getInterAdsOnBackShow() {
        return ADSPrefManage().getBoolean(InterAdsOnBackShow, false);
    }

    public static void setInterAdsOnBackShow(boolean value) {
        ADSPrefManage().edit().putBoolean(InterAdsOnBackShow, value).apply();
    }

    public static boolean getAppOpenBackgroundShow() {
        return ADSPrefManage().getBoolean(AppOpenBackgroundShow, true);
    }

    public static void setAppOpenBackgroundShow(boolean value) {
        ADSPrefManage().edit().putBoolean(AppOpenBackgroundShow, value).apply();
    }

    public static int getAppOpenAdDailyLimit() {
        return ADSPrefManage().getInt("AppOpenAdDailyLimit", 3);
    }

    public static void setAppOpenAdDailyLimit(int value) {
        ADSPrefManage().edit().putInt("AppOpenAdDailyLimit", value).apply();
    }


    public static boolean getSplashAppOpenShow() {
        return ADSPrefManage().getBoolean(SplashAppOpenShow, true);
    }

    public static void setSplashAppOpenShow(boolean value) {
        ADSPrefManage().edit().putBoolean(SplashAppOpenShow, value).apply();
    }

    public static int getSplashAppOpenAfterCount() {
        return ADSPrefManage().getInt(SplashAppOpenAfterCount, 1);
    }

    public static void setSplashAppOpenAfterCount(int value) {
        ADSPrefManage().edit().putInt(SplashAppOpenAfterCount, value).apply();
    }

    public static int getSplashAppOpenVisitCount() {
        return ADSPrefManage().getInt(SplashAppOpenVisitCount, 0);
    }

    public static void setSplashVisitCount(int value) {
        ADSPrefManage().edit().putInt(SplashAppOpenVisitCount, value).apply();
    }


    public static boolean shouldShowSplashAd() {
        if (!getSplashAppOpenShow()) {
            return false;
        }

        int afterCount = getSplashAppOpenAfterCount();
        if (afterCount <= 0) {
            return false;
        }

        int currentVisit = getSplashAppOpenVisitCount() + 1;
        setSplashVisitCount(currentVisit);

        if (currentVisit % afterCount == 0) {
            return true;
        }

        return false;
    }

    public static String getCallEndLastAdDate() {
        return ADSPrefManage().getString(call_end_last_ad_date, "");
    }

    public static void setCallEndLastAdDate(String value) {
        ADSPrefManage().edit().putString(call_end_last_ad_date, value).apply();
    }


    public static int getCallEndAdCountToday() {
        return ADSPrefManage().getInt(call_end_ad_count_today, 0);
    }

    public static void setCallEndAdCountToday(int value) {
        ADSPrefManage().edit().putInt(call_end_ad_count_today, value).apply();
    }

    public static boolean getIsShowCallEnd() {
        return ADSPrefManage().getBoolean(IsShowCallEnd, false);
    }

    public static void setIsShowCallEnd(boolean value) {
        ADSPrefManage().edit().putBoolean(IsShowCallEnd, value).apply();
    }

    public static void setCallEndInterDayCount(int value) {
        ADSPrefManage().edit().putInt(CALL_END_INTER_DAY_COUNT, value).apply();
    }

    public static int getCallEndInterDayCount() {
        return ADSPrefManage().getInt(CALL_END_INTER_DAY_COUNT, 0);
    }

    public static void setCallEndInterShowCount(int value) {
        ADSPrefManage().edit().putInt(CALL_END_INTER_SHOW_COUNT, value).apply();
    }

    public static int getCallEndInterShowCount() {
        return ADSPrefManage().getInt(CALL_END_INTER_SHOW_COUNT, 0);
    }


    public static void setCallEndAdCountries(List<String> countries) {
        String json = new Gson().toJson(countries);
        ADSPrefManage().edit().putString(CALL_END_INTER_ADS_SHOW_COUNTRIES, json).apply();
    }

    public static List<String> getCallEndAdCountries() {
        String json = ADSPrefManage().getString(CALL_END_INTER_ADS_SHOW_COUNTRIES, "[]");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        return new Gson().fromJson(json, type);
    }

    public static String getDeviceCountry(Context context) {
        if (getCountryGetWithIp() && !getIpCountryName().isEmpty()) {
            return getIpCountryName().toUpperCase();
        }
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String countryCode = tm.getNetworkCountryIso();
            if (countryCode == null || countryCode.isEmpty()) {
                countryCode = tm.getSimCountryIso();
            }
            if (countryCode == null || countryCode.isEmpty()) {
                countryCode = context.getResources().getConfiguration().locale.getCountry();
            }
            return countryCode != null ? countryCode.toUpperCase() : "";
        } catch (Exception e) {
            try {
                return context.getResources().getConfiguration().locale.getCountry().toUpperCase();
            } catch (Exception e2) {
                return "";
            }
        }
    }

    public static boolean isCountryAllowedForCallEnd(Context context) {
        List<String> allowedCountries = getCallEndAdCountries();
        if (allowedCountries == null || allowedCountries.isEmpty()) {
            return true; // Default to show if no countries are specified
        }
        String currentCountry = getDeviceCountry(context);
        for (String country : allowedCountries) {
            if (country.equalsIgnoreCase(currentCountry)) {
                return true;
            }
        }
        return false;
    }


    public static boolean shouldShowCallEndAd(Context context) {
        if (!getIsShowCallEnd()) return false;
        if (getAds_Free()) return false;
        if (!getCallEndInterAdsShow()) return false;
        if (!isCountryAllowedForCallEnd(context)) return false;

        int interval = getCallEndInterDayCount();
        int maxCount = getCallEndInterShowCount();

        if (interval <= 0 || maxCount <= 0) return false;

        if (getDaysSinceInstall() < interval) {
            return false;
        }

        String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String lastDate = getCallEndLastAdDate();

        if (todayDate.equals(lastDate)) {
            int currentCount = getCallEndAdCountToday();
            return currentCount < maxCount;
        }

        return true;
    }

    public static void updateCallEndAdCount(Context context) {
        String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String lastDate = getCallEndLastAdDate();

        if (todayDate.equals(lastDate)) {
            setCallEndAdCountToday(getCallEndAdCountToday() + 1);
        } else {
            // New day or first time
            setCallEndLastAdDate(todayDate);
            setCallEndAdCountToday(1);
        }
    }


    public static void setMessageCallEndShow(Boolean i) {
        SharedPreferencesClass.getInstance().setBoolean(MESSAGE_CALLEND_SHOW, i);
    }

    public static boolean getMessageCallEndShow() {
        return SharedPreferencesClass.getInstance().getBoolean(MESSAGE_CALLEND_SHOW, false);
    }


    public static void setMessageCallEndADShow(Boolean i) {
        SharedPreferencesClass.getInstance().setBoolean(MESSAGE_CALLEND_AD_SHOW, i);
    }

    public static boolean getMessageCallEndADShow() {
        return SharedPreferencesClass.getInstance().getBoolean(MESSAGE_CALLEND_AD_SHOW, false);
    }

    public static void setOverlayPermissionNotificationShow(boolean value) {
        ADSPrefManage().edit().putBoolean(OVERLAY_PERMISSION_NOTIFICATION_SHOW, value).apply();
    }

    public static boolean getOverlayPermissionNotificationShow() {
        return ADSPrefManage().getBoolean(OVERLAY_PERMISSION_NOTIFICATION_SHOW, false);
    }

    public static void setOverlayPermissionNotificationDaysShowCount(int value) {
        ADSPrefManage().edit().putInt(OVERLAY_PERMISSION_NOTIFICATION_DAYS_SHOW_COUNT, value).apply();
    }

    public static int getOverlayPermissionNotificationDaysShowCount() {
        return ADSPrefManage().getInt(OVERLAY_PERMISSION_NOTIFICATION_DAYS_SHOW_COUNT, 0);
    }

    public static void setNotificationInstallDays(int value) {
        ADSPrefManage().edit().putInt(NOTIFICATION_INSTALL_DAYS, value).apply();
    }

    public static int getNotificationInstallDays() {
        return ADSPrefManage().getInt(NOTIFICATION_INSTALL_DAYS, 0);
    }

    public static void setNotificationCallInstallDays(int value) {
        ADSPrefManage().edit().putInt(NOTIFICATION_CALL_INSTALL_DAYS, value).apply();
    }

    public static int getNotificationCallInstallDays() {
        return ADSPrefManage().getInt(NOTIFICATION_CALL_INSTALL_DAYS, 0);
    }

    public static void setNotificationCallOverlayInstallDays(int value) {
        ADSPrefManage().edit().putInt(NOTIFICATION_CALL_OVERLAY_INSTALL_DAYS, value).apply();
    }

    public static int getNotificationCallOverlayInstallDays() {
        return ADSPrefManage().getInt(NOTIFICATION_CALL_OVERLAY_INSTALL_DAYS, 0);
    }

    public static void setNotificationCountries(List<String> countries) {
        String json = new Gson().toJson(countries);
        ADSPrefManage().edit().putString(NOTIFICATION_COUNTRY, json).apply();
    }

    public static List<String> getNotificationCountries() {
        String json = ADSPrefManage().getString(NOTIFICATION_COUNTRY, "[]");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        return new Gson().fromJson(json, type);
    }

    public static void setNotificationCallCountries(List<String> countries) {
        String json = new Gson().toJson(countries);
        ADSPrefManage().edit().putString(NOTIFICATION_CALL_COUNTRY, json).apply();
    }

    public static List<String> getNotificationCallCountries() {
        String json = ADSPrefManage().getString(NOTIFICATION_CALL_COUNTRY, "[]");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        return new Gson().fromJson(json, type);
    }

    public static void setNotificationCallOverlayCountries(List<String> countries) {
        String json = new Gson().toJson(countries);
        ADSPrefManage().edit().putString(NOTIFICATION_CALL_OVERLAY_COUNTRY, json).apply();
    }

    public static List<String> getNotificationCallOverlayCountries() {
        String json = ADSPrefManage().getString(NOTIFICATION_CALL_OVERLAY_COUNTRY, "[]");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        return new Gson().fromJson(json, type);
    }

    public static void setAllAllowPermissionShowFbNotification(boolean value) {
        ADSPrefManage().edit().putBoolean(ALL_ALLOW_PERMISSION_SHOW_FB_NOTIFICATION, value).apply();
    }

    public static boolean getAllAllowPermissionShowFbNotification() {
        return ADSPrefManage().getBoolean(ALL_ALLOW_PERMISSION_SHOW_FB_NOTIFICATION, false);
    }

    // --- Scenario Filtering Logic ---

    public static boolean isNotificationGranted(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true; // Granted by default on older versions
    }

    public static boolean isCallStateGranted(Context context) {
        return androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isOverlayGranted(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return android.provider.Settings.canDrawOverlays(context);
        }
        return true;
    }

    /**
     * Determines if CallEnd performance is allowed based on the 3-scenario plan.
     *
     * @param context      App context
     * @param isFcmTrigger True if checking for a Firebase Notification, false if for local call event.
     * @return True if all conditions for the current scenario are met.
     */
    public static boolean isCallEndPerformanceAllowed(Context context, boolean isFcmTrigger) {
        if (!getIsShowCallEnd()) {
            android.util.Log.d("CallEndCheck", "Blocked: getIsShowCallEnd is false");
            return false;
        }

        boolean notif = isNotificationGranted(context);
        boolean call = isCallStateGranted(context);
        boolean overlay = isOverlayGranted(context);

        long installDays = getDaysSinceInstall();
        String currentCountry = getDeviceCountry(context);

        android.util.Log.d("CallEndCheck", "Status: Notif=" + notif + ", Call=" + call + ", Overlay=" + overlay +
                ", Days=" + installDays + ", Country=" + currentCountry + ", FCM=" + isFcmTrigger);

        if (notif && call && overlay) {
            // Scenario 3: All permissions allowed
            if (isFcmTrigger && !getAllAllowPermissionShowFbNotification()) {
                android.util.Log.d("CallEndCheck", "Blocked: Scenario 3 and getAllAllowPermissionShowFbNotification is false");
                return false;
            }
            boolean result = checkDaysAndCountry(installDays, getNotificationCallOverlayInstallDays(),
                    currentCountry, getNotificationCallOverlayCountries());
            android.util.Log.d("CallEndCheck", "Scenario 3 Result: " + result);
            return result;

        } else if (notif && call) {
            // Scenario 2: Notification + Call State
            boolean result = checkDaysAndCountry(installDays, getNotificationCallInstallDays(),
                    currentCountry, getNotificationCallCountries());
            android.util.Log.d("CallEndCheck", "Scenario 2 Result: " + result);
            return result;

        } else if (notif) {
            // Scenario 1: Only Notification
            if (!isFcmTrigger) {
                android.util.Log.d("CallEndCheck", "Blocked: Scenario 1 but not an FCM trigger");
                return false;
            }
            boolean result = checkDaysAndCountry(installDays, getNotificationInstallDays(),
                    currentCountry, getNotificationCountries());
            android.util.Log.d("CallEndCheck", "Scenario 1 Result: " + result);
            return result;
        }

        android.util.Log.d("CallEndCheck", "Blocked: No matching scenario (possibly no notification permission)");
        return false;
    }

    private static boolean checkDaysAndCountry(long currentDays, int requiredDays,
                                               String currentCountry, List<String> allowedCountries) {
        if (currentDays < requiredDays) return false;

        if (allowedCountries == null || allowedCountries.isEmpty()) return true;

        for (String country : allowedCountries) {
            if (country.equalsIgnoreCase(currentCountry)) return true;
        }
        return false;
    }


    public static void updateConsecutiveStreak(Context context) {
        String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String lastDate = ADSPrefManage().getString(LAST_OPEN_DATE, "");
        int currentStreak = ADSPrefManage().getInt(CONSECUTIVE_DAYS_COUNT, 0);

        if (todayDate.equals(lastDate)) {
            // Already counted today
            return;
        }

        if (lastDate.isEmpty()) {
            // First time ever
            currentStreak = 1;
        } else {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.util.Date d1 = sdf.parse(lastDate);
                java.util.Date d2 = sdf.parse(todayDate);
                long diff = d2.getTime() - d1.getTime();
                long diffDays = diff / (24 * 60 * 60 * 1000);

                if (diffDays == 1) {
                    // Consecutive day
                    currentStreak++;
                } else {
                    // Day missed, reset to 1
                    currentStreak = 1;
                }
            } catch (Exception e) {
                currentStreak = 1;
            }
        }

        ADSPrefManage().edit()
                .putString(LAST_OPEN_DATE, todayDate)
                .putInt(CONSECUTIVE_DAYS_COUNT, currentStreak)
                .apply();

        // Days Since Install Logic
        String installDate = ADSPrefManage().getString(APP_INSTALL_DATE, "");
        if (installDate.isEmpty()) {
            installDate = todayDate;
            ADSPrefManage().edit().putString(APP_INSTALL_DATE, installDate).apply();
        }
    }

    public static long getDaysSinceInstall() {
        String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String installDate = ADSPrefManage().getString(APP_INSTALL_DATE, "");
        if (installDate.isEmpty()) {
            return 1;
        }

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date d1 = sdf.parse(installDate);
            java.util.Date d2 = sdf.parse(todayDate);
            long diff = d2.getTime() - d1.getTime();
            return (diff / (24 * 60 * 60 * 1000)) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    public static int getConsecutiveDaysCount() {
        return ADSPrefManage().getInt(CONSECUTIVE_DAYS_COUNT, 0);
    }


    public static Boolean getBooleanValue(String key) {
        return ADSPrefManage().getBoolean(key, false);
    }

    public static void setBooleanValue(String key, Boolean value) {
        ADSPrefManage().edit().putBoolean(key, value).apply();
    }

    public static String getStringValue(String key) {
        return ADSPrefManage().getString(key, "");
    }

    public static void setStringValue(String key, String value) {
        ADSPrefManage().edit().putString(key, value).apply();
    }


    public static int geIntTIMEValue(String key) {
        return ADSPrefManage().getInt(key, 30000);
    }

    public static void setIntTIMEValue(String key, int value) {
        ADSPrefManage().edit().putInt(key, value).apply();
    }

    public static void putshow(Boolean i) {
        SharedPreferencesClass.getInstance().setBoolean("showDefaultDone", i);
    }

    public static boolean getIsshow() {
        return SharedPreferencesClass.getInstance().getBoolean("showDefaultDone", true);
    }

    public static boolean getHomeRetention1Day() {
        return SharedPreferencesClass.getInstance().getBoolean(lehfv_HomeRetention1Day, false);
    }

    public static void setHomeRetention1Day(boolean value) {
        SharedPreferencesClass.getInstance().setBoolean(lehfv_HomeRetention1Day, value);
    }

    public static boolean getHomeRetention3Day() {
        return SharedPreferencesClass.getInstance().getBoolean(lehfv_HomeRetention3Day, false);
    }


    public static void setHomeRetention3Day(boolean value) {
        SharedPreferencesClass.getInstance().setBoolean(lehfv_HomeRetention3Day, value);
    }

    public static boolean getHomeRetention7Day() {
        return SharedPreferencesClass.getInstance().getBoolean(lehfv_HomeRetention7Day, false);
    }

    public static void setHomeRetention7Day(boolean value) {
        SharedPreferencesClass.getInstance().setBoolean(lehfv_HomeRetention7Day, value);
    }

    public static String getLanguage() {
        return SharedPreferencesClass.getInstance().getString(LANGUAGE_CODE, "en");
    }

    public static void setLanguage(String key) {
        SharedPreferencesClass.getInstance().setString(LANGUAGE_CODE, key);
    }

    public static boolean getLanguageScreen() {
        return SharedPreferencesClass.getInstance().getBoolean(LANGUAGE_SCREEN, false);
    }

    public static void setLanguageScreen(Boolean key) {
        SharedPreferencesClass.getInstance().setBoolean(LANGUAGE_SCREEN, key);
    }


    public static boolean getCallEndShow50() {
        return ADSPrefManage().getBoolean(CallEndShow50, false);
    }

    public static void setCallEndShow50(boolean value) {
        ADSPrefManage().edit().putBoolean(CallEndShow50, value).apply();
    }

    public static boolean getCallEndShow10() {
        return ADSPrefManage().getBoolean(CallEndShow10, false);
    }

    public static void setCallEndShow10(boolean value) {
        ADSPrefManage().edit().putBoolean(CallEndShow10, value).apply();
    }

    public static boolean getCallEndShow1() {
        return ADSPrefManage().getBoolean(CallEndShow1, false);
    }

    public static void setCallEndShow1(boolean value) {
        ADSPrefManage().edit().putBoolean(CallEndShow1, value).apply();
    }

    public static boolean getCallEndShow5() {
        return ADSPrefManage().getBoolean(CallEndShow5, false);
    }

    public static void setCallEndShow5(boolean value) {
        ADSPrefManage().edit().putBoolean(CallEndShow5, value).apply();
    }

    public static boolean getCallEndShow100() {
        return ADSPrefManage().getBoolean(CallEndShow100, false);
    }

    public static void setCallEndShow100(boolean value) {
        ADSPrefManage().edit().putBoolean(CallEndShow100, value).apply();
    }

    public static int getCallEndShowEvent() {
        return ADSPrefManage().getInt(CallEndShowEvent, 0);
    }

    public static void setCallEndShowEvent(int value) {
        ADSPrefManage().edit().putInt(CallEndShowEvent, value).apply();
    }

    public static boolean getCallRetention7Day() {
        return ADSPrefManage().getBoolean(CallRetention7Day, false);
    }

    public static void setCallRetention7Day(boolean value) {
        ADSPrefManage().edit().putBoolean(CallRetention7Day, value).apply();
    }

    public static boolean getCallRetention3Day() {
        return ADSPrefManage().getBoolean(CallRetention3Day, false);
    }

    public static void setCallRetention3Day(boolean value) {
        ADSPrefManage().edit().putBoolean(CallRetention3Day, value).apply();
    }

    public static boolean getCallRetention1Day() {
        return ADSPrefManage().getBoolean(CallRetention1Day, false);
    }

    public static void setCallRetention1Day(boolean value) {
        ADSPrefManage().edit().putBoolean(CallRetention1Day, value).apply();
    }


    public static boolean getNavigationShow() {
        return ADSPrefManage().getBoolean(NavigationShow, false);
    }

    public static void setNavigationShow(boolean value) {
        ADSPrefManage().edit().putBoolean(NavigationShow, value).apply();
    }

    public static String getRefferel() {
        return ADSPrefManage().getString(Refferel, "");
    }

    public static void setRefferel(String value) {
        ADSPrefManage().edit().putString(Refferel, value).apply();
    }

    public static String getAdsInterstitialType() {
        return ADSPrefManage().getString(AdsInterstitialType, "");
    }

    public static void setAdsInterstitialType(String value) {
        ADSPrefManage().edit().putString(AdsInterstitialType, value).apply();
    }

    public static String getSplashADType() {
        return ADSPrefManage().getString(SplashADType, "");
    }

    public static void setSplashADType(String value) {
        ADSPrefManage().edit().putString(SplashADType, value).apply();
    }

    public static String getFBNativeBannerID() {
        return ADSPrefManage().getString(FBNativeBannerID, "");
    }

    public static void setFBNativeBannerID(String value) {
        ADSPrefManage().edit().putString(FBNativeBannerID, value).apply();
    }

    private static SharedPreferences ADSPrefManage() {
        return ADSAppManage.getApp().getSharedPreferences(
                "ADSAppManage", Context.MODE_PRIVATE);
    }

    public static boolean getAds_Free() {
        return ADSPrefManage().getBoolean(Ads_Free, false);
    }

    public static void setAds_Free(boolean value) {
        ADSPrefManage().edit().putBoolean(Ads_Free, value).apply();
    }

    public static String getAdsAdmobNativeID() {
        return ADSPrefManage().getString(AdsAdmobNativeID, "");
    }

    public static void setAdsAdmobNativeID(String value) {
        ADSPrefManage().edit().putString(AdsAdmobNativeID, value).apply();
    }

    public static String getAdsAdmobNativeIdTwo() {
        return ADSPrefManage().getString(AdsAdmobNativeIdTwo, "");
    }

    public static void setAdsAdmobNativeIdTwo(String value) {
        ADSPrefManage().edit().putString(AdsAdmobNativeIdTwo, value).apply();
    }

    public static int getNativeByPage() {
        return ADSPrefManage().getInt(NativeByPage, 0);
    }

    public static void setNativeByPage(int value) {
        ADSPrefManage().edit().putInt(NativeByPage, value).apply();
    }

    public static String getAdsDisplayType() {
        return ADSPrefManage().getString(AdsDisplayType, "");
    }

    public static void setAdsDisplayType(String value) {
        ADSPrefManage().edit().putString(AdsDisplayType, value).apply();
    }

    public static String getAdsTypeManage() {
        return ADSPrefManage().getString(AdsTypeManage, "");
    }

    public static void setAdsTypeManage(String value) {
        ADSPrefManage().edit().putString(AdsTypeManage, value).apply();
    }

    public static String getBannerTypes() {
        return ADSPrefManage().getString(BannerTypes, "native_banner");
    }

    public static void setBannerTypes(String value) {
        ADSPrefManage().edit().putString(BannerTypes, value).apply();
    }

    public static String getNativeButtonColor() {
        return ADSPrefManage().getString(NativeButtonColor, "");
    }

    public static void setNativeButtonColor(String value) {
        ADSPrefManage().edit().putString(NativeButtonColor, value).apply();
    }

    public static String getNativeButtonTextColor() {
        return ADSPrefManage().getString(NativeButtonTextColor, "");
    }

    public static void setNativeButtonTextColor(String value) {
        ADSPrefManage().edit().putString(NativeButtonTextColor, value).apply();
    }

    public static int getAdsBackClick() {
        return ADSPrefManage().getInt(AdsBackClick, 0);
    }

    public static void setAdsBackClick(int value) {
        ADSPrefManage().edit().putInt(AdsBackClick, value).apply();
    }

    public static int getAdsClick() {
        return ADSPrefManage().getInt(AdsClick, 0);
    }

    public static void setAdsClick(int value) {
        ADSPrefManage().edit().putInt(AdsClick, value).apply();
    }

    public static String getPrivacyPolicy() {
        return ADSPrefManage().getString(PrivacyPolicy, "https://sites.google.com/view/messages02/home");
    }

    public static void setPrivacyPolicy(String value) {
        ADSPrefManage().edit().putString(PrivacyPolicy, value).apply();
    }

    public static boolean getComingSoon() {
        return ADSPrefManage().getBoolean(ComingSoon, false);
    }

    public static void setComingSoon(boolean value) {
        ADSPrefManage().edit().putBoolean(ComingSoon, value).apply();
    }


    public static boolean getExitAds() {
        return ADSPrefManage().getBoolean(ExitAds, false);
    }

    public static void setExitAds(Boolean value) {
        ADSPrefManage().edit().putBoolean(ExitAds, value).apply();
    }

    public static void scheduleInstallDayWorker(Context context) {
        androidx.work.PeriodicWorkRequest installDayWorkRequest =
//                new androidx.work.PeriodicWorkRequest.Builder(InstallDayWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
                new androidx.work.PeriodicWorkRequest.Builder(InstallDayWorker.class, 24, java.util.concurrent.TimeUnit.HOURS)
                        .addTag("InstallDayWork")
                        .build();

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "InstallDayUniqueWork",
//                androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                installDayWorkRequest
        );
//        android.util.Log.d("ADSMainClass", "InstallDayWorker scheduled for every 15 minutes (Testing).");
    }

}

