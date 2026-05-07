package com.example.contactmanager.activities.details

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
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
import com.example.contactmanager.activities.setRingtone.SetRingtoneActivity
import com.example.contactmanager.activities.storageLocation.StorageLocationActivity
import com.example.contactmanager.databinding.ActivityContactsDetailsBinding
import com.example.contactmanager.databinding.MoreDetailDesignBinding
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.viewmodels.ContactDetailsViewModel
import com.example.contactmanager.viewmodels.FavoriteViewModel
import com.example.contactmanager.viewmodels.RecentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContactsDetailsActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityContactsDetailsBinding
    private val viewModel: ContactDetailsViewModel by viewModels()
    private val favoriteViewModel: FavoriteViewModel by viewModels()
    private val recentViewModel: RecentViewModel by viewModels()
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

        // 👉 Display initial data from intent for instant loading
        showInitialData()
    }

    private fun showInitialData() {
        val name = intent.getStringExtra(Constance.NAME)
        val number = intent.getStringExtra(Constance.NUMBER)
        val photoUri = intent.getStringExtra(Constance.PHOTO_URI)

        if (!name.isNullOrEmpty() || !number.isNullOrEmpty()) {
            contactDetail = CallLogEntry(
                stringNumber = number,
                stringCallName = name,
                stringPhotoUri = photoUri,
                contactId = intent.getStringExtra(Constance.DATA_FETCH)
            )
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
                binding.tvFirstName.isVisible = false
                binding.ivContactPhoto.isVisible = true
                Glide.with(this)
                    .load(it.stringPhotoUri)
                    .signature(
                        com.bumptech.glide.signature.ObjectKey(
                            System.currentTimeMillis().toString()
                        )
                    )
                    .into(binding.ivContactPhoto)
            } else {
                val color = Common.profileColors[1 % Common.profileColors.size]
                binding.cvAddPhoto.setCardBackgroundColor(
                    ContextCompat.getColor(this, color)
                )
                val firstChar = name?.trim()
                    ?.split(" ")
                    ?.filter { data -> data.isNotEmpty() }
                    ?.take(2)
                    ?.map { data -> data[0].uppercaseChar() }
                    ?.joinToString("")

                binding.tvFirstName.text = firstChar

                binding.ivContactPhoto.isVisible = false
                binding.tvFirstName.isVisible = true
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
                    Common.shareContact(this, it.stringNumber)
                }
            }

            binding.cvStorageLocation.id -> {
                val intent = Intent(this, StorageLocationActivity::class.java)
                contactDetail?.let {
                    intent.putExtra("contact_id", it.contactId)
                    val name =
                        if (it.stringCallName.isNullOrEmpty()) it.stringNumber else it.stringCallName
                    intent.putExtra("contact_name", name)
                    intent.putExtra("contact_number", it.stringNumber)
                    intent.putExtra("contact_photo_uri", it.stringPhotoUri)
                }
                startActivity(intent)
            }

            binding.llMore.id -> {

                val popUpBinding = MoreDetailDesignBinding.inflate(
                    LayoutInflater.from(this),
                    null,
                    false
                )


                val popupWindow = PopupWindow(
                    popUpBinding.root,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    true
                )

                popupWindow.elevation = 10f

                popUpBinding.root.measure(
                    View.MeasureSpec.UNSPECIFIED,
                    View.MeasureSpec.UNSPECIFIED
                )

                val popupWidth = popUpBinding.root.measuredWidth
                val popupHeight = popUpBinding.root.measuredHeight
                val margin = (12 * resources.displayMetrics.density).toInt()
                val xOffset = binding.llMore.width - popupWidth - margin
                val yOffset = -binding.llMore.height - popupHeight
                popupWindow.showAsDropDown(
                    binding.llMore,
                    xOffset,
                    yOffset
                )

                popUpBinding.tvDelete.setOnClickListener {
                    contactDetail?.let { model ->
                        model.contactId?.let { id ->
                            Common.alertDialog(
                                context = this,
                                title = getString(R.string.delete_contact),
                                description = getString(R.string.are_you_sure_you_want_to_delete_this_contact),
                                btnOkay = getString(R.string.delete),
                                onItemClick = {
                                    viewModel.deleteContact(id)
                                    finish()
                                }
                            )
                        }
                    }
                    popupWindow.dismiss()
                }

                contactDetail?.let {
                    val isBlockNumber = Common.isNumberBlocked(this, it.stringNumber)
                    popUpBinding.tvBlock.text =
                        if (isBlockNumber) getString(R.string.unblock) else getString(R.string.block)
                }

                popUpBinding.tvBlock.setOnClickListener {
                    contactDetail?.let { model ->
                        Common.ensureDefaultDialer(this) {
                            if (Common.isNumberBlocked(this, model.stringNumber)) {
                                Common.alertDialog(
                                    context = this,
                                    title = getString(R.string.unblock_contact),
                                    description = getString(R.string.you_will_be_able_to_receive_call),
                                    btnOkay = getString(R.string.unblock),
                                    isImageVisible = true,
                                    onItemClick = {
                                        recentViewModel.unblockNumber(model.stringNumber)
                                        popUpBinding.tvBlock.text = getString(R.string.block)
                                    })
                            } else {
                                Common.alertDialog(
                                    context = this,
                                    title = getString(R.string.block_contact),
                                    description = getString(R.string.you_will_be_able_to_receive_call),
                                    btnOkay = getString(R.string.block),
                                    isImageVisible = true,
                                    onItemClick = {
                                        recentViewModel.blockNumber(model.stringNumber)
                                        popUpBinding.tvBlock.text = getString(R.string.unblock)
                                    })
                            }
                        }
                    }
                    popupWindow.dismiss()
                }

                popUpBinding.tvChangeRingtone.setOnClickListener {
                    if (Settings.System.canWrite(this)) {
                        startActivity(Intent(this, SetRingtoneActivity::class.java))
                    } else {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = "package:$packageName".toUri()
                        startActivity(intent)
                        Toast.makeText(
                            this,
                            getString(R.string.please_allow_modify_system_settings_to_change_ringtone),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    popupWindow.dismiss()
                }

            }

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