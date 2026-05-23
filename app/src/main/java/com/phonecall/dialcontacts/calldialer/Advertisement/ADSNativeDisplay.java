package com.phonecall.dialcontacts.calldialer.Advertisement;


import static com.phonecall.dialcontacts.calldialer.Advertisement.ADSInterDisplay.AdsDisplayCheck;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.facebook.ads.NativeAdLayout;
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
import com.phonecall.dialcontacts.calldialer.R;

import java.util.Objects;

public class ADSNativeDisplay {
    public static int ArrayIndex = 0;
    public static boolean FailArrayId = false;
    public static Context contexts;
    public static int NativeByPage = 0;
    //    public static com.facebook.ads.NativeAd FBNativeAdpater;
    public static NativeAdLayout NativeAdLayout;
    public static boolean LoadingCheck = true;
    public static NativeAd AdmobNativeAd;
    public static String AdsDisplayType = "small";

    public static void loadAdmobNativeAdBig(String adsId, final FrameLayout linearLayout, ShimmerFrameLayout shimmerFrameLayout, String ads_type, final Context context) {
        AdsDisplayType = ads_type;
        contexts = context;
        ArrayIndex = 0;
        if (ADSMainClass.getAds_Free()) {
            Log.e("TAG", "loadAdmobNativeAdBig: 111" );
//            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.GONE);
            shimmerFrameLayout.setVisibility(View.GONE);
            return;
        }
        if (adsId.isEmpty()) {
            Log.e("TAG", "loadAdmobNativeAdBig: 222" );
            linearLayout.setVisibility(View.GONE);
            shimmerFrameLayout.setVisibility(View.GONE);
            return;
        }

        if (NativeByPage == ADSMainClass.getNativeByPage()) {
            Log.e("TAG", "loadAdmobNativeAdBig: 333" );
            NativeByPage = 0;

//            if (ADSMainClass.getAdsDisplayType().equals("facebook")) {
//                if (ads_type.equals("big")) {
//                    FBBigDisplay(context, linearLayout, lnr_view, false);
//                } else {
//                    FBSmallDisplay(context, linearLayout, lnr_view, true);
//                }
//            } else {

            if (ads_type.equals("big")) {
                Log.e("TAG", "loadAdmobNativeAdBig: 555" );
                AdmobBigDisplay(context, adsId, linearLayout, shimmerFrameLayout, false);
            } else {
                Log.e("TAG", "loadAdmobNativeAdBig: 666" );
                AdmobSmallDisplay(context, adsId, linearLayout, shimmerFrameLayout, true);
            }
//            }

        } else {
            Log.e("TAG", "loadAdmobNativeAdBig: 444" );
            NativeByPage++;
            shimmerFrameLayout.setVisibility(View.GONE);
            linearLayout.setVisibility(View.GONE);
        }
    }


    public static void AdmobBigDisplay(Context context, String adsId, final FrameLayout linearLayout, ShimmerFrameLayout shimmerFrameLayout, Boolean banner_flag) {
        Log.e("TAG", "AdmobBigDisplay: 0000 "+adsId );
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
        linearLayout.setVisibility(View.GONE);

        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
            Log.e("TAG", "AdmobBigDisplay: 7777" );
            AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, banner_flag);
            return;
        }

        if (AdmobNativeAd != null) {
            Log.e("TAG", "loadAdmobNativeAdBig: 888" );
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            NativeAdView adView;
            if (AdsDisplayType.equals("big")) {
                adView = (NativeAdView) inflater.inflate(R.layout.admob_big_native_ad, null);
            } else {
                adView = (NativeAdView) inflater.inflate(R.layout.admob_mid_native_ad, null);
            }

            if (shimmerFrameLayout != null) {
                Log.e("TAG", "AdmobBigDisplay: 9999" );
                shimmerFrameLayout.setVisibility(View.GONE);
                shimmerFrameLayout.stopShimmer();
            }
            linearLayout.setVisibility(View.VISIBLE);
            PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false);
            linearLayout.removeAllViews();
            linearLayout.addView(adView);
            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
        } else {
            if (shimmerFrameLayout != null) {
                Log.e("TAG", "AdmobBigDisplay: 9999" );
                shimmerFrameLayout.setVisibility(View.GONE);
                shimmerFrameLayout.stopShimmer();
            }
        }
        AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, banner_flag);
    }

    public static void AdmobSmallDisplay(Context context, String adsId, final FrameLayout linearLayout, ShimmerFrameLayout shimmerFrameLayout, Boolean banner_flag) {

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
        linearLayout.setVisibility(View.GONE);

        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
            AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, banner_flag);
            return;
        }

        if (AdmobNativeAd != null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            NativeAdView adView = (NativeAdView) inflater.inflate(R.layout.admob_mid_native_ad, null);
            if (shimmerFrameLayout != null) {
                shimmerFrameLayout.setVisibility(View.GONE);
                shimmerFrameLayout.stopShimmer();
            }
            linearLayout.setVisibility(View.VISIBLE);
            PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false);
            linearLayout.removeAllViews();
            linearLayout.addView(adView);
            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
        }
        AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, banner_flag);
    }

    public static void AdmobFullNative(final Context context, String adsId, ShimmerFrameLayout shimmerFrameLayout, final FrameLayout linearLayout, boolean banner_flag) {

//        if (!FailArrayId) {
//            LoadingCheck = true;
//            if (ADSMainClass.getAdsTypeManage().equals("Load")) {
//                linearLayout.setVisibility(View.GONE);
//                shimmerFrameLayout.setVisibility(View.GONE);
//            }
//
//            return;
//        }
//        Log.d("SSSSSSSSSS", "adsId: " + adsId);

        AdLoader.Builder builder = new AdLoader.Builder(context, adsId)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
//                        Log.d("SSSSSSSSSS", "onNativeAdLoaded: ");
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

//                        if (AdsDisplayType.equals("big")) {
                            ADSUtilitis.trackScreen(context, "Native_Load");
//                        } else {
//                            ADSUtilitis.trackScreen(context, "Native_Small_Loaded");
//                        }

//                        if (ADSMainClass.getAdsOneByOneIDS()) {
//                            if (ADSMainClass.getAdmobFullNativeIDList() != null && ADSMainClass.getAdmobFullNativeIDList().size() != 0 && ADSMainClass.getAdmobFullNativeIDList().size() == ArrayIndex) {
//                                ArrayIndex = 0;
//                            }
//                        } else {
//                            ArrayIndex = 0;
//                        }

                        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                            if (AdmobNativeAd != null) {

                                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                                NativeAdView adView;
                                if (AdsDisplayType.equals("big")) {
                                    adView = (NativeAdView) inflater.inflate(R.layout.admob_big_native_ad, null);
                                } else {
                                    adView = (NativeAdView) inflater.inflate(R.layout.admob_mid_native_ad, null);
                                }

                                if (shimmerFrameLayout != null) {
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    shimmerFrameLayout.stopShimmer();
                                }
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
//                        Log.d("SSSSSSSSSS", "onAdFailedToLoad: " + adError.getMessage());
                        ADSUtilitis.trackScreen(context, "Native_Fail");
                        if (shimmerFrameLayout != null) {
                            shimmerFrameLayout.setVisibility(View.GONE);
                            shimmerFrameLayout.stopShimmer();
                        }
                        linearLayout.setVisibility(View.GONE);
                        if (AdmobNativeAd != null) {
                            AdmobNativeAd = null;
                        }
                    }

                    @Override
                    public void onAdClicked() {
                        ADSAppManage.FastStart = true;
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

    public static void displayAlreadyLoadedAdSmall(NativeAd ad, FrameLayout fl_adplaceholder, NativeAdLayout flNativeFBSmall, Activity activity, ShimmerFrameLayout shimmerFrameLayout) {
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.setVisibility(View.GONE);
            shimmerFrameLayout.stopShimmer();
        }
        if (ad != null) {
            try {
                // Optimization: If the same ad is already displayed, skip re-inflation to prevent blinking
                if (fl_adplaceholder.getChildCount() > 0) {
                    View firstChild = fl_adplaceholder.getChildAt(0);
                    if (firstChild instanceof NativeAdView && firstChild.getTag() == ad) {
                        fl_adplaceholder.setVisibility(View.VISIBLE);
                        return;
                    }
                }

                NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(R.layout.ad_unifiled_small, null);
                adView.setTag(ad); // Tag it to identify during refreshes
                populateUnifiedNativeSmallAdView(ad, adView);
                fl_adplaceholder.setVisibility(View.VISIBLE);
                fl_adplaceholder.removeAllViews();
                fl_adplaceholder.addView(adView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void showNativeAdsSmallMainWithListener(String adunit, FrameLayout fl_adplaceholder, Activity activity, ShimmerFrameLayout shimmerFrameLayout, AdLoadListener listener) {
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            if (!shimmerFrameLayout.isShimmerStarted()) {
                shimmerFrameLayout.startShimmer();
            }
        }
        if (!ADSUtilitis.IsNetworkConnected(activity)) {
            if (shimmerFrameLayout != null && shimmerFrameLayout.isShimmerStarted()) {
                shimmerFrameLayout.stopShimmer();
            }
            if (shimmerFrameLayout != null) shimmerFrameLayout.setVisibility(View.GONE);
            if (listener != null) listener.onAdFailed();
            return;
        }
        /*if (AdmobNativeAd != null) {
            AdmobNativeAd.destroy();
            AdmobNativeAd = null;
        }*/
        loadAdmobNativeAdSmallWithListener(adunit, fl_adplaceholder, shimmerFrameLayout, activity, listener);
    }

    public interface AdLoadListener {
        void onAdLoaded(NativeAd nativeAd);

        void onAdFailed();
    }

    @SuppressLint("MissingPermission")
    public static void loadAdmobNativeAdSmallWithListener(String adId, FrameLayout fl_adplaceholder, ShimmerFrameLayout shimmerFrameLayout, Activity activity, AdLoadListener listener) {
        String adUnitId;
        adUnitId = adId;
        if (TextUtils.isEmpty(adUnitId)) {
            shimmerFrameLayout.setVisibility(View.GONE);
            if (listener != null) listener.onAdFailed();
            return;
        }

        AdLoader.Builder builder = new AdLoader.Builder(activity, adUnitId);

        builder.forNativeAd(nativeAd -> {
            AdmobNativeAd = nativeAd;
            showNativeOptionAdSmall(nativeAd, fl_adplaceholder, activity);
            if (shimmerFrameLayout.isShimmerStarted()) {
                shimmerFrameLayout.stopShimmer();
            }
            shimmerFrameLayout.setVisibility(View.GONE);
            ADSUtilitis.trackScreen(activity, "Native_Load");
            if (listener != null) listener.onAdLoaded(nativeAd);
        }).withAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
            }
        });

        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);
        AdLoader adLoader = builder.withAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
//                Log.d("TAGNative", "onAdLoaded: ");
                FirebaseAnalytics firebaseAnalytics;
                firebaseAnalytics = FirebaseAnalytics.getInstance(activity);
                shimmerFrameLayout.setVisibility(View.GONE);
                shimmerFrameLayout.stopShimmer();
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
//                Log.d("TAGNative", "onAdLoaded: " +adError);
//                loadAdxNativeAdSmall(fl_adplaceholder, flNativeFBSmall, shimmerFrameLayout, activity);
                ADSUtilitis.trackScreen(activity, "Native_Fail");
                shimmerFrameLayout.setVisibility(View.GONE);
                shimmerFrameLayout.stopShimmer();
//                Log.d("loadAdmobNativeAdSmall", "onAdFailedToLoad: 2222222222  " + adError);

            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void showNativeOptionAdSmall(NativeAd nativeAd, FrameLayout fl_adplaceholder, Activity activity) {
        try {
            if (nativeAd != null) {
                // Optimization: If already showing the same ad, don't re-inflate
                if (fl_adplaceholder.getChildCount() > 0) {
                    View firstChild = fl_adplaceholder.getChildAt(0);
                    if (firstChild instanceof NativeAdView && firstChild.getTag() == nativeAd) {
                        fl_adplaceholder.setVisibility(View.VISIBLE);
                        return;
                    }
                }

                NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(R.layout.ad_unifiled_small, null);
                adView.setTag(nativeAd);
                populateUnifiedNativeSmallAdView(nativeAd, adView);
                fl_adplaceholder.setVisibility(View.VISIBLE);
                fl_adplaceholder.removeAllViews();
                fl_adplaceholder.addView(adView);

            } else {
                fl_adplaceholder.setVisibility(View.GONE);
//                flNativeFBSmall.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fl_adplaceholder.setVisibility(View.GONE);
//            flNativeFBSmall.setVisibility(View.GONE);

        }
    }

    public static void populateUnifiedNativeSmallAdView(NativeAd nativeAd, NativeAdView adView) {
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        try {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
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
        ((TextView) adView.getCallToActionView()).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(ADSMainClass.getNativeButtonColor())));
        if (nativeAd.getIcon() == null) {
            Objects.requireNonNull(adView.getIconView()).setVisibility(View.GONE);
        } else {
            ((ImageView) Objects.requireNonNull(adView.getIconView())).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            Objects.requireNonNull(adView.getPriceView()).setVisibility(View.INVISIBLE);
        } else {
            Objects.requireNonNull(adView.getPriceView()).setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }


        if (nativeAd.getStore() == null) {
            Objects.requireNonNull(adView.getStoreView()).setVisibility(View.INVISIBLE);
        } else {
            Objects.requireNonNull(adView.getStoreView()).setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            Objects.requireNonNull(adView.getStarRatingView()).setVisibility(View.INVISIBLE);
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
    }
}
