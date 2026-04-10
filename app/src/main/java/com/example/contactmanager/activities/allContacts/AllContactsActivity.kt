package com.example.contactmanager.activities.allContacts

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.adapters.AllContactsAdapter
import com.example.contactmanager.databinding.ActivityAllContactsBinding
import com.example.contactmanager.models.ContactListItem
import com.example.contactmanager.utils.CommonDialog
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager
import com.example.contactmanager.viewmodels.ContactViewModel
import com.example.contactmanager.viewmodels.FavoriteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

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

    private fun initView() {

        binding.onClickHandler = this

        allContactsAdapter =
            AllContactsAdapter(onClick = { itemData: ContactListItem, position: Int ->
                if (itemData is ContactListItem.Contact) {

                    val isFavorite = itemData.data.isFavourite == 1

                    if (isFavorite) {
                        CommonDialog.showDefaultDialerDialog(
                            this,
                            Constance.REMOVE_TO_FAVORITE,
                            onclick = {
                                viewModel.addToFavoriteUnFavorite(
                                    itemData.data.contactId.toString(),
                                    false
                                )

                                itemData.data.isFavourite = 0
                                allContactsAdapter.notifyItemChanged(position)

                                Toast.makeText(this, "Remove to Favorite", Toast.LENGTH_SHORT)
                                    .show()
                                finish()
                            }
                        )
                    } else {
                        CommonDialog.showDefaultDialerDialog(
                            this,
                            Constance.ADD_TO_FAVORITE,
                            onclick = {
                                viewModel.addToFavoriteUnFavorite(
                                    itemData.data.contactId.toString(),
                                    true
                                )

                                itemData.data.isFavourite = 1
                                allContactsAdapter.notifyItemChanged(position)

                                Toast.makeText(this, "Add to Favorite", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        )
                    }
                }
            })

        binding.rvAllContacts.adapter = allContactsAdapter
        binding.rvAllContacts.layoutManager = LinearLayoutManager(this)

        if (PermissionManager.hasPermissions(this)) {
            viewModelContact.loadAllContacts()
        }

        viewModelContact.allContactList.observe(this) { allContacts ->
            allContactsAdapter.addAll(allContacts)
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                allContactsAdapter.filter(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.tvCancel.id -> {
                finish()
            }
        }
    }

}