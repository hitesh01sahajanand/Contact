package com.example.contactmanager.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.R
import com.example.contactmanager.databinding.RingtoneDesignBinding
import com.example.contactmanager.models.RingtoneModel

class RingtonesAdapter(
    private var ringtoneList: MutableList<RingtoneModel>,
    private val onRingtoneSelected: (RingtoneModel) -> Unit,
    private val onPlayPauseClick: (RingtoneModel, Int) -> Unit
) : RecyclerView.Adapter<RingtonesAdapter.RingtoneViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RingtoneViewHolder {
        val binding =
            RingtoneDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RingtoneViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RingtoneViewHolder,
        position: Int
    ) {
        val ringtone = ringtoneList[position]
        holder.setData(ringtone, position)
        holder.itemView.setOnClickListener {
            updateSelection(position)
        }
    }

    private fun updateSelection(position: Int) {
        if (selectedPosition != -1) {
            ringtoneList[selectedPosition].isSelected = false
            notifyItemChanged(selectedPosition)
        }
        selectedPosition = position
        ringtoneList[selectedPosition].isSelected = true
        notifyItemChanged(selectedPosition)
        onRingtoneSelected(ringtoneList[selectedPosition])
    }

    override fun getItemCount(): Int = ringtoneList.size

    fun updateList(newList: List<RingtoneModel>) {
        if (ringtoneList !== newList) {
            ringtoneList.clear()
            ringtoneList.addAll(newList)
        }
        selectedPosition = ringtoneList.indexOfFirst { it.isSelected }
        notifyDataSetChanged()
    }

    fun notifyPlayStateChanged(position: Int) {
        notifyItemChanged(position)
    }

    fun clearSelection() {
        if (selectedPosition >= 0 && selectedPosition < ringtoneList.size) {
            ringtoneList[selectedPosition].isSelected = false
            notifyItemChanged(selectedPosition)
        }
        selectedPosition = -1
    }

    inner class RingtoneViewHolder(private val binding: RingtoneDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(ringtone: RingtoneModel, position: Int) {

            binding.run {
                viewSep.isVisible = position != ringtoneList.size - 1
                tvRingtoneName.text = ringtone.name
                rbSelection.isChecked = ringtone.isSelected

                if (ringtone.isPlaying) {
                    ivPlay.setImageResource(R.drawable.ic_fav)
                } else {
                    ivPlay.setImageResource(R.drawable.ic_play)
                }

                rbSelection.setOnClickListener {
                    updateSelection(position)
                }
                ivPlay.setOnClickListener {
                    onPlayPauseClick(ringtone, position)
                }
            }

        }
    }
}
