package com.example.contactmanager.Advertisement;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.facebook.ads.Ad;
import com.facebook.ads.InterstitialAdListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.analytics.FirebaseAnalytics;


public class ADSInterDisplay {

    public static int ADSClick = 0;
    public static int BackADClick = 0;
    public static Boolean ADSDisplayCheck = false;
    public static int ArrayIndexInterstitialId = 0;
    public static boolean ADSFailArrayInterstitialId = false;
    public static OnFinishAds onFinishAds;
    public static com.facebook.ads.InterstitialAd ADSFBInterstitial;
    public static int ADSFBArrayIndexInterstitialId = 0;
    public static boolean ADSFBFailArrayInterstitialId = false;
    private static InterstitialAd ADSAdmobmInterstitial;
    public static boolean LoadingCheck = true;


    public static void ADSInterstitialShowing(Activity context, String adsId, OnFinishAds onFinishAd, boolean... doShowAds) {

        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free()) {
            onFinishAds.onFinishAds(true);
            return;
        }

        if (ADSInterDisplay.ADSClick == ADSMainClass.getAdsClick()) {
            ADSInterDisplay.ADSClick = 0;
            ADSInterDisplay.ArrayIndexInterstitialId = 0;

            if (ADSMainClass.getAdsDisplayType().equals("admob")) {
                ADSMedDisplay(context, adsId);
            } else {
                ADSFbDisplay(context);
            }

        } else {
            onFinishAds.onFinishAds(true);


            ADSInterDisplay.ADSClick++;
        }
    }

    public static void ADSMedDisplay(Activity context, String adsId) {
        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
            if (LoadingCheck) {
                LoadingCheck = false;
                AdmobInterstitialAd(context, adsId);
            }
            return;
        }


        if (ADSAdmobmInterstitial != null) {

            AdsDisplayCheck(false);
            ADSAdmobmInterstitial.show(context);

        } else if (ADSFBInterstitial != null && ADSFBInterstitial.isAdLoaded()) {

            AdsDisplayCheck(false);
            ADSFBInterstitial.show();

        } else {
            if (ADSMainClass.getAdsInterstitialType().equals("none")) {
                onFinishAds.onFinishAds(true);
            } else {
            }
        }


        if (LoadingCheck) {
            LoadingCheck = false;
            AdmobInterstitialAd(context, adsId);
        }
    }

    public static void ADSFbDisplay(Activity context) {

        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
            FbInterstitialAd(context);
            return;
        }


        if (ADSFBInterstitial != null && ADSFBInterstitial.isAdLoaded()) {

            ADSFBInterstitial.show();
            AdsDisplayCheck(false);

        } else if (ADSAdmobmInterstitial != null) {
            ADSAdmobmInterstitial.show(context);
            AdsDisplayCheck(false);

        } else {
            if (ADSMainClass.getAdsInterstitialType().equals("none")) {
                onFinishAds.onFinishAds(true);
            } else {
            }
        }
        FbInterstitialAd(context);
    }

//    public static void ADSBackDisplayInterstitial(Activity context, OnFinishAds onFinishAd, boolean... doShowAds) {
//        onFinishAds = onFinishAd;
//        if (ADSMainClass.getAds_Free()) {
//            onFinishAds.onFinishAds(true);
//            return;
//        }
//
//        if (ADSInterDisplay.BackADClick == ADSMainClass.getAdsBackClick()) {
//            ADSInterDisplay.BackADClick = 0;
//            ADSInterDisplay.ArrayIndexInterstitialId = 0;
//
//            if (ADSMainClass.getAdsDisplayType().equals("admob")) {
//                ADSMedDisplay(context);
//            } else {
//                ADSFbDisplay(context);
//            }
//
//        } else {
//            onFinishAds.onFinishAds(true);
//            ADSInterDisplay.BackADClick++;
//        }
//    }

    public static void AdsDisplayCheck(Boolean aBoolean) {

        if (aBoolean) {
            ADSAppStarting.ADSIsShowing = false;
            MyApplication.FastStart = false;
            ADSDisplayCheck = true;
        } else {
            ADSAppStarting.ADSIsShowing = true;
            ADSDisplayCheck = false;
            MyApplication.FastStart = true;
//            App.Companion.setFastStart(true);
        }

    }

    public static void InterstitialIdsRandomId() {
        try {
            if (ADSMainClass.getAdmobInterstitialIdList() != null && ADSMainClass.getAdmobInterstitialIdList().size() != 0 && ADSMainClass.getAdmobInterstitialIdList().size() != ArrayIndexInterstitialId) {
                ADSFailArrayInterstitialId = true;
                ADSMainClass.setAdsAdmobInterstitialID(ADSMainClass.getAdmobInterstitialIdList().get(ArrayIndexInterstitialId));
                ArrayIndexInterstitialId = ArrayIndexInterstitialId + 1;
            } else {
                ADSFailArrayInterstitialId = false;
            }
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void AdmobInterstitialAd(Activity context, String AdsID) {
//        Log.d("TAG", "ADSMedDisplay: " + AdsID);
        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
            ADSUtilitis.MassageBoxFull(context);
        }

        ADSInterDisplay.InterstitialIdsRandomId();
        if (!ADSInterDisplay.ADSFailArrayInterstitialId) {

            LoadingCheck = true;

            if (ADSMainClass.getAdsDisplayType().equals("admob")) {
                FbInterstitialAd(context);
            } else {
                if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    if (ADSMainClass.getAdsInterstitialType().equals("none")) {
                        ADSUtilitis.MassageBoxFullDismiss();
                        onFinishAds.onFinishAds(true);
                    } else {
                        ADSUtilitis.MassageBoxFullDismiss();
                    }

                }
            }
            return;
        }

//        String Interstitialad_ids = ADSMainClass.getAdsAdmobInterstitialID();

        AdRequest adRequest = new AdRequest.Builder().build();
        ADSAdmobmInterstitial = null;

        InterstitialAd.load(context, AdsID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
//                    Log.d("InterAds", "onAdLoaded: ");
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

                    LoadingCheck = true;
                    if (ADSMainClass.getAdsOneByOneIDS()) {
                        if (ADSMainClass.getAdmobInterstitialIdList() != null && ADSMainClass.getAdmobInterstitialIdList().size() != 0 && ADSMainClass.getAdmobInterstitialIdList().size() == ArrayIndexInterstitialId) {
                            ArrayIndexInterstitialId = 0;
                        }
                    } else {
                        ArrayIndexInterstitialId = 0;
                    }

                    ADSAdmobmInterstitial = interstitialAd;
                    if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                        ADSUtilitis.MassageBoxFullDismiss();

                        if (MyApplication.isActivityChecked) {
                            MyApplication.isActivityChecked = false;

                            ADSAdmobmInterstitial.show((Activity) context);
                            ADSInterDisplay.AdsDisplayCheck(false);
                        }

                    }
                    onAdsLoadAdListener(context);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                    Log.d("InterAds", "onAdFailedToLoad: " +   loadAdError.getMessage());
                    AdmobInterstitialAd(context, AdsID);

                }
            });


    }

    public static void onAdsLoadAdListener(Context context) {
        ADSAdmobmInterstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
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

    public static void FBInterstitialIdsRandomId() {
        try {
            if (ADSMainClass.getFBInterstitialIdList() != null && ADSMainClass.getFBInterstitialIdList().size() != 0 && ADSMainClass.getFBInterstitialIdList().size() != ADSFBArrayIndexInterstitialId) {
                ADSFBFailArrayInterstitialId = true;
                ADSMainClass.setAdsFBInterstitialID(ADSMainClass.getFBInterstitialIdList().get(ADSFBArrayIndexInterstitialId));
                ADSFBArrayIndexInterstitialId = ADSFBArrayIndexInterstitialId + 1;
            } else {
                ADSFBArrayIndexInterstitialId = 0;
                ADSFBFailArrayInterstitialId = false;
            }
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void FbInterstitialAd(Activity context) {


        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
            ADSUtilitis.MassageBoxFull(context);
        }

        FBInterstitialIdsRandomId();

        if (!ADSFBFailArrayInterstitialId) {
            if (ADSMainClass.getAdsDisplayType().equals("facebook")) {
//                AdmobInterstitialAd(context);
            } else {
                if (ADSMainClass.getAdsTypeManage().equals("Load")) {

                    if (ADSMainClass.getAdsInterstitialType().equals("none")) {
                        ADSUtilitis.MassageBoxFullDismiss();
                        onFinishAds.onFinishAds(true);
                    } else {
                        ADSUtilitis.MassageBoxFullDismiss();
                    }
                }

            }
            return;
        }

        String fb_interstitialAd_id = ADSMainClass.getAdsFBInterstitialID();

        ADSFBInterstitial = null;
        ADSFBInterstitial = new com.facebook.ads.InterstitialAd(context, fb_interstitialAd_id);

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
                    if (ADSMainClass.getFBInterstitialIdList() != null && ADSMainClass.getFBInterstitialIdList().size() != 0 && ADSMainClass.getFBInterstitialIdList().size() == ADSFBArrayIndexInterstitialId) {
                        ADSFBArrayIndexInterstitialId = 0;
                    }
                } else {
                    ADSFBArrayIndexInterstitialId = 0;
                }

                if (ADSMainClass.getAdsTypeManage().equals("Load")) {

                    ADSUtilitis.MassageBoxFullDismiss();

                    if (MyApplication.isActivityChecked) {
                        MyApplication.isActivityChecked = false;

                        ADSFBInterstitial.show();
                        ADSInterDisplay.AdsDisplayCheck(false);
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

        ADSFBInterstitial.loadAd(
            ADSFBInterstitial.buildLoadAdConfig()
                .withAdListener(interstitialAdListener)
                .build());

    }

    public interface OnFinishAds {
        void onFinishAds(boolean b);

    }


}
