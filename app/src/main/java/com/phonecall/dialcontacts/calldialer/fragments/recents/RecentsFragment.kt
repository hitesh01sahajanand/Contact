package com.phonecall.dialcontacts.calldialer.fragments.recents

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.CallLog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.details.ContactsDetailsActivity
import com.phonecall.dialcontacts.calldialer.activities.history.HistoryActivity
import com.phonecall.dialcontacts.calldialer.activities.newContact.NewContactActivity
import com.phonecall.dialcontacts.calldialer.activities.settings.SettingsActivity
import com.phonecall.dialcontacts.calldialer.adapters.RecentAdapter
import com.phonecall.dialcontacts.calldialer.databinding.FilterBottomSheetDialogBinding
import com.phonecall.dialcontacts.calldialer.databinding.FragmentRecentsBinding
import com.phonecall.dialcontacts.calldialer.models.CallHistoryListItems
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.utils.PermissionManager
import com.phonecall.dialcontacts.calldialer.viewmodels.RecentViewModel
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
        if (PermissionManager.hasCallLogPermissions(requireActivity())) {
            viewModel.loadAllRecentsHistory(0, Constance.LOAD_DATA_COUNT)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            if (::adapter.isInitialized) {
                adapter.clearSelection()
            }
        } else if (PermissionManager.hasCallLogPermissions(requireActivity())) {
            viewModel.loadAllRecentsHistory(0, Constance.LOAD_DATA_COUNT)
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
                    intent.putExtra(Constance.NAME, callLogModel.stringCallName)
                    intent.putExtra(Constance.NUMBER, callLogModel.stringNumber)
                    intent.putExtra(Constance.PHOTO_URI, callLogModel.stringPhotoUri)
                    requireActivity().startActivity(intent)
                }

                Constance.ACTION_CALL_HISTORY -> {
                    val intent = Intent(requireActivity(), HistoryActivity::class.java)
                    intent.putExtra(Constance.NUMBER, callLogModel.stringNumber)
                    requireActivity().startActivity(intent)
                }

                Constance.ACTION_ADD_TO_CONTACT -> {
                    val isContactSaved = callLogModel.contactId.isNullOrEmpty()
                    val intent = Intent(requireActivity(), NewContactActivity::class.java)
                    intent.putExtra(Constance.NUMBER, callLogModel.stringNumber)
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
                    Common.ensureDefaultDialer(requireActivity()) {
                        if (callLogModel.isBlocked) {
                            Common.alertDialog(
                                context = requireActivity(),
                                title = requireActivity().getString(R.string.unblock_contact),
                                description = requireActivity().getString(R.string.you_will_be_able_to_receive_call),
                                btnOkay = requireActivity().getString(R.string.unblock),
                                isImageVisible = true,
                                onItemClick = {
                                    callLogModel.stringNumber?.let {
                                        viewModel.unblockNumber(it)
                                    }
                                })
                        } else {
                            Common.alertDialog(
                                context = requireActivity(),
                                isImageVisible = true,
                                title = requireActivity().getString(R.string.block_contact),
                                description = requireActivity().getString(R.string.you_will_be_able_to_receive_call),
                                btnOkay = requireActivity().getString(R.string.block),
                                onItemClick = {
                                    callLogModel.stringNumber?.let {
                                        viewModel.blockNumber(it)
                                    }
                                })
                        }
                    }
                }
            }

        })


        binding.rvRecents.adapter = adapter
        binding.rvRecents.layoutManager = LinearLayoutManager(requireActivity())

        val itemTouchHelper = adapter.getItemTouchHelper(requireActivity())
        itemTouchHelper.attachToRecyclerView(binding.rvRecents)

        val onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                adapter.clearSelection()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )

        adapter.onSelectionModeChanged = { isSelectionMode ->
            onBackPressedCallback.isEnabled = isSelectionMode
            binding.llAllSelection.isVisible = isSelectionMode
            binding.inHeader.root.isVisible = !isSelectionMode
            /*binding.inHeader.root.isVisible = !isSelectionMode
            binding.cvSearch.isVisible = !isSelectionMode*/
            if (!isSelectionMode) {
                binding.cbSelectAll.isChecked = false
                binding.tvCount.text = ""
            }
        }

        adapter.onSelectionCountChanged = { count ->
            binding.tvCount.text = requireActivity().getString(R.string.selected, count)
        }

        binding.llSelectionContact.setOnClickListener {
            val isChecked = !binding.cbSelectAll.isChecked
            binding.cbSelectAll.isChecked = isChecked

            if (isChecked) {
                adapter.selectAll()
            } else {
                adapter.deselectAll()
            }
        }

        binding.ivDelete.setOnClickListener {
            val selected = adapter.getSelectedEntries()
            if (selected.isNotEmpty()) {
                viewModel.deleteHistory(selected)
                adapter.clearSelection()
            } else {
                adapter.clearSelection()
            }
        }

        viewModel.allRecentCallHistory.observe(viewLifecycleOwner) { recentList ->
            allList.clear()
            allList.addAll(recentList)
            updateAdapterList()
            updateVisibility()
        }

        viewModel.isLoadingFirstTime.observe(viewLifecycleOwner) { _ ->
            updateVisibility()
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
            updateVisibility()
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
        if (!isValidClick()) return
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
                        Common.alertDialog(
                            context = requireActivity(),
                            title = requireActivity().getString(R.string.clear_history),
                            description = requireActivity().getString(R.string.clear_history_desc),
                            btnOkay = requireActivity().getString(R.string.clear),
                            onItemClick = {
                                viewModel.clearAllHistory()
                            })
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
//                    Toast.makeText(requireActivity(), type, Toast.LENGTH_SHORT).show()
                })
            }

            binding.ivBack.id -> {
                adapter.clearSelection()
            }
        }
    }

    private fun updateVisibility() {
        val isLoading = viewModel.isLoadingFirstTime.value ?: false
        val recentList = viewModel.allRecentCallHistory.value
        val isEmpty = adapter.getCurrentList().isEmpty()

        val showLoading = isLoading || recentList == null
        binding.pbLoading.isVisible = showLoading

        if (showLoading) {
            binding.llHistorySpaceHolder.isVisible = false
            binding.rvRecents.isVisible = false
        } else {
            binding.llHistorySpaceHolder.isVisible = isEmpty
            binding.rvRecents.isVisible = !isEmpty
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

        binding.llHistorySpaceHolder.isVisible = displayList.isEmpty()
        binding.rvRecents.isVisible = displayList.isNotEmpty()
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

    fun clearSearch() {
        if (::binding.isInitialized) {
            binding.edtSearch.setText("")
        }
    }
}