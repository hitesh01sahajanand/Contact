package com.phonecall.dialcontacts.calldialer.activities.allContacts

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
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.adapters.AllContactsAdapter
import com.phonecall.dialcontacts.calldialer.databinding.ActivityAllContactsBinding
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.utils.PermissionManager
import com.phonecall.dialcontacts.calldialer.viewmodels.ContactViewModel
import com.phonecall.dialcontacts.calldialer.viewmodels.FavoriteViewModel
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
        if (!isValidClick()) return
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