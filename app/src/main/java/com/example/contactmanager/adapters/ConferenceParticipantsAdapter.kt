package com.example.contactmanager.adapters

import android.telecom.Call
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.databinding.ParticipantItemDesignBinding
import com.example.contactmanager.utils.Common

class ConferenceParticipantsAdapter(
    private val participants: List<Call>,
    private val onDisconnect: (Call) -> Unit
) : RecyclerView.Adapter<ConferenceParticipantsAdapter.ParticipantViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ParticipantItemDesignBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(participants[position])
    }

    override fun getItemCount(): Int = participants.size

    inner class ParticipantViewHolder(val binding: ParticipantItemDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(call: Call) {
            val number = call.details.handle?.schemeSpecificPart ?: "Unknown"
            val name = call.details.callerDisplayName.takeIf { !it.isNullOrBlank() && it != number }
                ?: Common.getContactName(binding.root.context, number)
            binding.tvName.text = name
            binding.tvInitials.text = if (name.isNotEmpty()) name[0].toString() else "?"

            binding.ivDisconnect.setOnClickListener {
                onDisconnect(call)
            }
        }
    }
}
