package com.example.contactmanager

import android.app.Application
import android.telecom.Call
import com.example.contactmanager.services.InCallMainService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ApplicationClass : Application() {

    var appCall: Call? = null
    var inCallService: InCallMainService? = null

    override fun onCreate() {
        super.onCreate()
//        registerDialerAccount()
    }

    /* private fun registerDialerAccount() {
         val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager

         val componentName = ComponentName(this, MyConnectionService::class.java)

         val phoneAccountHandle = PhoneAccountHandle(componentName, "MyDialer")

         val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "My Dialer")
             .setCapabilities(
                 PhoneAccount.CAPABILITY_CALL_PROVIDER or
                         PhoneAccount.CAPABILITY_CONNECTION_MANAGER
             )
             .setSupportedUriSchemes(listOf(PhoneAccount.SCHEME_TEL))
             .build()

         telecomManager.registerPhoneAccount(phoneAccount)


     }*/

}