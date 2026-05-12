package com.example.contactmanager.Advertisement.Views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.contactmanager.Advertisement.ADSBanner;
import com.example.contactmanager.R;

public class ADSSmallBannerNative extends LinearLayout {

    static Context context;

    public ADSSmallBannerNative(Context context) {
        super(context);
    }

    public ADSSmallBannerNative(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ADSSmallBannerNative.context = context;

        LayoutInflater.from(context).inflate(R.layout.layout_small_native_banner_load, this, true);

        LinearLayout KP_Main_Layout = findViewById(R.id.KP_Main_Layout);
        LinearLayout KP_Main_Native = findViewById(R.id.KP_Main_Native);

        ADSBanner.CustomNativeDisplay(context, KP_Main_Native, KP_Main_Layout);

    }

    public ADSSmallBannerNative(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
