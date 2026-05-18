package com.phonecall.dialcontacts.calldialer.Advertisement;

import static com.phonecall.dialcontacts.calldialer.Advertisement.ADSInterDisplay.AdsDisplayCheck;
import static com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass.APP_OPEN_ID;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.analytics.FirebaseAnalytics;

public class ADSAppStarting {

    public static OnFinishAds onFinishAds;
    public static boolean LoadingCheck = true;
    public static AppOpenAd AppStartingAd;
    public static AppOpenAd.AppOpenAdLoadCallback LoadCallBack;
    public static boolean ADSIsShowing = true;
    private static InterstitialAd AdmobmInterstitialAd;


    public static void AdsSplashAppStartingDisplay(Activity context, OnFinishAds onFinishAd, boolean... doShowAds) {
        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free()) {
            onFinishAds.onFinishAds(true);
            return;
        }

        ADSSplashAppStartingLoad(context);

    }

    public static void ADSSplashAppStartingLoad(Activity context) {
        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
            ADSUtilitis.MassageBoxFull(context);
        }

        String app_open_id = ADSMainClass.getStringValue(APP_OPEN_ID);

        if (app_open_id == null || app_open_id.isEmpty()) {
            if (ADSMainClass.getSplashADType().equalsIgnoreCase("Appopen")) {
                AdmobInterstitialAd(context);
            } else {
                if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    ADSUtilitis.MassageBoxFullDismiss();
                }
                onFinishAds.onFinishAds(true);
            }

            return;
        }
        try {
            ADSAppStarting.LoadCallBack =
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(AppOpenAd ad) {
//                            Log.d("ADSSplashAppStartingLoad", "onAdLoaded: ");
                            FirebaseAnalytics firebaseAnalytics;
                            firebaseAnalytics  = FirebaseAnalytics.getInstance(context);
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

                            ADSAppStarting.AppStartingAd = ad;
                            ADSAppStarting.ADSIsShowing = false;
                            ADSUtilitis.trackScreen(context, "AppOpen_Load");

                            if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                                ADSUtilitis.MassageBoxFullDismiss();
                            }

                            FullScreenContentCallback fullScreenContentCallback =
                                    new FullScreenContentCallback() {
                                        @Override
                                        public void onAdDismissedFullScreenContent() {
                                            ADSIsShowing = false;
                                            ADSAppStarting.AppStartingAd = null;
                                            onFinishAds.onFinishAds(true);

                                        }

                                        @Override
                                        public void onAdFailedToShowFullScreenContent(AdError adError) {
//                                            ADSUtilitis.trackScreen(context, "AppOpen_FailedToShow");
                                            ADSAppStarting.AppStartingAd = null;
                                            onFinishAds.onFinishAds(true);
                                        }

                                        @Override
                                        public void onAdShowedFullScreenContent() {
                                            ADSIsShowing = true;
                                        }
                                    };

                            AppStartingAd.setFullScreenContentCallback(fullScreenContentCallback);
                            AppStartingAd.show(context);

                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError loadAdError) {
//                            Log.d("ADSSplashAppStartingLoad", "onAdFailedToLoad: ");
                            ADSUtilitis.trackScreen(context, "AppOpen_Fail");
                            ADSAppStarting.AppStartingAd = null;
                            ADSAppStarting.ADSIsShowing = false;

                            AdmobInterstitialAd(context);
                        }
                    };

            Bundle bundle = new Bundle();
            AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, bundle).build();
            AppOpenAd.load(context, app_open_id, adRequest, ADSAppStarting.LoadCallBack);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void AdmobInterstitialAd(Activity context) {

        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
            ADSUtilitis.MassageBoxFull(context);
        }


        AdRequest adRequest = new AdRequest.Builder().build();

        String Interstitialad_ids = ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME);

        AdmobmInterstitialAd = null;

        InterstitialAd.load(context, Interstitialad_ids, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {

                        FirebaseAnalytics firebaseAnalytics;
                        firebaseAnalytics  = FirebaseAnalytics.getInstance(context);
                        interstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
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

                        LoadingCheck = true;
                        AdmobmInterstitialAd = interstitialAd;
//                        ADSUtilitis.trackScreen(context, "Splash_Inter_Loaded");
                        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                            ADSUtilitis.MassageBoxFullDismiss();
                            AdmobmInterstitialAd.show((Activity) context);
                            AdsDisplayCheck(false);
                        } else {

                            AdmobmInterstitialAd.show((Activity) context);
                            AdsDisplayCheck(false);
                        }
                        onAdsLoadAdListener(context);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        ADSUtilitis.trackScreen(context, "Splash_Inter_FailedToLoad");
                        ADSUtilitis.MassageBoxFullDismiss();
                        onFinishAds.onFinishAds(true);

                    }
                });
    }


    public static void onAdsLoadAdListener(Context context) {
        AdmobmInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                AdsDisplayCheck(true);
                onFinishAds.onFinishAds(true);

            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
            }

            @Override
            public void onAdImpression() {
            }

            @Override
            public void onAdShowedFullScreenContent() {

            }
        });
    }

    public interface OnFinishAds {
        void onFinishAds(boolean b);
    }

}
