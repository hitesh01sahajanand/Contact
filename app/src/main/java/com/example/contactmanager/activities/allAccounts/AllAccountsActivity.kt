package com.example.contactmanager.activities.allAccounts

import android.content.Intent
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
import com.example.contactmanager.adapters.AllAccountAdapter
import com.example.contactmanager.databinding.ActivityAllAccountsBinding
import com.example.contactmanager.models.AccountModel
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Common.isValidClick
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.viewmodels.NewContactViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AllAccountsActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityAllAccountsBinding

    private val viewModel: NewContactViewModel by viewModels()
    private lateinit var adapter: AllAccountAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_all_accounts)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
    }

    private fun initView() {
        binding.onClickHandler = this

        adapter = AllAccountAdapter(onClick = { accountModel ->
            val intent = Intent().apply {
                putExtra("account_name", accountModel.email)
            }
            setResult(RESULT_OK, intent)
            finish()
        })
        binding.rvAccounts.adapter = adapter
        binding.rvAccounts.layoutManager = LinearLayoutManager(this)



        viewModel.getGoogleAccounts()
        viewModel.googleAccount.observe(this) { list ->

            val accountList = mutableListOf<AccountModel>()
            val name = getString(R.string.all_contacts)

            binding.inAccountDesign.tvIdName.text = name

            list.forEach {
                val nameBitmap = Common.generateAvatar(it.first)
                accountList.add(AccountModel(it.first, it.second, avtar = nameBitmap))
            }

            adapter.addAll(accountList)
        }
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        when (view.id) {
            binding.llBack.id -> {
                onBackPressedDispatcher.onBackPressed()
            }

            binding.tvClose.id -> {
                finish()
            }

            binding.inAccountDesign.root.id -> {
                val intent = Intent().apply {
                    putExtra("account_name", "All Accounts")
                }
                setResult(RESULT_OK, intent)
                finish()
            }

        }
    }
}