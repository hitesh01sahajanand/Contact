package com.example.contactmanager.activities.history

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.adapters.HistoryAdapter
import com.example.contactmanager.databinding.ActivityHistoryBinding
import com.example.contactmanager.models.HistoryListItem
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.viewmodels.ContactDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class HistoryActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private val viewModel: ContactDetailsViewModel by viewModels()


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

        val number = intent.getStringExtra("Number")
        if (!number.isNullOrEmpty()) {
            viewModel.getNumberToHistory(number, 0, 1000)
            viewModel.contactHistory.observe(this) { list ->
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
        when (view.id) {
            binding.ivBack.id -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}