package com.phonecall.dialcontacts.calldialer.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.home.HomeActivity
import com.phonecall.dialcontacts.calldialer.adapters.AllAccountAdapter
import com.phonecall.dialcontacts.calldialer.adapters.AppsAdapter
import com.phonecall.dialcontacts.calldialer.adapters.QuickResponseAdapter
import com.phonecall.dialcontacts.calldialer.databinding.AddBlockNumbersDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.AlertDialogDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.AppThemeDialogBinding
import com.phonecall.dialcontacts.calldialer.databinding.CommonTypePopUpDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.ContactPopUpDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.DialerPopUpDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.EditQuickMessageBinding
import com.phonecall.dialcontacts.calldialer.databinding.ItemVideoCallBinding
import com.phonecall.dialcontacts.calldialer.databinding.PopUpMenuDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.RemindMeDialogDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.SaveTagDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.SendMessageDialogDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.SimSelectDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.SimSelectionDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.VideoCallDialogBinding
import com.phonecall.dialcontacts.calldialer.models.AccountModel
import com.phonecall.dialcontacts.calldialer.models.ContactModel
import com.phonecall.dialcontacts.calldialer.models.QuickResponseModel
import com.phonecall.dialcontacts.calldialer.receivers.ReminderReceiver
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSBannerSmall
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSNativeDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Common {

    val profileColors = listOf(
        R.color.color_1,
        R.color.color_2,
        R.color.color_3,
        R.color.color_4,
        R.color.color_5,
        R.color.color_6
    )

    fun hideKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun isNumberBlocked(context: Context, number: String?): Boolean {
        if (number.isNullOrEmpty()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                if (PermissionManager.isDefaultDialer(context)) {
                    return android.provider.BlockedNumberContract.isBlocked(context, number)
                }
            } catch (e: Exception) {
                Log.e("Common", "isNumberBlocked: ${e.message}")
            }
        }
        return false
    }

    fun openHomeActivity(activity: Activity, tab: String? = null) {
        val intent = Intent(activity, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            tab?.let { putExtra("open_tab", it) }
        }
        activity.startActivity(intent)
        activity.finishAndRemoveTask()
    }

    fun formatHeaderDate(date: Date?): String {
        /*val sdf = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
        return sdf.format(Date(timestamp))*/
        if (date == null) return ""

        val sdf = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
        return sdf.format(date)
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun isNumberSaved(context: Context, number: String): Boolean {
        if (number.isEmpty()) return false

        // Layer 1: Standard PhoneLookup (Fastest & Handles international formatting)
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        try {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.count > 0) return true
            }
        } catch (e: Exception) {
            Log.e("Common", "PhoneLookup failed: ${e.message}")
        }

        // Layer 2: Cleaned Digit Search (Handles mismatched formatting in DB)
        val cleanNumber = number.replace(Regex("\\D"), "")
        if (cleanNumber.length >= 7) {
            val last7 = cleanNumber.takeLast(7)
            val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            // Match numbers ending with the clean number (helps with varied formatting)
            val selectionArgs =
                arrayOf("%${last7.first()}%${last7.substring(1).chunked(1).joinToString("%")}%")

            try {
                context.contentResolver.query(
                    phoneUri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone._ID),
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.count > 0) return true
                }
            } catch (e: Exception) {
                Log.e("Common", "Cleaned search failed: ${e.message}")
            }
        }

        // Layer 3: High-Accuracy Comparison (Final fallback for tricky cases)
        // Note: Iterating contacts is a bit slower but extremely accurate
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )?.use { cursor ->
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val storedNumber = cursor.getString(numberIdx)
                    if (PhoneNumberUtils.compare(number, storedNumber)) return true
                }
            }
        } catch (e: Exception) {
            Log.e("Common", "Deep lookup failed: ${e.message}")
        }

        return false
    }


    fun hideSystemUI(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window?.decorView?.post {

                activity.window?.insetsController?.let { controller ->

                    controller.hide(WindowInsets.Type.navigationBars())

                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
    }

    fun hideDialogSystemUI(window: Window?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window?.decorView?.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }


    fun generateAvatar(input: String): Bitmap {
        val text = input.trim()

        fun Char.isValidChar(): Boolean {
            return this != '+'
        }

        val initials = when {

            // 🔥 UPDATED: digits case (ignore '+')
            text.any { it.isDigit() } && text.filter { it.isDigit() }.isNotEmpty() -> {
                val digitsOnly = text.filter { it.isDigit() }

                when {
                    digitsOnly.length >= 2 -> digitsOnly.substring(0, 2)
                    digitsOnly.length == 1 -> digitsOnly + "0"
                    else -> "00"
                }
            }

            !text.contains(" ") -> {
                text.firstOrNull { it.isLetter() && it.isValidChar() }?.uppercase()
                    ?: text.firstOrNull { it.isValidChar() }?.toString()
                    ?: ""
            }

            else -> {
                val parts = text.split(" ")
                    .filter { it.isNotEmpty() }

                val firstPart = parts.getOrNull(0) ?: ""
                val secondPart = parts.getOrNull(1) ?: ""

                val firstChar = firstPart.firstOrNull { it.isValidChar() }
                    ?.toString()
                    ?.uppercase()
                    ?: ""

                val secondChar = secondPart.firstOrNull { it.isLetter() && it.isValidChar() }
                    ?.uppercase()
                    ?: secondPart.firstOrNull { it.isValidChar() }
                        ?.toString()
                        ?.uppercase()
                    ?: ""

                "$firstChar$secondChar"
            }
        }

        val size = 200
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
            color = getColorFromKey(text)
        }

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = size / 2.5f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }

        val x = size / 2f
        val y = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2

        canvas.drawText(initials, x, y, textPaint)

        return bitmap
    }

    fun getColorFromKey(key: String): Int {

        var hash = key.hashCode()

        hash = hash xor (hash shl 13)
        hash = hash xor (hash shr 17)
        hash = hash xor (hash shl 5)

        val hue = (hash % 360).let { if (it < 0) it + 360 else it }.toFloat()

        val saturation = 0.6f
        val value = 0.8f

        return Color.HSVToColor(floatArrayOf(hue, saturation, value))
    }

    fun getPhotoBytes(uri: Uri, context: Context): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            Log.e("TAG", "isAppInstalled: ${e.message}")
            null
        }
    }

    fun getCallType(context: Context, i: Int): String {
        return when (i) {
            1 -> context.getString(R.string.incoming)
            2 -> context.getString(R.string.outgoing)
            3 -> context.getString(R.string.missed)
            5 -> context.getString(R.string.rejected)
            else -> context.getString(R.string.unknown)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getCallImageType(i: Int, context: Context): Drawable? {
        return when (i) {
            1 -> context.getDrawable(R.drawable.ic_incoming_call)
            2 -> context.getDrawable(R.drawable.ic_outgoing_call)
            3 -> context.getDrawable(R.drawable.ic_miss_call)
            5 -> context.getDrawable(R.drawable.ic_incoming_call)

            else -> context.getDrawable(R.drawable.ic_all_call)
        }
    }

    fun extractTimeFromDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat(
                "EEE MMM dd HH:mm:ss z yyyy",
                Locale.ENGLISH
            )

            val outputFormat = SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
            )

            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            Log.e("TAG", "isAppInstalled: ${e.message}")
            ""
        }
    }

    fun compareNumbers(n1: String?, n2: String?): Boolean {
        if (n1.isNullOrEmpty() || n2.isNullOrEmpty()) return false
        if (n1 == n2 || PhoneNumberUtils.compare(n1, n2)) return true

        val s1 = n1.replace(Regex("\\D"), "")
        val s2 = n2.replace(Regex("\\D"), "")

        if (s1.isEmpty() || s2.isEmpty()) return false
        if (s1 == s2) return true

        // Match last 10 digits (common for country code vs 0 prefix differences)
        if (s1.length >= 10 && s2.length >= 10) {
            if (s1.takeLast(10) == s2.takeLast(10)) return true
        }

        // Final fallback for shorter numbers or partial matches
        return s1.length >= 7 && s2.length >= 7 &&
                (s1.endsWith(s2) || s2.endsWith(s1))
    }

    fun getHandleForSubId(subId: Int, context: Context): PhoneAccountHandle? {

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null // or handle properly
        }

        val telecomManager =
            context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        return try {
            val phoneAccounts = telecomManager.callCapablePhoneAccounts
            for (account in phoneAccounts) {
                if (account.id.contains(subId.toString())) {
                    return account
                }
            }
            null
        } catch (e: SecurityException) {
            e.printStackTrace()
            null
        }
    }

    fun shareContact(context: Context, phoneNumber: String?) {
        if (phoneNumber.isNullOrEmpty()) return

        // Run all I/O off the main thread to avoid ANR
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resolver = context.contentResolver
                val lookupUri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber)
                )

                var lookupKey: String? = null
                var displayName: String? = null

                resolver.query(
                    lookupUri,
                    arrayOf(
                        ContactsContract.PhoneLookup.LOOKUP_KEY,
                        ContactsContract.PhoneLookup.DISPLAY_NAME
                    ),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        lookupKey = cursor.getString(0)
                        displayName = cursor.getString(1)
                    }
                }

                if (lookupKey == null) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.please_save_contact_first),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    return@launch
                }

                val vCardUri = Uri.withAppendedPath(
                    ContactsContract.Contacts.CONTENT_VCARD_URI,
                    lookupKey
                )

                // Create temporary file in cache
                val fileName = "Contact_${System.currentTimeMillis()}.vcf"
                val cacheFile = File(context.cacheDir, fileName)

                var success = false
                try {
                    resolver.openAssetFileDescriptor(vCardUri, "r")?.use { fd ->
                        fd.createInputStream().use { input ->
                            FileOutputStream(cacheFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    success = cacheFile.exists() && cacheFile.length() > 0
                } catch (e: Exception) {
                    Log.e("TAG", "Error reading vCard: ${e.message}")
                }

                if (!success) {
                    // Fallback: Generate a simple vCard manually if system one fails or is empty
                    val vcardContent = "BEGIN:VCARD\n" +
                            "VERSION:3.0\n" +
                            "N:;${displayName ?: ""};;;\n" +
                            "FN:${displayName ?: ""}\n" +
                            "TEL;TYPE=CELL:$phoneNumber\n" +
                            "END:VCARD"
                    cacheFile.writeText(vcardContent)
                }

                val shareUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    cacheFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/x-vcard"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                // Grant read permission to every app that can handle this intent,
                // including the system process that generates the share-sheet preview.
                val chooser = Intent.createChooser(intent, "Share Contact").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val resolvedActivities =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.queryIntentActivities(
                            intent,
                            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.queryIntentActivities(
                            intent,
                            PackageManager.MATCH_DEFAULT_ONLY
                        )
                    }
                for (resolveInfo in resolvedActivities) {
                    val pkg = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(
                        pkg,
                        shareUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                // Switch to main thread only for the UI call
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    context.startActivity(chooser)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.unable_to_share_contact), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun popUpMenu(
        context: Context,
        anchorView: View,
        option1: String,
        option2: String,
        option1Click: () -> Unit,
        option2Click: () -> Unit
    ) {

        val popUpBinding = PopUpMenuDesignBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        )

        val popupWindow = PopupWindow(
            popUpBinding.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 10f
        /*popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true*/

        popUpBinding.tvOption1.text = option1
        popUpBinding.tvOption2.text = option2

        popUpBinding.root.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )

        val popupWidth = popUpBinding.root.measuredWidth
        val margin = (2 * context.resources.displayMetrics.density).toInt()
        val xOffset = anchorView.width - popupWidth - margin

        popupWindow.showAsDropDown(anchorView, xOffset, 20)

        popUpBinding.tvOption1.setOnClickListener {
            option1Click()
            popupWindow.dismiss()
        }

        popUpBinding.tvOption2.setOnClickListener {
            option2Click()
            popupWindow.dismiss()
        }
    }


    fun typePopUp(
        context: Context,
        anchorView: View,
        title: String,
        typeArray: Array<String>,
        selectedType: String,
        onItemClick: (String) -> Unit
    ) {

        val popUpBinding = CommonTypePopUpDesignBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        )

        val popupWindow = PopupWindow(
            popUpBinding.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 10f

        popUpBinding.tvType.text = title

        val typeface = ResourcesCompat.getFont(context, R.font.fig_tree_medium)

        val selectedIndex = typeArray.indexOf(selectedType)

        typeArray.forEachIndexed { index, item ->

            val radioButton = RadioButton(context).apply {

                id = View.generateViewId()
                text = item
                textSize = 15f

                typeface?.let {
                    this.typeface = it
                }

                setTextColor(
                    ContextCompat.getColor(context, R.color.black_color)
                )

                buttonTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.main_color)
                )

                setPadding(0, 12, 0, 12)

                setOnClickListener {
                    onItemClick(item)
                    popupWindow.dismiss()
                }
            }

            if (selectedIndex != -1) {
                if (index == selectedIndex) {
                    radioButton.isChecked = true
                }
            } else if (index == 0) {
                radioButton.isChecked = true
            }

            popUpBinding.rgTypeItem.addView(radioButton)
        }

        // Measure popup size
        popUpBinding.root.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )

        val popupWidth = popUpBinding.root.measuredWidth
        val popupHeight = popUpBinding.root.measuredHeight

        val margin = (2 * context.resources.displayMetrics.density).toInt()
        val xOffset = anchorView.width - popupWidth - margin

        // Screen location
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)

        val anchorY = location[1]

        // Screen height
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        // Remaining space below anchor
        val spaceBelow = screenHeight - (anchorY + anchorView.height)

        if (spaceBelow < popupHeight) {

            // Show ABOVE
            popupWindow.showAsDropDown(
                anchorView,
                xOffset,
                -(anchorView.height + popupHeight)
            )

        } else {
            // Show BELOW
            popupWindow.showAsDropDown(
                anchorView,
                xOffset,
                10
            )
        }
    }


    fun contactPopUpMenu(
        context: Context,
        anchorView: View,
        accountsList: List<AccountModel>,
        onItemClick: (String) -> Unit
    ) {
        val popUpBinding = ContactPopUpDesignBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        )

        val popupWindow = PopupWindow(
            popUpBinding.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 10f
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        popUpBinding.root.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )

        popupWindow.showAsDropDown(anchorView, 0, 20)

        val adapter = AllAccountAdapter(isCountVisible = true, onClick = { model ->
            onItemClick(model.email)
            popupWindow.dismiss()
        })

        popUpBinding.rvAccounts.adapter = adapter
        popUpBinding.rvAccounts.layoutManager = LinearLayoutManager(context)
        adapter.addAll(accountsList)
    }

    fun showQuickMessageDialog(
        context: Context,
        messages: List<QuickResponseModel>,
        onItemClick: (String) -> Unit
    ) {

        val dialog = Dialog(context)
        val bindingSendMessage =
            SendMessageDialogDesignBinding.inflate(LayoutInflater.from(context))

        dialog.setContentView(bindingSendMessage.root)

        // Optional: transparent background (important)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val margin = (10 * context.resources.displayMetrics.density).toInt()

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        dialog.window?.setLayout(
            screenWidth - (margin * 3),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val adapter = QuickResponseAdapter(onItemClick = { model, action ->
            if (action == Constance.DATA_FETCH) {
                onItemClick(model.message)
                dialog.dismiss()
            }
        })
        bindingSendMessage.rvQuickResponse.adapter = adapter
        bindingSendMessage.rvQuickResponse.layoutManager = LinearLayoutManager(context)
        adapter.updateData(messages)

        bindingSendMessage.cvAddMessage.setOnClickListener {
            bindingSendMessage.llSendMessage.isVisible = !bindingSendMessage.llSendMessage.isVisible
        }

        bindingSendMessage.cvSend.setOnClickListener {
            onItemClick(bindingSendMessage.edtSendMassage.text.toString())
            dialog.dismiss()
        }

        dialog.show()
    }

    fun editQuickMessageDialog(
        context: Context,
        onItemClick: (String) -> Unit
    ) {

        val dialog = Dialog(context)
        val bindingSendMessage = EditQuickMessageBinding.inflate(LayoutInflater.from(context))

        dialog.setContentView(bindingSendMessage.root)
        dialog.setCancelable(false)

        // Optional: transparent background (important)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val margin = (10 * context.resources.displayMetrics.density).toInt()

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        dialog.window?.setLayout(
            screenWidth - (margin * 3),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )


        bindingSendMessage.ivClose.setOnClickListener {
            dialog.dismiss()
        }

        bindingSendMessage.cvOkay.setOnClickListener {
            onItemClick(bindingSendMessage.edtMessage.text.toString())
            dialog.dismiss()
        }


        dialog.show()
    }

    fun showDialerPopUp(
        context: Context,
        contactName: String,
        contactNumber: String,
        callCurrentTime: String,
        imageUrl: String,
        onItemClick: (String) -> Unit
    ) {

        val dialog = Dialog(context)
        val bindingDialerPopUp =
            DialerPopUpDesignBinding.inflate(LayoutInflater.from(context))

        dialog.setContentView(bindingDialerPopUp.root)
        dialog.setCancelable(false)

        // Optional: transparent background (important)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        if (context !is Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                @Suppress("DEPRECATION")
                dialog.window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            }
        }

        val margin = (10 * context.resources.displayMetrics.density).toInt()

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        dialog.window?.setLayout(
            screenWidth - (margin * 3),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        bindingDialerPopUp.contactName.text = contactName
        bindingDialerPopUp.contactNumber.text = contactNumber
        bindingDialerPopUp.callCurrentTime.text = callCurrentTime

        if (imageUrl.isNotEmpty()) {
            Glide.with(context).load(imageUrl).into(bindingDialerPopUp.profileImage)
        } else {
            bindingDialerPopUp.profileImage.setImageBitmap(generateAvatar(contactName.ifEmpty { contactNumber }))
        }

        bindingDialerPopUp.actionClose.setOnClickListener {
            onItemClick("")
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showAppThemeBottomSheet(
        context: Context,
        onItemClick: (String) -> Unit
    ) {
        val dialog = Dialog(context)
        val bindingAppTheme =
            AppThemeDialogBinding.inflate(LayoutInflater.from(context))

        val margin = (10 * context.resources.displayMetrics.density).toInt()

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        dialog.window?.setLayout(
            screenWidth - (margin * 3),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.setContentView(bindingAppTheme.root)
        dialog.setCancelable(false)

        bindingAppTheme.ivClose.setOnClickListener {
            dialog.dismiss()
        }

        bindingAppTheme.llLightMode.setOnClickListener {
            onItemClick(bindingAppTheme.tvLightMode.text.toString())
            dialog.dismiss()
        }

        bindingAppTheme.llDarkMode.setOnClickListener {
            onItemClick(bindingAppTheme.tvDarkMode.text.toString())
            dialog.dismiss()
        }

        bindingAppTheme.llDefaultMode.setOnClickListener {
            onItemClick(bindingAppTheme.tvDefaultMode.text.toString())
            dialog.dismiss()
        }

        dialog.show()
    }


    fun selectSimDialog(
        context: Context,
        onItemClick: (Int) -> Unit
    ) {
        val dialog = Dialog(context)
        val binding = SimSelectDesignBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val margin = (20 * context.resources.displayMetrics.density).toInt()
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        dialog.window?.setLayout(
            screenWidth - (margin * 3),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        binding.tvOkay.text = context.getString(R.string.set)

        val subscriptionManager =
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        val activeSimList = if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            subscriptionManager?.activeSubscriptionInfoList
        } else null

        if (activeSimList.isNullOrEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.no_sim_cards_found),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        binding.radioGroup.removeAllViews()

        val params = RadioGroup.LayoutParams(
            RadioGroup.LayoutParams.MATCH_PARENT,
            RadioGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 10, 0, 10)

        val typeface = ResourcesCompat.getFont(context, R.font.fig_tree_medium)

        // Always ask option
        val rbAlwaysAsk = RadioButton(context).apply {
            text = context.getString(R.string.always_ask)
            id = View.generateViewId()
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.black_color))
            buttonTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.main_color)
            )
            this.typeface = typeface
        }
        binding.radioGroup.addView(rbAlwaysAsk, params)

        activeSimList.forEachIndexed { index, info ->
            val rb = RadioButton(context).apply {
                text = "SIM ${index + 1} (${info.carrierName})"
                id = View.generateViewId()
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.black_color))
                buttonTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.main_color)
                )
                this.typeface = typeface
            }
            binding.radioGroup.addView(rb, params)
        }

        // Set current selection
        val simPref = SharedPreferenceManager.getInt(context, Constance.SIM_PREFERENCE, -1)
        if (simPref == -1) {
            rbAlwaysAsk.isChecked = true
        } else {
            val index = activeSimList.indexOfFirst { it.subscriptionId == simPref }
            if (index != -1) {
                val radioButton = binding.radioGroup.getChildAt(index + 1) as? RadioButton
                radioButton?.isChecked = true
            } else {
                rbAlwaysAsk.isChecked = true
            }
        }

        binding.tvCancel.setOnClickListener { dialog.dismiss() }
        binding.tvOkay.setOnClickListener {
            val checkedId = binding.radioGroup.checkedRadioButtonId
            val checkedView = binding.radioGroup.findViewById<View>(checkedId)
            val checkedIndex = binding.radioGroup.indexOfChild(checkedView)

            val resultSubId = if (checkedIndex == 0) -1 else {
                activeSimList[checkedIndex - 1].subscriptionId
            }
            onItemClick(resultSubId)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun getContactByNumber(context: Context, number: String): ContactModel? {
        val resolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI,
            ContactsContract.PhoneLookup._ID
        )

        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                val photoUri =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
                val id =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
                return ContactModel().apply {
                    this.displayName = name
                    this.userThumbnail = photoUri
                    this.contactId = id
                    this.number = number
                }
            }
        }
        return null
    }

    fun getNumbersForContactByNumber(context: Context, number: String): List<Pair<String, String>> {
        if (!PermissionManager.hasContactPermissions(context)) return emptyList()
        val contact = getContactByNumber(context, number) ?: return emptyList()
        val contactId = contact.contactId ?: return emptyList()

        val numbers = mutableListOf<Pair<String, String>>()
        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId)

        try {
            resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

                while (cursor.moveToNext()) {
                    val num = cursor.getString(numIdx) ?: continue
                    val type = cursor.getInt(typeIdx)
                    val label = cursor.getString(labelIdx)
                    val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        context.resources,
                        type,
                        label
                    ).toString()
                    numbers.add(num to typeLabel)
                }
            }
        } catch (e: Exception) {
            Log.e("Common", "Error fetching numbers: ${e.message}")
        }

        // De-duplicate by cleaned number
        val uniqueNumbers = mutableListOf<Pair<String, String>>()
        val seenClean = mutableSetOf<String>()
        for (pair in numbers) {
            val clean = cleanNumber(pair.first)
            if (clean.isNotEmpty() && seenClean.add(clean)) {
                uniqueNumbers.add(pair)
            }
        }
        return uniqueNumbers
    }

    fun showNumberSelectionDialog(
        context: Context,
        contactName: String,
        numbers: List<Pair<String, String>>,
        onNumberSelected: (String) -> Unit
    ) {
        val dialog = Dialog(context)
        val binding = SimSelectionDesignBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val margin = (10 * context.resources.displayMetrics.density).toInt()
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        dialog.window?.setLayout(
            screenWidth - (margin * 3),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)

        binding.tvTitle.text = context.getString(R.string.contact_info)
        binding.viewSep.isVisible = true

        val typeface = ResourcesCompat.getFont(context, R.font.fig_tree_medium)
        val semiBoldTypeface = ResourcesCompat.getFont(context, R.font.fig_tree_semi_bold)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 5, 0, 5)

        numbers.forEach { (number, type) ->
            val llItem = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                /* setPadding(
                     (15 * context.resources.displayMetrics.density).toInt(),
                     (12 * context.resources.displayMetrics.density).toInt(),
                     (15 * context.resources.displayMetrics.density).toInt(),
                     (12 * context.resources.displayMetrics.density).toInt()
                 )*/
                background = ContextCompat.getDrawable(context, R.drawable.ripple_effect_bg)
                setOnClickListener {
                    onNumberSelected(number)
                    dialog.dismiss()
                }
            }

            // Icon with circle background
            val iconSize = (60 * context.resources.displayMetrics.density).toInt()

            val ivIcon = ImageView(context).apply {
                setImageResource(R.drawable.ic_add_contact)
                val padding = (11 * context.resources.displayMetrics.density).toInt()
                setPadding(padding, padding, padding, padding)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            }

            // Vertical layout for text
            val llText = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = (15 * context.resources.displayMetrics.density).toInt()
                }
            }

            val tvNumberText = TextView(context).apply {
                text = number
                textSize = 17f
                setTextColor(ContextCompat.getColor(context, R.color.black_color))
                this.typeface = semiBoldTypeface ?: typeface
            }

            val tvTypeText = TextView(context).apply {
                text = type
                textSize = 14f
                setTextColor(context.getColor(R.color.grey_color))
                this.typeface = typeface
            }

            llText.addView(tvNumberText)
            llText.addView(tvTypeText)

            llItem.addView(ivIcon)
            llItem.addView(llText)
            binding.llSimContainer.addView(llItem, params)
        }

        dialog.show()
    }


    fun showRemindMeDialog(
        context: Context,
        contactName: String,
        contactNumber: String,
        onReminderSet: () -> Unit = {}
    ) {
        val dialog = Dialog(context)
        val binding = RemindMeDialogDesignBinding.inflate(LayoutInflater.from(context))

        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val margin = (20 * context.resources.displayMetrics.density).toInt()
        val screenWidth = context.resources.displayMetrics.widthPixels

        dialog.window?.setLayout(
            screenWidth - margin,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Helper to schedule reminder and dismiss
        fun scheduleAndDismiss(delayMinutes: Int) {
            setReminderAfterDelay(context, delayMinutes, contactName, contactNumber)
            onReminderSet()
            dialog.dismiss()
        }

        binding.cvRemind5min.setOnClickListener { scheduleAndDismiss(5) }
        binding.cvRemind10min.setOnClickListener { scheduleAndDismiss(10) }
        binding.cvRemind20min.setOnClickListener { scheduleAndDismiss(20) }
        binding.cvRemind30min.setOnClickListener { scheduleAndDismiss(30) }
        binding.cvRemind40min.setOnClickListener { scheduleAndDismiss(40) }
        binding.cvRemind50min.setOnClickListener { scheduleAndDismiss(50) }
        binding.cvRemind1hour.setOnClickListener { scheduleAndDismiss(60) }

        binding.cvCancelReminder.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    fun setReminderAfterDelay(
        context: Context,
        delayMinutes: Int,
        contactName: String,
        contactNumber: String
    ) {
        val triggerAtMillis = System.currentTimeMillis() + delayMinutes * 60 * 1000L

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("contactName", contactName)
            putExtra("contactNumber", contactNumber)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intentSetting = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intentSetting.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intentSetting)
                Toast.makeText(
                    context,
                    context.getString(R.string.please_allow_exact_alarm_permission),
                    Toast.LENGTH_SHORT
                )
                    .show()
                return
            }
        }

        // ✅ Schedule alarm relative to now
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        Toast.makeText(
            context,
            "Reminder set for ${if (delayMinutes < 60) "$delayMinutes min" else "1 hour"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    @Deprecated(
        "Use setReminderAfterDelay instead",
        ReplaceWith("setReminderAfterDelay(context, delayMinutes, contactName, contactNumber)")
    )
    fun setReminder(
        context: Context,
        hour: Int,
        minute: Int,
        contactName: String,
        contactNumber: String
    ) {
        // Kept for backward compatibility — not used by the new dialog
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        val now = Calendar.getInstance()
        if (calendar.before(now)) {
            Toast.makeText(
                context,
                context.getString(R.string.please_select_future_time), Toast.LENGTH_SHORT
            ).show()
            return
        }
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("contactName", contactName)
            putExtra("contactNumber", contactNumber)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intentSetting = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intentSetting.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intentSetting)
                Toast.makeText(
                    context,
                    context.getString(R.string.please_allow_exact_alarm_permission),
                    Toast.LENGTH_SHORT
                )
                    .show()
                return
            }
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun cleanNumber(number: String?): String {
        if (number == null) return ""
        val sb = StringBuilder()
        for (char in number) {
            if (char.isDigit() || char == '+') {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    fun actionCall(number: String?, context: Context, showSelection: Boolean = true) {
        if (number.isNullOrEmpty()) return

        if (showSelection && PermissionManager.hasContactPermissions(context)) {
            val allNumbers = getNumbersForContactByNumber(context, number)
            if (allNumbers.size > 1) {
                val contact = getContactByNumber(context, number)
                showNumberSelectionDialog(
                    context,
                    contact?.displayName ?: number,
                    allNumbers
                ) { selectedNumber ->
                    actionCall(selectedNumber, context, false)
                }
                return
            }
        }

        if (NewCallManager.isNumberActive(number)) {
            Toast.makeText(
                context,
                context.getString(R.string.number_already_in_a_call),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // If app is not default dialer, then direct call (Call Anyway)
        if (!PermissionManager.isDefaultDialer(context)) {
            try {
                val intent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("TAG", "actionCall: ${e.message}")
                val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
            return
        }

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        val telecomManager =
            context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return

        val callUri = Uri.fromParts("tel", number, null)
        val callBundle = Bundle().apply {
            putBoolean("android.telecom.extra.START_CALL_WITH_SPEAKERPHONE", false)
        }

        val subscriptionManager =
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        val activeSimList = subscriptionManager?.activeSubscriptionInfoList
        val simPref = SharedPreferenceManager.getInt(context, Constance.SIM_PREFERENCE, -1)

        if (!activeSimList.isNullOrEmpty() && activeSimList.size > 1) {
            if (simPref != -1) {
                val preferredSim = activeSimList.find { it.subscriptionId == simPref }
                if (preferredSim != null) {
                    val callBundlePref = Bundle().apply {
                        putParcelable(
                            TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                            getHandleForSubId(preferredSim.subscriptionId, context)
                        )
                    }
                    telecomManager.placeCall(callUri, callBundlePref)
                    return
                }
            }

            val simDialog = Dialog(context)
            val binding = SimSelectionDesignBinding.inflate(LayoutInflater.from(context))
            simDialog.setContentView(binding.root)
            simDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            val margin = (20 * context.resources.displayMetrics.density).toInt()
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            simDialog.window?.setLayout(
                screenWidth - (margin * 3),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            simDialog.setCancelable(true)

            val typeface = ResourcesCompat.getFont(context, R.font.fig_tree_medium)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 10, 0, 10)

            activeSimList.forEachIndexed { index, info ->
                val tvSim = TextView(context).apply {
                    text = "SIM ${index + 1} (${info.carrierName})"
                    id = View.generateViewId()
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(context, R.color.black_color))
                    this.typeface = typeface
                    setPadding(
                        (10 * context.resources.displayMetrics.density).toInt(),
                        (10 * context.resources.displayMetrics.density).toInt(),
                        (10 * context.resources.displayMetrics.density).toInt(),
                        (10 * context.resources.displayMetrics.density).toInt()
                    )
                    background = ContextCompat.getDrawable(context, R.drawable.ripple_effect_bg)

                    setOnClickListener {
                        val selectedSim = activeSimList[index]
                        val callBundle2 = Bundle().apply {
                            putParcelable(
                                TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                                getHandleForSubId(selectedSim.subscriptionId, context)
                            )
                        }
                        val callUri2 = Uri.fromParts("tel", number, null)
                        telecomManager.placeCall(callUri2, callBundle2)
                        simDialog.dismiss()
                    }
                }
                binding.llSimContainer.addView(tvSim, params)
            }

            simDialog.show()

        } else {
            // Single SIM or SIM preference not set/matched
            if (!activeSimList.isNullOrEmpty() && simPref != -1) {
                val preferredSim = activeSimList.find { it.subscriptionId == simPref }
                if (preferredSim != null) {
                    val callBundlePref = Bundle().apply {
                        putParcelable(
                            TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                            getHandleForSubId(preferredSim.subscriptionId, context)
                        )
                    }
                    telecomManager.placeCall(callUri, callBundlePref)
                    return
                }
            }
            telecomManager.placeCall(callUri, callBundle)
        }
    }


    fun ensureDefaultDialer(context: Context, onProceed: () -> Unit) {
        if (PermissionManager.isDefaultDialer(context)) {
            onProceed()
        } else {
            if (context is Activity) {
                val image = R.drawable.ic_default_app

                alertDialog(
                    context = context,
                    title = context.getString(R.string.set_as_default_dialer),
                    description = context.getString(R.string.to_manage_blocked_numbers),
                    btnOkay = context.getString(R.string.set_as_default),
                    image = image,
                    isImageVisible = true,
                    onItemClick = {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val roleManager =
                                    context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                                val intent =
                                    roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                                context.startActivityForResult(intent, 123)
                            } else {
                                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                                intent.putExtra(
                                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                                    context.packageName
                                )
                                context.startActivity(intent)
                            }
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.unable_to_open_default_app_settings),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.please_set_this_app_as_default_in_settings),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun isAppInstalled(context: Context?, packageName: String?): Boolean {
        if (context == null || packageName.isNullOrBlank()) return false

        return try {
            val pm = context.packageManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }

            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("TAG", "isAppInstalled: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("TAG", "isAppInstalled: ${e.message}")
            false
        }
    }

    fun showVideoAppChooser(
        activity: Activity,
        number: String,
        onSelection: (() -> Unit)? = null
    ) {

        val appList = Constance.videoCallList

        val dialog = BottomSheetDialog(activity, R.style.TransparentDialog)

        val view = VideoCallDialogBinding.inflate(
            activity.layoutInflater,
            null,
            false
        )

        dialog.setContentView(view.root)

        view.rvApps.isVisible = false
        view.loutVideoCall.isVisible = true
        view.loutVideoCall.removeAllViews()

        appList.forEach { pkg ->

            val isInstalled = isAppInstalled(activity, pkg)

            val itemBinding = ItemVideoCallBinding.inflate(
                activity.layoutInflater,
                view.loutVideoCall,
                false
            )

            try {

                if (isInstalled) {

                    val info = activity.packageManager.getPackageInfo(pkg, 0)

                    itemBinding.ivImage.setImageDrawable(
                        info.applicationInfo?.loadIcon(activity.packageManager)
                    )

                    itemBinding.tvTitle.text =
                        info.applicationInfo?.loadLabel(activity.packageManager)

                } else {

                    itemBinding.ivImage.setImageResource(R.drawable.ic_video_call)

                    itemBinding.tvTitle.text = when (pkg) {

                        Constance.WHATSAPP ->
                            "Install WhatsApp"

                        Constance.WHATSAPP_BUSINESS ->
                            "Install WA Business"

                        Constance.DUO ->
                            "Install Meet"

                        else ->
                            "Install App"
                    }
                }

            } catch (_: Exception) {

                itemBinding.ivImage.setImageResource(R.drawable.ic_video_call)

                itemBinding.tvTitle.text =
                    if (pkg.contains("whatsapp"))
                        "WhatsApp"
                    else
                        "Meet"
            }

            itemBinding.root.setOnClickListener {

                dialog.dismiss()

                if (isInstalled) {

                    onSelection?.invoke()

                    val formattedNumber = number
                        .replace("+", "")
                        .replace(" ", "")
                        .replace("-", "")

                    when (pkg) {

                        Constance.DUO -> {

                            startDuoCall(activity, formattedNumber)
                        }

                        Constance.WHATSAPP,
                        Constance.WHATSAPP_BUSINESS -> {

                            try {

                                val intent = Intent(Intent.ACTION_VIEW).apply {

                                    data =
                                        "https://wa.me/$formattedNumber?video=1".toUri()

                                    setPackage(pkg)

                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }

                                activity.startActivity(intent)

                            } catch (_: Exception) {

                                Toast.makeText(
                                    activity,
                                    activity.getString(R.string.unable_to_start_whatsapp_video_call),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        else -> {

                            try {

                                activity.packageManager
                                    .getLaunchIntentForPackage(pkg)
                                    ?.let {
                                        activity.startActivity(it)
                                    }

                            } catch (e: Exception) {

                                Log.e(
                                    "VideoCall",
                                    "Launch error: ${e.message}"
                                )
                            }
                        }
                    }

                } else {

                    try {

                        activity.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "market://details?id=$pkg".toUri()
                            )
                        )

                    } catch (e: Exception) {

                        Log.e(
                            "TAG",
                            "PlayStore Error: ${e.message}"
                        )

                        activity.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://play.google.com/store/apps/details?id=$pkg".toUri()
                            )
                        )
                    }
                }
            }

            view.loutVideoCall.addView(itemBinding.root)
        }

        dialog.show()
    }

    fun startDuoCall(activity: Activity, number: String) {
        try {
            val intent = Intent("com.google.android.apps.tachyon.action.DIAL").apply {
                setPackage(Constance.DUO)
                data = "tel:$number".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e("TAG", "isAppInstalled: ${e.message}")
            Toast.makeText(
                activity,
                activity.getString(R.string.meet_not_available), Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun launchContactIntent(activity: Activity, id: Long, pkg: String, mime: String) {
        try {
            val uri = "content://com.android.contacts/data/$id".toUri()
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e("TAG", "isAppInstalled: ${e.message}")
            Toast.makeText(
                activity,
                activity.getString(R.string.failed_to_start_video_call), Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("Range")
    fun getVideoCallID(context: Context, number: String, mimeType: String): Long? {
        val resolver = context.applicationContext.contentResolver

        // Helper to perform query and comparison
        fun findId(selection: String, args: Array<String>): Long? {
            return try {
                resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data._ID, ContactsContract.Data.DATA1),
                    selection,
                    args,
                    null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID))
                        val data1 =
                            cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1))
                        if (compareNumbers(number, data1)) {
                            return id
                        }
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }

        // 1. Primary search: By Contact ID and Exact MimeType (Fastest)
        val contactId = getContactByNumber(context, number)?.contactId
        if (contactId != null) {
            val id = findId(
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId, mimeType)
            )
            if (id != null) return id
        }

        // 2. Secondary search: All rows with Exact MimeType (if contact lookup missed it)
        val id = findId("${ContactsContract.Data.MIMETYPE} = ?", arrayOf(mimeType))
        if (id != null) return id

        // 3. Last resort: Flexible search by account type and "video" in mime
        val accountType =
            if (mimeType.contains("w4b")) Constance.WHATSAPP_BUSINESS else Constance.WHATSAPP
        return findId(
            "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ? AND ${ContactsContract.Data.MIMETYPE} LIKE ?",
            arrayOf(accountType, "%video%")
        )
    }

    fun showMessageAppChooser(activity: Activity, number: String) {
        val pm = activity.packageManager
        val finalResolveInfos = mutableListOf<android.content.pm.ResolveInfo>()

        // 1. Query smsto:
        val smstoIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
        val smstoInfos = pm.queryIntentActivities(smstoIntent, 0)
        finalResolveInfos.addAll(smstoInfos)

        // 2. Query sms: (some apps might only handle this)
        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("sms:$number"))
        val smsInfos = pm.queryIntentActivities(smsIntent, 0)
        finalResolveInfos.addAll(smsInfos)

        // 3. Explicitly check for default SMS app and add if missing
        val defaultSmsPkg = android.provider.Telephony.Sms.getDefaultSmsPackage(activity)
        if (defaultSmsPkg != null && finalResolveInfos.none { it.activityInfo.packageName == defaultSmsPkg }) {
            val launchIntent = pm.getLaunchIntentForPackage(defaultSmsPkg)
            if (launchIntent != null) {
                pm.resolveActivity(launchIntent, 0)?.let {
                    finalResolveInfos.add(it)
                }
            }
        }

        val dialog = BottomSheetDialog(activity, R.style.TransparentDialog)
        val view = VideoCallDialogBinding.inflate(activity.layoutInflater, null, false)
        dialog.setContentView(view.root)
        view.tvTitleDialog.text = activity.getString(R.string.send_message)
        view.tvSubtitle.text = activity.getString(R.string.choose_your_preferred_messaging_app)
        view.loutVideoCall.isVisible = false
        view.rvApps.isVisible = true

        val appsAdapter = AppsAdapter(pm, onClick = { app ->
            val pkg = app.activityInfo.packageName

            dialog.dismiss()
            val isWhatsApp = pkg == Constance.WHATSAPP || pkg == Constance.WHATSAPP_BUSINESS
            if (isWhatsApp) {
                val mime =
                    if (pkg == Constance.WHATSAPP) "vnd.android.cursor.item/vnd.com.whatsapp.profile"
                    else "vnd.android.cursor.item/vnd.com.whatsapp.w4b.profile"
                val waId = getVideoCallID(activity, number, mime)
                waId?.let {
                    launchContactIntent(activity, it, pkg, mime)
                } ?: launchGenericMessage(activity, pkg, number)
            } else {
                launchGenericMessage(activity, pkg, number)
            }
        })
        view.rvApps.adapter = appsAdapter
        view.rvApps.layoutManager = GridLayoutManager(activity, 4)

        // Filter duplicates by package name
        val uniqueInfos = finalResolveInfos.distinctBy { it.activityInfo.packageName }
        appsAdapter.addAll(uniqueInfos)

        Log.e("TAG", "showMessageAppChooser: ${uniqueInfos.size}")

        dialog.show()
    }

    fun saveTag(context: Activity, initialTag: String? = null, onItemClick: (String) -> Unit) {
        val dialog = BottomSheetDialog(context, R.style.TransparentDialog)
        val view = SaveTagDesignBinding.inflate(context.layoutInflater, null, false)
        dialog.setContentView(view.root)

        if (!initialTag.isNullOrEmpty()) {
            view.edtTag.setText(initialTag)
            view.edtTag.setSelection(initialTag.length)
        }

        view.tvSave.setOnClickListener {
            val tag = view.edtTag.text.toString()
            onItemClick(tag)
            dialog.dismiss()
        }

        dialog.show()
    }


    fun alertDialog(
        context: Context,
        title: String,
        description: String,
        btnOkay: String,
        isImageVisible: Boolean = false,
        image: Int = 0,
        onItemClick: (String) -> Unit
    ) {

        val dialog = Dialog(context)
        val alertBinding = AlertDialogDesignBinding.inflate(LayoutInflater.from(context))

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

        alertBinding.ivImage.isVisible = isImageVisible
        alertBinding.tvTitle.text = title
        alertBinding.tvDescription.text = description
        alertBinding.tvOkay.text = btnOkay
        if (image != 0) {
            alertBinding.ivImage.setImageResource(image)
        }


        alertBinding.tvOkay.setOnClickListener {
            onItemClick("")
            dialog.dismiss()
        }

        alertBinding.tvCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun setBlockNumbersDialog(context: Context, onItemClick: (String) -> Unit) {
        val dialog = Dialog(context)
        val bindingBlock = AddBlockNumbersDesignBinding.inflate(LayoutInflater.from(context))

        dialog.setContentView(bindingBlock.root)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.setCancelable(false)

        val margin = (20 * context.resources.displayMetrics.density).toInt()

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        dialog.window?.setLayout(
            screenWidth - (margin * 3),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        bindingBlock.tvOkay.setOnClickListener {
            if (bindingBlock.edtNumber.text.isNotEmpty()) {
                onItemClick(bindingBlock.edtNumber.text.toString().trim())
                dialog.dismiss()
            } else {
                bindingBlock.edtNumber.error = context.getString(R.string.enter_number)
            }
        }
        bindingBlock.tvCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

    }

    fun copyToClipboard(context: Context, text: String, message: String = "Copied") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("copied_text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }


    private fun launchGenericMessage(activity: Activity, pkg: String, number: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "smsto:$number".toUri()
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e("TAG", "isAppInstalled: ${e.message}")
            Toast.makeText(
                activity,
                activity.getString(R.string.failed_to_send_message), Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun getContactName(context: Context, phoneNumber: String): String {
        var name = phoneNumber
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    name = it.getString(0)
                }
            }
        } catch (e: Exception) {
            Log.e("TAG", "getContactName: ${e.message}")
        }
        return name
    }

    fun getContactRingtoneUri(context: Context, phoneNumber: String): Uri? {
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.CUSTOM_RINGTONE),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val ringtoneStr = it.getString(0)
                    if (!ringtoneStr.isNullOrEmpty()) {
                        return Uri.parse(ringtoneStr)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TAG", "getContactRingtoneUri: ${e.message}")
        }
        return null
    }

    fun getSimLabel(context: Context, subscriptionId: Int): String {
        if (subscriptionId == -1) return ""
        val subscriptionManager =
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val activeSubscriptionInfoList = try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                subscriptionManager.activeSubscriptionInfoList
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

        if (activeSubscriptionInfoList == null || activeSubscriptionInfoList.size <= 1) return ""

        activeSubscriptionInfoList.forEachIndexed { index, subscriptionInfo ->
            if (subscriptionInfo.subscriptionId == subscriptionId) {
                return "SIM ${index + 1}"
            }
        }
        return ""
    }

    private var lastClickTime = 0L

    fun isValidClick(delay: Long = 500L): Boolean {
        val currentTime = System.currentTimeMillis()

        return if (currentTime - lastClickTime >= delay) {
            lastClickTime = currentTime
            true
        } else {
            false
        }
    }
}