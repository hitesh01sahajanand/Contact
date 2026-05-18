package com.phonecall.dialcontacts.calldialer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phonecall.dialcontacts.calldialer.databinding.AvallaibleAccountsDesignBinding
import com.phonecall.dialcontacts.calldialer.models.AvailableAccountModel

class AvailableAccountsAdapter :
    RecyclerView.Adapter<AvailableAccountsAdapter.AccountsViewHolder>() {
    private val accounts = mutableListOf<AvailableAccountModel>()

    override fun onCreateViewHolder(
        p0: ViewGroup,
        p1: Int
    ): AccountsViewHolder {
        val binding =
            AvallaibleAccountsDesignBinding.inflate(LayoutInflater.from(p0.context), p0, false)
        return AccountsViewHolder(binding)
    }


    override fun onBindViewHolder(
        p0: AccountsViewHolder,
        p1: Int
    ) {
        val account = accounts[p1]
        p0.binding.tvAccountName.text = "${account.displayName} (${account.count})"
        p0.binding.cbAccount.isChecked = account.isSelected

        p0.binding.root.setOnClickListener {
            account.isSelected = !account.isSelected
            p0.binding.cbAccount.isChecked = account.isSelected
        }
    }

    override fun getItemCount(): Int {
        return accounts.size
    }

    fun addAll(availableAccounts: List<AvailableAccountModel>) {
        accounts.clear()
        accounts.addAll(availableAccounts)
        notifyDataSetChanged()
    }

    fun getSelectedAccounts(): List<AvailableAccountModel> {
        return accounts.filter { it.isSelected }
    }

    class AccountsViewHolder(val binding: AvallaibleAccountsDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }
}