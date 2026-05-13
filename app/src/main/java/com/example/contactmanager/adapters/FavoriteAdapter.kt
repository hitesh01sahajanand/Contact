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
import com.example.contactmanager.databinding.FavoriteDesignBinding
import com.example.contactmanager.models.ContactModel
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Common.isValidClick
import com.example.contactmanager.utils.Constance

class FavoriteAdapter(private val onClick: (ContactModel, String) -> Unit) :
    RecyclerView.Adapter<FavoriteAdapter.FavoriteDataHolder>() {
    private val contactList = ArrayList<ContactModel>()
    private var filteredList: MutableList<ContactModel> = mutableListOf()
    private var expandedPosition = -1

    var isSelectionMode = false
    private val selectedEntries = mutableSetOf<ContactModel>()

    var onSelectionModeChanged: ((Boolean) -> Unit)? = null
    var onSelectionCountChanged: ((Int) -> Unit)? = null

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
        contactList.clear()
        filteredList.clear()
        contactList.addAll(newList)
        filteredList.addAll(newList)
        expandedPosition = -1
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val searchText = query.trim()

        filteredList = if (searchText.isEmpty()) {
            contactList.toMutableList()
        } else {
            contactList.filter {
                (it.displayName?.contains(searchText, ignoreCase = true) == true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun getCurrentList(): List<ContactModel> {
        return filteredList
    }

    fun selectAll() {
        selectedEntries.addAll(filteredList)
        onSelectionCountChanged?.invoke(selectedEntries.size)
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectedEntries.clear()
        onSelectionCountChanged?.invoke(0)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        isSelectionMode = false
        selectedEntries.clear()
        onSelectionModeChanged?.invoke(false)
        notifyDataSetChanged()
    }

    fun getSelectedEntries(): List<ContactModel> {
        return selectedEntries.toList()
    }

    inner class FavoriteDataHolder(private val binding: FavoriteDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(
            itemData: ContactModel, position: Int
        ) {
            val isExpanded = position == expandedPosition
            val context = binding.root.context

            // 🔥 Check neighbors
            val isNextExpanded = position + 1 == expandedPosition
            val isPrevExpanded = position - 1 == expandedPosition

            val isFirst = position == 0 || isPrevExpanded
            val isLast = position == filteredList.size - 1 || isNextExpanded

            val backgroundRes = when {
                isExpanded -> com.example.contactmanager.R.drawable.bg_all_rounded
                isFirst && isLast -> com.example.contactmanager.R.drawable.bg_all_rounded
                isFirst -> com.example.contactmanager.R.drawable.bg_top_rounded
                isLast -> com.example.contactmanager.R.drawable.bg_bottom_rounded
                else -> com.example.contactmanager.R.drawable.bg_middle
            }

            binding.llMainView.setBackgroundResource(backgroundRes)

            val params = binding.root.layoutParams as RecyclerView.LayoutParams
            val vertical =
                context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)

            if (isExpanded) {
                binding.viewSep.isVisible = false
                params.setMargins(0, vertical, 0, vertical)
            } else {
                // Separator should be hidden if this is the last in its visual group (includes if next is expanded)
                binding.viewSep.isVisible = !isLast
                params.setMargins(0, 0, 0, 0)
            }

            binding.root.layoutParams = params

            binding.run {
                llCollapseView.visibility = View.VISIBLE
                llExpandedView.visibility = if (isExpanded) View.VISIBLE else View.GONE

                cbSelect.isVisible = isSelectionMode
                cbSelect.isChecked = selectedEntries.contains(itemData)

                llMainView.setOnLongClickListener {
                    if (!isSelectionMode) {
                        isSelectionMode = true
                        expandedPosition = -1
                        selectedEntries.add(itemData)
                        onSelectionModeChanged?.invoke(true)
                        onSelectionCountChanged?.invoke(selectedEntries.size)
                        notifyDataSetChanged()
                    }
                    true
                }

                llMainView.setOnClickListener {
                    if (isSelectionMode) {
                        if (selectedEntries.contains(itemData)) {
                            selectedEntries.remove(itemData)
                        } else {
                            selectedEntries.add(itemData)
                        }
                        onSelectionCountChanged?.invoke(selectedEntries.size)
                        notifyItemChanged(position)

                        if (selectedEntries.isEmpty()) {
                            isSelectionMode = false
                            onSelectionModeChanged?.invoke(false)
                            notifyDataSetChanged()
                        }
                    } else {
                        val previousPosition = expandedPosition
                        expandedPosition = if (isExpanded) -1 else position

                        val transition = TransitionSet()
                            .addTransition(Fade())
                            .addTransition(ChangeBounds())
                            .setDuration(250)

                        TransitionManager.beginDelayedTransition(llMainView, transition)

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
                }


                ivCall.setOnClickListener {
                    if (!isValidClick()) return@setOnClickListener
                    onClick(itemData, Constance.ACTION_CALL)
                }

                ivMessage.setOnClickListener {
                    if (!isValidClick()) return@setOnClickListener
                    onClick(itemData, Constance.ACTION_SEND_MESSAGE)
                }

                ivVideoCall.setOnClickListener {
                    if (!isValidClick()) return@setOnClickListener
                    onClick(itemData, Constance.ACTION_VIDEO_CALL)
                }

                ivCallInfo.setOnClickListener {
                    if (!isValidClick()) return@setOnClickListener
                    onClick(itemData, Constance.ACTION_INFO)
                }


                tvName.text = itemData.displayName
                tvExpandedContactNumber.text = context.getString(R.string.mobile_, itemData.number)

                if (itemData.userThumbnail.isNullOrEmpty()) {
                    tvContactName.isVisible = true
                    ivContactPhoto.isVisible = false
                    val color = Common.profileColors[position % Common.profileColors.size]
                    cvProfile.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, color)
                    )
                    val firstChar = itemData.displayName?.firstOrNull()?.uppercase() ?: ""
                    tvContactName.text = firstChar

                } else {
                    tvContactName.isVisible = false
                    ivContactPhoto.isVisible = true
                    Glide.with(ivContactPhoto.context).load(itemData.userThumbnail)
                        .into(ivContactPhoto)
                }

            }
        }

    }
}
