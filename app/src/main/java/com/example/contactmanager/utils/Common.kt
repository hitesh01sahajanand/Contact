package com.example.contactmanager.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
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
import com.example.contactmanager.databinding.RemindMeDialogDesignBinding
import com.example.contactmanager.receivers.ReminderReceiver

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

                val secondChar = secondPart.firstOrNull { it.isLetter() }
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

                Toast.makeText(context, "Please allow exact alarm permission", Toast.LENGTH_SHORT).show()
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

}