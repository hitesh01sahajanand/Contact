package com.example.contactmanager.activities.blockNumbers

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.adapters.BlockNumberAdapter
import com.example.contactmanager.databinding.ActivityBlockNumbersBinding
import com.example.contactmanager.models.BlockModel
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager
import com.example.contactmanager.viewmodels.BlockViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BlockNumbersActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityBlockNumbersBinding
    private lateinit var adapter: BlockNumberAdapter
    private val viewModel: BlockViewModel by viewModels()

    private val defaultDialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkDefaultDialer()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_block_numbers)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        checkDefaultDialer()
    }

    private fun checkDefaultDialer() {
        if (!PermissionManager.isDefaultDialer(this)) {
            showSetDefaultDialerDialog()
        }
    }

    private fun showSetDefaultDialerDialog() {
        MaterialAlertDialogBuilder(this).setTitle("Set as Default Dialer")
            .setMessage("To view and manage system-blocked numbers, this app must be set as your default dialer. Would you like to set it now?")
            .setPositiveButton("Set as Default") { _, _ ->
                val intent = PermissionManager.getDefaultDialerIntent(this)
                if (intent != null) {
                    defaultDialerLauncher.launch(intent)
                }
            }.setNegativeButton("Not Now", null).setCancelable(false).show()
    }

    private fun initView() {
        binding.onClickHandler = this

        adapter = BlockNumberAdapter { blockModel ->
            showUnblockDialog(blockModel)
        }
        binding.rvBlockNumbers.adapter = adapter
        binding.rvBlockNumbers.layoutManager = LinearLayoutManager(this)
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.allBlockedNumbers.collect { list ->
                adapter.setBlockList(list)
                binding.llBlockSpaceHolder.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
                binding.cvBlockNumbers.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE

                if (list.isEmpty() && !PermissionManager.isDefaultDialer(this@BlockNumbersActivity)) {
                    Toast.makeText(
                        this@BlockNumbersActivity,
                        "Set as default dialer to sync system blocked numbers",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showUnblockDialog(blockModel: BlockModel) {
        if (!PermissionManager.isDefaultDialer(this)) {
            showSetDefaultDialerDialog()
            return
        }

        Common.alertDialog(
            context = this,
            title = getString(R.string.block_contact),
            description = getString(R.string.you_will_be_able_to_receive_call),
            btnOkay = getString(R.string.unblock),
            onItemClick = {
                viewModel.unblockNumber(blockModel.phoneNumber)
            })
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.ivBack.id -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}