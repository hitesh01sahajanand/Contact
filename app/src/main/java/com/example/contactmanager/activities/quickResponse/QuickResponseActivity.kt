package com.example.contactmanager.activities.quickResponse

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.adapters.QuickResponseAdapter
import com.example.contactmanager.databinding.ActivityQuickResponseBinding
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.viewmodels.QuickResponseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuickResponseActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityQuickResponseBinding
    private lateinit var adapter: QuickResponseAdapter
    private val viewModel: QuickResponseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quick_response)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel.initializeDefaultMessages()
        initView()
        observeData()
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
        })

        binding.rvQuickResponse.adapter = adapter
        binding.rvQuickResponse.layoutManager = LinearLayoutManager(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.ivBack.id -> {
                onBackPressedDispatcher.onBackPressed()
            }

            binding.llQuickResponse.id -> {
                Common.editQuickMessageDialog(this, onItemClick = { message ->
                    if (message.isNotBlank()) {
                        viewModel.insertMessage(message.trim())
                    }
                })
            }
        }
    }
}