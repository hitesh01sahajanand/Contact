package com.example.contactmanager.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactmanager.databinding.AllContactDesignBinding
import com.example.contactmanager.databinding.HeaderItemDesignBinding
import com.example.contactmanager.models.ContactListItem
import com.example.contactmanager.utils.Common

class AllContactsAdapter(private val onClick: (ContactListItem, Int) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val contactList = ArrayList<ContactListItem>()
    private var filteredList: MutableList<ContactListItem> = mutableListOf()

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_CONTACT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (filteredList[position]) {
            is ContactListItem.Header -> TYPE_HEADER
            is ContactListItem.Contact -> TYPE_CONTACT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == TYPE_HEADER) {
            val binding = HeaderItemDesignBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = AllContactDesignBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ContactViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (val item = filteredList[position]) {

            is ContactListItem.Header -> {
                (holder as HeaderViewHolder).bind(item)
            }

            is ContactListItem.Contact -> {
                (holder as ContactViewHolder).bind(item)
                holder.itemView.setOnClickListener {
                    onClick(item, position)
                }
            }
        }


    }

    override fun getItemCount(): Int = filteredList.size

    fun addAll(newList: List<ContactListItem>) {
        contactList.clear()
        filteredList.clear()
        contactList.addAll(newList)
        filteredList.addAll(newList)
        notifyDataSetChanged()
    }

    fun clearList() {
        contactList.clear()
        filteredList.clear()
    }

    fun filter(query: String) {

        val searchText = query.trim()
        val tempList = mutableListOf<ContactListItem>()

        if (searchText.isEmpty()) {
            filteredList = contactList.toMutableList()
        } else {

            var lastHeader: ContactListItem.Header? = null

            contactList.forEach {

                when (it) {

                    is ContactListItem.Header -> {
                        lastHeader = it
                    }

                    is ContactListItem.Contact -> {

                        val match = it.data.displayName?.contains(searchText, true) == true ||
                                it.data.number?.contains(searchText, true) == true

                        if (match) {
                            if (lastHeader != null && !tempList.contains(lastHeader)) {
                                tempList.add(lastHeader)
                            }
                            tempList.add(it)
                        }
                    }
                }
            }

            filteredList = tempList
        }

        notifyDataSetChanged()
    }

    fun getCurrentList(): List<ContactListItem> {
        return filteredList
    }

    class HeaderViewHolder(private val binding: HeaderItemDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContactListItem.Header) {
            binding.tvHeaderTitle.text = item.title
        }
    }

    class ContactViewHolder(private val binding: AllContactDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContactListItem.Contact) {

            val data = item.data

            binding.run {
                tvName.text = data.displayName

                if (data.userThumbnail.isNullOrEmpty()) {
                    binding.tvContactName.isVisible = true
                    binding.ivContactPhoto.isVisible = false
                    val color = Common.profileColors[position % Common.profileColors.size]
                    binding.cvProfile.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, color)
                    )
                    val firstChar = data.displayName?.firstOrNull()?.uppercase() ?: ""
                    tvContactName.text = firstChar

                } else {
                    binding.tvContactName.isVisible = false
                    binding.ivContactPhoto.isVisible = true
                    Glide.with(ivContactPhoto.context).load(data.userThumbnail)
                        .into(ivContactPhoto)
                }

            }

        }
    }
}