package com.example.contactmanager.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.R
import com.example.contactmanager.databinding.LanguageDesignBinding
import com.example.contactmanager.models.LanguageModel
import android.view.View

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

    fun getSelectedPosition(): Int {
        return selectedPosition
    }


    inner class LanguageDataHolder(private val binding: LanguageDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(itemData: LanguageModel, bindingAdapterPosition: Int) {

            if (selectedPosition == -1 && bindingAdapterPosition == 0) {
                binding.lottiHandClick.visibility = View.VISIBLE
                binding.lottiHandClick.setAnimation(R.raw.hand_click)
                binding.lottiHandClick.playAnimation()
            } else {
                binding.lottiHandClick.visibility = View.GONE
            }

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

            val oldPosition = selectedPosition
            selectedPosition = position

            if (oldPosition == -1) {
                notifyItemChanged(0)
            } else {
                list[oldPosition].isSelected = false
                notifyItemChanged(oldPosition)
            }

            list[position].isSelected = true
            notifyItemChanged(position)
        }

    }
}