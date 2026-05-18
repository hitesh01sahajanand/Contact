package com.phonecall.dialcontacts.calldialer.adapters

import android.provider.CallLog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.databinding.HeaderItemDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.HistoryDesignBinding
import com.phonecall.dialcontacts.calldialer.models.HistoryListItem
import com.phonecall.dialcontacts.calldialer.utils.Common

class HistoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var historyList: MutableList<HistoryListItem> = mutableListOf()

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_HISTORY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (historyList[position]) {
            is HistoryListItem.Header -> TYPE_HEADER
            is HistoryListItem.History -> TYPE_HISTORY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = HeaderItemDesignBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = HistoryDesignBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            HistoryHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = historyList[position]) {
            is HistoryListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is HistoryListItem.History -> (holder as HistoryHolder).bind(item, position)
        }
    }

    override fun getItemCount(): Int = historyList.size

    fun addAll(newList: List<HistoryListItem>) {
        this.historyList.clear()
        this.historyList.addAll(newList)
        notifyDataSetChanged()
    }

    class HeaderViewHolder(private val binding: HeaderItemDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryListItem.Header) {
            binding.tvHeaderTitle.text = item.title
        }
    }

    inner class HistoryHolder(private val binding: HistoryDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryListItem.History, position: Int) {
            val data = item.data
            val context = binding.root.context

            // logic for rounded background
            val isFirst =
                position == 0 || (historyList.getOrNull(position - 1) is HistoryListItem.Header)
            val isLast =
                position == historyList.size - 1 || (historyList.getOrNull(position + 1) is HistoryListItem.Header)

            val backgroundRes = when {
                isFirst && isLast -> R.drawable.bg_all_rounded
                isFirst -> R.drawable.bg_top_rounded
                isLast -> R.drawable.bg_bottom_rounded
                else -> R.drawable.bg_middle
            }

            binding.llMainView.setBackgroundResource(backgroundRes)

            // Set data
            data.dateData?.let {
                binding.tvTime.text = Common.formatTime(it.time)
            }

            val callTypeText = when (data.intType) {
                CallLog.Calls.INCOMING_TYPE -> context.getString(R.string.incoming_call)
                CallLog.Calls.OUTGOING_TYPE -> context.getString(R.string.outgoing_calls)
                CallLog.Calls.MISSED_TYPE -> context.getString(R.string.missed_call)
                CallLog.Calls.VOICEMAIL_TYPE -> context.getString(R.string.voicemail)
                CallLog.Calls.REJECTED_TYPE -> context.getString(R.string.rejected_call)
                CallLog.Calls.BLOCKED_TYPE -> context.getString(R.string.blocked_call)
                else -> context.getString(R.string.unknown_call)
            }
            binding.tvCallTypeText.text = callTypeText

            val iconRes = when (data.intType) {
                CallLog.Calls.INCOMING_TYPE -> R.drawable.ic_incoming_call
                CallLog.Calls.OUTGOING_TYPE -> R.drawable.ic_outgoing_call
                CallLog.Calls.MISSED_TYPE -> R.drawable.ic_miss_call
                else -> R.drawable.ic_outgoing_call
            }
            binding.ivCallType.setImageResource(iconRes)

            if (data.intType == CallLog.Calls.MISSED_TYPE) {
                binding.tvCallTypeText.setTextColor(ContextCompat.getColor(context, R.color.red))
            } else {
                binding.tvCallTypeText.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.black_light
                    )
                )
            }
        }
    }
}