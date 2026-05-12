package com.example.contactmanager.activities.allContacts

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.adapters.AllContactsAdapter
import com.example.contactmanager.databinding.ActivityAllContactsBinding
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager
import com.example.contactmanager.viewmodels.ContactViewModel
import com.example.contactmanager.viewmodels.FavoriteViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AllContactsActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityAllContactsBinding
    private lateinit var allContactsAdapter: AllContactsAdapter
    private val viewModel: FavoriteViewModel by viewModels()
    private val viewModelContact: ContactViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_all_contacts)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
    }

    override fun onResume() {
        super.onResume()
        if (PermissionManager.hasPermissions(this)) {
            viewModelContact.loadAllContacts()
        }
    }

    private fun initView() {
        binding.onClickHandler = this

        allContactsAdapter = AllContactsAdapter(
            isAllContact = true, onClick = { _, _ ->
            })

        binding.rvAllContacts.adapter = allContactsAdapter
        binding.rvAllContacts.layoutManager = LinearLayoutManager(this)

        val itemTouchHelper = allContactsAdapter.getItemTouchHelper(this)
        itemTouchHelper.attachToRecyclerView(binding.rvAllContacts)

        if (PermissionManager.hasPermissions(this)) {
            viewModelContact.loadAllContacts()
        }

        viewModelContact.allContactList.observe(this) { allContacts ->
            binding.llContactSpaceHolder.visibility =
                if (allContacts.isEmpty()) View.VISIBLE else View.GONE
            binding.rvAllContacts.visibility =
                if (allContacts.isNotEmpty()) View.VISIBLE else View.GONE
            allContactsAdapter.addAll(allContacts)
        }

        binding.edtSearch.addTextChangedListener { editable ->
            val query = editable.toString()
            allContactsAdapter.filter(query)
            binding.llContactSpaceHolder.isVisible = allContactsAdapter.getCurrentList().isEmpty()
            binding.rvAllContacts.isVisible = allContactsAdapter.getCurrentList().isNotEmpty()

        }

        binding.edtSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.edtSearch.text.toString()
                allContactsAdapter.filter(query)
                binding.llContactSpaceHolder.isVisible =
                    allContactsAdapter.getCurrentList().isEmpty()
                binding.rvAllContacts.isVisible = allContactsAdapter.getCurrentList().isNotEmpty()
                binding.edtSearch.clearFocus()
                Common.hideKeyboard(this, v)
                true
            } else {
                false
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.tvDone.id -> {
                val changedContacts = allContactsAdapter.getChangedContacts()
                if (changedContacts.isNotEmpty()) {
                    viewModel.updateFavoriteStatus(changedContacts)
                }
                finish()
            }
        }
    }

}