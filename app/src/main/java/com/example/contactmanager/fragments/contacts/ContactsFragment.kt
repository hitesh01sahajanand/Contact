package com.example.contactmanager.fragments.contacts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.activities.allAccounts.AllAccountsActivity
import com.example.contactmanager.activities.details.ContactsDetailsActivity
import com.example.contactmanager.adapters.AllContactsAdapter
import com.example.contactmanager.databinding.FragmentContactsBinding
import com.example.contactmanager.models.ContactListItem
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager
import com.example.contactmanager.viewmodels.ContactViewModel
import com.example.contactmanager.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.example.contactmanager.activities.newContact.NewContactActivity
import com.example.contactmanager.activities.settings.SettingsActivity
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.SendData
import kotlin.getValue

@AndroidEntryPoint
class ContactsFragment : Fragment(), OnClickHandler {
    private lateinit var binding: FragmentContactsBinding
    private lateinit var allContactsAdapter: AllContactsAdapter
    private val viewModel: ContactViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactsBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (PermissionManager.hasPermissions(requireActivity())) {
            viewModel.loadAllContacts()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && PermissionManager.hasPermissions(requireActivity())) {
            viewModel.loadAllContacts()
        }
    }

    private val accountLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val selectedAccount = data?.getStringExtra("account_name")

                if (selectedAccount == "All Accounts") {
                    viewModel.loadAllContacts()
                } else {
                    selectedAccount?.let {
                        viewModel.getContactsByAccountWithHeaders(it)
                    }
                }
            }
        }


    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        binding.onClickHandler = this
        binding.inHeader.onClickHandler = this
        binding.inHeader.tvTitle.text = requireActivity().getString(R.string.contact)
        binding.inHeader.cvMore.isVisible = true
        binding.inHeader.cvAdd.isVisible = true

        allContactsAdapter = AllContactsAdapter(onClick = { contactModel, clickAction ->
            when (clickAction) {
                Constance.ACTION_CALL -> {
                    Common.actionCall(contactModel.number, requireActivity())

                }

                Constance.ACTION_SEND_MESSAGE -> {
                    contactModel.number?.let {
                        Common.showMessageAppChooser(requireActivity(), it)
                    }
                }

                Constance.ACTION_VIDEO_CALL -> {
                    contactModel.number?.let {
                        Common.showVideoAppChooser(requireActivity(), it)
                    }
                }

                Constance.ACTION_INFO -> {
                    val intent = Intent(requireActivity(), ContactsDetailsActivity::class.java)
                    intent.putExtra(Constance.DATA_FETCH, contactModel.contactId)
                    requireActivity().startActivity(intent)
                }

                Constance.ACTION_ADD_TO_CONTACT -> {
                    val isContactSaved = contactModel.contactId.isNullOrEmpty()
                    val intent = Intent(requireActivity(), NewContactActivity::class.java)
                    intent.putExtra("Number", contactModel.number)
                    intent.putExtra(Constance.IS_CONTACT_SAVED, !isContactSaved)
                    requireActivity().startActivity(intent)
                }

                Constance.ACTION_ADD_TAG -> {

                }
            }
        })
        binding.rvAllContacts.adapter = allContactsAdapter
        binding.rvAllContacts.layoutManager = LinearLayoutManager(requireActivity())

        viewModel.allContactList.observe(viewLifecycleOwner) { allContacts ->
            allContactsAdapter.addAll(allContacts)
            binding.tvNoData.isVisible = allContacts.isEmpty()
        }

        viewModel.googleAccountList.observe(viewLifecycleOwner) { allContacts ->
            allContactsAdapter.addAll(allContacts)
            binding.tvNoData.isVisible = allContacts.isEmpty()
        }

        val letters = ('A'..'Z') + "#"

        letters.forEach { letter ->
            val tv = TextView(context).apply {
                text = letter.toString()
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.grey_color))
                typeface = ResourcesCompat.getFont(context, R.font.fig_tree_semi_bold)
                setPadding(4, 2, 4, 2)
            }

            binding.indexBar.addView(tv)
        }

        binding.indexBar.setOnTouchListener { _, event ->

            val y = event.y
            val itemHeight = binding.indexBar.height / binding.indexBar.childCount
            val index = (y / itemHeight).toInt()

            if (index in 0 until binding.indexBar.childCount) {
                val letter = (binding.indexBar.getChildAt(index) as TextView).text.toString()
                scrollToLetter(letter)
            }
            true
        }

        binding.edtSearch.addTextChangedListener { editable ->
            val query = editable.toString()
            allContactsAdapter.filter(query)
        }

        binding.edtSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.edtSearch.text.toString()
                allContactsAdapter.filter(query)
                binding.edtSearch.clearFocus()
                Common.hideKeyboard(requireActivity(), v)
                true
            } else {
                false
            }
        }

    }

    private fun scrollToLetter(letter: String) {
        val list = allContactsAdapter.getCurrentList()

        for (i in list.indices) {
            val item = list[i]

            if (item is ContactListItem.Header && item.title == letter) {
                binding.rvAllContacts.scrollToPosition(i)
                break
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            /* binding.tvList.id -> {
                 val intent = Intent(requireActivity(), AllAccountsActivity::class.java)
                 accountLauncher.launch(intent)
             }*/

            binding.inHeader.cvMore.id -> {
                val syncContact = requireActivity().getString(R.string.sync_contact)
                val settings = requireActivity().getString(R.string.setting)
                Common.popUpMenu(
                    requireActivity(),
                    binding.inHeader.cvMore,
                    syncContact,
                    settings,
                    option1Click = {
                        Toast.makeText(requireActivity(), syncContact, Toast.LENGTH_SHORT).show()
                    },
                    option2Click = {
                        requireActivity().startActivity(
                            Intent(
                                requireActivity(), SettingsActivity::class.java
                            )
                        )
                    }
                )
            }

            binding.inHeader.cvAdd.id -> {
                val intent = Intent(requireActivity(), NewContactActivity::class.java)
                startActivity(intent)
            }
        }
    }
}