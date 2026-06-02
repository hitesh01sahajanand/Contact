package com.phonecall.dialcontacts.calldialer.fragments.newMessage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phonecall.dialcontacts.calldialer.adapters.QuickResponseAdapter
import com.phonecall.dialcontacts.calldialer.databinding.FragmentRemindBinding
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.viewmodels.QuickResponseViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.net.toUri
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.endCall.EndCallActivity
import com.phonecall.dialcontacts.calldialer.activities.endCall.CallEndActivity
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class NewMessageFragment : Fragment(), OnClickHandler {
    private lateinit var binding: FragmentRemindBinding
    private lateinit var adapter: QuickResponseAdapter
    private val viewModel: QuickResponseViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRemindBinding.inflate(inflater, container, false)
        viewModel.refreshForLocaleChange()
        initView()
        observeData()
        return binding.root
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.messages.collectLatest { list ->
                adapter.updateData(list)
            }
        }
    }

    private fun initView() {
        binding.onClickHandler = this

        adapter = QuickResponseAdapter(onItemClick = { model, action ->
            if (action == Constance.ACTION_DELETE) {
                viewModel.deleteMessageById(model.id)
            }

            if (action == Constance.DATA_FETCH) {
                val mobileNumber = (requireActivity() as? EndCallActivity)?.mobileNumber
                    ?: (requireActivity() as? CallEndActivity)?.mobileNumber
                if (!mobileNumber.isNullOrEmpty()) {
                    val uri = Uri.parse("smsto:$mobileNumber")
                    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                        putExtra("sms_body", model.message)
                    }
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    Toast.makeText(
                        requireContext(),
                        requireActivity().getString(R.string.phone_number_not_found),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        })

        binding.rvQuickResponse.adapter = adapter
        binding.rvQuickResponse.layoutManager = LinearLayoutManager(requireActivity())
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        when (view.id) {
            binding.llQuickResponse.id -> {
                Common.editQuickMessageDialog(requireActivity(), onItemClick = { message ->
                    if (message.isNotBlank()) {
                        viewModel.insertMessage(message.trim())
                    }
                })
            }
        }
    }
}