package com.example.contactmanager.Advertisement;


import static com.example.contactmanager.Advertisement.ADSInterDisplay.AdsDisplayCheck;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.contactmanager.R;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ADSNativeDisplay {
    public static int ArrayIndex = 0;
    public static boolean FailArrayId = false;
    public static Context contexts;
    public static int NativeByPage = 0;
    public static com.facebook.ads.NativeAd FBNativeAdpater;
    public static NativeAdListener NativeAdListener;
    public static NativeAdLayout NativeAdLayout;
    public static boolean LoadingCheck = true;
    public static String AdviewNative = "";
    public static NativeAd AdmobNativeAd;
    public static int FBArrayIndexNativeId = 0;
    public static boolean FBFailArrayIdNativeId = false;
    public static String AdsDisplayType = "small";

    public static void FullNativeDisplay(final Context context, final LinearLayout linearLayout, final LinearLayout lnr_view, String ads_type) {
        AdsDisplayType = ads_type;
        contexts = context;
        ArrayIndex = 0;
        if (ADSMainClass.getAds_Free()) {
            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.GONE);
            return;
        }

        if (NativeByPage == ADSMainClass.getNativeByPage()) {
            NativeByPage = 0;

            if (ADSMainClass.getAdsDisplayType().equals("facebook")) {
                if (ads_type.equals("big")) {
                    FBBigDisplay(context, linearLayout, lnr_view, false);
                } else {
                    FBSmallDisplay(context, linearLayout, lnr_view, true);
                }
            } else {
                if (ads_type.equals("big")) {
                    AdmobBigDisplay(context, linearLayout, lnr_view, false);
                } else {
                    AdmobSmallDisplay(context, linearLayout, lnr_view, true);
                }
            }

        } else {
            NativeByPage++;
            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.GONE);
        }
    }

    @SuppressLint("MissingPermission")
    public static void loadAdmobNativeAdBig(String adId, FrameLayout fl_adplaceholder, com.facebook.ads.NativeAdLayout flNativeFbBig, ShimmerFrameLayout shimmerFrameLayout, Activity activity) {

        String adUnitId;
        adUnitId = adId;

        if (TextUtils.isEmpty(adUnitId)) {
            shimmerFrameLayout.setVisibility(View.GONE);
            fl_adplaceholder.setVisibility(View.GONE);
            flNativeFbBig.setVisibility(View.GONE);
            return;
        }
        if (!ADSMainClass.getAdsTypeManage().equals("Load")) {
            shimmerFrameLayout.setVisibility(View.GONE);
        }
//        Pack5AdsUtils.PrintLog(Pack5AdManager.PACK5_ADMOB_TAG, "Admob NativeAds Big ID ==> " + adUnitId);

        AdLoader.Builder builder = new AdLoader.Builder(activity, adUnitId);

        builder.forNativeAd(nativeAd -> {
            AdmobNativeAd = nativeAd;
            showNativeOptionAdBig(fl_adplaceholder, flNativeFbBig, activity);
            if (shimmerFrameLayout.isShimmerStarted()) {
                shimmerFrameLayout.stopShimmer();
            }
            shimmerFrameLayout.setVisibility(View.GONE);
        }).withAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
//                Log.d("NativeAds", "onAdLoaded: " );
//                Pack5AdsUtils.PrintLog(Pack5AdManager.PACK5_ADMOB_TAG, "Admob NativeAds Big ==> " + "onAdLoaded");

            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
//                Log.d("NativeAds", "onAdFailedToLoad: " +loadAdError.getMessage() );
//                Pack5AdsUtils.PrintLog(Pack5AdManager.PACK5_ADMOB_TAG, "Admob NativeAds Big => onAdFailedToLoad \n" + loadAdError.getMessage());
                shimmerFrameLayout.setVisibility(View.GONE);
                fl_adplaceholder.setVisibility(View.GONE);
                flNativeFbBig.setVisibility(View.GONE);
            }
        });


        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);
        AdLoader adLoader = builder.withAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
//                Log.d("NativeAds", "onAdLoaded: " );
                FirebaseAnalytics firebaseAnalytics;
                firebaseAnalytics = FirebaseAnalytics.getInstance(contexts);
                AdmobNativeAd.setOnPaidEventListener(new OnPaidEventListener() {
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

            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
//                Log.d("NativeAds", "onAdFailedToLoad: " + adError.toString());
                shimmerFrameLayout.setVisibility(View.GONE);
                fl_adplaceholder.setVisibility(View.GONE);
                flNativeFbBig.setVisibility(View.GONE);
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());

    }

    public static void showNativeOptionAdBig(FrameLayout fl_adplaceholder, com.facebook.ads.NativeAdLayout flNativeFbBig, Activity activity) {
        try {
            if (AdmobNativeAd != null) {
                NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(R.layout.admob_big_native_ad, null);
                populateUnifiedNativeRegularAdView(AdmobNativeAd, flNativeFbBig, adView);
                fl_adplaceholder.setVisibility(View.VISIBLE);
                fl_adplaceholder.removeAllViews();
                fl_adplaceholder.addView(adView);
            } else {
                fl_adplaceholder.setVisibility(View.GONE);
                flNativeFbBig.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fl_adplaceholder.setVisibility(View.GONE);
            flNativeFbBig.setVisibility(View.GONE);

        }
    }

    public static void populateUnifiedNativeRegularAdView(NativeAd nativeAd, com.facebook.ads.NativeAdLayout flNativeFbBig, NativeAdView adView) {
        com.google.android.gms.ads.nativead.MediaView mediaView = adView.findViewById(R.id.ad_media);
        adView.setMediaView(mediaView);

        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        try {
            ((TextView) Objects.requireNonNull(adView.getHeadlineView())).setText(nativeAd.getHeadline());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (nativeAd.getBody() == null) {
            Objects.requireNonNull(adView.getBodyView()).setVisibility(View.INVISIBLE);
        } else {
            Objects.requireNonNull(adView.getBodyView()).setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.INVISIBLE);
        } else {
            Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.VISIBLE);
            ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            Objects.requireNonNull(adView.getIconView()).setVisibility(View.GONE);
        } else {
            ((ImageView) Objects.requireNonNull(adView.getIconView())).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }
        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) Objects.requireNonNull(adView.getStarRatingView())).setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            Objects.requireNonNull(adView.getAdvertiserView()).setVisibility(View.INVISIBLE);
        } else {
            ((TextView) Objects.requireNonNull(adView.getAdvertiserView())).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        adView.getStoreView().setVisibility(View.GONE);
        adView.getPriceView().setVisibility(View.GONE);

        adView.setNativeAd(nativeAd);

        VideoController vc = Objects.requireNonNull(nativeAd.getMediaContent()).getVideoController();

        if (vc.hasVideoContent()) {
            vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {
                    super.onVideoEnd();
                }
            });
        }

    }


    private static void FBSmallDisplay(Context context, LinearLayout linearLayout, LinearLayout lnr_view, boolean banner_flag) {

        if (ADSMainClass.getAdsTypeManage().equals("Load")) {

            FBNativeAd(context, linearLayout, lnr_view, banner_flag);
            return;
        }
        if (FBNativeAdpater != null && FBNativeAdpater.isAdLoaded()) {

            linearLayout.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(context);
            LinearLayout adView = (LinearLayout) inflater.inflate(R.layout.fb_med_native, NativeAdLayout, false);
            linearLayout.addView(adView);
            ADSFBFullInflate(context, FBNativeAdpater, linearLayout, lnr_view, adView);
            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
        } else if (AdmobNativeAd != null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            NativeAdView adView;
            if (AdsDisplayType.equals("big")) {

                adView = (NativeAdView) inflater.inflate(R.layout.admob_big_native_ad, null);
            } else {

                adView = (NativeAdView) inflater.inflate(R.layout.admob_mid_native_ad, null);
            }

            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);
            PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false);
            linearLayout.removeAllViews();
            linearLayout.addView(adView);
            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
        } else {
            linearLayout.setVisibility(View.GONE);
            lnr_view.setVisibility(View.GONE);
        }

        FBNativeAd(context, linearLayout, lnr_view, banner_flag);

    }

    public static void FBBigDisplay(Context context, final LinearLayout linearLayout, final LinearLayout lnr_view, boolean banner_flag) {
        ADSFBBigShowing(context, linearLayout, lnr_view, banner_flag);
    }

    public static void ADSFBBigShowing(Context context, final LinearLayout linearLayout, final LinearLayout lnr_view, boolean banner_flag) {

        if (ADSMainClass.getAdsTypeManage().equals("Load")) {

            FBNativeAd(context, linearLayout, lnr_view, banner_flag);
            return;
        }
        if (FBNativeAdpater != null && FBNativeAdpater.isAdLoaded()) {
            linearLayout.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(context);
            LinearLayout adView;
            if (AdsDisplayType.equals("big")) {
                adView = (LinearLayout) inflater.inflate(R.layout.fb_big_native, NativeAdLayout, false);
            } else {
                adView = (LinearLayout) inflater.inflate(R.layout.fb_med_native, NativeAdLayout, false);
            }
            linearLayout.addView(adView);
            ADSFBFullInflate(context, FBNativeAdpater, linearLayout, lnr_view, adView);
            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
        } else if (AdmobNativeAd != null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            NativeAdView adView;
            if (AdsDisplayType.equals("big")) {
                adView = (NativeAdView) inflater.inflate(R.layout.admob_big_native_ad, null);
            } else {
                adView = (NativeAdView) inflater.inflate(R.layout.admob_mid_native_ad, null);
            }

            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);
            PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false);
            linearLayout.removeAllViews();
            linearLayout.addView(adView);
            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
        } else {
            linearLayout.setVisibility(View.GONE);
            lnr_view.setVisibility(View.GONE);
        }

        FBNativeAd(context, linearLayout, lnr_view, banner_flag);

    }

    public static void AdmobBigDisplay(Context context, final LinearLayout linearLayout, final LinearLayout lnr_view, Boolean banner_flag) {

        if (ADSMainClass.getAdsTypeManage().equals("Load")) {

            if (LoadingCheck) {
                LoadingCheck = false;
                AdmobFullNative(context, linearLayout, lnr_view, banner_flag);
            }
            return;
        }

        if (AdmobNativeAd != null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            NativeAdView adView;
            if (AdsDisplayType.equals("big")) {
                adView = (NativeAdView) inflater.inflate(R.layout.admob_big_native_ad, null);
            } else {
                adView = (NativeAdView) inflater.inflate(R.layout.admob_mid_native_ad, null);
            }

            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);
            PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false);
            linearLayout.removeAllViews();
            linearLayout.addView(adView);
            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
        } else if (FBNativeAdpater != null && FBNativeAdpater.isAdLoaded()) {
            LayoutInflater inflater = LayoutInflater.from(context);
            linearLayout.removeAllViews();
            LinearLayout adView = (LinearLayout) inflater.inflate(R.layout.fb_big_native, NativeAdLayout, false);
            linearLayout.addView(adView);
            ADSFBFullInflate(context, FBNativeAdpater, linearLayout, lnr_view, adView);
            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
        } else {
            linearLayout.setVisibility(View.GONE);
            lnr_view.setVisibility(View.GONE);
        }
        if (LoadingCheck) {
            LoadingCheck = false;
            AdmobFullNative(context, linearLayout, lnr_view, banner_flag);
        }
    }

    public static void AdmobSmallDisplay(Context context, final LinearLayout linearLayout, final LinearLayout lnr_view, Boolean banner_flag) {

        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
            AdmobFullNative(context, linearLayout, lnr_view, banner_flag);
            return;
        }

        if (AdmobNativeAd != null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            NativeAdView adView = (NativeAdView) inflater.inflate(R.layout.admob_mid_native_ad, null);
            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);
            PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false);
            linearLayout.removeAllViews();
            linearLayout.addView(adView);
            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
        } else if (FBNativeAdpater != null && FBNativeAdpater.isAdLoaded()) {
            LayoutInflater inflater = LayoutInflater.from(context);
            linearLayout.removeAllViews();
            LinearLayout adView = (LinearLayout) inflater.inflate(R.layout.fb_big_native, NativeAdLayout, false);
            linearLayout.addView(adView);
            ADSFBFullInflate(context, FBNativeAdpater, linearLayout, lnr_view, adView);
            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
        } else {
            linearLayout.setVisibility(View.GONE);
            lnr_view.setVisibility(View.GONE);
        }
        AdmobFullNative(context, linearLayout, lnr_view, banner_flag);
    }

    public static void FBNativeIdsRandomId() {
        try {
            if (ADSMainClass.getFBNativeFullList() != null && ADSMainClass.getFBNativeFullList().size() != 0 && ADSMainClass.getFBNativeFullList().size() != FBArrayIndexNativeId) {
                FBFailArrayIdNativeId = true;
                ADSMainClass.setFBNativeId(ADSMainClass.getFBNativeFullList().get(FBArrayIndexNativeId));
                FBArrayIndexNativeId = FBArrayIndexNativeId + 1;
            } else {
                FBArrayIndexNativeId = 0;
                FBFailArrayIdNativeId = false;
            }
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void FBNativeAd(final Context context, final LinearLayout linearLayout, final LinearLayout lnr_view, boolean banner_flag) {

        FBNativeIdsRandomId();

        if (!FBFailArrayIdNativeId) {
            if (ADSMainClass.getAdsDisplayType().equals("facebook")) {
                AdmobFullNative(context, linearLayout, lnr_view, banner_flag);
            } else {
                if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    linearLayout.setVisibility(View.GONE);
                    lnr_view.setVisibility(View.GONE);
                }
            }
            return;
        }

        FBNativeAdpater = new com.facebook.ads.NativeAd(context, ADSMainClass.getFBNativeId());

        NativeAdListener = new NativeAdListener() {
            @Override
            public void onMediaDownloaded(Ad ad) {

            }

            @Override
            public void onError(Ad ad, AdError adError) {

                FBNativeAd(context, linearLayout, lnr_view, banner_flag);

            }

            @Override
            public void onAdLoaded(Ad ad) {
                if (ADSMainClass.getAdsOneByOneIDS()) {
                    if (ADSMainClass.getFBNativeFullList() != null && ADSMainClass.getFBNativeFullList().size() != 0 && ADSMainClass.getFBNativeFullList().size() == FBArrayIndexNativeId) {
                        FBArrayIndexNativeId = 0;
                    }
                } else {
                    FBArrayIndexNativeId = 0;
                }

                if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    if (FBNativeAdpater != null && FBNativeAdpater.isAdLoaded()) {
                        linearLayout.removeAllViews();
                        LayoutInflater inflater = LayoutInflater.from(context);
                        LinearLayout adView;
                        if (banner_flag) {
                            adView = (LinearLayout) inflater.inflate(R.layout.fb_med_native, NativeAdLayout, false);
                        } else {
                            adView = (LinearLayout) inflater.inflate(R.layout.fb_big_native, NativeAdLayout, false);
                        }
                        linearLayout.addView(adView);
                        ADSFBFullInflate(context, FBNativeAdpater, linearLayout, lnr_view, adView);

                        ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
                    }
                }
            }

            @Override
            public void onAdClicked(Ad ad) {
                MyApplication.FastStart = true;
//                App.Companion.setFastStart(true);
            }

            @Override
            public void onLoggingImpression(Ad ad) {

            }
        };

        if (!FBNativeAdpater.isAdLoaded()) {
            //   Log.d("NativeFull_Show", "fb  Loadding..");
            FBNativeAdpater.loadAd(FBNativeAdpater.buildLoadAdConfig()
                    .withAdListener(NativeAdListener)
                    .build());
        }
    }

    public static void ADSFBFullInflate(Context context, com.facebook.ads.NativeAd nativeAd, LinearLayout adViewContainer, LinearLayout load, LinearLayout adView) {
        load.setVisibility(View.GONE);
        adViewContainer.setVisibility(View.VISIBLE);
        nativeAd.unregisterView();

        LinearLayout adChoicesContainer = adView.findViewById(R.id.ad_choices_container);
        AdOptionsView adOptionsView = new AdOptionsView(context, nativeAd, NativeAdLayout);
        adChoicesContainer.removeAllViews();
        adChoicesContainer.addView(adOptionsView, 0);

        MediaView nativeAdIcon = adView.findViewById(R.id.native_ad_icon);
        TextView nativeAdTitle = adView.findViewById(R.id.native_ad_title);
        MediaView nativeAdMedia = adView.findViewById(R.id.native_ad_media);
        TextView nativeAdSocialContext = adView.findViewById(R.id.native_ad_social_context);
        TextView nativeAdBody = adView.findViewById(R.id.native_ad_body);
        TextView sponsoredLabel = adView.findViewById(R.id.native_ad_sponsored_label);
        TextView nativeAdCallToAction = adView.findViewById(R.id.native_ad_call_to_action);

        nativeAdTitle.setText(nativeAd.getAdvertiserName());
        nativeAdBody.setText(nativeAd.getAdBodyText());
        nativeAdSocialContext.setText(nativeAd.getAdSocialContext());
        nativeAdCallToAction.setVisibility(nativeAd.hasCallToAction() ? View.VISIBLE : View.INVISIBLE);
        nativeAdCallToAction.setText(nativeAd.getAdCallToAction());
        sponsoredLabel.setText(nativeAd.getSponsoredTranslation());

        List<View> clickableViews = new ArrayList<>();
        clickableViews.add(nativeAdTitle);
        clickableViews.add(nativeAdCallToAction);

        nativeAd.registerViewForInteraction(adView, nativeAdMedia, nativeAdIcon, clickableViews);
    }

    public static void NativeRandomId() {
        try {
            if (ADSMainClass.getAdmobFullNativeIDList() != null && ADSMainClass.getAdmobFullNativeIDList().size() != 0 && ADSMainClass.getAdmobFullNativeIDList().size() != ArrayIndex) {
                FailArrayId = true;
                ADSMainClass.setAdsAdmobNativeID(ADSMainClass.getAdmobFullNativeIDList().get(ArrayIndex));
                ArrayIndex = ArrayIndex + 1;
            } else {
                FailArrayId = false;
            }
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void AdmobFullNative(final Context context, final LinearLayout linearLayout, final LinearLayout lnr_view, boolean banner_flag) {

        NativeRandomId();
        if (!FailArrayId) {
            LoadingCheck = true;
            if (!ADSMainClass.getAdsDisplayType().equals("facebook")) {
                FBNativeAd(context, linearLayout, lnr_view, banner_flag);
            } else {
                if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    linearLayout.setVisibility(View.GONE);
                    lnr_view.setVisibility(View.GONE);
                }

            }
            return;
        }

        AdviewNative = "";
        AdviewNative = ADSMainClass.getAdsAdmobNativeID();

        AdLoader.Builder builder = new AdLoader.Builder(context, AdviewNative)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {

                        FirebaseAnalytics firebaseAnalytics;
                        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
                        nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
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
                        if (AdmobNativeAd != null) {
                            AdmobNativeAd = null;
                        }
                        AdmobNativeAd = nativeAd;

                        if (ADSMainClass.getAdsOneByOneIDS()) {
                            if (ADSMainClass.getAdmobFullNativeIDList() != null && ADSMainClass.getAdmobFullNativeIDList().size() != 0 && ADSMainClass.getAdmobFullNativeIDList().size() == ArrayIndex) {
                                ArrayIndex = 0;
                            }
                        } else {
                            ArrayIndex = 0;
                        }

                        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                            if (AdmobNativeAd != null) {

                                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                                NativeAdView adView;
                                if (AdsDisplayType.equals("big")) {
                                    adView = (NativeAdView) inflater.inflate(R.layout.admob_big_native_ad, null);
                                } else {
                                    adView = (NativeAdView) inflater.inflate(R.layout.admob_mid_native_ad, null);
                                }

                                lnr_view.setVisibility(View.GONE);
                                linearLayout.setVisibility(View.VISIBLE);
                                PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false);

                                linearLayout.removeAllViews();
                                linearLayout.addView(adView);
                                ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
                            }
                        }
                    }
                });

        AdLoader adLoader = builder.withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        if (AdmobNativeAd != null) {
                            AdmobNativeAd = null;
                        }
                        AdmobFullNative(context, linearLayout, lnr_view, banner_flag);

                    }

                    @Override
                    public void onAdClicked() {
                        MyApplication.FastStart = true;
//                        App.Companion.setFastStart(true);
                        AdsDisplayCheck(false);
                    }
                })
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void PopulateUnifiedFullNativeAdView(NativeAd nativeAd, NativeAdView adView, boolean flag) {

        com.google.android.gms.ads.nativead.MediaView mediaView = adView.findViewById(R.id.ad_media);

        adView.setMediaView(mediaView);
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());


        try {
            ((TextView) adView.getCallToActionView()).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(ADSMainClass.getNativeButtonColor())));
            ((TextView) adView.getCallToActionView()).setTextColor(Color.parseColor(ADSMainClass.getNativeButtonTextColor()));
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.GONE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            if (flag) {
                adView.getStarRatingView().setVisibility(View.GONE);
            } else {
                adView.getStarRatingView().setVisibility(View.GONE);
            }
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
            if (flag) {
                adView.getStarRatingView().setVisibility(View.GONE);
            } else {
                adView.getStarRatingView().setVisibility(View.VISIBLE);
            }
        }


        if (nativeAd.getAdvertiser() == null) {
            if (flag) {
                adView.getAdvertiserView().setVisibility(View.GONE);
            } else {
                adView.getAdvertiserView().setVisibility(View.GONE);
            }
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            if (flag) {
                adView.getAdvertiserView().setVisibility(View.GONE);
            } else {
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
            }
        }

        adView.setNativeAd(nativeAd);

        MediaContent vc = nativeAd.getMediaContent();

        if (vc.hasVideoContent()) {
            nativeAd.getMediaContent().getVideoController().setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {
                    super.onVideoEnd();
                }
            });
        } else {
            mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }

    public static void setCallEndAdmobRandomIDS() {
        try {
            if (ADSMainClass.getAdmobNativeCallEndIDSList() != null && ADSMainClass.getAdmobNativeCallEndIDSList().size() != 0 && ADSMainClass.getAdmobNativeCallEndIDSList().size() != FBArrayIndexNativeId) {
                FBFailArrayIdNativeId = true;
                ADSMainClass.setAdmobNativeCallEndUnitId(ADSMainClass.getAdmobNativeCallEndIDSList().get(FBArrayIndexNativeId));
                FBArrayIndexNativeId = FBArrayIndexNativeId + 1;
            } else {
                FBFailArrayIdNativeId = false;
            }
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
