package com.example.contactmanager.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.databinding.HistoryDesignBinding
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.utils.Common

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryHolder>() {
    private var historyList: MutableList<CallLogEntry> = mutableListOf()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        p1: Int
    ): HistoryHolder {
        val binding =
            HistoryDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryHolder(binding)
    }

    override fun onBindViewHolder(
        holder: HistoryHolder,
        position: Int
    ) {
        val itemData = historyList[position]
        holder.setData(itemData)
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    fun addAll(historyList: MutableList<CallLogEntry>) {
        this.historyList.clear()
        this.historyList.addAll(historyList)
        notifyDataSetChanged()
    }

    class HistoryHolder(private val binding: HistoryDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(itemData: CallLogEntry) {
//            binding.tvTime.text = Common.formatSmartDate(itemData.dateData)

            val tag = Common.getCallType(itemData.intType)

//            val duration = Common.formatDuration(itemData.duration)
            binding.tvInOutCallsWithDuration.text = tag
        }

    }
}