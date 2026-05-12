package com.example.contactmanager.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.databinding.LayoutItemReminderBinding
import com.example.contactmanager.models.ReminderModel
import java.text.SimpleDateFormat
import java.util.*

class ReminderMainAdapter(
    private val context: Context,
    private val onDeleteClick: (ReminderModel) -> Unit
) : RecyclerView.Adapter<ReminderMainAdapter.ViewHolder>() {

    private var reminderList: List<ReminderModel> = emptyList()

    fun setReminders(list: List<ReminderModel>) {
        this.reminderList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutItemReminderBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reminder = reminderList[position]
        holder.bind(reminder)
    }

    override fun getItemCount(): Int = reminderList.size

    inner class ViewHolder(private val binding: LayoutItemReminderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(reminder: ReminderModel) {
            binding.reminderTitleTextViewItem.text = reminder.title.replace("\n", " ")

            val calendar = Calendar.getInstance().apply {
                timeInMillis = reminder.reminder_date_time
            }

            val timeFormat = if (DateFormat.is24HourFormat(context)) {
                SimpleDateFormat("h:mm", Locale.getDefault())
            } else {
                SimpleDateFormat("h:mm a", Locale.getDefault())
            }
            binding.reminderDateTimeTextViewItem1.text = timeFormat.format(calendar.time)

            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            binding.reminderDateTimeTextViewItem2.text = dateFormat.format(calendar.time)

            val colorCode = when (reminder.color.replace("\n", " ")) {
                "1" -> "#62CA6E"
                "2" -> "#78DCC7"
                "3" -> "#FF2ED9"
                "4" -> "#FF6A00"
                "5" -> "#8F0FFF"
                "6" -> "#FFC300"
                "7" -> "#00B2FF"
                else -> "#62CA6E"
            }
            binding.imageColor.setColorFilter(Color.parseColor(colorCode), PorterDuff.Mode.SRC_IN)

            binding.reminderDelete.setOnClickListener {
                onDeleteClick(reminder)
            }
        }
    }
}
