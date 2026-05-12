package com.example.contactmanager.Advertisement;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.telecom.Call;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

import com.example.contactmanager.utils.ThemeManager;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.yalantis.ucrop.BuildConfig;


import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.HiltAndroidApp;
import kotlin.jvm.internal.DefaultConstructorMarker;


public class MyApplication extends Application implements Configuration.Provider {
    private static MyApplication mInstance;

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    public static final Companion INSTANCE = new Companion(null);

    private static MyApplication EMIControllerOurInstance = new MyApplication();
    public static boolean FastStart = false;
    public static boolean isActivityChecked = false;
    public static boolean AppStartingScreenOpen = false;
    private int NumStarted = 0;
    private Call appCall;

    public static MyApplication getApp() {
        if (EMIControllerOurInstance == null) {
            EMIControllerOurInstance = new MyApplication();
        }
        return EMIControllerOurInstance;
    }

    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

    }

    public Call getAppCall() {
        return appCall;
    }

    public void setAppCall(Call appCall) {
        this.appCall = appCall;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        EMIControllerOurInstance = this;

        ThemeManager.INSTANCE.applyAppTheme(this);

        SetADSAppStarting();
//        registerActivityLifecycleCallbacks(this);
        //Admob
        List<String> testDeviceIds = Arrays.asList("");
        RequestConfiguration configuration = new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
        MobileAds.setRequestConfiguration(configuration);

        MobileAds.initialize(this, initializationStatus -> {
        });

        //facebook
        if (BuildConfig.DEBUG) {
            AdSettings.setTestMode(false);
        }
        AudienceNetworkAds.initialize(this);
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build();
    }

    public void onTerminate() {
        super.onTerminate();
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
                if (!ADSMainClass.getComingSoon()) {
                    if (AppStartingScreenOpen) {
                        if (NumStarted == 1) {
                           /* if (activity instanceof LanguageActivity ||
                                activity instanceof PermissionActivity ||
                                activity instanceof OverlayPermissionActivity ||
                                activity instanceof DefaultPermissionActivity ||
                                activity instanceof CallEndActivity ||
                                activity instanceof SplashActivity ||
                                activity instanceof ParentCallActivity ||
                                IS_LANGUAGE_SCREEN_SHOW
                            ) {
                                return;
                            }*/
                            if (!FastStart) {
                                if (ADSUtilitis.IsNetworkConnected(activity)) {

                                    new Handler().postDelayed(() -> {
                                        if (ADSMainClass.getSplashADType().equals("Appopen")) {
                                            ADSAppStartingLoad.ADSLoadAppOpen(activity);
                                        } else {
                                            ADSAppStartingLoad.ADSAdmobInterstitial(activity);
                                        }
                                    }, 100);


                                }
                            } else {
                                FastStart = false;
                            }
                        }
                    }
                }
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

                } else {
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
}
