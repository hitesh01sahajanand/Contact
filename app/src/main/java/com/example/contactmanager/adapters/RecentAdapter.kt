package com.example.contactmanager.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactmanager.databinding.DateHeaderDesignBinding
import com.example.contactmanager.databinding.RecentsDesignBinding
import com.example.contactmanager.models.CallHistoryListItems
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.utils.Common

class RecentAdapter(
    private val onClickCall: (CallLogEntry) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTACT = 1
    }

    private val originalList = ArrayList<CallHistoryListItems>()
    private var filteredList = ArrayList<CallHistoryListItems>()

    override fun getItemViewType(position: Int): Int {
        return when (filteredList[position]) {
            is CallHistoryListItems.Header -> TYPE_HEADER
            is CallHistoryListItems.Contact -> TYPE_CONTACT
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

    fun clearList(){
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

            binding.run {

                val name = if (data.callCount > 1) {
                    "${data.stringCallName ?: data.stringNumber} (${data.callCount})"
                } else {
                    data.stringCallName ?: data.stringNumber
                }

                binding.tvCallType.text = Common.getCallType(data.intType)

                tvName.text = name

                tvTime.text = Common.extractTimeFromDate(data.dateData.toString())
                ivCallType.setImageDrawable(Common.getCallImageType(data.intType, root.context))

                ivCall.setOnClickListener {
                    onClickCall(item.data)
                }

                // 🖼️ Profile Logic
                if (data.stringPhotoUri.isNullOrEmpty()) {

                    if (data.stringCallName.isNullOrEmpty()) {
                        tvContactName.isVisible = false
                        ivContactPhoto.isVisible = false
                        ivUser.isVisible = true

                        val color = Common.profileColors[position % Common.profileColors.size]
                        cvProfile.setCardBackgroundColor(
                            ContextCompat.getColor(root.context, color)
                        )

                    } else {
                        tvContactName.isVisible = true
                        ivContactPhoto.isVisible = false
                        ivUser.isVisible = false

                        val color = Common.profileColors[position % Common.profileColors.size]
                        cvProfile.setCardBackgroundColor(
                            ContextCompat.getColor(root.context, color)
                        )

                        val firstChar =
                            data.stringCallName?.firstOrNull()?.uppercase() ?: ""
                        tvContactName.text = firstChar
                    }

                } else {
                    tvContactName.isVisible = false
                    ivContactPhoto.isVisible = true
                    ivUser.isVisible = false

                    Glide.with(ivContactPhoto.context)
                        .load(data.stringPhotoUri)
                        .into(ivContactPhoto)
                }
            }
        }
    }
}
