package com.example.contactmanager.Advertisement;


import static com.example.contactmanager.Advertisement.ADSInterDisplay.AdsDisplayCheck;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.ads.Ad;
import com.facebook.ads.InterstitialAdListener;
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
    public static String AdmobAppStartingIDS = "";
    public static int ArrayIndexAppStartingId = 0;
    public static boolean FailArrayAppStartingID = false;
    public static AppOpenAd AppStartingAd;
    public static AppOpenAd.AppOpenAdLoadCallback LoadCallBack;
    public static boolean ADSIsShowing = true;
    public static com.facebook.ads.InterstitialAd FBInterstitialAd;
    private static InterstitialAd AdmobmInterstitialAd;

    public static void AppStartingRandomId() {
        try {
            if (ADSMainClass.getAdmobAPPStartingIdList() != null && ADSMainClass.getAdmobAPPStartingIdList().size() != 0 && ADSMainClass.getAdmobAPPStartingIdList().size() != ArrayIndexAppStartingId) {
                FailArrayAppStartingID = true;
                AdmobAppStartingIDS = ADSMainClass.getAdmobAPPStartingIdList().get(ArrayIndexAppStartingId);
                ADSMainClass.setAppStartingId(AdmobAppStartingIDS);
                ArrayIndexAppStartingId = ArrayIndexAppStartingId + 1;
            } else {
                ArrayIndexAppStartingId = 0;
                FailArrayAppStartingID = false;
            }
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void ADSSplashAppStartingLoad(Activity context) {

        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
            ADSUtilitis.MassageBoxFull(context);
        }

        ADSAppStarting.AppStartingRandomId();
        if (!ADSAppStarting.FailArrayAppStartingID) {

            if (ADSMainClass.getSplashADType().equals("Appopen")) {
                if (ADSMainClass.getAdsDisplayType().equals("facebook")) {
                    FbInterstitialAd(context);
                } else {
                    AdmobInterstitialAd(context);
                }
            } else {
                if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    ADSUtilitis.MassageBoxFullDismiss();
                }
                onFinishAds.onFinishAds(true);
            }

            return;
        }
        try {
            String app_open_id = ADSMainClass.getAppStartingId();
            ADSAppStarting.LoadCallBack =
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(AppOpenAd ad) {
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
                            if (ADSMainClass.getAdsOneByOneIDS()) {
                                if (ADSMainClass.getAdmobAPPStartingIdList() != null && ADSMainClass.getAdmobAPPStartingIdList().size() != 0 && ADSMainClass.getAdmobAPPStartingIdList().size() == ADSAppStarting.ArrayIndexAppStartingId) {
                                    ADSAppStarting.ArrayIndexAppStartingId = 0;
                                }
                            } else {
                                ADSAppStarting.ArrayIndexAppStartingId = 0;
                            }

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

                                            ADSAppStarting.AppStartingAd = null;
                                            onFinishAds.onFinishAds(true);
                                        }

                                        @Override
                                        public void onAdShowedFullScreenContent() {
                                            ADSIsShowing = true;
                                            Log.d("TAG", "onAdShowedFullScreenContent:************* ");
//                                            onFinishAds.onFinishAds(true);

                                        }
                                    };

                            AppStartingAd.setFullScreenContentCallback(fullScreenContentCallback);
                            AppStartingAd.show(context);

                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError loadAdError) {

                            ADSAppStarting.AppStartingAd = null;
                            ADSAppStarting.ADSIsShowing = false;

                            ADSSplashAppStartingLoad(context);
                        }
                    };

            Bundle bundle = new Bundle();
            AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, bundle).build();
            AppOpenAd.load(context, app_open_id, adRequest, ADSAppStarting.LoadCallBack);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void ADSSplashInterstitialDisplay(Activity context, OnFinishAds onFinishAd, boolean... doShowAds) {
        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free()) {
            onFinishAds.onFinishAds(true);
            return;
        }

        if (ADSMainClass.getAdsDisplayType().equals("admob")) {
            AdmobInterstitialAd(context);
        } else {
            FbInterstitialAd(context);
        }

    }

    private static void AdmobInterstitialAd(Activity context) {

        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
            ADSUtilitis.MassageBoxFull(context);
        }

        ADSInterDisplay.InterstitialIdsRandomId();
        if (!ADSInterDisplay.ADSFailArrayInterstitialId) {

            LoadingCheck = true;

            if (ADSMainClass.getSplashADType().equals("Interstitial")) {
                if (ADSMainClass.getAdsDisplayType().equals("admob")) {
                    ADSAppStarting.ADSSplashAppStartingLoad(context);
                } else {
                    onFinishAds.onFinishAds(true);
                }
            } else {
                ADSUtilitis.MassageBoxFullDismiss();
                onFinishAds.onFinishAds(true);
            }
            return;
        }

        String Interstitialad_ids = ADSMainClass.getAdsAdmobInterstitialID();

        AdRequest adRequest = new AdRequest.Builder().build();
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
                        if (ADSMainClass.getAdsOneByOneIDS()) {
                            if (ADSMainClass.getAdmobInterstitialIdList() != null && ADSMainClass.getAdmobInterstitialIdList().size() != 0 && ADSMainClass.getAdmobInterstitialIdList().size() == ADSInterDisplay.ArrayIndexInterstitialId) {
                                ADSInterDisplay.ArrayIndexInterstitialId = 0;
                            }
                        } else {
                            ADSInterDisplay.ArrayIndexInterstitialId = 0;
                        }

                        AdmobmInterstitialAd = interstitialAd;
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
                        AdmobInterstitialAd(context);

                    }
                });
    }

    public static void FbInterstitialAd(Activity context) {


        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
            ADSUtilitis.MassageBoxFull(context);
        }

        ADSInterDisplay.FBInterstitialIdsRandomId();

        if (!ADSInterDisplay.ADSFBFailArrayInterstitialId) {
            if (ADSMainClass.getSplashADType().equals("Interstitial")) {
                if (ADSMainClass.getAdsDisplayType().equals("facebook")) {
                    AdmobInterstitialAd(context);
                } else {
                    ADSUtilitis.MassageBoxFullDismiss();
                    onFinishAds.onFinishAds(true);
                }
            } else {
                ADSUtilitis.MassageBoxFullDismiss();
                onFinishAds.onFinishAds(true);
            }
            return;
        }

        String fb_interstitialAd_id = ADSMainClass.getAdsFBInterstitialID();

        FBInterstitialAd = null;
        FBInterstitialAd = new com.facebook.ads.InterstitialAd(context, fb_interstitialAd_id);

        InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                AdsDisplayCheck(true);
                onFinishAds.onFinishAds(true);

            }


            @Override
            public void onError(Ad ad, com.facebook.ads.AdError adError) {
                FbInterstitialAd(context);

            }

            @Override
            public void onAdLoaded(Ad ad) {


                if (ADSMainClass.getAdsOneByOneIDS()) {
                    if (ADSMainClass.getFBInterstitialIdList() != null && ADSMainClass.getFBInterstitialIdList().size() != 0 && ADSMainClass.getFBInterstitialIdList().size() == ADSInterDisplay.ADSFBArrayIndexInterstitialId) {
                        ADSInterDisplay.ADSFBArrayIndexInterstitialId = 0;
                    }
                } else {
                    ADSInterDisplay.ADSFBArrayIndexInterstitialId = 0;
                }

                if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    ADSUtilitis.MassageBoxFullDismiss();

                    if (MyApplication.isActivityChecked) {
                        MyApplication.isActivityChecked =false;

                        FBInterstitialAd.show();
                        AdsDisplayCheck(false);
                    }
                } else {
                    if (MyApplication.isActivityChecked) {
                        MyApplication.isActivityChecked =false;
                        FBInterstitialAd.show();
                        AdsDisplayCheck(false);
                    }
                }
            }

            @Override
            public void onAdClicked(Ad ad) {
                AdsDisplayCheck(false);
            }

            @Override
            public void onLoggingImpression(Ad ad) {

            }
        };

        FBInterstitialAd.loadAd(
                FBInterstitialAd.buildLoadAdConfig()
                        .withAdListener(interstitialAdListener)
                        .build());

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
