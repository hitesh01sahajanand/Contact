package com.example.contactmanager.services

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class SimpleCallScreeningService : CallScreeningService() {
    companion object {
//        var callerIdPopupmain: CallerIdPopup? = null

//        fun getCallerIdPopup(): CallerIdPopup? = callerIdPopupmain

        /* fun setCallerIdPopup(popup: CallerIdPopup?) {
             callerIdPopupmain = popup
         }*/
    }

    override fun onScreenCall(details: Call.Details) {

        val uri = details.handle?.toString() ?: ""

        val builder = CallResponse.Builder()

        startCallerScreen(uri, "", "0")

        // Allow call
        respondToCall(details, builder.build())
    }

    private fun startCallerScreen(str: String, str2: String, str3: String) {
        try {
            val looper = Looper.myLooper() ?: return

            Handler(looper).postDelayed({
                showCallerPopup(str, str2, str3)
            }, 1000L)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showCallerPopup(str: String, str2: String, str3: String) {

        // Overlay permission check
        if (Build.VERSION.SDK_INT >= 26 &&
            !Settings.canDrawOverlays(this)
        ) return

        // Call Screening role check (Android 10+)
        if (Build.VERSION.SDK_INT >= 29) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager == null ||
                !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            ) {
                return
            }
        }

        // User setting check
        /* val pref = PreferenceShareCalls(this)
         if (!pref.getBooleanPreference("show_call_confirmation", false)) {
             return
         }

         val number = str.replace("tel:", "").replace("%2B", "+")

         setCallerIdPopup(
             CallerIdPopup(this, number, str2, str3)
         )*/
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.e("CallScreening", "onTaskRemoved")
    }

}

