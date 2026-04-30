package com.example.contactmanager.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.databinding.LanguageDesignBinding
import com.example.contactmanager.models.LanguageModel

class LanguageAdapter(
    private val list: MutableList<LanguageModel>,
) : RecyclerView.Adapter<LanguageAdapter.LanguageDataHolder>() {

    private var selectedPosition = list.indexOfFirst { it.isSelected }

    override fun onCreateViewHolder(
        parent: ViewGroup, p1: Int
    ): LanguageDataHolder {
        val binding =
            LanguageDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageDataHolder(binding)
    }

    override fun onBindViewHolder(
        holder: LanguageDataHolder, position: Int
    ) {
        val itemData = list[position]
        holder.setData(itemData, holder.bindingAdapterPosition)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getSelectedLanguage(): LanguageModel {
        return list[selectedPosition]
    }


    inner class LanguageDataHolder(private val binding: LanguageDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(itemData: LanguageModel, bindingAdapterPosition: Int) {
            binding.ivFlag.setImageResource(itemData.flag)
            binding.tvLanguageNameLocal.text = itemData.nameLocal
            binding.tvLanguageEnglish.text = itemData.nameEnglish

            binding.rbLanguage.isChecked = itemData.isSelected

            binding.llLanguage.setOnClickListener {
                updateSelection(bindingAdapterPosition)
            }
            binding.rbLanguage.setOnClickListener {
                updateSelection(bindingAdapterPosition)
            }

        }

        private fun updateSelection(position: Int) {
            if (position == selectedPosition) return

            if (selectedPosition != -1) {
                list[selectedPosition].isSelected = false
                notifyItemChanged(selectedPosition)
            }

            list[position].isSelected = true
            notifyItemChanged(position)

            selectedPosition = position
        }

    }
}