package com.phonecall.dialcontacts.calldialer.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SharedPreferenceManager {

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(Constance.PREF_NAME, Context.MODE_PRIVATE)
    }

    fun putString(context: Context, key: String, value: String) {
        getPrefs(context).edit { putString(key, value) }
    }

    fun getString(context: Context, key: String, defaultValue: String = ""): String {
        return getPrefs(context).getString(key, defaultValue) ?: defaultValue
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit { putBoolean(key, value) }
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return getPrefs(context).getBoolean(key, defaultValue)
    }

    fun putInt(context: Context, key: String, value: Int) {
        getPrefs(context).edit { putInt(key, value) }
    }

    fun getInt(context: Context, key: String, defaultValue: Int = 0): Int {
        return getPrefs(context).getInt(key, defaultValue)
    }


    fun putLong(context: Context, key: String, value: Long) {
        getPrefs(context).edit { putLong(key, value) }
    }

    fun getLong(context: Context, key: String, defaultValue: Long = 0L): Long {
        return getPrefs(context).getLong(key, defaultValue)
    }


    fun remove(context: Context, key: String) {
        getPrefs(context).edit { remove(key) }
    }


    fun clear(context: Context) {
        getPrefs(context).edit { clear() }
    }
}