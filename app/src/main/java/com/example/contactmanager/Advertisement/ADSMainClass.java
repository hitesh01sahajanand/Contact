package com.example.contactmanager.Advertisement;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

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
    public static String AdmobInterstitialIdList = "AdmobInterstitialIdList";
    public static String AdmobFullNativeIDList = "AdmobFullNativeIDList";
    public static String AdmobCustomIdList = "AdmobCustomIdList";
    public static String AdmobBannerIdList = "AdmobBannerIdList";
    public static String setAdmob_Call_End_Banner_ID_List = "setAdmob_Call_End_Banner_ID_List";
    public static String AdmobAPPStartingIdList = "AdmobAPPStartingIdList";
    public static String FBNativeFullList = "FBNativeFullList";
    public static String FB_Call_End_Native_ID_List = "FB_Call_End_Native_ID_List";
    public static String FB_Call_End_Banner_ID_List = "FB_Call_End_Banner_ID_List";
    public static String FBInterstitialIdList = "FBInterstitialIdList";
    public static String fbNativeFullList = "fbNativeFullList";
    public static String Refferel = "Refferel";

    public static String NavigationShow = "NavigationShow";
    public static String IsShowCallEnd = "IsShowCallEnd";
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
    public static String lehfv_HomeRetention1Day = "Simple_HomeRetention1Day";
    public static String lehfv_HomeRetention3Day = "Simple_HomeRetention3Day";
    public static String lehfv_HomeRetention7Day = "Simple_HomeRetention7Day";
    //

    public static Boolean IS_LANGUAGE_SCREEN_SHOW = false;
    public static String SHOW_ADS_AFTER_LANGUAGE = "show_ads_after_language";
    public static String APP_OPEN_ID = "app_open_id";
    public static String LANGUAGE_SCREEN_NATIVE = "language_screen_native";
    public static String HOME_SCREEN_BANNER = "home_screen_banner";
    public static String INTER_FIRST_TIME = "inter_first_time";
    public static String INTER_SECOND_TIME = "inter_second_time";
    public static String CALL_END_ADAPTIVE_BANNER = "call_end_adaptive_banner";
    public static String CONTACT_SCREEN_BANNER = "contact_screen_banner";
    public static String OTHER_BANNER = "other_banner";
    public static String PERMISSION_SCREEN_BANNER = "permission_screen_banner";
    public static String EXIT_SCREEN_NATIVE = "exit_screen_native";


    public static String conl_SpeedDialContact1 = "AdEvent_Mains_SpeedDialContact1";
    public static String conl_SpeedDialContact2 = "AdEvent_Mains_SpeedDialContact2";
    public static String conl_SpeedDialContact3 = "AdEvent_Mains_SpeedDialContact3";
    public static String conl_SpeedDialContact4 = "AdEvent_Mains_SpeedDialContact4";
    public static String conl_SpeedDialContact5 = "AdEvent_Mains_SpeedDialContact5";
    public static String conl_SpeedDialContact6 = "AdEvent_Mains_SpeedDialContact6";
    public static String conl_SpeedDialContact7 = "AdEvent_Mains_SpeedDialContact7";
    public static String conl_SpeedDialContact8 = "AdEvent_Mains_SpeedDialContact8";
    public static String conl_SpeedDialContact9 = "AdEvent_Mains_SpeedDialContact9";

    public static String conl_SpeedDialNumber1 = "AdEvent_Mains_SpeedDialNumber1";
    public static String conl_SpeedDialNumber2 = "AdEvent_Mains_SpeedDialNumber2";
    public static String conl_SpeedDialNumber3 = "AdEvent_Mains_SpeedDialNumber3";
    public static String conl_SpeedDialNumber4 = "AdEvent_Mains_SpeedDialNumber4";
    public static String conl_SpeedDialNumber5 = "AdEvent_Mains_SpeedDialNumber5";
    public static String conl_SpeedDialNumber6 = "AdEvent_Mains_SpeedDialNumber6";
    public static String conl_SpeedDialNumber7 = "AdEvent_Mains_SpeedDialNumber7";
    public static String conl_SpeedDialNumber8 = "AdEvent_Mains_SpeedDialNumber8";
    public static String conl_SpeedDialNumber9 = "AdEvent_Mains_SpeedDialNumber9";


    public static List<String> getAdmobNativeCallEndIDSList() {
        String Json = ADSPrefManage().getString(AdmobCallEndNativeList, "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<String> stringArrayList = gson.fromJson(Json, type);
        return stringArrayList;
    }

    public static void setAdmobNativeCallEndIDSList(List<String> admobInterstitialIdList) {
        Gson gson = new Gson();
        String json = gson.toJson(admobInterstitialIdList);
        ADSPrefManage().edit().putString(AdmobCallEndNativeList, json).apply();
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


    public static void setSpeedDialContact1(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialContact1, value).apply();
    }
    public static String getSpeedDialContact1() {
        return ADSPrefManage().getString(conl_SpeedDialContact1, "");
    }

    public static void setSpeedDialContact2(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialContact2, value).apply();
    }

    public static String getSpeedDialContact2() {
        return ADSPrefManage().getString(conl_SpeedDialContact2, "");
    }

    public static void setSpeedDialContact3(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialContact3, value).apply();
    }

    public static String getSpeedDialContact3() {
        return ADSPrefManage().getString(conl_SpeedDialContact3, "");
    }

    public static void setSpeedDialContact4(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialContact4, value).apply();
    }

    public static String getSpeedDialContact4() {
        return ADSPrefManage().getString(conl_SpeedDialContact4, "");
    }


    public static void setSpeedDialContact5(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialContact5, value).apply();
    }

    public static String getSpeedDialContact5() {
        return ADSPrefManage().getString(conl_SpeedDialContact5, "");
    }

    public static void setSpeedDialContact6(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialContact6, value).apply();
    }

    public static String getSpeedDialContact6() {
        return ADSPrefManage().getString(conl_SpeedDialContact6, "");
    }

    public static void setSpeedDialContact7(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialContact7, value).apply();
    }

    public static String getSpeedDialContact7() {
        return ADSPrefManage().getString(conl_SpeedDialContact7, "");
    }

    public static void setSpeedDialContact8(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialContact8, value).apply();
    }

    public static String getSpeedDialContact8() {
        return ADSPrefManage().getString(conl_SpeedDialContact8, "");
    }

    public static void setSpeedDialContact9(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialContact9, value).apply();
    }

    public static String getSpeedDialContact9() {
        return ADSPrefManage().getString(conl_SpeedDialContact9, "");
    }



    public static void setSpeedDialNumber1(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialNumber1, value).apply();
    }

    public static String getSpeedDialNumber1() {
        return ADSPrefManage().getString(conl_SpeedDialNumber1, "");
    }

    public static void setSpeedDialNumber2(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialNumber2, value).apply();
    }

    public static String getSpeedDialNumber2() {
        return ADSPrefManage().getString(conl_SpeedDialNumber2, "");
    }

    public static void setSpeedDialNumber3(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialNumber3, value).apply();
    }

    public static String getSpeedDialNumber3() {
        return ADSPrefManage().getString(conl_SpeedDialNumber3, "");
    }

    public static void setSpeedDialNumber4(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialNumber4, value).apply();
    }

    public static String getSpeedDialNumber4() {
        return ADSPrefManage().getString(conl_SpeedDialNumber4, "");
    }


    public static void setSpeedDialNumber5(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialNumber5, value).apply();
    }

    public static String getSpeedDialNumber5() {
        return ADSPrefManage().getString(conl_SpeedDialNumber5, "");
    }

    public static void setSpeedDialNumber6(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialNumber6, value).apply();
    }

    public static String getSpeedDialNumber6() {
        return ADSPrefManage().getString(conl_SpeedDialNumber6, "");
    }

    public static void setSpeedDialNumber7(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialNumber7, value).apply();
    }

    public static String getSpeedDialNumber7() {
        return ADSPrefManage().getString(conl_SpeedDialNumber7, "");
    }

    public static void setSpeedDialNumber8(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialNumber8, value).apply();
    }

    public static String getSpeedDialNumber8() {
        return ADSPrefManage().getString(conl_SpeedDialNumber8, "");
    }

    public static void setSpeedDialNumber9(String value) {
        ADSPrefManage().edit().putString(conl_SpeedDialNumber9, value).apply();
    }

    public static String getSpeedDialNumber9() {
        return ADSPrefManage().getString(conl_SpeedDialNumber9, "");
    }







    public static boolean getHomeRetention1Day() {
        return ADSPrefManage().getBoolean(lehfv_HomeRetention1Day, false);
    }
    public static void setHomeRetention1Day(boolean value) {
        ADSPrefManage().edit().putBoolean(lehfv_HomeRetention1Day, value);
    }

    public static boolean getHomeRetention3Day() {
        return ADSPrefManage().getBoolean(lehfv_HomeRetention3Day, false);
    }

    public static void setHomeRetention3Day(boolean value) {
        ADSPrefManage().edit().putBoolean(lehfv_HomeRetention3Day, value);
    }

    public static boolean getHomeRetention7Day() {
        return ADSPrefManage().getBoolean(lehfv_HomeRetention7Day, false);
    }

    public static void setHomeRetention7Day(boolean value) {
        ADSPrefManage().edit().putBoolean(lehfv_HomeRetention7Day, value);
    }


    public static String getAdmobNativeCallEndUnitId() {
        return ADSPrefManage().getString(Admob_CallEnd_Native_Unit_Id, "");
    }

    public static void setAdmobNativeCallEndUnitId(String value) {
        ADSPrefManage().edit().putString(Admob_CallEnd_Native_Unit_Id, value).apply();
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

    public static void isAdShowComplete(Boolean i) {
        ADSPrefManage().edit().putBoolean("AdInterOneTimeShow", i).apply();
    }

    public static boolean isAdShowComplete() {
        return ADSPrefManage().getBoolean("AdInterOneTimeShow", false);
    }

    public static boolean getIsShowCallEnd() {
        return ADSPrefManage().getBoolean(IsShowCallEnd, false);
    }

    public static void setIsShowCallEnd(boolean value) {
        ADSPrefManage().edit().putBoolean(IsShowCallEnd, value).apply();
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
        return MyApplication.getApp().getSharedPreferences(
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

    public static String getFBNativeId() {
        return ADSPrefManage().getString(FBNativeId, "");
    }

    public static void setFBNativeId(String value) {
        ADSPrefManage().edit().putString(FBNativeId, value).apply();
    }

    public static String get_FB_Call_End_Native_Id() {
        return ADSPrefManage().getString(FB_Call_End_Native_Id, "");
    }

    public static void set_FB_Call_End_Native_Id(String value) {
        ADSPrefManage().edit().putString(FB_Call_End_Native_Id, value).apply();
    }

    public static String get_FB_Call_End_Banner_Id() {
        return ADSPrefManage().getString(FB_Call_End_Banner_Id, "");
    }

    public static void set_FB_Call_End_Banner_Id(String value) {
        ADSPrefManage().edit().putString(FB_Call_End_Banner_Id, value).apply();
    }

    public static String getAdsAdmobBannerId() {
        return ADSPrefManage().getString(AdsAdmobBannerId, "");
    }

    public static void setAdsAdmobBannerId(String value) {
        ADSPrefManage().edit().putString(AdsAdmobBannerId, value).apply();
    }

    public static String get_Admob_call_end_BannerId() {
        return ADSPrefManage().getString(Admob_call_end_BannerId, "");
    }

    public static void set_Admob_call_end_BannerId(String value) {
        ADSPrefManage().edit().putString(Admob_call_end_BannerId, value).apply();
    }

    public static String getAdsAdmobInterstitialID() {
        return ADSPrefManage().getString(AdsAdmobInterstitialID, "");
    }

    public static void setAdsAdmobInterstitialID(String value) {
        ADSPrefManage().edit().putString(AdsAdmobInterstitialID, value).apply();
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

    public static String getAppStartingId() {
        return ADSPrefManage().getString(AppStartingId, "");
    }

    public static void setAppStartingId(String value) {
        ADSPrefManage().edit().putString(AppStartingId, value).apply();
    }

    public static String getAdsFBInterstitialID() {
        return ADSPrefManage().getString(AdsFBInterstitialID, "");
    }

    public static void setAdsFBInterstitialID(String value) {
        ADSPrefManage().edit().putString(AdsFBInterstitialID, value).apply();
    }

    public static boolean getAdsOneByOneIDS() {
        return ADSPrefManage().getBoolean(AdsOneByOneIDS, false);
    }

    public static void setAdsOneByOneIDS(boolean value) {
        ADSPrefManage().edit().putBoolean(AdsOneByOneIDS, value).apply();
    }

    public static String getPrivacyPolicy() {
        return ADSPrefManage().getString(PrivacyPolicy, "");
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


    public static void setScreenShow(Integer value) {
        ADSPrefManage().edit().putInt(ScreenShow, value).apply();
    }

    public static List<String> getFBInterstitialIdList() {
        String Json = ADSPrefManage().getString(FBInterstitialIdList, "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<String> arrayList = gson.fromJson(Json, type);
        return arrayList;
    }

    public static void setFBInterstitialIdList(List<String> admobBannerIdList) {
        Gson gson = new Gson();
        String json = gson.toJson(admobBannerIdList);
        ADSPrefManage().edit().putString(FBInterstitialIdList, json).apply();
    }

    public static List<String> getfbNativeFullList() {
        String Json = ADSPrefManage().getString(fbNativeFullList, "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<String> arrayList = gson.fromJson(Json, type);
        return arrayList;
    }

    public static void setfbNativeFullList(List<String> admobBannerIdList) {
        Gson gson = new Gson();
        String json = gson.toJson(admobBannerIdList);
        ADSPrefManage().edit().putString(fbNativeFullList, json).apply();
    }

    public static List<String> getFBNativeFullList() {
        String Json = ADSPrefManage().getString(FBNativeFullList, "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<String> arrayList = gson.fromJson(Json, type);
        return arrayList;
    }

    public static void setFBNativeFullList(List<String> admobBannerIdList) {
        Gson gson = new Gson();
        String json = gson.toJson(admobBannerIdList);
        ADSPrefManage().edit().putString(FBNativeFullList, json).apply();
    }

    public static List<String> getFB_Call_End_Native_ID_List() {
        String Json = ADSPrefManage().getString(FB_Call_End_Native_ID_List, "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<String> arrayList = gson.fromJson(Json, type);
        return arrayList;
    }

    public static void setFB_Call_End_Native_ID_List(List<String> admobBannerIdList) {
        Gson gson = new Gson();
        String json = gson.toJson(admobBannerIdList);
        ADSPrefManage().edit().putString(FB_Call_End_Native_ID_List, json).apply();
    }

    public static List<String> getFB_Call_End_Banner_ID_List() {
        String Json = ADSPrefManage().getString(FB_Call_End_Banner_ID_List, "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<String> arrayList = gson.fromJson(Json, type);
        return arrayList;
    }

    public static void setFB_Call_End_Banner_ID_List(List<String> admobBannerIdList) {
        Gson gson = new Gson();
        String json = gson.toJson(admobBannerIdList);
        ADSPrefManage().edit().putString(FB_Call_End_Banner_ID_List, json).apply();
    }

    public static List<String> getAdmobAPPStartingIdList() {
        String Json = ADSPrefManage().getString(AdmobAPPStartingIdList, "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<String> arrayList = gson.fromJson(Json, type);
        return arrayList;
    }

    public static void setAdmobAPPStartingIdList(List<String> admobBannerIdList) {
        Gson gson = new Gson();
        String json = gson.toJson(admobBannerIdList);
        ADSPrefManage().edit().putString(AdmobAPPStartingIdList, json).apply();
    }

    public static List<String> getAdmobBannerIdList() {
        String Json = ADSPrefManage().getString(AdmobBannerIdList, "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<String> arrayList = gson.fromJson(Json, type);
        return arrayList;
    }

    public static void setAdmobBannerIdList(List<String> admobBannerIdList) {
        Gson gson = new Gson();
        String json = gson.toJson(admobBannerIdList);
        ADSPrefManage().edit().putString(AdmobBannerIdList, json).apply();
    }

    public static List<String> getAdmob_Call_End_Banner_ID_List() {
        String Json = ADSPrefManage().getString(setAdmob_Call_End_Banner_ID_List, "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<String> arrayList = gson.fromJson(Json, type);
        return arrayList;
    }

    public static void setAdmob_Call_End_Banner_ID_List(List<String> admobBannerIdList) {
        Gson gson = new Gson();
        String json = gson.toJson(admobBannerIdList);
        ADSPrefManage().edit().putString(setAdmob_Call_End_Banner_ID_List, json).apply();
    }

    public static List<String> getAdmobCustomIdList() {
        String Json = ADSPrefManage().getString(AdmobCustomIdList, "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<String> arrayList = gson.fromJson(Json, type);
        return arrayList;
    }

    public static void setAdmobCustomIdList(List<String> admobNativeCustomIdList) {
        Gson gson = new Gson();
        String json = gson.toJson(admobNativeCustomIdList);
        ADSPrefManage().edit().putString(AdmobCustomIdList, json).apply();
    }

    public static List<String> getAdmobFullNativeIDList() {
        String Json = ADSPrefManage().getString(AdmobFullNativeIDList, "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<String> stringArrayList = gson.fromJson(Json, type);
        return stringArrayList;
    }

    public static void setAdmobFullNativeIDList(List<String> admobNativeIdList) {
        Gson gson = new Gson();
        String json = gson.toJson(admobNativeIdList);
        ADSPrefManage().edit().putString(AdmobFullNativeIDList, json).apply();
    }


    public static List<String> getAdmobInterstitialIdList() {
        String Json = ADSPrefManage().getString(AdmobInterstitialIdList, "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<String> stringArrayList = gson.fromJson(Json, type);
        return stringArrayList;
    }

    public static void setAdmobInterstitialIdList(List<String> admobInterstitialIdList) {
        Gson gson = new Gson();
        String json = gson.toJson(admobInterstitialIdList);
        ADSPrefManage().edit().putString(AdmobInterstitialIdList, json).apply();
    }

    public static boolean getExitAds() {
        return ADSPrefManage().getBoolean(ExitAds, false);
    }

    public static void setExitAds(Boolean value) {
        ADSPrefManage().edit().putBoolean(ExitAds, value).apply();
    }

}

