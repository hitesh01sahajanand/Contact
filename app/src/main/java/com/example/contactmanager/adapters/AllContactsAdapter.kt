package com.example.contactmanager.adapters

import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactmanager.R
import com.example.contactmanager.databinding.AllContactDesignBinding
import com.example.contactmanager.databinding.HeaderItemDesignBinding
import com.example.contactmanager.models.CallHistoryListItems
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

    private var expandedPosition = -1

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

    inner class ContactViewHolder(private val binding: AllContactDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContactListItem.Contact, position: Int) {

            val data = item.data

            val context = binding.root.context
            val isExpanded = position == expandedPosition

            binding.llCollapseView.visibility = View.VISIBLE
            binding.llExpandedView.visibility = if (isExpanded) View.VISIBLE else View.GONE

            // 🔥 Check neighbors (ignore headers)
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

            binding.llMainView.setBackgroundResource(backgroundRes)

            val params = binding.root.layoutParams as RecyclerView.LayoutParams
            val vertical = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)

            if (isExpanded) {
                binding.viewSep.isVisible = false
                params.setMargins(0, vertical, 0, vertical)
            } else {
                // Separator should be hidden if this is the last in its visual group (includes if next is expanded)
                binding.viewSep.isVisible = !isLast
                params.setMargins(0, 0, 0, 0)
            }

            binding.root.layoutParams = params

            binding.llMainView.setOnClickListener {

                val previousPosition = expandedPosition
                expandedPosition = if (isExpanded) -1 else position

                val transition = TransitionSet()
                    .addTransition(Fade())
                    .addTransition(ChangeBounds())
                    .setDuration(250)

                TransitionManager.beginDelayedTransition(binding.llMainView, transition)

                // Notify all affected items
                val itemsToNotify = mutableSetOf<Int>()
                if (previousPosition != -1) {
                    itemsToNotify.add(previousPosition)
                    itemsToNotify.add(previousPosition - 1)
                    itemsToNotify.add(previousPosition + 1)
                }
                itemsToNotify.add(position)
                itemsToNotify.add(position - 1)
                itemsToNotify.add(position + 1)

                itemsToNotify.forEach { pos ->
                    if (pos in 0 until itemCount) {
                        notifyItemChanged(pos)
                    }
                }
            }

            binding.run {
                tvCollapseName.text = data.displayName
                tvExpandedContactNumber.text = "Mobile +${data.number}"

                ivCall.setOnClickListener { onClick(item, position) }
                ivMessage.setOnClickListener { onClick(item, position) }
                ivVideoCall.setOnClickListener { onClick(item, position) }
                ivCallInfo.setOnClickListener { onClick(item, position) }

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