package com.phonecall.dialcontacts.calldialer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.phonecall.dialcontacts.calldialer.databinding.AccountsDesignBinding
import com.phonecall.dialcontacts.calldialer.models.AccountModel
import com.phonecall.dialcontacts.calldialer.utils.Common

class AllAccountAdapter(
    private val isCountVisible: Boolean = false,
    private val onClick: (AccountModel) -> Unit
) :
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

    inner class AccountHolder(private val binding: AccountsDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(itemData: AccountModel) {

            if (isCountVisible) {
                binding.llAccounts.isVisible = false
                binding.llAccountsCount.isVisible = true
            } else {
                binding.llAccountsCount.isVisible = false
                binding.llAccounts.isVisible = true
            }

            if (itemData.email.isNotEmpty()) {
                binding.tvIdName.text = itemData.email
            } else {
                binding.tvIdName.text = itemData.name
            }

            binding.tvEmailName.text = itemData.email
            binding.tvCount.text = itemData.count.toString()

            val color = Common.profileColors[adapterPosition % Common.profileColors.size]
            binding.cvProfile.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, color)
            )
            val firstChar = itemData.email.firstOrNull()?.uppercase() ?: ""
            binding.tvContactName.text = firstChar
            if (firstChar.isEmpty()){
                val nextChar = itemData.name.firstOrNull()?.uppercase() ?: ""
                binding.tvContactName.text = nextChar
            }
        }
    }
}