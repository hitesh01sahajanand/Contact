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
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ADSAppBackground {

    public static void ADSLoadAppOpen(Activity context) {
//        Log.d("TAG", "ADSLoadAppOpen: " +ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID));
        if (ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID).isEmpty()) {
            ADSUtilitis.MassageBoxFullDismiss();
            return;
        }

        if (!ADSAppStarting.ADSIsShowing) {
            ADSUtilitis.MassageBoxFull(context);
            try {
                String app_open_id = ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID);

                ADSAppStarting.LoadCallBack = new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(AppOpenAd ad) {

                        FirebaseAnalytics firebaseAnalytics;
                        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
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
//                        if (ADSMainClass.getAdsOneByOneIDS()) {
//                            if (ADSMainClass.getAdmobAPPStartingIdList() != null && ADSMainClass.getAdmobAPPStartingIdList().size() != 0 && ADSMainClass.getAdmobAPPStartingIdList().size() == ADSAppStarting.ArrayIndexAppStartingId) {
//                                ADSAppStarting.ArrayIndexAppStartingId = 0;
//                            }
//                        } else {
//                            ADSAppStarting.ArrayIndexAppStartingId = 0;
//                        }

                        ADSUtilitis.MassageBoxFullDismiss();

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

                        ADSAppStarting.AppStartingAd.setFullScreenContentCallback(fullScreenContentCallback);
                        ADSAppStarting.AppStartingAd.show(context);

                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
//                        Log.d("AppOpenAds", "onAdFailedToLoad: " + loadAdError.getMessage() );
                        ADSUtilitis.trackScreen(context, "AppOpen_Fail");
                        ADSAppStarting.AppStartingAd = null;
                        ADSAppStarting.ADSIsShowing = false;
//                        ADSLoadAppOpen(context);
                    }
                };
                Bundle bundle = new Bundle();
                AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, bundle).build();
                AppOpenAd.load(context, app_open_id, adRequest, ADSAppStarting.LoadCallBack);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
