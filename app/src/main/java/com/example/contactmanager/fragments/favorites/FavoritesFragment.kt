package com.example.contactmanager.fragments.favorites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.activities.allContacts.AllContactsActivity
import com.example.contactmanager.activities.details.ContactsDetailsActivity
import com.example.contactmanager.adapters.FavoriteAdapter
import com.example.contactmanager.databinding.FragmentFavoritesBinding
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager
import com.example.contactmanager.viewmodels.FavoriteViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FavoritesFragment : Fragment(), OnClickHandler {
    private lateinit var binding: FragmentFavoritesBinding
    private lateinit var favoriteAdapter: FavoriteAdapter
    private val viewModel: FavoriteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (PermissionManager.hasContactPermissions(requireActivity())) {
            viewModel.getAllFavoriteContact()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && PermissionManager.hasContactPermissions(requireActivity())) {
            viewModel.getAllFavoriteContact()
        }
    }

    private fun initView() {
        binding.onClickHandler = this
        binding.inHeader.onClickHandler = this
        binding.inHeader.tvTitle.text = requireActivity().getString(R.string.favorite)
        binding.inHeader.cvAdd.isVisible = true

        viewModel.allFavoriteContacts.observe(viewLifecycleOwner) { favoriteList ->
            if (favoriteList.isNotEmpty()) {
                favoriteAdapter.addAll(favoriteList)
                binding.rvFavorite.isVisible = true
                binding.llFavoriteSpaceHolder.isVisible = false
            } else {
                binding.rvFavorite.isVisible = false
                binding.llFavoriteSpaceHolder.isVisible = true
            }
        }

        favoriteAdapter = FavoriteAdapter(onClick = { contactModel, clickAction ->
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
            }
        })

        binding.rvFavorite.adapter = favoriteAdapter
        binding.rvFavorite.layoutManager = LinearLayoutManager(requireActivity())

        val onBackPressedCallback = object : androidx.activity.OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                favoriteAdapter.clearSelection()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )

        favoriteAdapter.onSelectionModeChanged = { isSelectionMode ->
            onBackPressedCallback.isEnabled = isSelectionMode
            binding.llAllSelection.isVisible = isSelectionMode
            binding.inHeader.root.isVisible = !isSelectionMode
            if (!isSelectionMode) {
                binding.cbSelectAll.isChecked = false
            }
        }

        binding.llAll.setOnClickListener {
            val isChecked = !binding.cbSelectAll.isChecked
            binding.cbSelectAll.isChecked = isChecked

            if (isChecked) {
                favoriteAdapter.selectAll()
            } else {
                favoriteAdapter.deselectAll()
            }
        }

        binding.tvDoneSelection.text = getString(R.string.unfavorite)
        binding.tvDoneSelection.setOnClickListener {
            val selected = favoriteAdapter.getSelectedEntries()
            if (selected.isNotEmpty()) {
                selected.forEach { it.isFavourite = 0 }
                viewModel.updateFavoriteStatus(selected)
                favoriteAdapter.clearSelection()
            } else {
                favoriteAdapter.clearSelection()
            }
        }

        binding.edtSearch.addTextChangedListener { editable ->
            val query = editable.toString()
            favoriteAdapter.filter(query)
            binding.rvFavorite.isVisible = favoriteAdapter.getCurrentList().isNotEmpty()
            binding.llFavoriteSpaceHolder.isVisible = favoriteAdapter.getCurrentList().isEmpty()
        }

        binding.edtSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.edtSearch.text.toString()
                favoriteAdapter.filter(query)
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
            binding.inHeader.cvAdd.id -> {
                requireActivity().startActivity(
                    Intent(
                        requireActivity(), AllContactsActivity::class.java
                    )
                )
            }

        }
    }
    fun clearSearch() {
        if (::binding.isInitialized) {
            binding.edtSearch.setText("")
        }
    }
}