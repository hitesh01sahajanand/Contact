package com.example.contactmanager.Advertisement.Views;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.contactmanager.Advertisement.ADSNativeDisplay;
import com.example.contactmanager.R;


public class ADSBigNative extends LinearLayout {

    static LinearLayout SP_Full_Native;
    static LinearLayout Full_SP_Native;
    static Context context;

    public ADSBigNative(Context context) {
        super(context);
    }

    public ADSBigNative(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ADSBigNative.context = context;
        LayoutInflater li = LayoutInflater.from(context);
        LinearLayout ll = (LinearLayout) li.inflate(R.layout.layout_big_native_ad_load, this);

        if (isInEditMode()) {
            return;
        }

        SP_Full_Native = (LinearLayout) findViewById(R.id.llline_full);
        Full_SP_Native = (LinearLayout) findViewById(R.id.llnative_full);
        ADSNativeDisplay.FullNativeDisplay(context, Full_SP_Native, SP_Full_Native, "big");
    }

    public ADSBigNative(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

}
