package com.phonecall.dialcontacts.calldialer.fragments.newRemind

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.endCall.EndCallActivity
import com.phonecall.dialcontacts.calldialer.activities.endCall.CallEndActivity
import com.phonecall.dialcontacts.calldialer.adapters.ReminderMainAdapter
import com.phonecall.dialcontacts.calldialer.databinding.FragmentMessageBinding
import com.phonecall.dialcontacts.calldialer.models.ReminderModel
import com.phonecall.dialcontacts.calldialer.receivers.ReminderReceiver
import com.phonecall.dialcontacts.calldialer.viewmodels.ReminderViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class NewRemindFragment : Fragment() {
    private lateinit var binding: FragmentMessageBinding
    private val viewModel: ReminderViewModel by viewModels()

    private var reminderDateTime: Calendar = Calendar.getInstance()
    private var color: String = "1"
    private lateinit var adapterReminder: ReminderMainAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessageBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        adapterReminder = ReminderMainAdapter(requireActivity()) { reminder ->
            viewModel.markAsDone(reminder.id)
            /*Toast.makeText(
                requireContext(),
                getString(R.string.reminder_marked_as_done), Toast.LENGTH_SHORT
            ).show()*/
        }
        binding.recycleView.adapter = adapterReminder
        binding.recycleView.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)

        viewModel.allPendingReminders.observe(viewLifecycleOwner) { reminders ->
            if (reminders.isNotEmpty()) {
                binding.recycleView.visibility = View.VISIBLE
                binding.tvNoReminder.visibility = View.GONE
            } else {
                binding.recycleView.visibility = View.GONE
                binding.tvNoReminder.visibility = View.VISIBLE
            }
            adapterReminder.setReminders(reminders)
        }

        binding.buttonNo.setOnClickListener {
            toggleCreateView(false)
        }

        binding.buttonReminder.setOnClickListener {
            toggleCreateView(true)
        }

        setupInitialDateTime()

        binding.buttonYes.setOnClickListener {
            saveReminder()
        }

        binding.SelectDate.setOnClickListener {
            showDateTimePicker()
        }

        setupColorSelection()
    }

    private fun toggleCreateView(show: Boolean) {
        binding.linearCreateView.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonReminder.visibility = if (show) View.GONE else View.VISIBLE
        binding.recycleView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun setupInitialDateTime() {
        reminderDateTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1)
        }
        updateDateTimeText()
    }

    private fun updateDateTimeText() {
        val dateFormat = if (DateFormat.is24HourFormat(requireActivity())) {
            SimpleDateFormat("MMMM dd, yyyy  HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("MMMM dd, yyyy  h:mm a", Locale.getDefault())
        }
        binding.addDateTimeReminder.text = dateFormat.format(reminderDateTime.time)
    }

    private fun showDateTimePicker() {
        val currentCalendar = Calendar.getInstance()
        TimePickerDialog(
            requireActivity(),
            { _, hourOfDay, minute ->
                reminderDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                reminderDateTime.set(Calendar.MINUTE, minute)

                DatePickerDialog(
                    requireActivity(),
                    { _, year, month, dayOfMonth ->
                        reminderDateTime.set(Calendar.YEAR, year)
                        reminderDateTime.set(Calendar.MONTH, month)
                        reminderDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        updateDateTimeText()
                    },
                    currentCalendar.get(Calendar.YEAR),
                    currentCalendar.get(Calendar.MONTH),
                    currentCalendar.get(Calendar.DAY_OF_MONTH)
                ).show()

            },
            currentCalendar.get(Calendar.HOUR_OF_DAY),
            currentCalendar.get(Calendar.MINUTE),
            DateFormat.is24HourFormat(requireActivity())
        ).show()
    }

    private fun saveReminder() {
        val title = binding.edittxt.text.toString().trim()
        if (title.isEmpty()) {
            binding.edittxt.error = requireActivity().getString(R.string.please_enter_reminder_title)
            return
        }

        if (reminderDateTime.before(Calendar.getInstance())) {
            Toast.makeText(
                requireContext(),
                requireActivity().getString(R.string.please_select_a_future_time), Toast.LENGTH_SHORT
            ).show()
            return
        }

        val reminder = ReminderModel(
            title = title,
            color = color,
            reminder_date_time = reminderDateTime.timeInMillis,
            reminder_done = 0
        )

        viewModel.insertReminder(reminder)
        val mobileNumber = (requireActivity() as? EndCallActivity)?.mobileNumber
            ?: (requireActivity() as? CallEndActivity)?.mobileNumber
        scheduleNotification(title, reminderDateTime.timeInMillis, mobileNumber)

        toggleCreateView(false)
        Toast.makeText(
            requireContext(),
            requireActivity().getString(R.string.reminder_set_successfully), Toast.LENGTH_SHORT
        ).show()
    }

    private fun scheduleNotification(title: String, timeInMillis: Long, mobileNumber: String?) {
        val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
            putExtra("contactName", title)
            putExtra("contactNumber", mobileNumber) // No specific number for general reminders
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intentSetting = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intentSetting.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                requireContext().startActivity(intentSetting)
                return
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }

    private fun setupColorSelection() {
        binding.linearColor1.setOnClickListener { updateSelectedColor(binding.linearColor1, "1") }
        binding.linearColor2.setOnClickListener { updateSelectedColor(binding.linearColor2, "2") }
        binding.linearColor3.setOnClickListener { updateSelectedColor(binding.linearColor3, "3") }
        binding.linearColor4.setOnClickListener { updateSelectedColor(binding.linearColor4, "4") }
        binding.linearColor5.setOnClickListener { updateSelectedColor(binding.linearColor5, "5") }
        binding.linearColor6.setOnClickListener { updateSelectedColor(binding.linearColor6, "6") }
        binding.linearColor7.setOnClickListener { updateSelectedColor(binding.linearColor7, "7") }
    }

    private fun updateSelectedColor(selected: LinearLayout, selectedColor: String) {
        unselectAllColors()
        selected.setBackgroundResource(R.drawable.iv_circle_bac_icon)
        color = selectedColor
    }

    private fun unselectAllColors() {
        binding.linearColor1.setBackgroundResource(android.R.color.transparent)
        binding.linearColor2.setBackgroundResource(android.R.color.transparent)
        binding.linearColor3.setBackgroundResource(android.R.color.transparent)
        binding.linearColor4.setBackgroundResource(android.R.color.transparent)
        binding.linearColor5.setBackgroundResource(android.R.color.transparent)
        binding.linearColor6.setBackgroundResource(android.R.color.transparent)
        binding.linearColor7.setBackgroundResource(android.R.color.transparent)
    }
}