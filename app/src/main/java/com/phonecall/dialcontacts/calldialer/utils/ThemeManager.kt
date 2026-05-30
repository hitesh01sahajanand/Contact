package com.phonecall.dialcontacts.calldialer.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.phonecall.dialcontacts.calldialer.R

object ThemeManager {
    fun applyAppTheme(context: Context) {
        val theme = SharedPreferenceManager.getString(context, Constance.APP_THEME)
        if (theme.isEmpty()) {
            val initialTheme = "default"
            SharedPreferenceManager.putString(context, Constance.APP_THEME, initialTheme)
            applyTheme(initialTheme, context)
        } else {
            applyTheme(theme, context)
        }
    }

    private fun applyTheme(theme: String, context: Context) {
        when (theme) {
            "light", context.getString(R.string.light_mode_app) -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            "dark", context.getString(R.string.dark_mode) -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            "default", context.getString(R.string.set_default) -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }

            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}
