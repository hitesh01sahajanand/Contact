package com.example.contactmanager.Advertisement;


import static com.example.contactmanager.Advertisement.ADSInterDisplay.AdsDisplayCheck;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.contactmanager.R;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.NativeBannerAd;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.firebase.analytics.FirebaseAnalytics;


import java.util.ArrayList;
import java.util.List;


public class ADSBanner {

    public static Context contexts;
    public static int ArrayIndexBottom = 0;
    public static int ADSArrayIndexBottom = 0;
    public static LinearLayout FBAdViewBottom;
    public static NativeAd ADSAdmobSmallNativeBanner;
    public static boolean FailArrayBottomID = false;
    public static String AdviewNativeBanner = "";
    public static NativeAdLayout NativeAdLayout;
    public static int FBArrayIndexNativeBannerId = 0;
    public static boolean FBFailArrayIDNativeBannerId = false;
    public static boolean FBFailArrayBannerId = false;
    public static AdView AdmobAdView;
    private static NativeBannerAd ADSmNativeBanner;

    public static void CustomNativeDisplay(final Context context, final LinearLayout linearLayout, final LinearLayout lnr_view) {

        contexts = context;
        ArrayIndexBottom = 0;

        if (ADSMainClass.getAds_Free()) {
            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.GONE);
            return;
        }

        if (ADSMainClass.getAdsDisplayType().equals("facebook")) {
            if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                FBBottomNativeBanner(context, linearLayout, lnr_view);
            } else {
                FBSmallNativeBanner(context, linearLayout, lnr_view);
            }
        } else {
            if (ADSMainClass.getBannerTypes().equals("banner")) {
                lnr_view.setVisibility(View.VISIBLE);
                AdmobBannerDisplay(context, linearLayout, lnr_view);
            } else {
                if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    AdmobNativeBanner(context, linearLayout, lnr_view);
                } else {
                    AdmobSmallNativeBanner(context, linearLayout, lnr_view);
                }
            }
        }

    }

    public static void FBSmallNativeBanner(final Context context, final LinearLayout linearLayout, final LinearLayout lnr_view) {
        if (ADSmNativeBanner != null && ADSmNativeBanner.isAdLoaded()) {

            linearLayout.setVisibility(View.VISIBLE);
            lnr_view.setVisibility(View.GONE);
            linearLayout.removeAllViews();
            InflateAdBottom(ADSmNativeBanner, context, linearLayout);
        } else if (ADSAdmobSmallNativeBanner != null) {

            if (ADSMainClass.getAdmobCustomIdList() != null && ADSMainClass.getAdmobCustomIdList().size() != 0) {

            }
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            NativeAdView adView = (NativeAdView) inflater.inflate(R.layout.bottom_native_banner, null);
            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);
            PopulateUnifiedNativeAdViewFull(ADSAdmobSmallNativeBanner, adView, false);

            linearLayout.removeAllViews();
            linearLayout.addView(adView);
        } else {
            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.GONE);
        }

        FBBottomNativeBanner(context, linearLayout, lnr_view);
    }

    @SuppressLint("MissingPermission")
    public static void loadAdMobBanner(String adUint, RelativeLayout adContainerView, ShimmerFrameLayout shimmerFrameLayout, Activity activity) {
        String adUnitId;
        adUnitId = adUint;

//        Log.d("BannerAdddd", "loadAdMobBanner: " +  adUint);
        if (TextUtils.isEmpty(adUnitId)) {
//            Log.d("BannerAdddd", "loadAdMobBanner 222 : " +  adUint);
            shimmerFrameLayout.setVisibility(View.GONE);
            adContainerView.setVisibility(View.GONE);
            return;
        }
        if (!ADSMainClass.getAdsTypeManage().equals("Load")) {
//            Log.d("BannerAdddd", "loadAdMobBanner: 23333" +  adUint);
            shimmerFrameLayout.setVisibility(View.GONE);
        }

        //BannerAd
        AdView admobManagerAdView = new AdView(activity);
        admobManagerAdView.setAdUnitId(adUnitId);
        adContainerView.addView(admobManagerAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        AdSize adSize = getAdSize(activity);
        admobManagerAdView.setAdSize(adSize);
        admobManagerAdView.loadAd(adRequest);
//        Log.d("BannerAdddd", "loadAdMobBanner:  444 ");
        admobManagerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
//                Log.d("BannerAdddd", "onAdLoaded: ");
                FirebaseAnalytics firebaseAnalytics;
                firebaseAnalytics  = FirebaseAnalytics.getInstance(activity);
                admobManagerAdView.setOnPaidEventListener(new OnPaidEventListener() {
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

                if (shimmerFrameLayout.isShimmerStarted()) {
                    shimmerFrameLayout.stopShimmer();
                }
                shimmerFrameLayout.setVisibility(View.GONE);
                adContainerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
//                Log.d("BannerAdddd", "onAdFailedToLoad: " + loadAdError.getMessage());
                if (shimmerFrameLayout.isShimmerStarted()) {
                    shimmerFrameLayout.stopShimmer();
                }
                shimmerFrameLayout.setVisibility(View.GONE);
                adContainerView.setVisibility(View.VISIBLE);

            }
        });
    }
    public static void AdmobSmallNativeBanner(final Context context, final LinearLayout linearLayout, final LinearLayout lnr_view) {
        if (ADSMainClass.getAdmobCustomIdList() != null && ADSMainClass.getAdmobCustomIdList().size() != 0) {

        }
        if (ADSAdmobSmallNativeBanner != null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            NativeAdView adView = (NativeAdView) inflater.inflate(R.layout.bottom_native_banner, null);
            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);
            PopulateUnifiedNativeAdViewFull(ADSAdmobSmallNativeBanner, adView, false);

            linearLayout.removeAllViews();
            linearLayout.addView(adView);

        } else if (ADSmNativeBanner != null && ADSmNativeBanner.isAdLoaded()) {

            linearLayout.setVisibility(View.VISIBLE);
            lnr_view.setVisibility(View.GONE);
            linearLayout.removeAllViews();
            InflateAdBottom(ADSmNativeBanner, context, linearLayout);
        } else {
            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.GONE);
        }
        AdmobNativeBanner(context, linearLayout, lnr_view);
    }

    public static void ADSFBNativeBannerRandomId() {
        try {
            if (ADSMainClass.getfbNativeFullList() != null && ADSMainClass.getfbNativeFullList().size() != 0 && ADSMainClass.getfbNativeFullList().size() != FBArrayIndexNativeBannerId) {
                FBFailArrayIDNativeBannerId = true;
                ADSMainClass.setFBNativeBannerID(ADSMainClass.getfbNativeFullList().get(FBArrayIndexNativeBannerId));
                FBArrayIndexNativeBannerId = FBArrayIndexNativeBannerId + 1;
            } else {
                FBArrayIndexNativeBannerId = 0;
                FBFailArrayIDNativeBannerId = false;
            }
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void FBBottomNativeBanner(Context context, LinearLayout linearLayout, LinearLayout lnr_view) {

        ADSFBNativeBannerRandomId();

        if (!FBFailArrayIDNativeBannerId) {
            if (ADSMainClass.getAdsDisplayType().equals("facebook")) {
                AdmobNativeBanner(context, linearLayout, lnr_view);
            } else {


                if (ADSMainClass.getBannerTypes().equals("banner")) {
                    linearLayout.setVisibility(View.GONE);
                    lnr_view.setVisibility(View.GONE);
                    return;
                }
                if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    linearLayout.setVisibility(View.GONE);
                    lnr_view.setVisibility(View.GONE);
                }
            }

            return;
        }

        ADSmNativeBanner = new NativeBannerAd(context, ADSMainClass.getFBNativeBannerID());

        NativeAdListener nativeAdListener = new NativeAdListener() {
            @Override
            public void onMediaDownloaded(Ad ad) {

            }

            @Override
            public void onError(Ad ad, AdError adError) {

                FBBottomNativeBanner(context, linearLayout, lnr_view);
            }

            @Override
            public void onAdLoaded(Ad ad) {

                if (ADSMainClass.getAdsOneByOneIDS()) {
                    if (ADSMainClass.getfbNativeFullList() != null && ADSMainClass.getfbNativeFullList().size() != 0 && ADSMainClass.getfbNativeFullList().size() == FBArrayIndexNativeBannerId) {
                        FBArrayIndexNativeBannerId = 0;
                    }
                } else {
                    FBArrayIndexNativeBannerId = 0;
                }

                if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    if (ADSmNativeBanner != null && ADSmNativeBanner.isAdLoaded()) {

                        linearLayout.setVisibility(View.VISIBLE);
                        lnr_view.setVisibility(View.GONE);
                        linearLayout.removeAllViews();
                        InflateAdBottom(ADSmNativeBanner, context, linearLayout);
                    }
                }
            }

            @Override
            public void onAdClicked(Ad ad) {
                MyApplication.FastStart=true;
            }

            @Override
            public void onLoggingImpression(Ad ad) {

            }
        };
        ADSmNativeBanner.loadAd(ADSmNativeBanner.buildLoadAdConfig().withAdListener(nativeAdListener).build());
    }

    public static void BottomNativeRandomId() {
        try {

            if (ADSMainClass.getAdmobCustomIdList() != null && ADSMainClass.getAdmobCustomIdList().size() != 0 && ADSMainClass.getAdmobCustomIdList().size() != ArrayIndexBottom) {
                FailArrayBottomID = true;
                ADSMainClass.setAdsAdmobNativeIdTwo(ADSMainClass.getAdmobCustomIdList().get(ArrayIndexBottom));
                ArrayIndexBottom = ArrayIndexBottom + 1;
            } else {
                FailArrayBottomID = false;

            }
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void AdmobNativeBanner(final Context context, LinearLayout linearLayout, LinearLayout lnr_view) {

        BottomNativeRandomId();
        if (!FailArrayBottomID) {
            if (!ADSMainClass.getAdsDisplayType().equals("facebook")) {
                FBBottomNativeBanner(context, linearLayout, lnr_view);
            } else {
                if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    linearLayout.setVisibility(View.GONE);
                    lnr_view.setVisibility(View.GONE);
                }
            }
            return;
        }
        AdviewNativeBanner = "";
        AdviewNativeBanner = ADSMainClass.getAdsAdmobNativeIdTwo();

        AdLoader.Builder builder = new AdLoader.Builder(context, AdviewNativeBanner)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {

                        FirebaseAnalytics firebaseAnalytics;
                        firebaseAnalytics  = FirebaseAnalytics.getInstance(context);
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

                        if (ADSAdmobSmallNativeBanner != null) {
                            ADSAdmobSmallNativeBanner = null;
                        }
                        ADSAdmobSmallNativeBanner = nativeAd;

                        if (ADSMainClass.getAdsOneByOneIDS()) {
                            if (ADSMainClass.getAdmobCustomIdList() != null && ADSMainClass.getAdmobCustomIdList().size() != 0 && ADSMainClass.getAdmobCustomIdList().size() == ArrayIndexBottom) {
                                ArrayIndexBottom = 0;
                            }
                        } else {
                            ArrayIndexBottom = 0;
                        }

                        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                            if (ADSAdmobSmallNativeBanner != null) {
                                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                NativeAdView adView = (NativeAdView) inflater.inflate(R.layout.bottom_native_banner, null);
                                lnr_view.setVisibility(View.GONE);
                                linearLayout.setVisibility(View.VISIBLE);
                                PopulateUnifiedNativeAdViewFull(ADSAdmobSmallNativeBanner, adView, false);

                                linearLayout.removeAllViews();
                                linearLayout.addView(adView);
                            }
                        }

                    }
                });

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                if (ADSAdmobSmallNativeBanner != null) {
                    ADSAdmobSmallNativeBanner = null;
                }

                AdmobNativeBanner(context, linearLayout, lnr_view);
            }

            @Override
            public void onAdClicked() {
                MyApplication.FastStart = true;
//                App.Companion.setFastStart(true);
            }
        }).build();

        Log.e("ArrayListId_bottom", "admob Native ad to loading: ");
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void InflateAdBottom(NativeBannerAd nativeBannerAd, Context context, LinearLayout linearLayout) {
        nativeBannerAd.unregisterView();

        LayoutInflater inflater = LayoutInflater.from(context);
        FBAdViewBottom = (LinearLayout) inflater.inflate(R.layout.fb_small_native_banner, NativeAdLayout, false);

        RelativeLayout adChoicesContainer = FBAdViewBottom.findViewById(R.id.ad_choices_container);
        AdOptionsView adOptionsView = new AdOptionsView(context, nativeBannerAd, NativeAdLayout);
        adChoicesContainer.removeAllViews();
        adChoicesContainer.addView(adOptionsView, 0);

        TextView nativeAdTitle = FBAdViewBottom.findViewById(R.id.native_ad_title);
        TextView nativeAdSocialContext = FBAdViewBottom.findViewById(R.id.native_ad_social_context);
        TextView sponsoredLabel = FBAdViewBottom.findViewById(R.id.native_ad_sponsored_label);
        MediaView nativeAdIconView = FBAdViewBottom.findViewById(R.id.native_icon_view);
        TextView nativeAdCallToAction = FBAdViewBottom.findViewById(R.id.native_ad_call_to_action);

        nativeAdCallToAction.setText(nativeBannerAd.getAdCallToAction());
        nativeAdCallToAction.setVisibility(
                nativeBannerAd.hasCallToAction() ? View.VISIBLE : View.INVISIBLE);
        nativeAdTitle.setText(nativeBannerAd.getAdvertiserName());
        nativeAdSocialContext.setText(nativeBannerAd.getAdSocialContext());
        sponsoredLabel.setText(nativeBannerAd.getSponsoredTranslation());

        List<View> clickableViews = new ArrayList<>();
        clickableViews.add(nativeAdTitle);
        clickableViews.add(nativeAdCallToAction);
        nativeBannerAd.registerViewForInteraction(FBAdViewBottom, nativeAdIconView, clickableViews);
        linearLayout.addView(FBAdViewBottom);
    }

    public static void PopulateUnifiedNativeAdViewFull(NativeAd nativeAd, NativeAdView adView, boolean flag) {


        com.google.android.gms.ads.nativead.MediaView mediaView = adView.findViewById(R.id.ad_media);

        //   adView.setMediaView(mediaView);
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
            //((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
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

    public static void AdmobBannerDisplay(Context context, LinearLayout lnr_adview, LinearLayout ll_label) {

        BannerRandomId();
        if (!FBFailArrayBannerId) {
            if (!ADSMainClass.getAdsDisplayType().equals("facebook")) {
                FBBottomNativeBanner(context, lnr_adview, ll_label);
            } else {
                if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    lnr_adview.setVisibility(View.GONE);
                    ll_label.setVisibility(View.GONE);
                }
            }
            return;
        }

        lnr_adview.removeAllViews();


        AdmobAdView = new AdView(context);
        AdmobAdView.setAdUnitId(ADSMainClass.getAdsAdmobBannerId());
        lnr_adview.addView(AdmobAdView);
        AdSize adSize = getAdSize((Activity) context);
        AdmobAdView.setAdSize(adSize);
        Bundle bundle = new Bundle();
        AdRequest build = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, bundle).build();
        AdmobAdView.loadAd(build);

        FirebaseAnalytics firebaseAnalytics;
        firebaseAnalytics  = FirebaseAnalytics.getInstance(context);
        AdmobAdView.setOnPaidEventListener(new OnPaidEventListener() {
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

        AdmobAdView.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                lnr_adview.setVisibility(View.VISIBLE);
                ll_label.setVisibility(View.GONE);

                if (ADSMainClass.getAdsOneByOneIDS()) {
                    if (ADSMainClass.getAdmobBannerIdList() != null && ADSMainClass.getAdmobBannerIdList().size() != 0 && ADSMainClass.getAdmobBannerIdList().size() == ADSArrayIndexBottom) {
                        ADSArrayIndexBottom = 0;
                    }
                } else {
                    ADSArrayIndexBottom = 0;
                }

            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                AdmobBannerDisplay(context, lnr_adview, ll_label);
            }

            @Override
            public void onAdOpened() {


            }

            @Override
            public void onAdClicked() {
                AdsDisplayCheck(false);
            }

            @Override
            public void onAdClosed() {


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

    public static void BannerRandomId() {
        try {
            if (ADSMainClass.getAdmobBannerIdList() != null && ADSMainClass.getAdmobBannerIdList().size() != 0 && ADSMainClass.getAdmobBannerIdList().size() != ADSArrayIndexBottom) {
                FBFailArrayBannerId = true;
                ADSMainClass.setAdsAdmobBannerId(ADSMainClass.getAdmobBannerIdList().get(ADSArrayIndexBottom));
                ADSArrayIndexBottom = ADSArrayIndexBottom + 1;
            } else {
                FBFailArrayBannerId = false;
            }
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void CallEndBannerRandomId() {
        try {
            if (ADSMainClass.getAdmob_Call_End_Banner_ID_List() != null && ADSMainClass.getAdmob_Call_End_Banner_ID_List().size() != 0 && ADSMainClass.getAdmob_Call_End_Banner_ID_List().size() != ADSArrayIndexBottom) {
                FBFailArrayBannerId = true;
                ADSMainClass.set_Admob_call_end_BannerId(ADSMainClass.getAdmob_Call_End_Banner_ID_List().get(ADSArrayIndexBottom));
                ADSArrayIndexBottom = ADSArrayIndexBottom + 1;
            } else {
                FBFailArrayBannerId = false;
            }
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
