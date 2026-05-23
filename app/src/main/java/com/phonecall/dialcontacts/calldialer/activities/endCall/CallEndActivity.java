package com.phonecall.dialcontacts.calldialer.activities.endCall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSAdpater;
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSAppManage;
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass;
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSNativeFullDisplay;
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSUtilitis;
import com.phonecall.dialcontacts.calldialer.Advertisement.CallEndInterAd;
import com.phonecall.dialcontacts.calldialer.R;
import com.phonecall.dialcontacts.calldialer.activities.home.HomeActivity;
import com.phonecall.dialcontacts.calldialer.activities.newContact.NewContactActivity;
import com.phonecall.dialcontacts.calldialer.activities.overlayPermission.OverlayPermissionActivity;
import com.phonecall.dialcontacts.calldialer.adapters.CallEndTabAdapter;
import com.phonecall.dialcontacts.calldialer.database.AppDatabase;
import com.phonecall.dialcontacts.calldialer.databinding.ActivityCallEndBinding;
import com.phonecall.dialcontacts.calldialer.models.ContactModel;
import com.phonecall.dialcontacts.calldialer.models.TagModel;
import com.phonecall.dialcontacts.calldialer.callEndUtils.CallEndPendingLaunch;
import com.phonecall.dialcontacts.calldialer.callEndUtils.PreferenceDayCycle;
import com.phonecall.dialcontacts.calldialer.utils.Common;
import com.phonecall.dialcontacts.calldialer.utils.Constance;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.CoroutineScope;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler;

@AndroidEntryPoint
public class CallEndActivity extends AppCompatActivity implements OnClickHandler {
    ActivityCallEndBinding binding;

    /**
     * When true, do not push overlay permission flow (call end opened via full-screen / lock notification).
     */
    public static final String EXTRA_SKIP_OVERLAY_PROMPT = "skip_overlay_prompt";
    /**
     * When true, allow showing on lockscreen / turning screen on. Default false for FCM flow.
     */
    public static final String EXTRA_ALLOW_SHOW_ON_LOCKSCREEN = "allow_show_on_lockscreen";

    private boolean isOverlay = false;
    private boolean isUserAskPermission = false;
    /**
     * Exposed so fragments (NewMessageFragment, NewRemindFragment, MoreFeaturesFragment)
     * can retrieve the caller's number regardless of whether they are hosted in
     * EndCallActivity or CallEndActivity.
     */
    public String mobileNumber = null;
    private List<Integer> defaultIcons = Arrays.asList(
            R.drawable.icon_img_call_first_icon,
            R.drawable.icon_img_call_second_icon,
            R.drawable.icon_img_call_four_icon,
            R.drawable.icon_img_call_third_icon
    );
    private AdView adView;
    public static NativeAd AdmobNativeAd;

    public static void sendFirebaseEvent(Activity activity, String str, String str2) {
        FirebaseAnalytics instance = FirebaseAnalytics.getInstance(activity);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, str);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, str2);
        instance.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } catch (IllegalStateException e) {
            // Ignore the exception if the device still complains about non-fullscreen opaque activities
        }
        super.onCreate(savedInstanceState);

        boolean allowShowWhenLocked = getIntent() != null
                && getIntent().getBooleanExtra(EXTRA_ALLOW_SHOW_ON_LOCKSCREEN, false);
        if (allowShowWhenLocked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
            } else {
                @SuppressWarnings("deprecation")
                int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                getWindow().addFlags(flags);
            }
        }

        EdgeToEdge.enable(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call_end);
        binding.setOnClickHandler(this);
        Common.INSTANCE.hideSystemUI(this);

        View mainView = binding.main;
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        Log.e("TAG", "onCreate dfgdgd: " + getIntent().getBooleanExtra("is_from_fcm", false));

        if (getIntent() != null && getIntent().getBooleanExtra("is_from_fcm", false)) {
            boolean notif = ADSMainClass.isNotificationGranted(this);
            boolean call = ADSMainClass.isCallStateGranted(this);
            boolean overlay = ADSMainClass.isOverlayGranted(this);

            if (notif && call && overlay) {
                ADSUtilitis.trackScreen(this, "FB_Notification_show_CallEnd_Allow3");
            } else if (notif && call) {
                ADSUtilitis.trackScreen(this, "FB_Notification_show_CallEnd_Allow2");
            } else if (notif) {
                ADSUtilitis.trackScreen(this, "FB_Notification_show_CallEnd_Allow1");
            }
        }
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            WindowInsetsController controller = getWindow().getInsetsController();
//            if (controller != null) {
//                controller.hide(WindowInsets.Type.navigationBars());
//                controller.setSystemBarsBehavior(
//                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//                );
//            }
//        }

        mobileNumber = getIntent().getStringExtra("mobile_number");

        MobileAds.initialize(this, (InitializationStatus initializationStatus) -> {
        });

//        if (ADSMainClass.getCallEndBottomAdsShow()) {
//            if (ADSMainClass.getCallEndBottomAdsType().equalsIgnoreCase("native")) {
//                Log.e("TAG", "onCreate: jkkkkkkkkk" );
//                loadAdmobNative(this);
//            } else {
//                loadAdmobBannerAd();
//            }
//        } else {
//            binding.linearBannerShimmer.setVisibility(View.GONE);
//        }


        setCallEndEvent();
        setOverLayPermission();
        setAppRetention();

        String callType = getIntent().getStringExtra("CallType");
        boolean isFirebaseFlow = "Notification".equalsIgnoreCase(callType) || "call_end".equalsIgnoreCase(callType);
        if (isFirebaseFlow) {
            binding.main.setVisibility(View.GONE);
            binding.rlFullAd.setVisibility(View.VISIBLE);
            binding.fullAdContainer.setVisibility(View.VISIBLE);

            // Show preloaded ad in fullAdContainer if available
            if (ADSNativeFullDisplay.AdmobNativeAd != null) {
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
                WindowInsetsControllerCompat windowInsetsController = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
                windowInsetsController.setAppearanceLightStatusBars(true);
                binding.getRoot().setBackgroundColor(ContextCompat.getColor(this, R.color.white));
                binding.rlFullAd.setBackgroundColor(ContextCompat.getColor(this, R.color.white));

                NativeAd preloadedAd = ADSNativeFullDisplay.AdmobNativeAd;
                ADSNativeFullDisplay.AdmobNativeAd = null;

                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                NativeAdView adView = (NativeAdView) inflater.inflate(R.layout.ads_full_native_load, null);
                PopulateUnifiedFullNativeAdView(preloadedAd, adView, false);
                binding.fullAdContainer.removeAllViews();
                binding.fullAdContainer.addView(adView);
            } else {
                binding.main.setVisibility(View.VISIBLE);
                binding.fullAdContainer.setVisibility(View.GONE);

                if (ADSMainClass.getCallEndBottomAdsShow()) {
                    if (ADSMainClass.getCallEndBottomAdsType().equalsIgnoreCase("native")) {
                        loadAdmobNative(this);
                    } else {
                        loadAdmobBannerAd();
                    }
                } else {
                    binding.linearBannerShimmer.setVisibility(View.GONE);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsetsController controller = getWindow().getInsetsController();
                    if (controller != null) {
                        controller.hide(WindowInsets.Type.navigationBars());
                        controller.setSystemBarsBehavior(
                                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        );
                    }
                }
//                getDetails();
            }

            Log.e("TAG", "onCreate:dggdggd " + ADSMainClass.getCloseButtonShowOnFullNativeAds());

            if (ADSMainClass.getCloseButtonShowOnFullNativeAds()) {
                binding.ivClose.setVisibility(View.VISIBLE);
            } else {
                binding.ivClose.setVisibility(View.GONE);
            }
        } else {
            binding.main.setVisibility(View.VISIBLE);
            binding.fullAdContainer.setVisibility(View.GONE);
            binding.rlFullAd.setVisibility(View.GONE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                }
            }
            getDetails();
        }

        if (!isFirebaseFlow) {
            if (ADSMainClass.getCallEndBottomAdsShow()) {
                if (ADSMainClass.getCallEndBottomAdsType().equalsIgnoreCase("native")) {
                    loadAdmobNative(this);
                } else {
                    loadAdmobBannerAd();
                }
            } else {
                binding.linearBannerShimmer.setVisibility(View.GONE);
            }
        }
        binding.ivClose.setOnClickListener(v -> finish());


//
//        if (ADSNativeFullDisplay.AdmobNativeAd != null) {
////                Log.d("FCM_AD", "Using preloaded Native Ad from ADSNativeFullDisplay.");
//            NativeAd preloadedAd = ADSNativeFullDisplay.AdmobNativeAd;
//            ADSNativeFullDisplay.AdmobNativeAd = null;
//
//            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            NativeAdView adView = (NativeAdView) inflater.inflate(R.layout.ads_full_native_load, null);
//            PopulateUnifiedFullNativeAdView(preloadedAd, adView, false);
//            binding.fullAdContainer.removeAllViews();
//            binding.fullAdContainer.addView(adView);
////                Log.d("FCM_AD", "Preloaded Ad displayed in fullAdContainer.");
//        } else {
////                Log.e("FCM_AD", "Preloaded Native Ad is NULL in CallEndActivity!");
//            binding.main.setVisibility(View.VISIBLE);
//            binding.fullAdContainer.setVisibility(View.GONE);
//
//            if (ADSMainClass.getCallEndBottomAdsShow()) {
//                if (ADSMainClass.getCallEndBottomAdsType().equalsIgnoreCase("native")) {
//                    loadAdmobNative(this);
//                } else {
//                    loadAdmobBannerAd();
//                }
//            } else {
//                binding.linearBannerShimmer.setVisibility(View.GONE);
//            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                WindowInsetsController controller = getWindow().getInsetsController();
//                if (controller != null) {
//                    controller.hide(WindowInsets.Type.navigationBars());
//                    controller.setSystemBarsBehavior(
//                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//                    );
//                }
//            }
//
//        }


        binding.tvTime.setText(
                new SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                        .format(java.util.Calendar.getInstance().getTime())
        );

        binding.ivCall.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "No Dialer app found", Toast.LENGTH_SHORT).show();
            }
        });

        // Consume clicks on the shimmer so they don't bubble up to the app-opening listener
        binding.linearBannerShimmer.setOnClickListener(v -> {
            // intentionally empty — prevents click-through to views behind the ad
        });

        /*if (binding.header != null) {
            binding.header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openMainActivity();
                }
            });
        }*/

        binding.wsfwTabslayout.addTab(binding.wsfwTabslayout.newTab().setIcon(R.drawable.icon_img_call_first_icon));
        binding.wsfwTabslayout.addTab(binding.wsfwTabslayout.newTab().setIcon(R.drawable.icon_img_call_second_icon));
        binding.wsfwTabslayout.addTab(binding.wsfwTabslayout.newTab().setIcon(R.drawable.icon_img_call_four_icon));
        binding.wsfwTabslayout.addTab(binding.wsfwTabslayout.newTab().setIcon(R.drawable.icon_img_call_third_icon));
        binding.wsfwTabslayout.setTabGravity(TabLayout.GRAVITY_FILL);
        binding.wsfwViewpager.setAdapter(new CallEndTabAdapter(getSupportFragmentManager(), binding.wsfwTabslayout.getTabCount()/*, stringExtra*/));
        binding.wsfwViewpager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(binding.wsfwTabslayout));
        binding.wsfwTabslayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.wsfwViewpager.setCurrentItem(tab.getPosition());
                if (tab.getPosition() == 0) {
                    tab.setIcon(R.drawable.icon_img_call_first_icon);
                } else if (tab.getPosition() == 1) {
                    tab.setIcon(R.drawable.icon_img_call_second_icon);
                } else if (tab.getPosition() == 2) {
                    tab.setIcon(R.drawable.icon_img_call_four_icon);
                } else {
                    tab.setIcon(R.drawable.icon_img_call_third_icon);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.setIcon(defaultIcons.get(tab.getPosition()));
            }
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getBack();
            }
        });

        if (ADSUtilitis.IsNetworkConnected(this) && ADSMainClass.shouldShowCallEndAd(this)) {
            CallEndInterAd.admobFullScreenAdLoad(this);
        }
        sendFirebaseEvent(this, "CallEndActivity", "CallEndActivity");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            boolean locked = km != null && km.isKeyguardLocked();
            if (!locked) {
                CallEndPendingLaunch.clear(this);
            } else {
                CallEndPendingLaunch.cancelCallEndNotification(this);
            }
        } catch (Exception ignored) {
        }
    }

    public void getDetails() {
        String stringExtra2 = getIntent().getStringExtra("formattedDuration");
        binding.tvDuration.setText("Duration : " + stringExtra2);
        // Set Status Label based on call type (ported from EndCallActivity)
        String callTypeValue = getIntent().getStringExtra("CallType");
        String callTypeLabel;
        if (callTypeValue == null) {
            callTypeLabel = "";
        } else {
            switch (callTypeValue) {
                case "incoming":
                    callTypeLabel = getString(R.string.incoming_call);
                    break;
                case "outgoing":
                    callTypeLabel = getString(R.string.outgoing_calls);
                    break;
                case "missed":
                    callTypeLabel = getString(R.string.missed_call);
                    break;
                case "rejected":
                    callTypeLabel = getString(R.string.call_declined);
                    break;
                default:
                    callTypeLabel = "";
                    break;
            }
        }
        binding.tvCallType.setText(callTypeLabel);

        if (mobileNumber == null || mobileNumber.isEmpty()) {
            binding.tvTitle.setText(getString(R.string.unknown));
            binding.ivContactPhoto.setVisibility(View.GONE);
            binding.tvFirstName.setVisibility(View.VISIBLE);
            binding.tvFirstName.setText("?");
            binding.llAddContact.setVisibility(View.GONE);
        } else {
            ContactModel contact = Common.INSTANCE.getContactByNumber(this, mobileNumber);
            if (contact != null) {
                binding.llAddContact.setVisibility(View.GONE);
                String displayName = (contact.getDisplayName() != null && !contact.getDisplayName().trim().isEmpty())
                        ? contact.getDisplayName() : mobileNumber;
                binding.tvTitle.setText(displayName);

                if (contact.getUserThumbnail() != null && !contact.getUserThumbnail().isEmpty()) {
                    binding.ivContactPhoto.setVisibility(View.VISIBLE);
                    binding.ivUser.setVisibility(View.GONE);
                    Glide.with(this).load(contact.getUserThumbnail())
                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                            .error(R.drawable.ic_contact_profile).into(binding.ivContactPhoto);
                } else {
                    binding.ivContactPhoto.setVisibility(View.GONE);
                    binding.tvFirstName.setVisibility(View.VISIBLE);
                    String[] parts = displayName.trim().split("\\s+");
                    StringBuilder initials = new StringBuilder();
                    int count = 0;
                    for (String part : parts) {
                        if (!part.isEmpty() && count < 2) {
                            initials.append(part.substring(0, 1).toUpperCase());
                            count++;
                        }
                    }
                    binding.tvFirstName.setText(initials.toString());
                }
            } else {
                binding.llAddContact.setVisibility(View.VISIBLE);
                binding.tvTitle.setText(mobileNumber);
                binding.ivContactPhoto.setVisibility(View.GONE);
                binding.tvFirstName.setText(!mobileNumber.trim().isEmpty() ? mobileNumber.trim().substring(0, 1) : "");

                new Thread(() -> {
                    AppDatabase db =
                            androidx.room.Room.databaseBuilder(
                                    getApplicationContext(), AppDatabase.class,
                                    Constance.DB_NAME
                            ).build();

                    TagModel tagModel =
                            db.tagDao().getTagByNumberSync(mobileNumber);
                    String tag = (tagModel != null) ? tagModel.getTagName() : null;

                    runOnUiThread(() -> {
                        if (tag == null || tag.trim().isEmpty()) {
                            binding.tvFirstName.setVisibility(View.GONE);
                            binding.ivUser.setVisibility(View.VISIBLE);
                        } else {
                            binding.ivUser.setVisibility(View.GONE);
                            binding.tvFirstName.setVisibility(View.VISIBLE);
                        }

                        String resolvedName;
                        if (tag != null && !tag.trim().isEmpty()) {
                            resolvedName = tag;
                        } else if (!mobileNumber.trim().isEmpty()) {
                            resolvedName = mobileNumber;
                        } else {
                            resolvedName = getString(R.string.unknown);
                        }

                        binding.tvTitle.setText(resolvedName);
                        binding.tvFirstName.setText(!resolvedName.trim().isEmpty() ? resolvedName.trim().substring(0, 1).toUpperCase() : "");
                    });
                }).start();
            }
        }
    }

    /*public void getBack() {
        if (ADSMainClass.shouldShowCallEndAd(this)) {
            CallEndInterAd.fullScreenAdShow(this, b -> {
                if (b) {
                    ADSMainClass.updateCallEndAdCount(this);
                }
                finish();
            });
        } else {
            finish();
        }
    }*/

    public void getBack() {
        String callType = getIntent().getStringExtra("CallType");
        boolean isFirebaseFlow = "Notification".equalsIgnoreCase(callType) || "call_end".equalsIgnoreCase(callType);

        boolean showAd;
        if (isFirebaseFlow) {
            showAd = ADSMainClass.getNotificationScreenBackAdsShow();
        } else {
            showAd = ADSMainClass.shouldShowCallEndAd(this);
        }

        if (showAd && !ADSMainClass.getAds_Free()) {
            CallEndInterAd.fullScreenAdShow(this, b -> {
                if (b) {
                    ADSMainClass.updateCallEndAdCount(this);
                }
                new Handler().postDelayed(this::finish, 1000);
            });
        } else {
            finish();
        }
    }

    private void setAppRetention() {
        PreferenceDayCycle.checkAndUpdateDayCount(this);
        int dayCount = PreferenceDayCycle.getDayCount(this);
        if (dayCount > 0 && dayCount <= 7) {
            setAppRetentionLogEvent(dayCount);
        }
    }

    private void setAppRetentionLogEvent(int dayCount) {
        FirebaseApp.initializeApp(this);
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();

        if (dayCount == 1 && !ADSMainClass.getCallRetention1Day()) {
            bundle.putBoolean("OneDayCallEndRetention", true);
            firebaseAnalytics.logEvent("OneDayCallEndRetention", bundle);
            ADSMainClass.setCallRetention1Day(true);
        } else if (dayCount == 3 && !ADSMainClass.getCallRetention3Day()) {
            bundle.putBoolean("ThreeDayCallEndRetention", true);
            firebaseAnalytics.logEvent("ThreeDayCallEndRetention", bundle);
            ADSMainClass.setCallRetention3Day(true);
        } else if (dayCount == 7 && !ADSMainClass.getCallRetention7Day()) {
            bundle.putBoolean("SevenDayCallEndRetention", true);
            firebaseAnalytics.logEvent("SevenDayCallEndRetention", bundle);
            ADSMainClass.setCallRetention7Day(true);
        }
    }

    public void AdsDisplayCheck(boolean adsDisplayCheck) {
        // dummy method logic based on missing method error
    }

    private void setOverLayPermission() {
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_SKIP_OVERLAY_PROMPT, false)) {
            return;
        }
        if (!isUserAskPermission) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please Accept Overlay Permission", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION",
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 132);

                if (!isOverlay) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent overlayIntent = new Intent(CallEndActivity.this, OverlayPermissionActivity.class);
                            startActivity(overlayIntent);
                        }
                    }, 1000);
                    isOverlay = true;
                    AdsDisplayCheck(false);
                }

                isUserAskPermission = true;
                return;
            }
        }
    }

    private void setCallEndEvent() {
        int countCallEnd = ADSMainClass.getCallEndShowEvent() + 1;
        ADSMainClass.setCallEndShowEvent(countCallEnd);
        FirebaseApp.initializeApp(this);
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();

        if (countCallEnd == 1 && !ADSMainClass.getCallEndShow1()) {
            bundle.putBoolean("OneTimeCallEndShow", true);
            firebaseAnalytics.logEvent("OneTimeCallEndShow", bundle);
            ADSMainClass.setCallEndShow1(true);
        } else if (countCallEnd == 5 && !ADSMainClass.getCallEndShow5()) {
            bundle.putBoolean("FiveTimeCallEndShow", true);
            firebaseAnalytics.logEvent("FiveTimeCallEndShow", bundle);
            ADSMainClass.setCallEndShow5(true);
        } else if (countCallEnd == 10 && !ADSMainClass.getCallEndShow10()) {
            bundle.putBoolean("TenTimeCallEndShow", true);
            firebaseAnalytics.logEvent("TenTimeCallEndShow", bundle);
            ADSMainClass.setCallEndShow10(true);
        } else if (countCallEnd == 50 && !ADSMainClass.getCallEndShow50()) {
            bundle.putBoolean("FiftyTimeCallEndShow", true);
            firebaseAnalytics.logEvent("FiftyTimeCallEndShow", bundle);
            ADSMainClass.setCallEndShow50(true);
        } else if (countCallEnd == 100 && !ADSMainClass.getCallEndShow100()) {
            bundle.putBoolean("HundredTimeCallEndShow", true);
            firebaseAnalytics.logEvent("HundredTimeCallEndShow", bundle);
            ADSMainClass.setCallEndShow100(true);
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        if (AdmobNativeAd != null) {
            AdmobNativeAd.destroy();
            AdmobNativeAd = null;
        }
        binding.adContainer.removeAllViews();
        ADSAppManage.dialogboolean = true;
        super.onDestroy();
    }

    /*public ContactModel getContactList(Context context, String str) {
        ContactModel contact;
        try {
            Cursor query = context.getContentResolver().query(
                    Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(str)),
                    new String[]{"display_name", "photo_uri", "photo_thumb_uri", "contact_id"},
                    null, null, null);

            if (query == null) return null;

            if (query.moveToFirst()) {
                contact = new ContactModel();
                contact.setContactId(query.getString(query.getColumnIndexOrThrow("contact_id")));
                contact.setDisplayName(query.getString(query.getColumnIndexOrThrow("display_name")));
                contact.setUserThumbnail(query.getString(query.getColumnIndexOrThrow("photo_uri")));
            } else {
                contact = null;
            }

            if (!query.isClosed()) {
                query.close();
            }
            return contact;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }*/

    private void loadAdmobBannerAd() {
        binding.linearBannerShimmer.setVisibility(View.VISIBLE);
        adView = new AdView(this);
        adView.setAdUnitId(ADSMainClass.getStringValue(ADSMainClass.CALL_END_BANNER));
        AdSize adSize = getAdaptiveAdSize();
        adView.setAdSize(adSize);
        adView.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        binding.adContainer.clearFocus();
        binding.adContainer.removeAllViews();
        binding.adContainer.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        FirebaseAnalytics firebaseAnalytics;
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        adView.setOnPaidEventListener(new OnPaidEventListener() {
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

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                binding.linearBannerShimmer.setVisibility(View.GONE);
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                binding.linearBannerShimmer.setVisibility(View.GONE);
            }
        });
    }

    public void loadAdmobNative(final Context context) {
        AdLoader.Builder builder = new AdLoader.Builder(context, ADSMainClass.getStringValue(ADSMainClass.CALL_END_Native).trim())
                .forNativeAd(nativeAd -> {
                    binding.linearBannerShimmer.setVisibility(View.GONE);
                    FirebaseAnalytics firebaseAnalytics;
                    firebaseAnalytics = FirebaseAnalytics.getInstance(context);
                    nativeAd.setOnPaidEventListener(adValue -> {
                        double revenue = adValue.getValueMicros() / 1_000_000.0;
                        String currency = adValue.getCurrencyCode();
                        Bundle adRevenueParams = new Bundle();
                        adRevenueParams.putString(FirebaseAnalytics.Param.AD_PLATFORM, "Google Ad Manager");
                        adRevenueParams.putString(FirebaseAnalytics.Param.CURRENCY, currency);
                        adRevenueParams.putDouble(FirebaseAnalytics.Param.VALUE, revenue);
                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, adRevenueParams);
                    });

                    AdmobNativeAd = nativeAd;

                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    NativeAdView adView;
                    adView = (NativeAdView) inflater.inflate(R.layout.admob_big_native_ad, null);

                    binding.adContainer.setVisibility(View.VISIBLE);
                    PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false);
                    adView.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                    binding.adContainer.clearFocus();
                    binding.adContainer.removeAllViews();
                    binding.adContainer.addView(adView);
                    ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
                });

        AdLoader adLoader = builder.withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        if (AdmobNativeAd != null) {
                            AdmobNativeAd = null;
                        }
                        binding.linearBannerShimmer.setVisibility(View.GONE);
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
        Log.e("TAG", "PopulateUnifiedFullNativeAdView: kkkkk");
        MediaView mediaView = adView.findViewById(R.id.ad_media);

        adView.setMediaView(mediaView);
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        if (adView.getHeadlineView() != null) {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        }

        try {
            if (adView.getCallToActionView() != null) {
                adView.getCallToActionView().setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(ADSMainClass.getNativeButtonColor())));
                ((TextView) adView.getCallToActionView()).setTextColor(Color.parseColor(ADSMainClass.getNativeButtonTextColor()));
            }
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (nativeAd.getBody() == null) {
            if (adView.getBodyView() != null) adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            if (adView.getBodyView() != null) {
                adView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
            }
        }

        if (nativeAd.getCallToAction() == null) {
            if (adView.getCallToActionView() != null)
                adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            if (adView.getCallToActionView() != null) {
                adView.getCallToActionView().setVisibility(View.VISIBLE);
                ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
            }
        }

        if (nativeAd.getIcon() == null) {
            if (adView.getIconView() != null) adView.getIconView().setVisibility(View.GONE);
        } else {
            if (adView.getIconView() != null) {
                ((ImageView) adView.getIconView()).setImageDrawable(
                        nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            }
        }

        if (nativeAd.getPrice() == null) {
            if (adView.getPriceView() != null) adView.getPriceView().setVisibility(View.GONE);
        } else {
            if (adView.getPriceView() != null) {
                adView.getPriceView().setVisibility(View.VISIBLE);
                ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
            }
        }

        if (nativeAd.getStore() == null) {
            if (adView.getStoreView() != null) adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            if (adView.getStoreView() != null) {
                adView.getStoreView().setVisibility(View.VISIBLE);
                ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
            }
        }

        if (nativeAd.getStarRating() == null) {
            if (adView.getStarRatingView() != null)
                adView.getStarRatingView().setVisibility(View.GONE);
        } else {
            if (adView.getStarRatingView() != null) {
                ((RatingBar) adView.getStarRatingView()).setRating(nativeAd.getStarRating().floatValue());
                adView.getStarRatingView().setVisibility(flag ? View.GONE : View.VISIBLE);
            }
        }

        if (nativeAd.getAdvertiser() == null) {
            if (adView.getAdvertiserView() != null)
                adView.getAdvertiserView().setVisibility(View.GONE);
        } else {
            if (adView.getAdvertiserView() != null) {
                ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
                adView.getAdvertiserView().setVisibility(flag ? View.GONE : View.VISIBLE);
            }
        }

        adView.setNativeAd(nativeAd);
        MediaContent vc = nativeAd.getMediaContent();

        if (vc != null && vc.hasVideoContent()) {
            vc.getVideoController().setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {
                    super.onVideoEnd();
                }
            });
        } else {
            if (mediaView != null) {
                mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        }
    }

    private AdSize getAdaptiveAdSize() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = outMetrics.density;
        int adWidth = (int) (outMetrics.widthPixels / density);
        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(this, adWidth);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 132) {
            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
            Bundle bundle = new Bundle();
            if (Settings.canDrawOverlays(this)) {
                bundle.putBoolean("OverlayPermissionAllowed", true);
                firebaseAnalytics.logEvent("OverlayPermissionAllowed", bundle);
            } else {
                bundle.putBoolean("OverlayPermissionDenied", true);
                firebaseAnalytics.logEvent("OverlayPermissionDenied", bundle);
            }
        }
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        if (!Common.INSTANCE.isValidClick(500)) return;

        int id = view.getId();
        if (id == binding.llCall.getId() || id == binding.ivCall.getId()) {
            if (mobileNumber != null) {
                Common.INSTANCE.actionCall(mobileNumber, this, false);
            }
            finish();
//            finishAndRemoveTask();
        } else if (id == binding.llAddContact.getId()) {
            Intent intent = new Intent(this, NewContactActivity.class);
            intent.putExtra(Constance.NUMBER, mobileNumber);
            startActivity(intent);
            finish();
        } else if (id == binding.llSms.getId()) {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + mobileNumber));
            startActivity(intent);
            finish();
        } else if (id == binding.llContact.getId()) {
            Common.INSTANCE.openHomeActivity(this, "contacts");
            finish();
        }
    }
}