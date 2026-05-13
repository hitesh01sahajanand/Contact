package com.example.contactmanager.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactmanager.databinding.BlockNumbersDesignBinding
import com.example.contactmanager.models.BlockModel
import com.example.contactmanager.utils.Common

class BlockNumberAdapter(
    private val onUnblockClick: (BlockModel) -> Unit
) : RecyclerView.Adapter<BlockNumberAdapter.BlockNumberViewHolder>() {

    private var blockList = listOf<BlockModel>()

    fun setBlockList(newList: List<BlockModel>) {
        blockList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        p1: Int
    ): BlockNumberViewHolder {
        val binding =
            BlockNumbersDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BlockNumberViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: BlockNumberViewHolder,
        position: Int
    ) {
        val blockModel = blockList[position]
        holder.bind(blockModel)
    }

    override fun getItemCount(): Int = blockList.size

    inner class BlockNumberViewHolder(private val binding: BlockNumbersDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(blockModel: BlockModel) {
            val context = binding.root.context
            val displayName = blockModel.name ?: blockModel.phoneNumber

            binding.tvName.text = displayName

            if (!blockModel.photoUri.isNullOrEmpty()) {
                binding.ivContactPhoto.visibility = View.VISIBLE
                binding.tvContactName.visibility = View.GONE
                Glide.with(context)
                    .load(blockModel.photoUri)
                    .into(binding.ivContactPhoto)
            } else {
                binding.ivContactPhoto.visibility = View.GONE
                binding.tvContactName.visibility = View.VISIBLE

                val initials = displayName
                    .trim()
                    .split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                binding.tvContactName.text = initials.removePrefix("+")

                val colorIndex =
                    (blockModel.phoneNumber.hashCode() and Int.MAX_VALUE) % Common.profileColors.size
                val color = Common.profileColors[colorIndex]
                binding.cvProfile.setCardBackgroundColor(ContextCompat.getColor(context, color))
            }

            binding.cvBlockContact.setOnClickListener {
                onUnblockClick(blockModel)
            }
        }
    }
}