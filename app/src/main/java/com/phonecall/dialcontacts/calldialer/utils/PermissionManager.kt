package com.phonecall.dialcontacts.calldialer.utils

import android.Manifest
import android.app.AppOpsManager
import android.app.Dialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.phonecall.dialcontacts.calldialer.databinding.PermissionDialogDesignBinding
import com.phonecall.dialcontacts.calldialer.utils.Common.hideDialogSystemUI

object PermissionManager {

    fun isDefaultDialer(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            val telecomManager =
                context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val defaultDialer = telecomManager.defaultDialerPackage
            context.packageName == defaultDialer
        }
    }

    fun getDefaultDialerIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val roleManager = context.getSystemService(RoleManager::class.java)

            if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else {
                null
            }

        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    context.packageName
                )
            }
        }
    }

    fun hasPermissions(context: Context): Boolean {
        return hasCallLogPermissions(context) && hasContactPermissions(context)
    }

    fun hasContactPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCallLogPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    fun isPermissionDialogShown(context: Context): Boolean {
        return SharedPreferenceManager.getBoolean(context, Constance.PERMISSION_DIALOG_SHOWN, false)
    }

    fun setPermissionDialogShown(context: Context) {
        SharedPreferenceManager.putBoolean(context, Constance.PERMISSION_DIALOG_SHOWN, true)
    }

    fun shouldShowInlinePermissionCard(context: Context, hasPermission: Boolean): Boolean {
        return !hasPermission && isPermissionDialogShown(context)
    }

    fun needsOverlayPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !hasOverlayPermission(context) &&
                !SharedPreferenceManager.getBoolean(context, Constance.OVERLAY_PERMISSION_SKIP)
    }

    @JvmStatic
    fun getOverlaySettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.fromParts("package", context.packageName, null)
        )
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun openPermissionDialog(context: Context, onClick: () -> Unit): Dialog {
        val dialog = Dialog(context)
        val alertBinding = PermissionDialogDesignBinding.inflate(LayoutInflater.from(context))

        dialog.setContentView(alertBinding.root)

        // Optional: transparent background (important)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.setCancelable(false)
        hideDialogSystemUI(dialog.window)

        val margin = (20 * context.resources.displayMetrics.density).toInt()

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        dialog.window?.setLayout(
            screenWidth - (margin * 3),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val hasContactPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val hasCallLogPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        val hasOverlayPermission =
            hasOverlayPermission(context) || SharedPreferenceManager.getBoolean(
                context,
                Constance.OVERLAY_PERMISSION_SKIP
            )

        val hasNotificationPermission =
            hasNotificationPermission(context) || SharedPreferenceManager.getBoolean(
                context,
                Constance.NOTIFICATION_PERMISSION_SKIP
            )

        alertBinding.llContact.visibility =
            if (hasContactPermission) View.GONE else View.VISIBLE
        alertBinding.llCallLog.visibility =
            if (hasCallLogPermission) View.GONE else View.VISIBLE
        alertBinding.llNotification.visibility =
            if (hasNotificationPermission) View.GONE else View.VISIBLE
        alertBinding.llDisplayOverOtherApps.visibility =
            if (hasOverlayPermission) View.GONE else View.VISIBLE

        alertBinding.cvContinue.setOnClickListener {
            dialog.dismiss()
            onClick()
        }

        dialog.show()
        return dialog
    }

    @JvmStatic
    fun hasOverlayPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

        // 1. Standard check
        if (Settings.canDrawOverlays(context)) return true

        // 2. AppOps fallback
        try {
            val appOps =
                context.applicationContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                android.os.Process.myUid(),
                context.packageName
            )
            if (mode == AppOpsManager.MODE_ALLOWED) return true

            // 3. Exception-based check for Android 8 (more aggressive)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                try {
                    appOps.checkOp(
                        AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                        android.os.Process.myUid(),
                        context.packageName
                    )
                    return true
                } catch (_: SecurityException) {
                    // Permission truly denied
                }
            }

            // 4. Numeric fallback via reflection
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                val method = appOps.javaClass.getMethod(
                    "checkOpNoThrow",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                val reflectionMode = method.invoke(
                    appOps,
                    24, // OP_SYSTEM_ALERT_WINDOW
                    android.os.Process.myUid(),
                    context.packageName
                ) as Int
                return reflectionMode == AppOpsManager.MODE_ALLOWED
            }
        } catch (_: Exception) {
        }

        return false
    }
}