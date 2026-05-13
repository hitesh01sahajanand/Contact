package com.example.contactmanager.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.contactmanager.R

object ThemeManager {
    fun applyAppTheme(context: Context) {
        val theme = SharedPreferenceManager.getString(context, Constance.APP_THEME)
        if (theme.isEmpty()) {
            val initialTheme = context.getString(R.string.light_mode_app)
            SharedPreferenceManager.putString(context, Constance.APP_THEME, initialTheme)
            applyTheme(initialTheme, context)
        } else {
            applyTheme(theme, context)
        }
    }

    private fun applyTheme(theme: String, context: Context) {
        when (theme) {
            context.getString(R.string.light_mode_app) -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            context.getString(R.string.dark_mode) -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            context.getString(R.string.set_default) -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}
