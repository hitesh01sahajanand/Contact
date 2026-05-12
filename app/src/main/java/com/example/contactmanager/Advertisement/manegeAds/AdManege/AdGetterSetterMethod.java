package com.example.contactmanager.Advertisement.manegeAds.AdManege;

import android.content.Context;
import android.content.SharedPreferences;


public class AdGetterSetterMethod {


    public static String conl_HomeRetention1Day = "AdEvent_Mains_HomeRetention1Day";
    public static String conl_HomeRetention3Day = "AdEvent_Mains_HomeRetention3Day";
    public static String conl_HomeRetention7Day = "AdEvent_Mains_HomeRetention7Day";

    //    public static String conl_SpeedDialNumber1 = "AdEvent_Mains_SpeedDialNumber1";
//    public static String conl_SpeedDialNumber2 = "AdEvent_Mains_SpeedDialNumber2";
//    public static String conl_SpeedDialNumber3 = "AdEvent_Mains_SpeedDialNumber3";
//    public static String conl_SpeedDialNumber4 = "AdEvent_Mains_SpeedDialNumber4";
//    public static String conl_SpeedDialNumber5 = "AdEvent_Mains_SpeedDialNumber5";
//    public static String conl_SpeedDialNumber6 = "AdEvent_Mains_SpeedDialNumber6";
//    public static String conl_SpeedDialNumber7 = "AdEvent_Mains_SpeedDialNumber7";
//    public static String conl_SpeedDialNumber8 = "AdEvent_Mains_SpeedDialNumber8";
//    public static String conl_SpeedDialNumber9 = "AdEvent_Mains_SpeedDialNumber9";
    public static String conl_AppIntroShow = "AdEvent_Mains_AppIntroShows";
    public static String conl_Number = "AdEvent_Mains_Number";
    public static String conl_Dialer_Permission = "AdEvent_Mains_dialer_Permission";
    public static String defultapp = "defultapp";
    public static String phone_dialer_Default = "phone_dialer_Default";
    public static String lkepv___AlternateType = "AdEvent_Mains_AlternateType";
    public static String lkepv_PostCallInformation = "AdEvent_Mains_PostCallInformation";
    public static String lkepv___CallerIdDefault = "AdEvent_Mains_CallerIdDefault";
    public static String LANGUAGE_SCREEN_SHOW = "language_screen_show1";

    public static void setPostCallInformation(boolean value) {
        conl_getPref().edit().putBoolean(lkepv_PostCallInformation, value).apply();
    }

    public static boolean getPostCallInformation() {
        return conl_getPref().getBoolean(lkepv_PostCallInformation, true);
    }

    public static void setLanguageScreenShow(boolean value) {
        conl_getPref().edit().putBoolean(LANGUAGE_SCREEN_SHOW, value).apply();
    }

    public static boolean getIsCallEndShow() {
        return conl_getPref().getBoolean("is_callend_show", false);
    }

    public static void setIsCallEndShow(boolean value) {
        conl_getPref().edit().putBoolean("is_callend_show", value).apply();
    }

    public static boolean getLanguageScreenShow() {
        return conl_getPref().getBoolean(LANGUAGE_SCREEN_SHOW, false);
    }


    public static void setCallerIdDefault(boolean value) {
        conl_getPref().edit().putBoolean(lkepv___CallerIdDefault, value).apply();
    }

    public static boolean getCallerIdDefault() {
        return conl_getPref().getBoolean(lkepv___CallerIdDefault, false);
    }


    public static boolean getDefaultScreen() {
        return conl_getPref().getBoolean(phone_dialer_Default, false);
    }

//    public static void setSpeedDialNumber1(String value) {
//        conl_getPref().edit().putString(conl_SpeedDialNumber1, value).apply();
//    }
//
//    public static String getSpeedDialNumber1() {
//        return conl_getPref().getString(conl_SpeedDialNumber1, "");
//    }
//
//    public static void setSpeedDialNumber2(String value) {
//        conl_getPref().edit().putString(conl_SpeedDialNumber2, value).apply();
//    }
//
//    public static String getSpeedDialNumber2() {
//        return conl_getPref().getString(conl_SpeedDialNumber2, "");
//    }
//
//    public static void setSpeedDialNumber3(String value) {
//        conl_getPref().edit().putString(conl_SpeedDialNumber3, value).apply();
//    }
//
//    public static String getSpeedDialNumber3() {
//        return conl_getPref().getString(conl_SpeedDialNumber3, "");
//    }
//
//    public static void setSpeedDialNumber4(String value) {
//        conl_getPref().edit().putString(conl_SpeedDialNumber4, value).apply();
//    }
//
//    public static String getSpeedDialNumber4() {
//        return conl_getPref().getString(conl_SpeedDialNumber4, "");
//    }
//
//
//    public static void setSpeedDialNumber5(String value) {
//        conl_getPref().edit().putString(conl_SpeedDialNumber5, value).apply();
//    }
//
//    public static String getSpeedDialNumber5() {
//        return conl_getPref().getString(conl_SpeedDialNumber5, "");
//    }
//
//    public static void setSpeedDialNumber6(String value) {
//        conl_getPref().edit().putString(conl_SpeedDialNumber6, value).apply();
//    }
//
//    public static String getSpeedDialNumber6() {
//        return conl_getPref().getString(conl_SpeedDialNumber6, "");
//    }
//
//    public static void setSpeedDialNumber7(String value) {
//        conl_getPref().edit().putString(conl_SpeedDialNumber7, value).apply();
//    }
//
//    public static String getSpeedDialNumber7() {
//        return conl_getPref().getString(conl_SpeedDialNumber7, "");
//    }
//
//    public static void setSpeedDialNumber8(String value) {
//        conl_getPref().edit().putString(conl_SpeedDialNumber8, value).apply();
//    }
//
//    public static String getSpeedDialNumber8() {
//        return conl_getPref().getString(conl_SpeedDialNumber8, "");
//    }
//
//    public static void setSpeedDialNumber9(String value) {
//        conl_getPref().edit().putString(conl_SpeedDialNumber9, value).apply();
//    }
//
//    public static String getSpeedDialNumber9() {
//        return conl_getPref().getString(conl_SpeedDialNumber9, "");
//    }
//

    public static void putPermission(Boolean bool) {
        conl_getPref().edit().putBoolean(conl_Dialer_Permission, bool).apply();

    }

    public static boolean getPermission() {
        return conl_getPref().getBoolean(conl_Dialer_Permission, false);
    }

    public static void setDefultapp(Boolean bool) {
        conl_getPref().edit().putBoolean(defultapp, bool).apply();

    }

    public static boolean getDefultapp() {
        return conl_getPref().getBoolean(defultapp, false);
    }

    public static void setNumbers(String value) {
        conl_getPref().edit().putString(conl_Number, value).apply();
    }

    public static String getNumbers() {
        return conl_getPref().getString(conl_Number, "");
    }

    public static void setAppIntroShow(boolean value) {
        conl_getPref().edit().putBoolean(conl_AppIntroShow, value).apply();
    }

    public static boolean getAppIntroShow() {
        return conl_getPref().getBoolean(conl_AppIntroShow, false);
    }


    public static void setHomeRetention7Day(boolean value) {
        conl_getPref().edit().putBoolean(conl_HomeRetention7Day, value).apply();
    }

    public static boolean getHomeRetention7Day() {
        return conl_getPref().getBoolean(conl_HomeRetention7Day, false);
    }

    public static void setHomeRetention3Day(boolean value) {
        conl_getPref().edit().putBoolean(conl_HomeRetention3Day, value).apply();
    }

    public static boolean getHomeRetention3Day() {
        return conl_getPref().getBoolean(conl_HomeRetention3Day, false);
    }

    public static void setHomeRetention1Day(boolean value) {
        conl_getPref().edit().putBoolean(conl_HomeRetention1Day, value).apply();
    }

    public static boolean getHomeRetention1Day() {
        return conl_getPref().getBoolean(conl_HomeRetention1Day, false);
    }


    private static SharedPreferences conl_getPref() {
        return MyAdAppController.conl_getMainApps().getSharedPreferences(
                "Contacts - Phone Dialer Main", Context.MODE_PRIVATE);
    }
}

