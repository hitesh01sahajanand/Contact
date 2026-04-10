package com.example.contactmanager.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactmanager.databinding.FavoriteDesignBinding
import com.example.contactmanager.models.ContactModel
import com.example.contactmanager.utils.Common

class FavoriteAdapter :
    RecyclerView.Adapter<FavoriteAdapter.FavoriteDataHolder>() {
    private val contactList = ArrayList<ContactModel>()
    private var filteredList: MutableList<ContactModel> = mutableListOf()
    override fun onCreateViewHolder(
        parent: ViewGroup, p1: Int
    ): FavoriteDataHolder {
        val binding =
            FavoriteDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavoriteDataHolder(binding)
    }

    override fun onBindViewHolder(
        holder: FavoriteDataHolder, position: Int
    ) {
        val itemData = filteredList[position]
        holder.setData(itemData, position)

    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    fun addAll(newList: List<ContactModel>) {
        /* contactList.clear()
         contactList.addAll(newList)
         notifyDataSetChanged()*/
        contactList.clear()
        filteredList.clear()
        contactList.addAll(newList)
        filteredList.addAll(newList)
        notifyDataSetChanged()
    }

    fun remove(item: ContactModel) {
        val position = filteredList.indexOf(item)
        if (position != -1) {
            filteredList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun filter(query: String) {
        val searchText = query.trim()

        filteredList = if (searchText.isEmpty()) {
            contactList.toMutableList()
        } else {
            contactList.filter {
                (it.displayName?.contains(searchText, ignoreCase = true) == true) ||
                        it.displayName?.contains(searchText, ignoreCase = true) == true
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    class FavoriteDataHolder(private val binding: FavoriteDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(
            itemData: ContactModel, position: Int
        ) {
            binding.run {
                tvName.text = itemData.displayName

                if (itemData.userThumbnail.isNullOrEmpty()) {
                    binding.tvContactName.isVisible = true
                    binding.ivContactPhoto.isVisible = false
                    val color = Common.profileColors[position % Common.profileColors.size]
                    binding.cvProfile.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, color)
                    )
                    val firstChar = itemData.displayName?.firstOrNull()?.uppercase() ?: ""
                    tvContactName.text = firstChar

                } else {
                    binding.tvContactName.isVisible = false
                    binding.ivContactPhoto.isVisible = true
                    Glide.with(ivContactPhoto.context).load(itemData.userThumbnail)
                        .into(ivContactPhoto)
                }

            }
        }

    }
}