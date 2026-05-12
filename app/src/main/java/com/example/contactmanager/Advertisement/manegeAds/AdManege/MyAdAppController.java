package com.example.contactmanager.Advertisement.manegeAds.AdManege;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telecom.Call;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleObserver;

import com.example.contactmanager.Advertisement.MyApplication;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.onesignal.notifications.INotificationClickEvent;
import com.onesignal.notifications.INotificationClickListener;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MyAdAppController extends MyApplication implements  LifecycleObserver {
    private static final String ONESIGNAL_APP_ID = "1300638c-ba4c-4adc-bd44-d8f2f95c5fb7";
    public Call app_call;
//    public CallReceiveService In_CallService;
    public Call New_Call;
    private static MyAdAppController conl_ourInstance = new MyAdAppController();

    public static MyAdAppController conl_getMainApps() {
        if (conl_ourInstance == null) {
            conl_ourInstance = new MyAdAppController();
        }
        return conl_ourInstance;
    }
    public int Sel_Language = 0;
    public static boolean dialogboolean = true;
    SharedPreferences.Editor editor;
//    PreferenceShareCalls sharedPrefStore;
    SharedPreferences sharedPreferences;
//    public SessionMainManager Session_Manager;


    @Override
    public void onCreate() {
        super.onCreate();

        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
        OneSignal.getNotifications().addClickListener(new INotificationClickListener() {
            @Override
            public void onClick(@NonNull INotificationClickEvent iNotificationClickEvent) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(iNotificationClickEvent.getNotification().getLaunchURL()));
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        /*SessionMainManager sessionManager2 = SessionMainManager.getSessionManager(getApplicationContext());
        this.Session_Manager = sessionManager2;
        this.Sel_Language = sessionManager2.getLanguage();

        conl_ourInstance = this;
        PreferenceUtils.getInstance().init(conl_ourInstance);
//        registerActivityLifecycleCallbacks(this);


        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.sharedPreferences = defaultSharedPreferences;
        this.editor = defaultSharedPreferences.edit();
        PreferenceShareCalls sharedPrefStore = new PreferenceShareCalls(this);
        this.sharedPrefStore = sharedPrefStore;
        setLocale(sharedPrefStore.getlanguage());*/

    }

    public void setLocale(String str) {
        if (str.equals("")) {
            str = "en";
        }
        Locale locale = new Locale(str);
        Resources resources = getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, displayMetrics);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }

    public static Context getContext() {
        return conl_ourInstance.getApplicationContext();
    }


    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= 34 && getApplicationInfo().targetSdkVersion >= 34) {
            return super.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            return super.registerReceiver(receiver, filter);
        }
    }

}