package com.example.contactmanager.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactmanager.R
import com.example.contactmanager.databinding.HeaderItemDesignBinding
import com.example.contactmanager.databinding.SelectContactDesignBinding
import com.example.contactmanager.models.ContactListItem
import com.example.contactmanager.models.ContactModel
import com.example.contactmanager.utils.Common

class SelectContactAdapter(
    private val onClick: (ContactModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val contactList = ArrayList<ContactListItem>()
    private var filteredList: MutableList<ContactListItem> = mutableListOf()

    private var expandedPosition = -1

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
            val binding = SelectContactDesignBinding.inflate(
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
                (holder as ContactViewHolder).bind(item, position)
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

    class HeaderViewHolder(private val binding: HeaderItemDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContactListItem.Header) {
            binding.tvHeaderTitle.text = item.title
        }
    }

    inner class ContactViewHolder(private val binding: SelectContactDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContactListItem.Contact, position: Int) {
            val data = item.data

            val isExpanded = position == expandedPosition
            val isNextExpanded = position + 1 == expandedPosition
            val isPrevExpanded = position - 1 == expandedPosition

            val isFirst =
                position == 0 || (filteredList.getOrNull(position - 1) is ContactListItem.Header) || isPrevExpanded
            val isLast = position == filteredList.size - 1 ||
                    (filteredList.getOrNull(position + 1) is ContactListItem.Header) || isNextExpanded

            val backgroundRes = when {
                isExpanded -> R.drawable.bg_all_rounded
                isFirst && isLast -> R.drawable.bg_all_rounded
                isFirst -> R.drawable.bg_top_rounded
                isLast -> R.drawable.bg_bottom_rounded
                else -> R.drawable.bg_middle
            }

            binding.llCollapseView.setBackgroundResource(backgroundRes)

            val params = binding.root.layoutParams as RecyclerView.LayoutParams

            params.setMargins(0, 0, 0, 0)

            binding.root.layoutParams = params

            binding.run {
                llCollapseView.setOnClickListener {
                    onClick(data)
                }

                tvCollapseName.text = data.displayName

                if (data.userThumbnail.isNullOrEmpty()) {
                    binding.tvCollapseContactName.isVisible = true
                    binding.ivCollapseContactPhoto.isVisible = false
                    val color = Common.profileColors[position % Common.profileColors.size]
                    binding.cvCollapseProfile.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, color)
                    )
                    val firstChar = data.displayName?.firstOrNull()?.uppercase() ?: ""
                    tvCollapseContactName.text = firstChar

                } else {
                    binding.tvCollapseContactName.isVisible = false
                    binding.ivCollapseContactPhoto.isVisible = true
                    Glide.with(ivCollapseContactPhoto.context).load(data.userThumbnail)
                        .into(ivCollapseContactPhoto)
                }

            }

        }
    }


}