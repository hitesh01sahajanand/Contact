package com.example.contactmanager.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactmanager.databinding.AccountsDesignBinding
import com.example.contactmanager.models.AccountModel
import com.example.contactmanager.utils.Common

class AllAccountAdapter(private val onClick: (AccountModel) -> Unit) :
    RecyclerView.Adapter<AllAccountAdapter.AccountHolder>() {
    private var accountsList = mutableListOf<AccountModel>()

    override fun onCreateViewHolder(
        parent: ViewGroup, p1: Int
    ): AccountHolder {
        val binding =
            AccountsDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AccountHolder, position: Int
    ) {
        val itemData = accountsList[position]
        holder.setData(itemData)
        holder.itemView.setOnClickListener {
            onClick(itemData)
        }
    }

    override fun getItemCount(): Int {
        return accountsList.size
    }

    fun addAll(list: List<AccountModel>) {
        accountsList.clear()
        accountsList.addAll(list)
        notifyDataSetChanged()
    }

    class AccountHolder(private val binding: AccountsDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(itemData: AccountModel) {
            binding.tvIdName.text = itemData.name
            /*val color = Common.profileColors[position % Common.profileColors.size]
            binding.cvProfile.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, color)
            )
            val firstChar = itemData.name.firstOrNull()?.uppercase() ?: ""
            binding.tvContactName.text = firstChar*/
        }
    }
}