package com.phonecall.dialcontacts.calldialer.activities.endCall;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass;
import com.phonecall.dialcontacts.calldialer.R;
import com.phonecall.dialcontacts.calldialer.databinding.ActivityCallEndBinding;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CallEndActivity extends AppCompatActivity {
    ActivityCallEndBinding binding;

    /** When true, do not push overlay permission flow (call end opened via full-screen / lock notification). */
    public static final String EXTRA_SKIP_OVERLAY_PROMPT = "skip_overlay_prompt";
    /** When true, allow showing on lockscreen / turning screen on. Default false for FCM flow. */
    public static final String EXTRA_ALLOW_SHOW_ON_LOCKSCREEN = "allow_show_on_lockscreen";


    //    public static NativeAd lkepv__AdmobNativeADS;
//    public static int lkepv__ArrayPos = 0;
//    public static String lkepv__NativeAdview = "";
//    public static NativeAdListener nativeAdListener;
    private boolean isOverlay = false;
    private boolean isUserAskPermission = false;
    private List<Integer> defaultIcons = Arrays.asList(
            R.drawable.icon_img_call_first_icon,
            R.drawable.icon_img_call_second_icon,
            R.drawable.icon_img_call_four_icon,
            R.drawable.icon_img_call_third_icon
    );

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        }

        String stringExtra = getIntent().getStringExtra("mobile_number");
        getIntent().getStringExtra("StartTime");
        getIntent().getStringExtra("EndTime");

        MobileAds.initialize(this, (InitializationStatus initializationStatus) -> {
        });
    }

}