package com.phonecall.dialcontacts.calldialer.activities.speedDialSelect

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.adapters.SelectContactAdapter
import com.phonecall.dialcontacts.calldialer.databinding.ActivitySpeedDialSelectBinding
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.viewmodels.ContactViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SpeedDialSelectActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivitySpeedDialSelectBinding
    private lateinit var adapter: SelectContactAdapter
    private val viewModel: ContactViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_speed_dial_select)
        Common.setStableStatusBarInsets(findViewById(R.id.main))
        initView()
    }

    private fun initView() {
        binding.onClickHandler = this

        adapter = SelectContactAdapter(onClick = { contactModel ->
            val resultIntent = Intent()
            resultIntent.putExtra(Constance.DATA_FETCH, contactModel.number)
            resultIntent.putExtra("NAME", contactModel.getFormattedName(false))
            resultIntent.putExtra("PHOTO_URI", contactModel.userThumbnail)
            setResult(RESULT_OK, resultIntent)
            finish()
        })

        binding.rvSelectContact.adapter = adapter
        binding.rvSelectContact.layoutManager = LinearLayoutManager(this)

        viewModel.loadAllContacts()
        viewModel.allContactList.observe(this) { allContacts ->
            adapter.addAll(allContacts)
        }

        binding.edtSearch.addTextChangedListener { editable ->
            val query = editable.toString()
            adapter.filter(query)
        }

        binding.edtSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.edtSearch.text.toString()
                adapter.filter(query)
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
            binding.ivBack.id -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}