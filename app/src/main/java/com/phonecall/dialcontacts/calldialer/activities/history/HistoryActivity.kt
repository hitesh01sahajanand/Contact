package com.phonecall.dialcontacts.calldialer.activities.history

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.newContact.NewContactActivity
import com.phonecall.dialcontacts.calldialer.adapters.HistoryAdapter
import com.phonecall.dialcontacts.calldialer.databinding.ActivityHistoryBinding
import com.phonecall.dialcontacts.calldialer.models.HistoryListItem
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.viewmodels.ContactDetailsViewModel
import com.phonecall.dialcontacts.calldialer.viewmodels.RecentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class HistoryActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private val viewModel: ContactDetailsViewModel by viewModels()
    private val recentViewModel: RecentViewModel by viewModels()
    private var number: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
    }

    private fun initView() {
        binding.onClickHandler = this

        adapter = HistoryAdapter()
        binding.rvHistory.adapter = adapter
        binding.rvHistory.layoutManager = LinearLayoutManager(this)

        number = intent.getStringExtra(Constance.NUMBER).toString()

        val isContactSaved = Common.isNumberSaved(this, number)
        binding.llAdd.isVisible = !isContactSaved
        binding.llTag.isVisible = !isContactSaved

        val isBlockNumber = Common.isNumberBlocked(this, number)
        binding.tvBlock.text =
            if (isBlockNumber) getString(R.string.unblock) else getString(R.string.block)

        if (number.isNotEmpty()) {
            viewModel.getNumberToHistory(number, 0, 1000)
            viewModel.contactHistory.observe(this) { list ->

                binding.llHistorySpaceHolder.isVisible = list.isEmpty()
                binding.rvHistory.isVisible = list.isNotEmpty()

                val groupedList = mutableListOf<HistoryListItem>()
                var lastDate = ""

                list.forEach { entry ->
                    val dateStr = Common.formatHeaderDate(entry.dateData)
                    if (dateStr != lastDate) {
                        groupedList.add(HistoryListItem.Header(dateStr))
                        lastDate = dateStr
                    }
                    groupedList.add(HistoryListItem.History(entry))
                }

                adapter.addAll(groupedList)
            }
        }
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        when (view.id) {
            binding.ivBack.id -> {
                onBackPressedDispatcher.onBackPressed()
            }

            binding.llAdd.id -> {
                if (number.isNotEmpty()) {
                    val intent = Intent(this, NewContactActivity::class.java)
                    intent.putExtra(Constance.NUMBER, number)
                    intent.putExtra(Constance.IS_CONTACT_SAVED, false)
                    startActivity(intent)
                }
            }

            binding.llTag.id -> {
                lifecycleScope.launch(Dispatchers.Main) {
                    val tag = recentViewModel.getTag(number)

                    Common.saveTag(this@HistoryActivity, tag, onItemClick = { tag ->
                        recentViewModel.saveTag(number, tag)
                        // Reload history to show the tag
                        Handler(mainLooper).postDelayed({
                            viewModel.getNumberToHistory(number, 0, 1000)
                        }, 500)
                    })
                }
            }

            binding.llShare.id -> {
                Common.shareContact(this, number)
            }

            binding.llBlock.id -> {
                Common.ensureDefaultDialer(this) {
                    if (Common.isNumberBlocked(this, number)) {
                        Common.alertDialog(
                            context = this,
                            title = getString(R.string.unblock_contact),
                            description = getString(R.string.you_will_be_able_to_receive_call),
                            btnOkay = getString(R.string.unblock),
                            isImageVisible = true,
                            onItemClick = {
                                recentViewModel.unblockNumber(number)
                                binding.tvBlock.text = getString(R.string.block)
                            })
                    } else {
                        Common.alertDialog(
                            context = this,
                            title = getString(R.string.block_contact),
                            description = getString(R.string.you_will_be_able_to_receive_call),
                            btnOkay = getString(R.string.block),
                            isImageVisible = true,
                            onItemClick = {
                                recentViewModel.blockNumber(number)
                                binding.tvBlock.text = getString(R.string.unblock)
                            })
                    }
                }
            }

            binding.llDelete.id -> {
                if (adapter.itemCount != 0) {
                    Common.alertDialog(
                        this,
                        getString(R.string.delete_history),
                        getString(R.string.are_you_sure_you_want_to_delete_all_history),
                        getString(R.string.delete),
                        onItemClick = {
                            viewModel.deleteCallHistoryForNumber(number)
                            Toast.makeText(
                                this,
                                getString(R.string.history_deleted),
                                Toast.LENGTH_SHORT
                            ).show()
                            Handler(mainLooper).postDelayed({
                                viewModel.getNumberToHistory(number, 0, 1000)
                            }, 1000)
                        })
                }
            }
        }
    }
}