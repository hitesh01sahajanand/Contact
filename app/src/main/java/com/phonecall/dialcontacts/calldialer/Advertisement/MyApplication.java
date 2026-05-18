package com.phonecall.dialcontacts.calldialer.Advertisement;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;

import com.facebook.ads.AdSettings;
import com.facebook.ads.BuildConfig;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.HiltAndroidApp;
import kotlin.jvm.Volatile;

@HiltAndroidApp
public class MyApplication extends ADSAppManage implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "MyApplication";

    private static MyApplication mInstance;

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    @Volatile
    private static boolean isAppInForeground = false;

    public static boolean isAppInForeground() {
        return isAppInForeground;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        isAppInForeground = true;
        Log.d(TAG, "App moved to Foreground");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        isAppInForeground = false;
        Log.d(TAG, "App moved to Background");
    }


    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        registerActivityLifecycleCallbacks(this);
//        InMobi.initialize(this, "YOUR_ACCOUNT_ID_HERE");

        initializeAdMob();
//        initializeInMobi();

//        AdSettings.addTestDevice("86738ADCA5544F4A4BED0C53C0F60CFD");
//        AdSettings.setTestMode(true);

        //facebook
        if (BuildConfig.DEBUG) {
            AdSettings.setTestMode(false);
        }
//        AudienceNetworkAds.initialize(this);
//        AudienceNetworkInitializeHelper.initialize(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    private void initializeAdMob() {
        List<String> testDeviceIds = Arrays.asList("");
        RequestConfiguration configuration = new RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build();
        MobileAds.setRequestConfiguration(configuration);
        MobileAds.initialize(this, initializationStatus -> {
        });
    }

//    private void initializeInMobi() {
//        String accountId = ADSMainClass.INMOBI_ACCOUNT_ID;
//        if (TextUtils.isEmpty(accountId)) {
////            Log.w(TAG, "Missing InMobi account id; skipping SDK initialization.");
//            return;
//        }
//
//        JSONObject consentObject = new JSONObject();
//        try {
//            consentObject.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, true);
////            consentObject.put("gdpr", "0");
////            Log.d(TAG, "initializeInMobi: " );
//        } catch (JSONException exception) {
//            Log.w(TAG, "Unable to build InMobi consent object", exception);
//        }
////        Log.d(TAG, "initializeInMobi: " +accountId);
//        InMobiSdk.setLogLevel(BuildConfig.DEBUG ? InMobiSdk.LogLevel.DEBUG : InMobiSdk.LogLevel.ERROR);
//        InMobiSdk.init(this, accountId,consentObject,null);
//    }
}
