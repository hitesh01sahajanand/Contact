package com.phonecall.dialcontacts.calldialer.fragments.moreFeatures

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSAppManage
import com.phonecall.dialcontacts.calldialer.activities.endCall.EndCallActivity
import com.phonecall.dialcontacts.calldialer.activities.endCall.CallEndActivity
import com.phonecall.dialcontacts.calldialer.databinding.FragmentMoreFeaturesBinding
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler

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
        if (!isValidClick()) return
        when (view.id) {
            binding.llContacts.id -> {
                Common.openHomeActivity(requireActivity(), "contacts")
            }

            binding.llMessage.id -> {
                val mobileNumber = (requireActivity() as? EndCallActivity)?.mobileNumber
                    ?: (requireActivity() as? CallEndActivity)?.mobileNumber
                if (!mobileNumber.isNullOrEmpty()) {
                    ADSAppManage.isAppOpenBlocked = true
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = "smsto:${mobileNumber}".toUri()
                    intent.putExtra("sms_body", "")
                    requireActivity().startActivity(intent)
                    requireActivity().finish()
                }
            }

            binding.llCalender.id -> {
                try {
                    ADSAppManage.isAppOpenBlocked = true
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = "content://com.android.calendar/time".toUri()
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    requireActivity().startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(
                        requireActivity(),
                        requireActivity().getString(R.string.no_calendar_app_found),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireActivity(),
                        requireActivity().getString(R.string.error_opening_calendar, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                requireActivity().finishAndRemoveTask()

            }

            binding.llSendMail.id -> {

                val mobileNumber = (requireActivity() as? EndCallActivity)?.mobileNumber
                    ?: (requireActivity() as? CallEndActivity)?.mobileNumber
                if (!mobileNumber.isNullOrEmpty()) {
                    ADSAppManage.isAppOpenBlocked = true
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
                    ADSAppManage.isAppOpenBlocked = true
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://www.google.com".toUri()
                    )
                    requireActivity().startActivity(intent)
                    requireActivity().finish()
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(
                        requireActivity(),
                        requireActivity().getString(R.string.no_browser_found),
                        Toast.LENGTH_SHORT
                    ).show()
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