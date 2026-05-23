package com.phonecall.dialcontacts.calldialer.Advertisement;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.firebase.analytics.FirebaseAnalytics;

@SuppressWarnings("all")
public class CallEndInterAd {

    public static int adsClickEvent = 0;
    public static int adsBackClick = 0;
    public static Boolean isAdsShowEnable = false;
    public static int fullScreenAdsPosition = 0;
    public static boolean fullScreenAdsFailed = false;
    public static OnCompeteAds onCompleteAdCallBack;
    public static boolean isAdsEnabled = true;
    private static InterstitialAd fullScreenAds;
    public static boolean isAdShowing = true;
    public static void fullScreenAdShow(Activity context, OnCompeteAds onFinishAd, boolean... doShowAds) {

        onCompleteAdCallBack = onFinishAd;
        if (ADSMainClass.getAds_Free()) {
            if (onCompleteAdCallBack != null) {
                onCompleteAdCallBack.onCompeteAds(true);
            }
            return;
        }

        // Removed click frequency check for CallEnd to ensure it follows daily limit only
        admobFullScreenAd(context);
    }

    public static void admobFullScreenAd(Activity context) {

        if (fullScreenAds != null) {
//            Log.d("TAG", "CallEndAd: ad is ready, showing now (22222)");
            adsShowCheckEvent(false);
            try {
                fullScreenAds.show(context);
                fullScreenAds = null; // Important: Clear the ad after showing so a new one can be loaded
            } catch (Exception e) {
//                Log.e("TAG", "CallEndAd: Error showing ad: " + e.getMessage());
                fullScreenAds = null;
                adsShowCheckEvent(true);
                if (onCompleteAdCallBack != null) {
                    onCompleteAdCallBack.onCompeteAds(false);
                }
            }

        } else {
//            Log.d("TAG", "CallEndAd: ad is NULL, cannot show (33333)");
            if (onCompleteAdCallBack != null) {
                onCompleteAdCallBack.onCompeteAds(false); // Return false because show failed
            }
        }

        if (isAdsEnabled) {
//            Log.d("TAG", "CallEndAd: Triggering reload for next time (44444)");
            isAdsEnabled = false;
            admobFullScreenAdLoad(context);
        }
    }

    public static void adsShowCheckEvent(Boolean aBoolean) {

        if (aBoolean) {
            isAdShowing = false;
//            AdViewController.isAppFastStart = false;
            isAdsShowEnable = true;
        } else {
            isAdShowing = true;
            isAdsShowEnable = false;
//            AdViewController.isAppFastStart = true;
        }

    }

//    public static void fullScreenInterstailAdsId( List<String> interAdsIds) {
//        try {
//            if (interAdsIds != null && interAdsIds.size() != 0 && interAdsIds.size() != fullScreenAdsPosition) {
//                fullScreenAdsFailed = true;
//                SetterMethodAdView.set_CallEndInter_AdsUnit_Id(interAdsIds.get(fullScreenAdsPosition));
//                fullScreenAdsPosition = fullScreenAdsPosition + 1;
//            } else {
//                fullScreenAdsFailed = false;
//            }
//        } catch (NullPointerException n) {
//            n.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static void admobFullScreenAdLoad(Activity context) {
        if (fullScreenAds != null) {
            isAdsEnabled = true;
            return;
        }

//        CallEndInterAd.fullScreenInterstailAdsId(interAdsIds);

//        String Interstitialad_ids = SetterMethodAdView.get_CallEndInter_Ads_Unit_Id();
//        Log.d("TAG", "CallEndAd: admobFullScreenAdLoad: ID used = " + Interstitialad_ids);

//        if (Interstitialad_ids == null || Interstitialad_ids.isEmpty()) {
//            Log.d("TAG", "CallEndAd: admobFullScreenAdLoad: EMPTY ID, Aborting load.");
//            isAdsEnabled = true;
//            return;
//        }
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, ADSMainClass.getStringValue(ADSMainClass.CALL_END_Inter), adRequest,
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

                        isAdsEnabled = true;
//                        if (interAdsIds != null && interAdsIds.size() == fullScreenAdsPosition) {
//                            fullScreenAdsPosition = 0;
//                        }

                        CallEndInterAd.fullScreenAds = interstitialAd;
//                        if (SetterMethodAdView.gettrsv__Ads_Type().equals("Load")) {
//                            AdViewLoadDialog.trsv__Hide_Ads_Loading_Dialog();
//
//                            if (AdViewController.isScreenCheckEnabled) {
//                                AdViewController.isScreenCheckEnabled = false;
//
//                                CallEndInterAd.fullScreenAds.show((Activity) context);
//                                CallEndInterAd.adsShowCheckEvent(false);
//                            }
//
//                        }
                        CallEndInterAd.fullScreenAds = interstitialAd;
//                        Log.d("TAG", "CallEndAd: Ad loaded successfully and assigned to fullScreenAds.");
                        onContactListner(context);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        Log.d("TAG", "CallEndAd: onAdFailedToLoad: " + loadAdError.getMessage());
                        isAdsEnabled = true; // Allow next attempt if needed
                    }
                });
    }

    public static void onContactListner(Context context) {
        fullScreenAds.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                adsShowCheckEvent(true);
                if (onCompleteAdCallBack != null) {
                    onCompleteAdCallBack.onCompeteAds(true);
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                adsShowCheckEvent(true);
                if (onCompleteAdCallBack != null) {
                    onCompleteAdCallBack.onCompeteAds(false);
                }
            }

            @Override
            public void onAdImpression() {
            }

            @Override
            public void onAdShowedFullScreenContent() {
            }
        });
    }

    public interface OnCompeteAds {
        void onCompeteAds(boolean b);
    }

}
