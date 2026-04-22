package com.example.contactmanager.adapters

import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactmanager.R
import com.example.contactmanager.databinding.DateHeaderDesignBinding
import com.example.contactmanager.databinding.RecentsDesignBinding
import com.example.contactmanager.models.CallHistoryListItems
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance

class RecentAdapter(
    private val onClickCall: (CallLogEntry, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTACT = 1
        private const val TYPE_LOADER = 2
    }

    private var expandedPosition = -1

    private val originalList = ArrayList<CallHistoryListItems>()
    private var filteredList = ArrayList<CallHistoryListItems>()

    override fun getItemViewType(position: Int): Int {
        return when (filteredList[position]) {
            is CallHistoryListItems.Header -> TYPE_HEADER
            is CallHistoryListItems.Contact -> TYPE_CONTACT
            is CallHistoryListItems.Loader -> TYPE_LOADER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {

            TYPE_HEADER -> {
                val binding = DateHeaderDesignBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HeaderViewHolder(binding)
            }

            TYPE_LOADER -> {
                val view = LayoutInflater.from(parent.context).inflate(
                    R.layout.loader_design, parent, false
                )
                LoaderViewHolder(view)
            }

            else -> {
                val binding = RecentsDesignBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ContactViewHolder(binding)
            }
        }
    }

    override fun getItemCount(): Int = filteredList.size


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (val item = filteredList[position]) {

            is CallHistoryListItems.Header -> {
                (holder as HeaderViewHolder).bind(item)
            }

            is CallHistoryListItems.Contact -> {
                (holder as ContactViewHolder).bind(item, position)
            }

            is CallHistoryListItems.Loader -> {
                // No binding needed for loader
            }
        }
    }

    // 🔹 Submit list
    fun submitList(list: List<CallHistoryListItems>) {
        originalList.clear()
        originalList.addAll(list)

        filteredList.clear()
        filteredList.addAll(list)

        notifyDataSetChanged()
    }

    fun addMoreData(list: List<CallHistoryListItems>) {
        val startPosition = originalList.size
        originalList.addAll(list)
        filteredList.addAll(list)
        notifyItemRangeInserted(startPosition, list.size)
    }

    fun showLoader() {
        if (filteredList.lastOrNull() !is CallHistoryListItems.Loader) {
            filteredList.add(CallHistoryListItems.Loader)
            notifyItemInserted(filteredList.size - 1)
        }
    }

    fun hideLoader() {
        if (filteredList.lastOrNull() is CallHistoryListItems.Loader) {
            val position = filteredList.size - 1
            filteredList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun clearList() {
        originalList.clear()
        filteredList.clear()
        notifyDataSetChanged()
    }


    fun filter(query: String) {

        val search = query.trim()

        if (search.isEmpty()) {
            filteredList = ArrayList(originalList)
        } else {

            val tempList = ArrayList<CallHistoryListItems>()
            var currentHeader: CallHistoryListItems.Header? = null

            for (item in originalList) {

                when (item) {

                    is CallHistoryListItems.Header -> {
                        currentHeader = item
                    }

                    is CallHistoryListItems.Contact -> {

                        val data = item.data

                        val name = data.stringCallName ?: ""
                        val number = data.stringNumber ?: ""

                        if (name.contains(search, true) || number.contains(search, true)) {

                            // ✅ HEADER ADD (ONLY ONCE)
                            currentHeader?.let { header ->
                                if (!tempList.contains(header)) {
                                    tempList.add(header)
                                }
                            }

                            // ✅ CONTACT ADD
                            tempList.add(item)
                        }
                    }

                    else -> {

                    }
                }
            }

            filteredList = tempList
        }

        notifyDataSetChanged()
    }

    // 🧩 HEADER VIEW HOLDER
    class HeaderViewHolder(private val binding: DateHeaderDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CallHistoryListItems.Header) {
            binding.tvDate.text = item.title
        }
    }

    // 📞 CONTACT VIEW HOLDER
    inner class ContactViewHolder(private val binding: RecentsDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CallHistoryListItems.Contact, position: Int) {
            val data = item.data

            val context = binding.root.context
            val isExpanded = position == expandedPosition

            binding.llCollapseView.visibility = if (isExpanded) View.GONE else View.VISIBLE
            binding.llExpandedView.visibility = if (isExpanded) View.VISIBLE else View.GONE

            val isSaved = !data.contactId.isNullOrEmpty()
            binding.llNotSavedContact.isVisible = !isSaved
            binding.ivCallInfo.isVisible = isSaved

            // 🔥 Check neighbors (ignore headers)
            val isNextExpanded = position + 1 == expandedPosition
            val isPrevExpanded = position - 1 == expandedPosition

            val isFirst =
                position == 0 || filteredList[position - 1] is CallHistoryListItems.Header || isPrevExpanded
            val isLast = position == filteredList.size - 1 ||
                    filteredList.getOrNull(position + 1) is CallHistoryListItems.Header ||
                    filteredList.getOrNull(position + 1) is CallHistoryListItems.Loader ||
                    isNextExpanded

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

                ivCall.setOnClickListener {
                    onClickCall(data, Constance.ACTION_CALL)
                }

                ivMessage.setOnClickListener {
                    onClickCall(data, Constance.ACTION_SEND_MESSAGE)
                }

                ivVideoCall.setOnClickListener {
                    onClickCall(data, Constance.ACTION_VIDEO_CALL)
                }

                ivCallInfo.setOnClickListener {
                    onClickCall(data, Constance.ACTION_INFO)
                }

                cvAddToContact.setOnClickListener {
                    onClickCall(data, Constance.ACTION_ADD_TO_CONTACT)
                }

                cvAddTag.setOnClickListener {
                    onClickCall(data, Constance.ACTION_ADD_TAG)
                }

                llBlockContact.setOnClickListener {
                    onClickCall(data, Constance.ACTION_BLOCK_CONTACT)
                }


                val name = if (data.callCount > 1) {
                    "${data.stringCallName ?: data.stringNumber} (${data.callCount})"
                } else {
                    data.stringCallName ?: data.stringNumber
                }

                tvExpandedCallType.text = Common.getCallType(data.intType)

                tvCollapseName.text = name
                tvExpandedName.text = name

                tvCollapseTime.text = Common.extractTimeFromDate(data.dateData.toString())
                tvExpandedTime.text = Common.extractTimeFromDate(data.dateData.toString())
                if (data.isBlocked) {
                    ivExpandedCallType.setImageDrawable(context.getDrawable(R.drawable.ic_block))
                    ivCollapseCallType.setImageDrawable(context.getDrawable(R.drawable.ic_block))
                } else {
                    ivExpandedCallType.setImageDrawable(
                        Common.getCallImageType(
                            data.intType,
                            root.context
                        )
                    )

                    ivCollapseCallType.setImageDrawable(
                        Common.getCallImageType(
                            data.intType,
                            root.context
                        )
                    )
                }


                tvBlockText.text =
                    if (data.isBlocked) "Unblock" else root.context.getString(R.string.block_contacts)

                // 🖼️ Profile Logic
                if (data.stringPhotoUri.isNullOrEmpty()) {

                    if (data.stringCallName.isNullOrEmpty()) {
                        tvExpandedContactName.isVisible = false
                        ivExpandedContactPhoto.isVisible = false
                        ivExpandedUser.isVisible = true

                        val color = Common.profileColors[position % Common.profileColors.size]
                        cvExpandedProfile.setCardBackgroundColor(
                            ContextCompat.getColor(root.context, color)
                        )

                    } else {
                        tvExpandedContactName.isVisible = true
                        ivExpandedContactPhoto.isVisible = false
                        ivExpandedUser.isVisible = false

                        val color = Common.profileColors[position % Common.profileColors.size]
                        cvExpandedProfile.setCardBackgroundColor(
                            ContextCompat.getColor(root.context, color)
                        )

                        val firstChar =
                            data.stringCallName?.firstOrNull()?.uppercase() ?: ""
                        tvExpandedContactName.text = firstChar
                    }

                } else {
                    tvExpandedContactName.isVisible = false
                    ivExpandedContactPhoto.isVisible = true
                    ivExpandedUser.isVisible = false

                    Glide.with(ivExpandedContactPhoto.context)
                        .load(data.stringPhotoUri)
                        .into(ivExpandedContactPhoto)
                }
            }
        }
    }

    // ⏳ LOADER VIEW HOLDER
    class LoaderViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
