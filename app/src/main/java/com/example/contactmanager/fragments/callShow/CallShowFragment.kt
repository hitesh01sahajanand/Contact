package com.example.contactmanager.fragments.callShow

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.contactmanager.activities.home.HomeActivity
import com.example.contactmanager.activities.newContact.NewContactActivity
import com.example.contactmanager.databinding.FragmentCallShowBinding
import com.example.contactmanager.utils.OnClickHandler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Random


class CallShowFragment : Fragment(), OnClickHandler {
    private lateinit var binding: FragmentCallShowBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCallShowBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.onClickHandler = this
        showCurrentDateTime()
        showGreeting()
    }

    private fun showCurrentDateTime() {
        val calendar = Calendar.getInstance()

        val dateFormat = SimpleDateFormat("HH:mm a", Locale.getDefault())
        val currentDateTime = dateFormat.format(calendar.time)

        val random = Random()

        val sunriseMinute = random.nextInt(60)
        val sunsetMinute = random.nextInt(60)

        val sunriseTime = "6:${String.format("%02d", sunriseMinute)}"
        val sunsetTime = "18:${String.format("%02d", sunsetMinute)}"

        binding.textViewDay2.text =
            "Today, the sun rises at $sunriseTime and sets at $sunsetTime"
    }

    private fun showGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when {
            hour in 5..11 -> "Good Morning!"
            hour in 12..17 -> "Good Afternoon!"
            else -> "Good Night!"
        }

        binding.textViewDay1.text = greeting
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.rlContacts.id -> {
                val intent = Intent(requireActivity(), HomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                requireActivity().finishAndRemoveTask()
            }

            binding.rlAddContact.id -> {
                val intent = Intent(requireActivity(), NewContactActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                requireActivity().finishAndRemoveTask()
            }
        }
    }

}