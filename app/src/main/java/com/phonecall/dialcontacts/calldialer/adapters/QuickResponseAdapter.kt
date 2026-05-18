package com.phonecall.dialcontacts.calldialer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.phonecall.dialcontacts.calldialer.databinding.QuickResponseDesignBinding
import com.phonecall.dialcontacts.calldialer.models.QuickResponseModel
import com.phonecall.dialcontacts.calldialer.utils.Constance

class QuickResponseAdapter(
    private val onItemClick: (QuickResponseModel, String) -> Unit
) : RecyclerView.Adapter<QuickResponseAdapter.QuickResponseViewHolder>() {
    private var messages: List<QuickResponseModel> = emptyList()

    fun updateData(newMessages: List<QuickResponseModel>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        p1: Int
    ): QuickResponseViewHolder {
        val binding =
            QuickResponseDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuickResponseViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: QuickResponseViewHolder,
        position: Int
    ) {
        holder.setData(messages[position], position)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    inner class QuickResponseViewHolder(private val binding: QuickResponseDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(model: QuickResponseModel, position: Int) {
            if (position <= 3) {
                binding.ivDelete.visibility = View.GONE
            } else {
                binding.ivDelete.visibility = View.VISIBLE
            }
            binding.tvMassage.text = model.message
            binding.root.setOnClickListener {
                onItemClick(model, Constance.DATA_FETCH)
            }
            binding.ivDelete.setOnClickListener {
                onItemClick(model, Constance.ACTION_DELETE)
            }

            binding.viewSep.isVisible = position != messages.size - 1
        }
    }
}