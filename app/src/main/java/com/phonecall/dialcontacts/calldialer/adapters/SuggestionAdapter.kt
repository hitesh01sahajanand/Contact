package com.phonecall.dialcontacts.calldialer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.phonecall.dialcontacts.calldialer.databinding.SuggestionDesignBinding
import com.phonecall.dialcontacts.calldialer.models.ContactModel
import com.phonecall.dialcontacts.calldialer.utils.Common

class SuggestionAdapter(
    private val onClick: (ContactModel) -> Unit,
    private val onFilterComplete: (Int) -> Unit
) :
    RecyclerView.Adapter<SuggestionAdapter.SuggestionHolder>(), android.widget.Filterable {
    var contactList: ArrayList<ContactModel> = ArrayList()
    private var filteredList: MutableList<ContactModel> = mutableListOf()

    override fun getFilter(): android.widget.Filter {
        return object : android.widget.Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString()?.trim() ?: ""
                val filtered = if (charString.isEmpty()) {
                    contactList
                } else {
                    contactList.filter {
                        it.number?.contains(charString, ignoreCase = true) == true
                    }
                }
                return FilterResults().apply { values = filtered }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = (results?.values as? List<ContactModel>)?.toMutableList() ?: mutableListOf()
                notifyDataSetChanged()
                onFilterComplete(filteredList.size)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, p1: Int
    ): SuggestionHolder {
        val binding =
            SuggestionDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SuggestionHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SuggestionHolder, p1: Int
    ) {
        if (p1 < filteredList.size) {
            val itemData = filteredList[p1]
            holder.setData(itemData, p1)
            holder.itemView.setOnClickListener {
                itemData.number?.let {
                    onClick(itemData)
                }
            }
        }
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
        filter.filter(query)
        return filteredList // Note: this will return the OLD list until publishResults is called, but KeypadFragment only uses it for visibility which might be a problem.
    }

    class SuggestionHolder(private val binding: SuggestionDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(itemData: ContactModel, position: Int) {

            binding.run {
                tvName.text = itemData.displayName
                tvNumber.text = itemData.number

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