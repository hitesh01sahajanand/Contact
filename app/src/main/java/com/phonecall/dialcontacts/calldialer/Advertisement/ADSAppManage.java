package com.phonecall.dialcontacts.calldialer.Advertisement;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.facebook.ads.AudienceNetworkAds;

public class ADSAppManage extends Application {

    private static ADSAppManage EMIControllerOurInstance = new ADSAppManage();
    public static boolean FastStart = false;
    public static boolean isActivityChecked = false;
    public static boolean AppStartingScreenOpen = false;
    private int NumStarted = 0;
    public static boolean dialogboolean = true;
    public static long appOpenBlockUntil = 0;
    public static boolean isAppOpenBlocked = false;

    public static void blockAppOpenAd(long durationMs) {
        appOpenBlockUntil = System.currentTimeMillis() + durationMs;
    }
//    private static final String ONESIGNAL_APP_ID = "b24089d3-5408-41d2-a830-0d9a96bd39ee";

//    public static final Companion INSTANCE = new Companion(null);

//    public static final class Companion {
//        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
//            this();
//        }
//
//        private Companion() {
//        }
//
//
////        public final void setShow_inter_ads(boolean z) {
////            ADSAppManage.show_inter_ads = z;
////        }
//
//    }


    public static ADSAppManage getApp() {
        if (EMIControllerOurInstance == null) {
            EMIControllerOurInstance = new ADSAppManage();
        }
        return EMIControllerOurInstance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        EMIControllerOurInstance = this;
        AudienceNetworkAds.initialize(this);
        SetADSAppStarting();

//        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
//        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
//
//        OneSignal.getNotifications().addClickListener(new INotificationClickListener() {
//            @Override
//            public void onClick(@NonNull INotificationClickEvent iNotificationClickEvent) {
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(iNotificationClickEvent.getNotification().getLaunchURL()));
//                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
//            }
//        });
    }

    private void SetADSAppStarting() {

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                isActivityChecked = true;
                NumStarted++;

                String className = activity.getClass().getSimpleName();
                if (className.equals("LanguageActivity") ||
                        className.equals("CallEndActivity") ||
                        className.equals("SplashActivity") ||
                        className.equals("CallActivity") ||
                        className.equals("OverlayPermissionActivity")
                ) {
                    return;
                }

                if (System.currentTimeMillis() < appOpenBlockUntil) {
                    return;
                }

                if (isAppOpenBlocked) {
                    isAppOpenBlocked = false;
                    return;
                }

                if (!ADSMainClass.getComingSoon()) {
                    if (AppStartingScreenOpen) {
                        if (NumStarted == 1) {

                            if (!FastStart) {
                                if (ADSUtilitis.IsNetworkConnected(activity)) {
                                    if (!ADSMainClass.getAppOpenBackgroundShow()) {
                                        return;
                                    }

                                    if (!canShowAppOpenAd()) {
                                        return;
                                    }

                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            incrementAdCount();
//                                            if (ADSMainClass.getSplashADType().equalsIgnoreCase("Appopen")) {
//                                                Log.d("Appopenss", "run: " );
                                                ADSAppBackground.ADSLoadAppOpen(activity);
//                                            } else {
//                                                ADSAppStartingLoad.ADSAdmobInterstitial(activity);
//                                            }
                                        }
                                    }, 100);
                                }
                            } else {
                                FastStart = false;
                            }
//                            }
                        }
                    }
                }
//                NumStarted++;

            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {

                NumStarted--;
                if (NumStarted == 0) {
                    ADSAppBackground.ADSPreloadAppOpen(activity);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });

    }


    public static boolean canShowAppOpenAd() {
        if (EMIControllerOurInstance == null) return false;

        int limit = ADSMainClass.getAppOpenAdDailyLimit();
        if (limit <= 0) {
            return false;
        }

        android.content.SharedPreferences prefs = EMIControllerOurInstance.getSharedPreferences("AdPrefs", Context.MODE_PRIVATE);
        String todayString = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String savedDate = prefs.getString("last_ad_date", "");

        if (!todayString.equals(savedDate)) {
            return true;
        }

        int count = prefs.getInt("ad_count_today", 0);
        return count < limit;
    }

    public static void incrementAdCount() {
        if (EMIControllerOurInstance == null) return;
        android.content.SharedPreferences prefs = EMIControllerOurInstance.getSharedPreferences("AdPrefs", Context.MODE_PRIVATE);
        String todayString = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String savedDate = prefs.getString("last_ad_date", "");
        int count = prefs.getInt("ad_count_today", 0);

        if (!todayString.equals(savedDate)) {
            count = 0;
            prefs.edit().putString("last_ad_date", todayString).apply();
        }
        prefs.edit().putInt("ad_count_today", count + 1).apply();
    }



    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }
}
