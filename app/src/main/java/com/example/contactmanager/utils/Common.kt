package com.example.contactmanager.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.createBitmap
import androidx.core.view.isVisible
import com.example.contactmanager.R
import com.example.contactmanager.databinding.PopUpMenuDesignBinding
import com.example.contactmanager.databinding.SendMessageDialogDesignBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.ByteArrayOutputStream
import java.util.Calendar
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import com.example.contactmanager.adapters.AppsAdapter
import com.example.contactmanager.databinding.RemindMeDialogDesignBinding
import com.example.contactmanager.receivers.ReminderReceiver
import com.example.contactmanager.databinding.VideoCallDialogBinding
import com.example.contactmanager.databinding.ItemVideoCallBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

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

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null) return ""

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatSmartDate(timestamp: Long): String {
        val cal = Calendar.getInstance()
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        cal.timeInMillis = timestamp

        return when {
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(timestamp))
            }

            cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> {
                "Yesterday " + SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(timestamp))
            }

            else -> {
                SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.ENGLISH).format(Date(timestamp))
            }
        }
    }

    fun formatDuration(seconds: Long): String {

        if (seconds <= 0) return "0 sec"

        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hrs > 0 -> "${hrs} hr ${mins} min"
            mins > 0 -> "${mins} min ${secs} sec"
            else -> "${secs} sec"
        }
    }

    fun generateAvatar(input: String): Bitmap {

        val text = input.trim()

        val initials = when {

            text.isNotEmpty() && text.all { it.isDigit() } -> {
                when {
                    text.length >= 2 -> text.substring(0, 2)
                    text.length == 1 -> text + "0"
                    else -> "00"
                }
            }

            !text.contains(" ") -> {
                text.firstOrNull { it.isLetter() }?.uppercase()
                    ?: text.firstOrNull()?.toString()
                    ?: ""
            }

            else -> {
                val parts = text.split(" ")
                    .filter { it.isNotEmpty() }

                val firstPart = parts.getOrNull(0) ?: ""
                val secondPart = parts.getOrNull(1) ?: ""

                val firstChar = firstPart.firstOrNull()
                    ?.toString()
                    ?.uppercase()
                    ?: ""

                val secondChar = secondPart.firstOrNull() { it.isLetter() }
                    ?.uppercase()
                    ?: secondPart.firstOrNull()
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

    fun getPhoneType(type: String): Int {
        return when (type) {
            "Mobile" -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
            "Work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
            "Home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
            "Main" -> ContactsContract.CommonDataKinds.Phone.TYPE_MAIN
            "Work Fax" -> ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK
            "Home Fax" -> ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME
            "Pager" -> ContactsContract.CommonDataKinds.Phone.TYPE_PAGER
            else -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
        }
    }

    fun getEmailType(type: String): Int {
        return when (type) {
            "Home" -> ContactsContract.CommonDataKinds.Email.TYPE_HOME
            "Work" -> ContactsContract.CommonDataKinds.Email.TYPE_WORK
            else -> ContactsContract.CommonDataKinds.Email.TYPE_OTHER
        }
    }

    fun getPhotoBytes(uri: Uri, context: Context): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    fun getCallType(i: Int): String {
        return when (i) {
            1 -> "Incoming"
            2 -> "Outgoing"
            3 -> "Missed"
            else -> "Unknown"
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getCallImageType(i: Int, context: Context): Drawable? {
        return when (i) {
            1 -> context.getDrawable(R.drawable.ic_incoming_call)
            2 -> context.getDrawable(R.drawable.ic_outgoing_call)
            3 -> context.getDrawable(R.drawable.ic_miss_call)
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

        return s1.length >= 10 && s2.length >= 10 &&
                (s1.endsWith(s2) || s2.endsWith(s1))
    }

    fun Activity.getAlertDialogBuilder() = MaterialAlertDialogBuilder(this)

    fun getHandleForSubId(subId: Int, context: Context): android.telecom.PhoneAccountHandle? {
        val telecomManager =
            context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
        val phoneAccounts = telecomManager.callCapablePhoneAccounts
        for (account in phoneAccounts) {
            if (account.id.contains(subId.toString())) {
                return account
            }
        }
        return null
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

    fun showMessageDialog(
        context: Context,
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


        bindingSendMessage.tvTextMe.setOnClickListener {
            onItemClick(bindingSendMessage.tvTextMe.text.toString())
            dialog.dismiss()
        }

        bindingSendMessage.tvCallBack.setOnClickListener {
            onItemClick(bindingSendMessage.tvCallBack.text.toString())
            dialog.dismiss()
        }

        bindingSendMessage.tvCallLater.setOnClickListener {
            onItemClick(bindingSendMessage.tvCallLater.text.toString())
            dialog.dismiss()
        }

        bindingSendMessage.cvAddMessage.setOnClickListener {
            bindingSendMessage.llSendMessage.isVisible = !bindingSendMessage.llSendMessage.isVisible
        }

        bindingSendMessage.cvSend.setOnClickListener {
            onItemClick(bindingSendMessage.edtSendMassage.text.toString())
            dialog.dismiss()
        }

        dialog.show()
    }


    fun showRemindMeDialog(
        context: Context,
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

        // ✅ Set current time by default
        val now = Calendar.getInstance()

        binding.timePicker.hour = now.get(Calendar.HOUR_OF_DAY)
        binding.timePicker.minute = now.get(Calendar.MINUTE)

        // ✅ 12-hour format (AM/PM)
        binding.timePicker.setIs24HourView(false)

        // ⏰ Set Reminder Click
        binding.cvSetReminder.setOnClickListener {

            val hour: Int = binding.timePicker.hour
            val minute: Int = binding.timePicker.minute

            val selectedCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            if (selectedCal.before(Calendar.getInstance())) {
                Toast.makeText(context, "Please select future time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Set reminder
            setReminder(context, hour, minute)

            onReminderSet()
            dialog.dismiss()
        }

        dialog.show()
    }

    fun setReminder(context: Context, hour: Int, minute: Int) {

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        val now = Calendar.getInstance()

        // ❗ Only today allowed
        if (calendar.before(now)) {
            Toast.makeText(context, "Please select future time", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (!alarmManager.canScheduleExactAlarms()) {
                // ❗ open settings safely
                val intentSetting = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intentSetting.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intentSetting)

                Toast.makeText(context, "Please allow exact alarm permission", Toast.LENGTH_SHORT)
                    .show()
                return
            }
        }

        // ✅ Alarm set
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    val Context.powerManager: PowerManager
        get() = getSystemService(Context.POWER_SERVICE) as PowerManager

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

    fun isValidEmail(email: String?): Boolean {
        return !email.isNullOrEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun sendSMSMessage(context: Context, number: String?, msg: String) {
        if (number.isNullOrEmpty()) return

        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "smsto:$number".toUri()
                putExtra("sms_body", msg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun actionCall(number: String?, context: Context) {
        if (number.isNullOrEmpty()) return

        if (NewCallManager.isNumberActive(number)) {
            Toast.makeText(context, "Number already in a call", Toast.LENGTH_SHORT).show()
            return
        }

        val telecomManager =
            context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return

        val callUri = Uri.fromParts("tel", number, null)
        val callBundle = Bundle().apply {
            putBoolean("android.telecom.extra.START_CALL_WITH_SPEAKERPHONE", false)
        }

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                val subscriptionManager =
                    context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val activeSimList = subscriptionManager?.activeSubscriptionInfoList

                if (!activeSimList.isNullOrEmpty() && activeSimList.size > 1) {

                    val simNames = Array(activeSimList.size) { i ->
                        "SIM ${i + 1}"
                    }

                    val builder = MaterialAlertDialogBuilder(context)

                    builder.setTitle("Select SIM").setItems(simNames) { _, which ->

                        val selectedSim = activeSimList[which]

                        val callBundle2 = Bundle().apply {
                            putParcelable(
                                TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                                Common.getHandleForSubId(
                                    selectedSim.subscriptionId, context
                                )
                            )
                        }

                        val callUri2 = Uri.fromParts("tel", number, null)
                        telecomManager.placeCall(callUri2, callBundle2)
                    }

                    val dialog = builder.create()
                    dialog.show()

                    dialog.getButton(Dialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)

                } else {
                    // Single SIM
                    telecomManager.placeCall(callUri, callBundle)
                }

            } else {
                // Pre-Marshmallow
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = "tel:$number".toUri()
                }
                context.startActivity(intent)
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
            false
        } catch (e: Exception) {
            false
        }
    }

    fun showVideoAppChooser(activity: Activity, number: String, onSelection: (() -> Unit)? = null) {
        val appList = Constance.videoCallList
        val dialog = BottomSheetDialog(activity, R.style.TransparentDialog)
        val view = VideoCallDialogBinding.inflate(activity.layoutInflater, null, false)
        dialog.setContentView(view.root)
        view.rvApps.isVisible = false
        view.loutVideoCall.isVisible = true

        view.loutVideoCall.removeAllViews()

        appList.forEach { pkg ->
            val isInstalled = isAppInstalled(activity, pkg)
            val itemBinding =
                ItemVideoCallBinding.inflate(activity.layoutInflater, view.loutVideoCall, false)

            try {
                if (isInstalled) {
                    val info = activity.packageManager.getPackageInfo(pkg, 0)
                    itemBinding.ivImage.setImageDrawable(info.applicationInfo?.loadIcon(activity.packageManager))
                    itemBinding.tvTitle.text =
                        info.applicationInfo?.loadLabel(activity.packageManager)
                } else {
                    itemBinding.ivImage.setImageResource(R.drawable.ic_video_call)
                    itemBinding.tvTitle.text = when (pkg) {
                        Constance.WHATSAPP -> "Install WhatsApp"
                        Constance.WHATSAPP_BUSSINESS -> "Install WA Business"
                        Constance.DUO -> "Install Meet"
                        else -> "Install App"
                    }
                }
            } catch (_: Exception) {
                itemBinding.ivImage.setImageResource(R.drawable.ic_video_call)
                itemBinding.tvTitle.text = if (pkg.contains("whatsapp")) "WhatsApp" else "Meet"
            }

            itemBinding.root.setOnClickListener {
                dialog.dismiss()
                if (isInstalled) {
                    onSelection?.invoke()
                    when (pkg) {
                        Constance.DUO -> startDuoCall(activity, number)
                        Constance.WHATSAPP -> {
                            val waId = getVideoCallID(
                                activity,
                                number,
                                "vnd.android.cursor.item/vnd.com.whatsapp.video.call"
                            )
                            waId?.let {
                                launchContactIntent(
                                    activity,
                                    it,
                                    pkg,
                                    "vnd.android.cursor.item/vnd.com.whatsapp.video.call"
                                )
                            } ?: run {
                                Toast.makeText(activity, "WhatsApp video call not available for this contact", Toast.LENGTH_SHORT).show()
                            }
                        }

                        Constance.WHATSAPP_BUSSINESS -> {
                            val wabId = getVideoCallID(
                                activity,
                                number,
                                "vnd.android.cursor.item/vnd.com.whatsapp.w4b.video.call"
                            )
                            wabId?.let {
                                launchContactIntent(
                                    activity,
                                    it,
                                    pkg,
                                    "vnd.android.cursor.item/vnd.com.whatsapp.w4b.video.call"
                                )
                            } ?: run {
                                Toast.makeText(activity, "WhatsApp Business video call not available for this contact", Toast.LENGTH_SHORT).show()
                            }
                        }

                        else -> {
                            // Default fallback is to try Duo if it's the only other option
                            if (pkg == Constance.DUO) {
                                startDuoCall(activity, number)
                            } else {
                                activity.packageManager.getLaunchIntentForPackage(pkg)?.let {
                                    activity.startActivity(it)
                                }
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
            Toast.makeText(activity, "Meet not available", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(activity, "Failed to start video call", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("Range")
    fun getVideoCallID(context: Context, number: String, mimeType: String): Long? {
        val cleanNumber = cleanNumber(number)
        return try {
            val resolver = context.applicationContext.contentResolver
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID, ContactsContract.Data.DATA1),
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(mimeType),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID))
                    val data1 = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1))
                    if (data1 != null) {
                        val cleanData1 = cleanNumber(data1)
                        if (PhoneNumberUtils.compare(context, cleanNumber, cleanData1)) {
                            return id
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
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
        view.tvTitleDialog.text = "Send Message"
        view.tvSubtitle.text = "Choose your preferred messaging app"
        view.loutVideoCall.isVisible = false
        view.rvApps.isVisible = true

        val  appsAdapter = AppsAdapter(pm, onClick =  { app ->
            val pkg = app.activityInfo.packageName

            dialog.dismiss()
            val isWhatsApp = pkg == Constance.WHATSAPP || pkg == Constance.WHATSAPP_BUSSINESS
            if (isWhatsApp) {
                val mime = if (pkg == Constance.WHATSAPP) "vnd.android.cursor.item/vnd.com.whatsapp.profile"
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
        view.rvApps.layoutManager = GridLayoutManager(activity, 4 )

        // Filter duplicates by package name
        val uniqueInfos = finalResolveInfos.distinctBy { it.activityInfo.packageName }
        appsAdapter.addAll(uniqueInfos)

        Log.e("TAG", "showMessageAppChooser: ${uniqueInfos.size}", )

        dialog.show()
    }

    private fun launchGenericMessage(activity: Activity, pkg: String, number: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$number")
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Failed to send message", Toast.LENGTH_SHORT).show()
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

    fun getDisplayName(context: Context, number: String, callerDisplayName: String?): String {
        val contactName = getContactName(context, number)
        if (contactName != number) {
            return contactName
        }
        if (!callerDisplayName.isNullOrBlank() && callerDisplayName != number) {
            return callerDisplayName
        }
        return number
    }
}