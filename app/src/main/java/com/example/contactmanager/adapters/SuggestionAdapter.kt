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

class SuggestionAdapter : RecyclerView.Adapter<SuggestionAdapter.SuggestionHolder>() {
    var contactList: ArrayList<ContactModel> = ArrayList()
    private var filteredList: MutableList<ContactModel> = mutableListOf()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        p1: Int
    ): SuggestionHolder {
        val binding =
            FavoriteDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SuggestionHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SuggestionHolder,
        p1: Int
    ) {
        val itemData = filteredList[p1]
        holder.setData(itemData,p1)

    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    fun addAll(list: ArrayList<ContactModel>) {
        contactList.clear()
        filteredList.clear()
        contactList.addAll(list)
        filteredList.addAll(list)
        notifyDataSetChanged()
    }

    fun filter(query: String): List<ContactModel> {

        val searchText = query.trim()

        filteredList = if (searchText.isEmpty()) {
            contactList.toMutableList()
        } else {
            contactList.filter {
                it.number?.contains(searchText, ignoreCase = true) == true
            }.toMutableList()
        }

        notifyDataSetChanged()
        return filteredList
    }

    class SuggestionHolder(private val binding: FavoriteDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(itemData: ContactModel, position: Int) {

            binding.run {
                tvName.text = itemData.displayName

                binding.ivCall.isVisible = true

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