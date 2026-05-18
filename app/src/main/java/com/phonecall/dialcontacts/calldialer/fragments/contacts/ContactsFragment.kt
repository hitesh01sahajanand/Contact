package com.phonecall.dialcontacts.calldialer.fragments.contacts

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.details.ContactsDetailsActivity
import com.phonecall.dialcontacts.calldialer.activities.newContact.NewContactActivity
import com.phonecall.dialcontacts.calldialer.activities.settings.SettingsActivity
import com.phonecall.dialcontacts.calldialer.adapters.AllContactsAdapter
import com.phonecall.dialcontacts.calldialer.databinding.FragmentContactsBinding
import com.phonecall.dialcontacts.calldialer.models.AccountModel
import com.phonecall.dialcontacts.calldialer.models.ContactListItem
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.utils.PermissionManager
import com.phonecall.dialcontacts.calldialer.utils.SharedPreferenceManager
import com.phonecall.dialcontacts.calldialer.viewmodels.ContactViewModel
import dagger.hilt.android.AndroidEntryPoint

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
        if (PermissionManager.hasContactPermissions(requireActivity())) {
            val isMerge = SharedPreferenceManager.getBoolean(
                requireActivity(),
                Constance.MERGE_DUPLICATE_CONTACT,
                false
            )
            allContactsAdapter.setMergeDuplicate(isMerge)
            updateAccountUI()

            val showLoader = allContactsAdapter.itemCount == 0
            viewModel.loadContacts(showLoader = showLoader)
            viewModel.fetchAccountCounts(isMerge)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && PermissionManager.hasContactPermissions(requireActivity())) {
            val isMerge = SharedPreferenceManager.getBoolean(
                requireActivity(),
                Constance.MERGE_DUPLICATE_CONTACT,
                false
            )
            allContactsAdapter.setMergeDuplicate(isMerge)
            updateAccountUI()

            val showLoader = allContactsAdapter.itemCount == 0
            viewModel.loadContacts(showLoader = showLoader)
            viewModel.fetchAccountCounts(isMerge)
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
                    intent.putExtra(Constance.NAME, contactModel.displayName)
                    intent.putExtra(Constance.NUMBER, contactModel.number)
                    intent.putExtra(Constance.PHOTO_URI, contactModel.userThumbnail)
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

        val itemTouchHelper = allContactsAdapter.getItemTouchHelper(requireActivity())
        itemTouchHelper.attachToRecyclerView(binding.rvAllContacts)

        viewModel.allContactList.observe(viewLifecycleOwner) { allContacts ->
            allContactsAdapter.addAll(allContacts)
            updateAccountUI()
            updateVisibility()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { _ ->
            updateVisibility()
        }

        val letters = (('A'..'Z').map { it.toString() } + "#")

        val sizeInPx = resources.getDimension(com.intuit.sdp.R.dimen._11sdp)
        binding.indexBar.removeAllViews()
        letters.forEach { letter ->
            val tv = TextView(context).apply {
                text = letter
                setTextSize(TypedValue.COMPLEX_UNIT_PX, sizeInPx)
                setTextColor(ContextCompat.getColor(context, R.color.main_color))
                typeface = ResourcesCompat.getFont(context, R.font.fig_tree_semi_bold)
                setPadding(4, 2, 4, 2)
            }

            binding.indexBar.addView(tv)
        }

        var isTouchingIndexBar = false

        binding.indexBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    isTouchingIndexBar = true
                    binding.cvIndexBubble.isVisible = true
                    val y = event.y
                    val itemHeight = binding.indexBar.height.toFloat() / binding.indexBar.childCount
                    var index = (y / itemHeight).toInt()

                    if (index < 0) index = 0
                    if (index >= binding.indexBar.childCount) index =
                        binding.indexBar.childCount - 1

                    val textView = binding.indexBar.getChildAt(index) as TextView
                    val letter = textView.text.toString()
                    binding.tvIndexBubble.text = letter

                    val childCenterY = textView.top + (textView.height / 2)
                    val middleOfIndexBar = binding.indexBar.height / 2f
                    binding.cvIndexBubble.translationY = childCenterY - middleOfIndexBar

                    scrollToLetter(letter)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isTouchingIndexBar = false
                    binding.cvIndexBubble.postDelayed({
                        if (!isTouchingIndexBar) {
                            binding.cvIndexBubble.isVisible = false
                        }
                    }, 500)
                }
            }
            true
        }

        binding.rvAllContacts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE && !isTouchingIndexBar) {
                    binding.cvIndexBubble.postDelayed({
                        if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE && !isTouchingIndexBar) {
                            binding.cvIndexBubble.isVisible = false
                        }
                    }, 500)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isTouchingIndexBar) return

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (firstVisibleItemPosition != RecyclerView.NO_POSITION) {
                    val list = allContactsAdapter.getCurrentList()
                    if (firstVisibleItemPosition < list.size) {
                        val item = list[firstVisibleItemPosition]
                        val letter = when (item) {
                            is ContactListItem.Header -> item.title
                            is ContactListItem.Contact -> item.data.displayName?.firstOrNull()
                                ?.uppercaseChar()?.toString() ?: "#"

                            else -> null
                        }

                        if (letter != null) {
                            val index = letters.indexOf(letter)
                            if (index != -1) {
                                val textView = binding.indexBar.getChildAt(index) as? TextView
                                if (textView != null) {
                                    binding.tvIndexBubble.text = letter
                                    val childCenterY = textView.top + (textView.height / 2)
                                    val middleOfIndexBar = binding.indexBar.height / 2f
                                    binding.cvIndexBubble.translationY =
                                        childCenterY - middleOfIndexBar
                                }
                            }
                        }
                    }
                }
            }
        })

        binding.edtSearch.addTextChangedListener { editable ->
            val query = editable.toString()
            allContactsAdapter.filter(query)
            updateVisibility()
        }

        binding.edtSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.edtSearch.text.toString()
                allContactsAdapter.filter(query)
                binding.edtSearch.clearFocus()
                Common.hideKeyboard(requireActivity(), v)
                updateVisibility()
                true
            } else {
                false
            }
        }

    }

    private fun updateVisibility() {
        val isLoading = viewModel.isLoading.value ?: false
        val itemCount = allContactsAdapter.itemCount

        if (isLoading && itemCount == 0) {
            binding.pbLoading.isVisible = true
            binding.clContacts.isVisible = false
            binding.llContactSpaceHolder.isVisible = false
        } else {
            binding.pbLoading.isVisible = false
            binding.clContacts.isVisible = itemCount > 0
            binding.llContactSpaceHolder.isVisible = itemCount == 0
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
        if (!isValidClick()) return
        when (view.id) {

            binding.inHeader.cvMore.id -> {
                val syncContact = requireActivity().getString(R.string.sync_contact)
                val settings = requireActivity().getString(R.string.setting)
                Common.popUpMenu(
                    requireActivity(),
                    binding.inHeader.cvMore,
                    syncContact,
                    settings,
                    option1Click = {
                        viewModel.loadContacts()
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

            binding.cvAccounts.id -> {
                val accountList = mutableListOf<AccountModel>()
                val counts = viewModel.accountCounts.value ?: emptyMap()

                accountList.add(
                    AccountModel(
                        requireActivity().getString(R.string.all),
                        requireActivity().getString(R.string.all_accounts),
                        count = counts["All Accounts"] ?: 0
                    )
                )
                accountList.add(
                    AccountModel(
                        requireActivity().getString(R.string.device),
                        requireActivity().getString(R.string.device_only),
                        count = counts["Device Only"] ?: 0
                    )
                )

                val existingEmails = mutableSetOf<String>()
                val cursor = requireActivity().contentResolver.query(
                    android.provider.ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(android.provider.ContactsContract.RawContacts.ACCOUNT_NAME),
                    "${android.provider.ContactsContract.RawContacts.ACCOUNT_TYPE} = ?",
                    arrayOf("com.google"),
                    null
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val email = it.getString(0)
                        if (!email.isNullOrEmpty()) {
                            existingEmails.add(email)
                        }
                    }
                }

                for (email in existingEmails) {
                    val name = email.substringBefore("@")
                    accountList.add(
                        AccountModel(
                            name,
                            email,
                            count = counts[email] ?: 0
                        )
                    )
                }

                Common.contactPopUpMenu(
                    requireActivity(),
                    binding.cvAccounts,
                    accountList
                ) { email ->
                    val selectedName = accountList.find { it.email == email }?.name ?: requireActivity().getString(R.string.all)
                    val tvTitle = binding.cvAccounts.findViewById<TextView>(R.id.tv_title)
                    tvTitle?.text = selectedName

                    viewModel.currentSelectedAccount = email
                    viewModel.loadContacts()
                }
            }
        }
    }

    private fun updateAccountUI() {
        val email = viewModel.currentSelectedAccount
        val tvTitle = binding.cvAccounts.findViewById<TextView>(R.id.tv_title)

        val title = when (email) {
            "All Accounts" -> "All"
            "Device Only" -> "Device"
            else -> email.substringBefore("@")
        }
        tvTitle?.text = title
    }

    fun clearSearch() {
        if (::binding.isInitialized) {
            binding.edtSearch.setText("")
        }
    }
}