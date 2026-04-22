package com.example.contactmanager.fragments.recents

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.CallLog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.R
import com.example.contactmanager.activities.call.CallActivity
import com.example.contactmanager.activities.details.ContactsDetailsActivity
import com.example.contactmanager.activities.newContact.NewContactActivity
import com.example.contactmanager.activities.settings.SettingsActivity
import com.example.contactmanager.adapters.RecentAdapter
import com.example.contactmanager.databinding.FilterBottomSheetDialogBinding
import com.example.contactmanager.databinding.FragmentRecentsBinding
import com.example.contactmanager.models.CallHistoryListItems
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager
import com.example.contactmanager.viewmodels.RecentViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecentsFragment : Fragment(), OnClickHandler {
    private lateinit var binding: FragmentRecentsBinding
    private lateinit var adapter: RecentAdapter
    private val viewModel: RecentViewModel by viewModels()
    private var allList: ArrayList<CallHistoryListItems> = ArrayList()
    private var selectedTypeFilter = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecentsBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (PermissionManager.hasPermissions(requireActivity())) {
            viewModel.loadAllRecentsHistory(0, 1000)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && PermissionManager.hasPermissions(requireActivity())) {
            viewModel.loadAllRecentsHistory(0, 1000)
        }
    }

    private fun initView() {
        binding.onClickHandler = this
        binding.inHeader.onClickHandler = this
        binding.inHeader.tvTitle.text = requireActivity().getString(R.string.recent)
        binding.inHeader.cvMore.isVisible = true
        binding.inHeader.cvFilter.isVisible = true
        selectedTypeFilter = requireActivity().getString(R.string.all_calls)

        adapter = RecentAdapter(onClickCall = { callLogModel, clickAction ->

            when (clickAction) {
                Constance.ACTION_CALL -> {
                    callLogModel.stringNumber?.let {
                        Common.actionCall(it, requireActivity())
                    }
                }

                Constance.ACTION_SEND_MESSAGE -> {
                    callLogModel.stringNumber?.let {
                        Common.showMessageAppChooser(requireActivity(), it)
                    }
                }

                Constance.ACTION_VIDEO_CALL -> {
                    callLogModel.stringNumber?.let {
                        Common.showVideoAppChooser(requireActivity(), it)
                    }
                }

                Constance.ACTION_INFO -> {
                    val intent = Intent(requireActivity(), ContactsDetailsActivity::class.java)
                    intent.putExtra(Constance.DATA_FETCH, callLogModel.contactId)
                    requireActivity().startActivity(intent)
                }

                Constance.ACTION_ADD_TO_CONTACT -> {
                    val isContactSaved = callLogModel.contactId.isNullOrEmpty()
                    val intent = Intent(requireActivity(), NewContactActivity::class.java)
                    intent.putExtra("Number", callLogModel.stringNumber)
                    intent.putExtra(Constance.IS_CONTACT_SAVED, !isContactSaved)
                    requireActivity().startActivity(intent)
                }

                Constance.ACTION_ADD_TAG -> {
                    val isSaved = !callLogModel.contactId.isNullOrEmpty()
                    // If not saved in contacts, stringCallName might be the tag
                    val initialTag = if (!isSaved) callLogModel.stringCallName else null

                    Common.saveTag(requireActivity(), initialTag, onItemClick = { tag ->
                        callLogModel.stringNumber?.let { number ->
                            viewModel.saveTag(number, tag)
                        }
                    })
                }

                Constance.ACTION_BLOCK_CONTACT -> {
                    if (callLogModel.isBlocked) {
                        Common.alertDialog(
                            context = requireActivity(),
                            title = "Unblock Contact?",
                            description = "You will be able to receive calls from this contact.",
                            btnOkay = "Unblock",
                            onItemClick = {
                                callLogModel.stringNumber?.let {
                                    viewModel.unblockNumber(it)
                                }
                            })
                    } else {
                        Common.alertDialog(
                            context = requireActivity(),
                            title = "Block Contact?",
                            description = "You will no longer be able to receive calls from this contact.",
                            btnOkay = "Block",
                            onItemClick = {
                                callLogModel.stringNumber?.let {
                                    viewModel.blockNumber(it)
                                }
                            })
                    }
                }
            }

        })


        binding.rvRecents.adapter = adapter
        binding.rvRecents.layoutManager = LinearLayoutManager(requireActivity())


        viewModel.allRecentCallHistory.observe(viewLifecycleOwner) { recentList ->
            allList.clear()
            allList.addAll(recentList)

            updateAdapterList()
        }

        viewModel.isNextPageLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                adapter.showLoader()
            } else {
                adapter.hideLoader()
            }
        }

        binding.rvRecents.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (totalItemCount <= (lastVisibleItem + 5)) {
                    viewModel.loadNextPage()
                }
            }
        })


        binding.edtSearch.addTextChangedListener { editable ->
            val query = editable.toString()
            adapter.filter(query)
        }

        binding.edtSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.edtSearch.text.toString()
                adapter.filter(query)
                binding.edtSearch.clearFocus()
                Common.hideKeyboard(requireActivity(), v)
                true
            } else {
                false
            }
        }

    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.inHeader.cvMore.id -> {

                val clearHistory = requireActivity().getString(R.string.clear_history)
                val settings = requireActivity().getString(R.string.setting)
                Common.popUpMenu(
                    requireActivity(),
                    binding.inHeader.cvMore,
                    clearHistory,
                    settings,
                    option1Click = {
                        Toast.makeText(requireActivity(), "Call Activity", Toast.LENGTH_SHORT)
                            .show()

                        val intent = Intent(requireContext(), CallActivity::class.java)
                        intent.putExtra("isNew", true)
                        requireActivity().startActivity(intent)
                    },
                    option2Click = {
                        requireActivity().startActivity(
                            Intent(
                                requireActivity(), SettingsActivity::class.java
                            )
                        )
                    })

            }

            binding.inHeader.cvFilter.id -> {
                showFilterBottomSheet(requireActivity(), onClick = { type ->
                    selectedTypeFilter = type
                    updateAdapterList()
                })
            }
        }
    }

    private fun updateAdapterList() {
        val displayList =
            if (selectedTypeFilter.isEmpty() || selectedTypeFilter == getString(R.string.all_calls)) {
                ArrayList(allList)
            } else {
                filterCallLogs(allList, selectedTypeFilter)
            }

        adapter.submitList(displayList)

        val query = binding.edtSearch.text.toString()
        if (query.isNotEmpty()) {
            adapter.filter(query)
        }
    }

    fun filterCallLogs(
        list: List<CallHistoryListItems>, type: String
    ): ArrayList<CallHistoryListItems> {

        val result = ArrayList<CallHistoryListItems>()
        var currentHeader: CallHistoryListItems.Header? = null

        for (item in list) {

            when (item) {

                is CallHistoryListItems.Header -> {
                    currentHeader = item
                }

                is CallHistoryListItems.Contact -> {

                    val callType = item.data.intType

                    val isMatch = when (type) {

                        requireActivity().getString(R.string.all_calls) -> true

                        requireActivity().getString(R.string.missed_calls) -> callType == CallLog.Calls.MISSED_TYPE

                        requireActivity().getString(R.string.incoming_calls) -> callType == CallLog.Calls.INCOMING_TYPE

                        requireActivity().getString(R.string.outgoing_calls) -> callType == CallLog.Calls.OUTGOING_TYPE

                        else -> false
                    }

                    if (isMatch) {
                        currentHeader?.let { header ->
                            if (!result.contains(header)) {
                                result.add(header)
                            }
                        }
                        result.add(item)
                    }
                }

                else -> {

                }
            }
        }

        return result
    }

    fun showFilterBottomSheet(context: Context, onClick: (String) -> Unit) {

        val dialog = BottomSheetDialog(context)
        val filterBinding = FilterBottomSheetDialogBinding.inflate(
            LayoutInflater.from(context), null, false
        )

        dialog.setContentView(filterBinding.root)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.show()

        filterBinding.ivClose.setOnClickListener { dialog.dismiss() }

        filterBinding.llAllCalls.setOnClickListener {
            onClick(context.getString(R.string.all_calls))
            dialog.dismiss()
        }

        filterBinding.llMissedCalls.setOnClickListener {
            onClick(context.getString(R.string.missed_calls))
            dialog.dismiss()
        }

        filterBinding.llIncomingCalls.setOnClickListener {
            onClick(context.getString(R.string.incoming_calls))
            dialog.dismiss()
        }

        filterBinding.llOutGoingCalls.setOnClickListener {
            onClick(context.getString(R.string.outgoing_calls))
            dialog.dismiss()
        }
    }
}