package com.example.contactmanager.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.databinding.StorageLocationDesignBinding
import com.example.contactmanager.utils.Common

class StorageLocationAdapter : RecyclerView.Adapter<StorageLocationAdapter.StorageViewHolder>() {
    var accountInfoDetails: ArrayList<Pair<String?, String?>> = ArrayList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        p1: Int
    ): StorageViewHolder {
        val binding =
            StorageLocationDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StorageViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: StorageViewHolder,
        position: Int
    ) {
        val itemData = accountInfoDetails[position]
        holder.setData(itemData)
    }

    override fun getItemCount(): Int {
        return accountInfoDetails.size
    }

    fun addAll(accountInfoDetails: List<Pair<String?, String?>>) {
        this.accountInfoDetails = ArrayList()
        this.accountInfoDetails.addAll(accountInfoDetails)
        notifyDataSetChanged()
    }

    class StorageViewHolder(private val binding: StorageLocationDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(itemData: Pair<String?, String?>) {

            binding.tvName.text = itemData.first
            binding.tvNumber.text = itemData.second

            val context = binding.root.context
            val accountType = itemData.second
            var iconDrawable: android.graphics.drawable.Drawable? = null

            if (accountType != null) {
                try {
                    val accountManager = android.accounts.AccountManager.get(context)
                    val authTypes = accountManager.authenticatorTypes
                    for (auth in authTypes) {
                        if (auth.type == accountType) {
                            iconDrawable = context.packageManager.getDrawable(
                                auth.packageName,
                                auth.iconId,
                                null
                            )
                            break
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (iconDrawable == null) {
                    try {
                        iconDrawable = context.packageManager.getApplicationIcon(accountType)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }


            if (iconDrawable != null) {
                binding.tvContactName.isVisible = false
                binding.ivContactPhoto.isVisible = true
                binding.ivContactPhoto.setImageDrawable(iconDrawable)
            } else {
                binding.tvContactName.isVisible = true
                binding.ivContactPhoto.isVisible = false
                val color = Common.profileColors[1 % Common.profileColors.size]
                binding.cvProfile.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, color)
                )
                val firstChar = itemData.first?.firstOrNull()?.uppercase() ?: ""
                binding.tvContactName.text = firstChar
            }

        }

    }
}