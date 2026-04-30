package com.example.contactmanager.services

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.contactmanager.repository.BlockRepository
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Common.showDialerPopUp
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.SharedPreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SimpleCallScreeningService : CallScreeningService() {

    @Inject
    lateinit var blockRepository: BlockRepository

    override fun onScreenCall(details: Call.Details) {

        val uri = details.handle?.schemeSpecificPart ?: ""

        val builder = CallResponse.Builder()

        CoroutineScope(Dispatchers.IO).launch {
            val isBlocked = blockRepository.isBlocked(uri)

            if (isBlocked) {
                builder.setDisallowCall(true)
                builder.setRejectCall(true)
                builder.setSkipCallLog(false)
                builder.setSkipNotification(true)
            } else {
                startCallerScreen(uri)
            }

            respondToCall(details, builder.build())
        }
    }

    private fun startCallerScreen(str: String) {
        try {
            val looper = Looper.myLooper() ?: return

            Handler(looper).postDelayed({
                showCallerPopup(str)
            }, 1000L)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showCallerPopup(str: String) {

        // Overlay permission check
        if (Build.VERSION.SDK_INT >= 26 &&
            !Settings.canDrawOverlays(this)
        ) return

        // Call Screening role check (Android 10+)
        if (Build.VERSION.SDK_INT >= 29) {
            val roleManager = getSystemService(ROLE_SERVICE) as? RoleManager
            if (roleManager == null ||
                !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            ) {
                return
            }
        }

        if (!SharedPreferenceManager.getBoolean(context = this, Constance.CONFIRM_DIALOG)) {
            return
        }

        val number = str.replace("tel:", "").replace("%2B", "+")
        val contact = Common.getContactByNumber(this, number)
        val name = contact?.displayName ?: "Unknown Number"
        val imageUrl = contact?.userThumbnail ?: ""
        val time = Common.formatTime(System.currentTimeMillis())

        showDialerPopUp(this, name, number, time, imageUrl) {
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.e("CallScreening", "onTaskRemoved")
    }

}

