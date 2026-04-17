package com.example.contactmanager.adapters

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.R
import com.example.contactmanager.databinding.ItemVideoCallBinding


class AppsAdapter(private val pm: PackageManager,private val onClick:(ResolveInfo) -> Unit) :
    RecyclerView.Adapter<AppsAdapter.AppsHolder>() {
    private var appsList = mutableListOf<ResolveInfo>()

    override fun onCreateViewHolder(
        parent: ViewGroup, p1: Int
    ): AppsHolder {
        val binding = ItemVideoCallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppsHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AppsHolder, position: Int
    ) {
        val itemData = appsList[position]
        holder.setData(itemData)
        holder.itemView.setOnClickListener {
            onClick(itemData)
        }
    }

    override fun getItemCount(): Int {
        return appsList.size
    }

    fun addAll(list: List<ResolveInfo>) {
        appsList.clear()
        appsList.addAll(list)
        notifyDataSetChanged()
    }

    inner class AppsHolder(private val binding: ItemVideoCallBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(itemData: ResolveInfo) {
            try {
                binding.ivImage.setImageDrawable(itemData.loadIcon(pm))
                binding.tvTitle.text = itemData.loadLabel(pm)
            } catch (_: Exception) {
                binding.ivImage.setImageResource(R.drawable.ic_message)
                binding.tvTitle.text = "Message"
            }
        }
    }
}