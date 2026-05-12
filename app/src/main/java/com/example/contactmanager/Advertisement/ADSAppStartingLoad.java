package com.example.contactmanager.Advertisement;


import static com.example.contactmanager.Advertisement.ADSInterDisplay.AdsDisplayCheck;

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


public class ADSAppStartingLoad {


    private static InterstitialAd AdmobInterstitialAd;

    public static void ADSLoadAppOpen(Activity context) {
        if (ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID).isEmpty()) {
            return;
        }

        if (!ADSAppStarting.ADSIsShowing) {
            ADSUtilitis.MassageBoxFull(context);

            ADSAppStarting.AppStartingRandomId();
            if (!ADSAppStarting.FailArrayAppStartingID) {


                if (ADSMainClass.getSplashADType().equals("Appopen")) {
                    ADSAdmobInterstitial(context);
                } else {
                    ADSUtilitis.MassageBoxFullDismiss();
                }

                return;
            }
            try {
                String app_open_id = ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID);

                ADSAppStarting.LoadCallBack = new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(AppOpenAd ad) {

//                        Log.d("AppOpenAds", "onAdLoaded: " );
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

                        ADSAppStarting.AppStartingAd = ad;
                        ADSAppStarting.ADSIsShowing = false;
                        if (ADSMainClass.getAdsOneByOneIDS()) {
                            if (ADSMainClass.getAdmobAPPStartingIdList() != null && ADSMainClass.getAdmobAPPStartingIdList().size() != 0 && ADSMainClass.getAdmobAPPStartingIdList().size() == ADSAppStarting.ArrayIndexAppStartingId) {
                                ADSAppStarting.ArrayIndexAppStartingId = 0;
                            }
                        } else {
                            ADSAppStarting.ArrayIndexAppStartingId = 0;
                        }

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


    public static void ADSAdmobInterstitial(Activity context) {

        if (!ADSAppStarting.ADSIsShowing) {
            ADSUtilitis.MassageBoxFull(context);

            ADSInterDisplay.InterstitialIdsRandomId();
            if (!ADSInterDisplay.ADSFailArrayInterstitialId) {

                ADSAppStarting.LoadingCheck = true;

                if (ADSMainClass.getSplashADType().equals("Interstitial")) {
                    ADSLoadAppOpen(context);
                } else {
                    ADSUtilitis.MassageBoxFullDismiss();
                }
                return;
            }

            String Interstitialad_ids = ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME);
            AdRequest adRequest = new AdRequest.Builder().build();
            AdmobInterstitialAd = null;

            InterstitialAd.load(context, Interstitialad_ids, adRequest,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {

                            FirebaseAnalytics firebaseAnalytics;
                            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
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

                            ADSAppStarting.LoadingCheck = true;
                            if (ADSMainClass.getAdsOneByOneIDS()) {
                                if (ADSMainClass.getAdmobInterstitialIdList() != null && ADSMainClass.getAdmobInterstitialIdList().size() != 0 && ADSMainClass.getAdmobInterstitialIdList().size() == ADSInterDisplay.ArrayIndexInterstitialId) {
                                    ADSInterDisplay.ArrayIndexInterstitialId = 0;
                                }
                            } else {
                                ADSInterDisplay.ArrayIndexInterstitialId = 0;
                            }

                            AdmobInterstitialAd = interstitialAd;

                            ADSUtilitis.MassageBoxFullDismiss();
                            AdmobInterstitialAd.show((Activity) context);
                            AdsDisplayCheck(false);

                            onAdsLoadListener(context);
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            ADSAdmobInterstitial(context);

                        }
                    });


        }

    }


    public static void onAdsLoadListener(Context context) {
        AdmobInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                AdsDisplayCheck(true);

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

}
