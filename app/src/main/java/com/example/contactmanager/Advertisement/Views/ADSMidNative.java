package com.example.contactmanager.Advertisement.Views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.contactmanager.Advertisement.ADSNativeDisplay;
import com.example.contactmanager.R;

public class ADSMidNative extends LinearLayout {

    static LinearLayout KP_Main_Full;
    static LinearLayout Native_Full_Main;

    public ADSMidNative(Context context) {
        super(context);
    }

    public ADSMidNative(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        ADSBigNative.context = context;
        LayoutInflater li = LayoutInflater.from(context);
        LinearLayout ll = (LinearLayout) li.inflate(R.layout.layout_mid_native_load, this);

        if (isInEditMode()) {
            return;
        }

        KP_Main_Full = (LinearLayout) findViewById(R.id.llline_full);
        Native_Full_Main = (LinearLayout) findViewById(R.id.llnative_full);
        ADSNativeDisplay.FullNativeDisplay(context, Native_Full_Main, KP_Main_Full, "small");
    }

    public ADSMidNative(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

}
