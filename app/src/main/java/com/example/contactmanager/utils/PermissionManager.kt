package com.example.contactmanager.utils

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.example.contactmanager.databinding.AlertDialogDesignBinding
import com.example.contactmanager.databinding.PermissionDialogDesignBinding

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
        return listOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        ).all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun openPermissionDialog(context: Context, onClick: () -> Unit): Dialog {
        val dialog = Dialog(context)
        val alertBinding = PermissionDialogDesignBinding.inflate(LayoutInflater.from(context))

        dialog.setContentView(alertBinding.root)

        // Optional: transparent background (important)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.setCancelable(false)

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
        ) == PackageManager.PERMISSION_GRANTED

        val hasCallLogPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        val hasOverlayPermission = hasOverlayPermission(context)

        alertBinding.llContact.visibility =
            if (hasContactPermission) android.view.View.GONE else android.view.View.VISIBLE
        alertBinding.llCallLog.visibility =
            if (hasCallLogPermission) android.view.View.GONE else android.view.View.VISIBLE
        alertBinding.llDisplayOverOtherApps.visibility =
            if (hasOverlayPermission) android.view.View.GONE else android.view.View.VISIBLE

        alertBinding.cvContinue.setOnClickListener {
            dialog.dismiss()
            onClick()
        }

        dialog.show()
        return dialog
    }

    fun hasOverlayPermission(context: Context): Boolean {

        if (Settings.canDrawOverlays(context)) return true

        return try {
            val appOpsManager =
                context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                appOpsManager.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }
}