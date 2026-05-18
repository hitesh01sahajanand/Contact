package com.phonecall.dialcontacts.calldialer.Advertisement;

import static com.phonecall.dialcontacts.calldialer.Advertisement.MyApplication.isAppInForeground;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Message handling is moved to handleIntent.
    }

    @Override
    public void handleIntent(Intent intent) {
        Map<String, String> data = null;
        RemoteMessage remoteMessage = null;
        if (intent != null && intent.getExtras() != null) {
            remoteMessage = new RemoteMessage(intent.getExtras());
        }

        if (remoteMessage != null) {
            // 1. Check if App is in Foreground (Using ProcessLifecycleObserver)
            if (isAppInForeground()) {
                Log.d("FCM", "Skipping notification: App is in foreground.");
                return;
            }

            // 2. Check if Device is Unlocked
            android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && !km.isKeyguardLocked()) {
                Log.d("FCM", "Skipping notification: Device is unlocked.");
                return;
            }

            if (!ADSMainClass.isCallEndPerformanceAllowed(this, true)) {
                Log.d("FCM", "handleIntent: CallEnd performance NOT allowed for this scenario/country/day.");
                return;
            }

            // Preload Native Ad for the screen that will be shown
            String nativeId = ADSMainClass.getStringValue(ADSMainClass.CALL_END_Native);
            Log.d("FCM_AD", "Notification received. Waiting for Native Ad preload. ID: " + nativeId);

            final Map<String, String> finalData = data;
            final RemoteMessage finalRemoteMessage = remoteMessage;

            ADSNativeFullDisplay.preloadNativeAd(this, nativeId, new ADSNativeFullDisplay.PreloadCallback() {
                @Override
                public void onAdLoaded() {
                    Log.d("FCM_AD", "Ad loaded. Now launching CallEndActivity.");
//                    launchCallEnd(finalData, finalRemoteMessage);
                }

                @Override
                public void onAdFailed() {
                    Log.d("FCM_AD", "Ad failed to load. Launching CallEndActivity anyway.");
//                    launchCallEnd(finalData, finalRemoteMessage);
                }
            });
            return;
        }

        super.handleIntent(intent);
    }

   /* private void launchCallEnd(Map<String, String> data, RemoteMessage remoteMessage) {
        if (CallEndLaunchHelper.tryOpenFromFcmData(this, data)) {
            return;
        }

        if (remoteMessage.getNotification() != null) {
            CallEndLaunchHelper.openGenericForFirebaseNotification(
                    this,
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody());
            return;
        }

        CallEndLaunchHelper.openGenericForFirebaseNotification(
                this,
                getString(R.string.app_name),
                "");
    }*/

    @Override
    public void onNewToken(@NonNull String token) {
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
    }
}
