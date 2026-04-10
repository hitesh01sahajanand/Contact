package com.example.contactmanager.fragments.favorites

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.activities.allContacts.AllContactsActivity
import com.example.contactmanager.adapters.FavoriteAdapter
import com.example.contactmanager.databinding.FragmentFavoritesBinding
import com.example.contactmanager.utils.Common
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.onClickHandler = this
        binding.inHeader.onClickHandler = this
        binding.inHeader.tvTitle.text = requireActivity().getString(R.string.favorite)
        binding.inHeader.cvAdd.isVisible = true

        viewModel.allFavoriteContacts.observe(requireActivity()) { favoriteList ->
            if (favoriteList.isNotEmpty()) {
                favoriteAdapter.addAll(favoriteList)
                binding.rvFavorite.isVisible = true
                binding.llFavoriteSpaceHolder.isVisible = false
            } else {
                binding.rvFavorite.isVisible = false
                binding.llFavoriteSpaceHolder.isVisible = true
            }
        }

        favoriteAdapter = FavoriteAdapter(/*onDeleteClick = { dataModel ->
            if (dataModel.isFavourite == 0) {
                viewModel.addToFavoriteUnFavorite(dataModel.contactId.toString(), true)
            } else {
                viewModel.addToFavoriteUnFavorite(dataModel.contactId.toString(), false)
            }
            favoriteAdapter.remove(dataModel)
        }*/
        )

        binding.rvFavorite.adapter = favoriteAdapter
        binding.rvFavorite.layoutManager = LinearLayoutManager(requireActivity())

        binding.edtSearch.addTextChangedListener { editable ->
            val query = editable.toString()
            favoriteAdapter.filter(query)
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
                        requireActivity(),
                        AllContactsActivity::class.java
                    )
                )
            }

        }
    }

    override fun onResume() {
        super.onResume()
        if (PermissionManager.hasPermissions(requireActivity())) {
            viewModel.getAllFavoriteContact()
        }

    }
}