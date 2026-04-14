package com.example.contactmanager.fragments.recents

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.activities.settings.SettingsActivity
import com.example.contactmanager.adapters.RecentAdapter
import com.example.contactmanager.databinding.FilterBottomSheetDialogBinding
import com.example.contactmanager.databinding.FragmentRecentsBinding
import com.example.contactmanager.models.CallHistoryListItems
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.models.ContactListItem
import com.example.contactmanager.models.ContactModel
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager
import com.example.contactmanager.utils.SendData
import com.example.contactmanager.viewmodels.RecentViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri
import com.example.contactmanager.activities.call.CallActivity

@AndroidEntryPoint
class RecentsFragment : Fragment(), OnClickHandler {
    private lateinit var binding: FragmentRecentsBinding
    private lateinit var adapter: RecentAdapter
    private val viewModel: RecentViewModel by viewModels()
    private var allList: ArrayList<CallHistoryListItems> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecentsBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.onClickHandler = this
        binding.inHeader.onClickHandler = this
        binding.inHeader.tvTitle.text = requireActivity().getString(R.string.recent)
        binding.inHeader.cvMore.isVisible = true
        binding.inHeader.cvFilter.isVisible = true

        adapter = RecentAdapter(/*onclickDetails = { logModel ->
            SendData.contactDetails = logModel
            val intent = Intent(requireActivity(), ContactsDetailsActivity::class.java)
            requireActivity().startActivity(intent)
        }*/ onClickCall = { logEntry ->
            actionCall(logEntry, requireActivity())
        })

        binding.rvRecents.adapter = adapter
        binding.rvRecents.layoutManager = LinearLayoutManager(requireActivity())


        if (SendData.isFirstTime) {
            SendData.isFirstTime = false
            SendData.allRecentCallHistory.observe(requireActivity()) { recentList ->
                allList.clear()
                allList.addAll(processRawCallLogs(recentList))
                adapter.submitList(allList)
            }
        } else {
            if (PermissionManager.hasPermissions(requireActivity())) {
                viewModel.loadAllRecentsHistory(0, 1000)
            }

            viewModel.allRecentCallHistory.observe(requireActivity()) { recentList ->
                allList.clear()
                allList.addAll(processRawCallLogs(recentList))
                adapter.submitList(allList)
            }
        }


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
            /* binding.tvAll.id -> {
                 adapter.clearList()
                 adapter.addAll(allList)
             }

             binding.tvMissed.id -> {
                 adapter.clearList()
                 val missedCalls = allList.filter { it.intType == 3 }
                 adapter.addAll(missedCalls)
             }*/

            binding.inHeader.cvMore.id -> {

                val clearHistory = requireActivity().getString(R.string.clear_history)
                val settings = requireActivity().getString(R.string.setting)
                Common.popUpMenu(
                    requireActivity(),
                    binding.inHeader.cvMore,
                    clearHistory,
                    settings,
                    option1Click = {
                        requireActivity().startActivity(
                            Intent(
                                requireActivity(),
                                CallActivity::class.java
                            )
                        )
                    },
                    option2Click = {
                        requireActivity().startActivity(
                            Intent(
                                requireActivity(),
                                SettingsActivity::class.java
                            )
                        )
                    }
                )

            }

            binding.inHeader.cvFilter.id -> {


                showFilterBottomSheet(requireActivity(), onClick = { type ->
                    when (type) {
                        requireActivity().getString(R.string.all_calls) -> {
                            val filteredList = filterCallLogs(allList, type)
                            Log.e("TAG", "onClick: ${filteredList.size} ")
                            adapter.clearList()
                            adapter.submitList(filteredList)
                        }

                        requireActivity().getString(R.string.missed_calls) -> {
                            val filteredList = filterCallLogs(allList, type)
                            adapter.clearList()
                            adapter.submitList(filteredList)
                            Log.e("TAG", "onClick: ${filteredList.size} ")
                        }

                        requireActivity().getString(R.string.incoming_calls) -> {
                            val filteredList = filterCallLogs(allList, type)
                            adapter.clearList()
                            adapter.submitList(filteredList)
                        }

                        requireActivity().getString(R.string.outgoing_calls) -> {
                            val filteredList = filterCallLogs(allList, type)
                            adapter.clearList()
                            adapter.submitList(filteredList)
                        }
                    }
                })
            }
        }
    }

    fun filterCallLogs(
        list: List<CallHistoryListItems>,
        type: String
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

                        requireActivity().getString(R.string.missed_calls) ->
                            callType == CallLog.Calls.MISSED_TYPE

                        requireActivity().getString(R.string.incoming_calls) ->
                            callType == CallLog.Calls.INCOMING_TYPE

                        requireActivity().getString(R.string.outgoing_calls) ->
                            callType == CallLog.Calls.OUTGOING_TYPE

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
            }
        }

        return result
    }

    fun showFilterBottomSheet(context: Context, onClick: (String) -> Unit) {

        val dialog = BottomSheetDialog(context)
        val filterBinding = FilterBottomSheetDialogBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
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

    fun actionCall(callLogEntry: CallLogEntry, context: Context) {
        if (callLogEntry.stringNumber.isNullOrEmpty()) return

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: return

        val callUri = Uri.fromParts("tel", callLogEntry.stringNumber, null)
        val callBundle = Bundle().apply {
            putBoolean("android.telecom.extra.START_CALL_WITH_SPEAKERPHONE", false)
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                val subscriptionManager =
                    context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val activeSimList = subscriptionManager?.activeSubscriptionInfoList

                if (!activeSimList.isNullOrEmpty() && activeSimList.size > 1) {

                    val simNames = Array(activeSimList.size) { i ->
                        "SIM ${i + 1}"
                    }

                    val builder = MaterialAlertDialogBuilder(context)

                    builder.setTitle("Select SIM")
                        .setItems(simNames) { _, which ->

                            val selectedSim = activeSimList[which]

                            val callBundle2 = Bundle().apply {
                                putParcelable(
                                    TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                                    Common.getHandleForSubId(
                                        selectedSim.subscriptionId,
                                        context
                                    )
                                )
                            }

                            val callUri2 = Uri.fromParts("tel", callLogEntry.stringNumber, null)
                            telecomManager.placeCall(callUri2, callBundle2)
                        }

                    val dialog = builder.create()
                    dialog.show()

                    dialog.getButton(Dialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)

                } else {
                    // Single SIM
                    telecomManager.placeCall(callUri, callBundle)
                }

            } else {
                // Pre-Marshmallow
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = "tel:${callLogEntry.stringNumber}".toUri()
                }
                context.startActivity(intent)
            }
        }
    }

    /*fun processRawCallLogs(
        list: ArrayList<CallLogEntry>
    ): ArrayList<CallLogEntry> {

        if (list.isEmpty()) return ArrayList()

        val colorList = listOf(
            Color.parseColor("#2173C2"),
            Color.parseColor("#FFB950"),
            Color.parseColor("#AEB33C"),
            Color.parseColor("#FF6082"),
            Color.parseColor("#60CB6B")
        )

        val processedList = ArrayList<CallLogEntry>()
        var lastEntry: CallLogEntry? = null
        var lastDateCategory = ""

        // Today & Yesterday setup
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayStart = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStart = calendar.time

        val todayStr = getString(R.string.today)
        val yesterdayStr = getString(R.string.yesterday)
        val olderStr = getString(R.string.older)

        for (entry in list) {

            val entryNum = entry.stringNumber
            val entryName = entry.stringCallName

            // 🚫 Skip entries without number
            if (entryNum.isNullOrEmpty()) continue

            // 1. Color assign (based only on number)
            val key = entryNum.replace(Regex("[^0-9+]"), "")
            val colorIndex = (key.hashCode() and Int.MAX_VALUE) % colorList.size
            entry.intColor = colorList[colorIndex]

            // 2. Date category
            val entryDate = entry.dateData

            val currentCategory = when {
                entryDate != null && entryDate.after(todayStart) -> todayStr
                entryDate != null && entryDate.after(yesterdayStart) -> yesterdayStr
                else -> olderStr
            }

            entry.stringDateCategory = currentCategory

            // 3. Duplicate check (same day + same number/name)
            var isDuplicate = false

            lastEntry?.let { last ->

                val sameDay = isSameDayStatic(entryDate, last.dateData)

                if (sameDay) {
                    val isSameNumber =
                        !last.stringNumber.isNullOrEmpty() &&
                                PhoneNumberUtils.compare(entryNum, last.stringNumber)

                    val isSameName =
                        !entryName.isNullOrEmpty() &&
                                entryName == last.stringCallName

                    if (isSameNumber || isSameName) {
                        isDuplicate = true
                    }
                }
            }

            if (isDuplicate) {
                lastEntry?.apply {
                    callCount += 1
                    callIds.addAll(entry.callIds)
                }
            } else {
                // 📌 Add header when category changes
                if (processedList.isNotEmpty() && currentCategory != lastDateCategory) {
                    val header = CallLogEntry().apply {
                        stringDateCategory = currentCategory
                    }
                    processedList.add(header)
                }

                lastDateCategory = currentCategory

                entry.callCount = 1
                processedList.add(entry)
                lastEntry = entry
            }
        }
        processedList.removeIf { it.stringNumber.isNullOrEmpty() }

        return processedList
    }*/

    fun processRawCallLogs(
        list: ArrayList<CallLogEntry>
    ): ArrayList<CallHistoryListItems> {

        if (list.isEmpty()) return ArrayList()

        val colorList = listOf(
            Color.parseColor("#2173C2"),
            Color.parseColor("#FFB950"),
            Color.parseColor("#AEB33C"),
            Color.parseColor("#FF6082"),
            Color.parseColor("#60CB6B")
        )

        val processedList = ArrayList<CallHistoryListItems>()
        var lastEntry: CallLogEntry? = null
        var lastDateCategory = ""

        // 📅 Date formatter for older dates
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

        // Today & Yesterday setup
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayStart = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStart = calendar.time

        val todayStr = "Today"       // ya getString(R.string.today)
        val yesterdayStr = "Yesterday"

        for (entry in list) {

            val entryNum = entry.stringNumber
            val entryName = entry.stringCallName

            // 🚫 Skip entries without number
            if (entryNum.isNullOrEmpty()) continue

            // 🎨 Color assign
            val key = entryNum.replace(Regex("[^0-9+]"), "")
            val colorIndex = (key.hashCode() and Int.MAX_VALUE) % colorList.size
            entry.intColor = colorList[colorIndex]

            val entryDate = entry.dateData

            // 📅 Category decide
            val currentCategory = when {
                entryDate != null && entryDate.after(todayStart) -> todayStr
                entryDate != null && entryDate.after(yesterdayStart) -> yesterdayStr
                entryDate != null -> dateFormat.format(entryDate)
                else -> ""
            }

            // 🔁 Duplicate check
            var isDuplicate = false

            lastEntry?.let { last ->

                val sameDay = isSameDayStatic(entryDate, last.dateData)

                if (sameDay) {
                    val isSameNumber =
                        !last.stringNumber.isNullOrEmpty() &&
                                PhoneNumberUtils.compare(entryNum, last.stringNumber)

                    val isSameName =
                        !entryName.isNullOrEmpty() &&
                                entryName == last.stringCallName

                    if (isSameNumber || isSameName) {
                        isDuplicate = true
                    }
                }
            }

            if (isDuplicate) {
                lastEntry?.apply {
                    callCount += 1
                    callIds.addAll(entry.callIds)
                }
            } else {

                // 📌 Add Header when category changes
                if (currentCategory != lastDateCategory) {
                    processedList.add(
                        CallHistoryListItems.Header(currentCategory)
                    )
                    lastDateCategory = currentCategory
                }

                entry.callCount = 1

                processedList.add(
                    CallHistoryListItems.Contact(entry)
                )

                lastEntry = entry
            }
        }

        return processedList
    }


    private fun isSameDayStatic(date1: Date?, date2: Date?): Boolean {
        if (date1 == null || date2 == null) return false

        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()

        cal1.time = date1
        cal2.time = date2

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }


}