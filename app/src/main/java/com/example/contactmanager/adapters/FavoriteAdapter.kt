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
import com.example.contactmanager.databinding.FavoriteDesignBinding
import com.example.contactmanager.models.ContactModel
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance

class FavoriteAdapter(private val onClick: (ContactModel, String) -> Unit) :
    RecyclerView.Adapter<FavoriteAdapter.FavoriteDataHolder>() {
    private val contactList = ArrayList<ContactModel>()
    private var filteredList: MutableList<ContactModel> = mutableListOf()
    private var expandedPosition = -1
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
                (it.displayName?.contains(searchText, ignoreCase = true) == true) ||
                        it.displayName?.contains(searchText, ignoreCase = true) == true
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    inner class FavoriteDataHolder(private val binding: FavoriteDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(
            itemData: ContactModel, position: Int
        ) {
            val isExpanded = position == expandedPosition

            binding.run {
                llCollapseView.visibility = View.VISIBLE
                llExpandedView.visibility = if (isExpanded) View.VISIBLE else View.GONE

                llMainView.setOnClickListener {
                    val previousPosition = expandedPosition
                    expandedPosition = if (isExpanded) -1 else position

                    val transition = TransitionSet()
                        .addTransition(Fade())
                        .addTransition(ChangeBounds())
                        .setDuration(250)

                    TransitionManager.beginDelayedTransition(llMainView, transition)

                    notifyItemChanged(position)
                    // Notify all affected items

                   /* val itemsToNotify = mutableSetOf<Int>()
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

                        }
                    }*/
                }


                ivCall.setOnClickListener {
                    onClick(itemData, Constance.ACTION_CALL)
                }

                ivMessage.setOnClickListener {
                    onClick(itemData, Constance.ACTION_SEND_MESSAGE)
                }

                ivVideoCall.setOnClickListener {
                    onClick(itemData, Constance.ACTION_VIDEO_CALL)
                }

                ivCallInfo.setOnClickListener {
                    onClick(itemData, Constance.ACTION_INFO)
                }


                tvName.text = itemData.displayName
                tvExpandedContactNumber.text = "Mobile +${itemData.number}"

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