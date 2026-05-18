package com.phonecall.dialcontacts.calldialer.Advertisement;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.firebase.analytics.FirebaseAnalytics;


public class ADSBannerSmall {

    @SuppressLint("MissingPermission")
    public static void loadAdMobBanner(String adUint, FrameLayout adContainerView, ShimmerFrameLayout shimmerFrameLayout, Activity activity) {

        adContainerView.removeAllViews();
        if (TextUtils.isEmpty(adUint)) {
            if (shimmerFrameLayout.isShimmerStarted()) {
                shimmerFrameLayout.stopShimmer();
            }
            shimmerFrameLayout.setVisibility(View.GONE);
            return;
        }

        //BannerAd
        AdView admobManagerAdView = new AdView(activity);
        admobManagerAdView.setAdUnitId(adUint);
        adContainerView.addView(admobManagerAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        AdSize adSize = getAdSize(activity);
        admobManagerAdView.setAdSize(adSize);
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            shimmerFrameLayout.post(new Runnable() {
                @Override
                public void run() {
                    shimmerFrameLayout.startShimmer();
                }
            });
        }
        admobManagerAdView.loadAd(adRequest);
        admobManagerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                ADSUtilitis.trackScreen(activity, "Banner_Load");
//                Log.d("DDDDDDDDDD", "onAdLoaded:  " );
                FirebaseAnalytics firebaseAnalytics;
                firebaseAnalytics = FirebaseAnalytics.getInstance(activity);
                admobManagerAdView.setOnPaidEventListener(new OnPaidEventListener() {
                    @Override
                    public void onPaidEvent(AdValue adValue) {
                        double revenue = adValue.getValueMicros() / 1_000_000.0;
//                        Log.d("lsaslasla;sla;s", "onPaidEvent triggered" +  revenue);
                        String currency = adValue.getCurrencyCode();
                        Bundle adRevenueParams = new Bundle();
                        adRevenueParams.putString(FirebaseAnalytics.Param.AD_PLATFORM, "Google Ad Manager");
                        adRevenueParams.putString(FirebaseAnalytics.Param.CURRENCY, currency);
                        adRevenueParams.putDouble(FirebaseAnalytics.Param.VALUE, revenue);
                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, adRevenueParams);
                    }
                });
//                Log.d("DDDDDDDDDD", "onAdLoaded:  ");
                if (shimmerFrameLayout != null) {
                    if (shimmerFrameLayout.isShimmerStarted()) {
                        shimmerFrameLayout.stopShimmer();
                    }
                    shimmerFrameLayout.setVisibility(View.GONE);
                }
                adContainerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                ADSUtilitis.trackScreen(activity, "Banner_Fail");
//                Log.d("DDDDDDDDDD", "onAdLoaded: ss " + loadAdError);
                if (shimmerFrameLayout != null) {
                    if (shimmerFrameLayout.isShimmerStarted()) {
                        shimmerFrameLayout.stopShimmer();
                    }
                    shimmerFrameLayout.setVisibility(View.GONE);
                }
                adContainerView.setVisibility(View.VISIBLE);

            }
        });
    }

    public static AdSize getAdSize(Activity context) {
        Display display = context.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth);
    }
}
