package com.example.contactmanager.fragments.moreFeatures

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.example.contactmanager.Advertisement.manegeAds.AdManege.AdGetterSetterMethod
import com.example.contactmanager.activities.endCall.EndCallActivity
import com.example.contactmanager.activities.home.HomeActivity
import com.example.contactmanager.databinding.FragmentMoreFeaturesBinding
import com.example.contactmanager.R
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.OnClickHandler

class MoreFeaturesFragment : Fragment(), OnClickHandler {
    private lateinit var binding: FragmentMoreFeaturesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMoreFeaturesBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.onClickHandler = this

    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.llContacts.id -> {
                Common.openHomeActivity(requireActivity(), "contacts")
            }

            binding.llMessage.id -> {
                val mobileNumber = (requireActivity() as? EndCallActivity)?.mobileNumber
                if (!mobileNumber.isNullOrEmpty()) {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = "smsto:${mobileNumber}".toUri()
                    intent.putExtra("sms_body", "")
                    requireActivity().startActivity(intent)
                    requireActivity().finishAndRemoveTask()
                }
            }

            binding.llCalender.id -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = "content://com.android.calendar/time".toUri()
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    requireActivity().startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(requireActivity(), "No Calendar app found!", Toast.LENGTH_SHORT)
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireActivity(),
                        "Error opening calendar: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                requireActivity().finishAndRemoveTask()

            }

            binding.llSendMail.id -> {

                val mobileNumber = (requireActivity() as? EndCallActivity)?.mobileNumber
                if (!mobileNumber.isNullOrEmpty()) {
                    val intent = Intent(
                        Intent.ACTION_SENDTO,
                        "mailto:${mobileNumber}".toUri()
                    )
                    requireActivity().startActivity(intent)
                    requireActivity().finishAndRemoveTask()
                }

            }

            binding.llWeb.id -> {
                try {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://www.google.com".toUri()
                    )
                    requireActivity().startActivity(intent)
                    requireActivity().finish()
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(requireActivity(), "No browser found!", Toast.LENGTH_SHORT)
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireActivity(),
                        "Error opening web: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

}