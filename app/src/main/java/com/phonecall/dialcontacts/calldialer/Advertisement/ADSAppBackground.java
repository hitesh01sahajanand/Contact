package com.phonecall.dialcontacts.calldialer.Advertisement;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.firebase.analytics.FirebaseAnalytics;

public class ADSAppBackground {

    private static boolean isBackgroundLoading = false;

    public static void ADSLoadAppOpen(Activity context) {
        if (ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID).isEmpty()) {
            ADSUtilitis.MassageBoxFullDismiss();
            return;
        }

        if (ADSAppStarting.ADSIsShowing) {
            return;
        }

        if (ADSMainClass.isBackgroundAppOpenPreLoadMode() && ADSAppStarting.AppStartingAd != null) {
            showAppOpenAd(context, ADSAppStarting.AppStartingAd);
            return;
        }

        if (ADSMainClass.isBackgroundAppOpenLoadMode()) {
            ADSUtilitis.MassageBoxFull(context);
        }

        loadAppOpenAd(context, true);
    }

    public static void ADSPreloadAppOpen(Activity context) {
        if (!ADSMainClass.isBackgroundAppOpenPreLoadMode()) {
            return;
        }
        if (ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID).isEmpty()) {
            return;
        }
        if (!ADSMainClass.getAppOpenBackgroundShow()) {
            return;
        }
        if (ADSAppStarting.AppStartingAd != null || ADSAppStarting.ADSIsShowing || isBackgroundLoading) {
            return;
        }

        loadAppOpenAd(context, false);
    }

    private static void loadAppOpenAd(Activity context, boolean showWhenLoaded) {
        if (isBackgroundLoading) {
            return;
        }

        try {
            isBackgroundLoading = true;
            String app_open_id = ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID);

            ADSAppStarting.LoadCallBack = new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(AppOpenAd ad) {
                    isBackgroundLoading = false;

                    FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
                    ad.setOnPaidEventListener(new OnPaidEventListener() {
                        @Override
                        public void onPaidEvent(AdValue adValue) {
                            double revenue = adValue.getValueMicros() / 1_000_000.0;
                            String currency = adValue.getCurrencyCode();
                            Bundle adRevenueParams = new Bundle();
                            adRevenueParams.putString(FirebaseAnalytics.Param.AD_PLATFORM, "Google Ad Manager");
                            adRevenueParams.putString(FirebaseAnalytics.Param.CURRENCY, currency);
                            adRevenueParams.putDouble(FirebaseAnalytics.Param.VALUE, revenue);
                            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, adRevenueParams);
                        }
                    });
                    ADSUtilitis.trackScreen(context, "AppOpen_Load");
                    ADSAppStarting.AppStartingAd = ad;
                    ADSAppStarting.ADSIsShowing = false;

                    if (showWhenLoaded) {
                        if (ADSMainClass.isBackgroundAppOpenLoadMode()) {
                            ADSUtilitis.MassageBoxFullDismiss();
                        }
                        showAppOpenAd(context, ad);
                    }
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    isBackgroundLoading = false;
                    ADSUtilitis.trackScreen(context, "AppOpen_Fail");
                    ADSAppStarting.AppStartingAd = null;
                    ADSAppStarting.ADSIsShowing = false;
                    if (showWhenLoaded && ADSMainClass.isBackgroundAppOpenLoadMode()) {
                        ADSUtilitis.MassageBoxFullDismiss();
                    }
                }
            };

            Bundle bundle = new Bundle();
            AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, bundle).build();
            AppOpenAd.load(context, app_open_id, adRequest, ADSAppStarting.LoadCallBack);

        } catch (Exception e) {
            isBackgroundLoading = false;
            e.printStackTrace();
            if (showWhenLoaded && ADSMainClass.isBackgroundAppOpenLoadMode()) {
                ADSUtilitis.MassageBoxFullDismiss();
            }
        }
    }

    private static void showAppOpenAd(Activity context, AppOpenAd ad) {
        FullScreenContentCallback fullScreenContentCallback =
                new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        ADSAppStarting.ADSIsShowing = false;
                        ADSAppStarting.AppStartingAd = null;
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        ADSAppStarting.AppStartingAd = null;
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        ADSAppStarting.ADSIsShowing = true;
                    }
                };

        ad.setFullScreenContentCallback(fullScreenContentCallback);
        ad.show(context);
    }
}
