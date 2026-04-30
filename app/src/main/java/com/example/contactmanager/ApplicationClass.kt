package com.example.contactmanager

import android.app.Application
import android.telecom.Call
import com.example.contactmanager.utils.ThemeManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ApplicationClass : Application() {
    var appCall: Call? = null

    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyAppTheme(this)
    }

}