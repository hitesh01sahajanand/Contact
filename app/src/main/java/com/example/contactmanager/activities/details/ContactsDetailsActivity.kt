package com.example.contactmanager.activities.details

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.contactmanager.R
import com.example.contactmanager.activities.history.HistoryActivity
import com.example.contactmanager.activities.newContact.NewContactActivity
import com.example.contactmanager.databinding.ActivityContactsDetailsBinding
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.SendData
import com.example.contactmanager.viewmodels.ContactDetailsViewModel
import com.example.contactmanager.viewmodels.FavoriteViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContactsDetailsActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityContactsDetailsBinding
    private val viewModel: ContactDetailsViewModel by viewModels()
    private val favoriteViewModel: FavoriteViewModel by viewModels()
    private var contactDetail: CallLogEntry? = null
    private var contactId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_contacts_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel.contactData.observe(this) {
            contactDetail = it
            initView()
        }
    }

    override fun onResume() {
        super.onResume()
        contactId = intent.getStringExtra(Constance.DATA_FETCH)
        contactId?.let { id ->
            viewModel.getUpdatedContact(id)
        }
    }

    private fun initView() {
        binding.onClickHandler = this

        contactDetail?.let {
            val name = if (it.stringCallName.isNullOrEmpty()) it.stringNumber else it.stringCallName
            binding.tvName.text = name
            binding.tvNumber.text = it.stringNumber
            if (!it.stringPhotoUri.isNullOrEmpty()) {
                Glide.with(this)
                    .load(it.stringPhotoUri)
                    .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis().toString()))
                    .into(binding.ivContactPhoto)
            } else {
                binding.ivContactPhoto.setImageBitmap(Common.generateAvatar(name ?: ""))
            }

            it.contactId?.let { id ->
                binding.ivFavorite.setImageResource(
                    if (isContactFavorite(
                            this,
                            id
                        )
                    ) R.drawable.ic_selected_star else R.drawable.ic_favorite
                )
            }
            val isSaved = !it.contactId.isNullOrEmpty()
            binding.llEdit.isVisible = isSaved
            binding.llShare.isVisible = isSaved
            binding.llFavorite.isVisible = isSaved

        }
    }

    private val editContactLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val newId = result.data?.getStringExtra(Constance.CONTACT_ID)
            if (newId != null) {
                // 🔥 Update both the local variable AND the intent
                contactId = newId
                intent.putExtra(Constance.DATA_FETCH, newId)
                viewModel.getUpdatedContact(newId)
            } else {
                // Refresh current ID if it was just a simple update
                contactId?.let { viewModel.getUpdatedContact(it) }
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.ivMessage.id -> {
                contactDetail?.stringNumber?.let {
                    Common.showMessageAppChooser(this, it)
                }
            }

            binding.ivBack.id -> {
                onBackPressedDispatcher.onBackPressed()
            }

            binding.ivCall.id -> {
                contactDetail?.let {
                    it.stringNumber?.let { number -> Common.actionCall(number, this) }
                }
            }

            binding.ivVideoCall.id -> {
                contactDetail?.stringNumber?.let {
                    Common.showVideoAppChooser(this, it)
                }
            }


            binding.cvHistory.id -> {
                contactDetail?.let {
                    val intent = Intent(this, HistoryActivity::class.java)
                    intent.putExtra("Number", it.stringNumber)
                    startActivity(intent)
                }
            }

            binding.llFavorite.id -> {
                contactDetail?.let {
                    it.contactId?.let { id ->
                        val currentFavorite = isContactFavorite(this, id)
                        val newFavorite = !currentFavorite
                        binding.ivFavorite.setImageResource(
                            if (newFavorite) R.drawable.ic_selected_star else R.drawable.ic_favorite
                        )
                        favoriteViewModel.addToFavoriteUnFavorite(id, newFavorite)
                    }
                }
            }

            binding.llEdit.id -> {
                contactDetail?.let {
                    val intent = Intent(this, NewContactActivity::class.java)
                    intent.putExtra("Number", it.stringNumber)
                    intent.putExtra(Constance.IS_CONTACT_SAVED, !it.contactId.isNullOrEmpty())
                    intent.putExtra(Constance.CONTACT_ID, it.contactId)
                    editContactLauncher.launch(intent)
                }
            }

            binding.llShare.id -> {
                contactDetail?.let {
                    shareContact(this, it.contactId)
                }
            }

            binding.llMore.id -> {

            }

        }
    }

    fun shareContact(context: Context, contactId: String?) {
        if (contactId.isNullOrEmpty()) return

        try {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            val projection = arrayOf(ContactsContract.Contacts.LOOKUP_KEY)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)

            var lookupKey: String? = null
            cursor?.use {
                if (it.moveToFirst()) {
                    lookupKey =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY))
                }
            }

            if (lookupKey == null) {
                Toast.makeText(context, "Contact not found", Toast.LENGTH_SHORT).show()
                return
            }

            val vcardUri =
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"
                putExtra(Intent.EXTRA_STREAM, vcardUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share Contact"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to share contact", Toast.LENGTH_SHORT).show()
        }
    }

    fun isContactFavorite(context: Context, contactId: String): Boolean {

        if (contactId.isEmpty()) return false

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        return try {

            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.STARRED),
                "${ContactsContract.Contacts._ID}=?",
                arrayOf(contactId),
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val isStarred = it.getInt(
                        it.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)
                    )
                    return isStarred == 1
                }
            }

            false

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}