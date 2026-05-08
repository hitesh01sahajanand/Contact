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
import com.example.contactmanager.databinding.ItemDetailEntryBinding
import com.example.contactmanager.databinding.MoreDetailDesignBinding
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.models.FullContactData
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

        viewModel.fullContactData.observe(this) { data ->
            data?.let { populateFullContactData(it) }
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
            viewModel.fetchFullContactData(id)
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


            binding.ivCallHistory.id -> {
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

            /*binding.cvStorageLocation.id -> {
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
            }*/

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

                popUpBinding.tvStorageLocation.setOnClickListener {
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
                    popupWindow.dismiss()
                }

            }

        }
    }

    private fun populateFullContactData(data: FullContactData) {
        // Update contactDetail with full info for top level actions
        contactDetail?.let {
            if (data.phones.isNotEmpty()) it.stringNumber = data.phones[0].value
            it.stringCallName = buildString {
                if (data.firstName.isNotEmpty()) append(data.firstName).append(" ")
                if (data.middleName.isNotEmpty()) append(data.middleName).append(" ")
                if (data.surname.isNotEmpty()) append(data.surname)
            }.trim()
            it.stringPhotoUri = data.photoUri
        }

        // Name Formatting Logic
        val name = buildString {
            if (data.firstName.isNotEmpty()) append(data.firstName).append(" ")
            if (data.middleName.isNotEmpty()) append(data.middleName).append(" ")
            if (data.surname.isNotEmpty()) append(data.surname)
        }.trim()
        
        if (name.isNotEmpty()) {
            binding.tvName.text = name
        }

        // Company
        if (data.company.isNotEmpty()) {
            binding.linearLayoutCompany.isVisible = true
            binding.txtCompany.text = data.company
        } else {
            binding.linearLayoutCompany.isVisible = false
        }

        // Phones
        binding.containerPhonetype.removeAllViews()
        if (data.phones.isNotEmpty()) {
            binding.linearLayoutCall.isVisible = true
            binding.txtNumber.text = data.phones[0].value
            binding.txtType.text = getPhoneTypeName(data.phones[0].type, data.phones[0].label)
            
            for (i in 1 until data.phones.size) {
                addDetailEntry(
                    binding.containerPhonetype,
                    data.phones[i].value,
                    getPhoneTypeName(data.phones[i].type, data.phones[i].label),
                    R.drawable.ic_call_info
                ) { Common.actionCall(data.phones[i].value, this) }
            }
        } else {
            binding.linearLayoutCall.isVisible = false
        }

        // Emails
        binding.containerEmail.removeAllViews()
        if (data.emails.isNotEmpty()) {
            binding.linearLayoutEmail.isVisible = true
            binding.txtMail.text = data.emails[0].value
            binding.txtEmailType.text = getEmailTypeName(data.emails[0].type, data.emails[0].label)
            binding.imgMail.setOnClickListener { openEmail(data.emails[0].value) }

            for (i in 1 until data.emails.size) {
                addDetailEntry(
                    binding.containerEmail,
                    data.emails[i].value,
                    getEmailTypeName(data.emails[i].type, data.emails[i].label),
                    null
                ) { openEmail(data.emails[i].value) }
            }
        } else {
            binding.linearLayoutEmail.isVisible = false
        }

        // Addresses
        binding.containerAddress.removeAllViews()
        if (data.addresses.isNotEmpty()) {
            binding.rrLayoutadres.isVisible = true
            binding.txtAddress.text = data.addresses[0].value
            binding.txtAddressType.text = getAddressTypeName(data.addresses[0].type, data.addresses[0].label)
            binding.ivDirection.setOnClickListener { openMap(data.addresses[0].value) }

            for (i in 1 until data.addresses.size) {
                addDetailEntry(
                    binding.containerAddress,
                    data.addresses[i].value,
                    getAddressTypeName(data.addresses[i].type, data.addresses[i].label),
                    null
                ) { openMap(data.addresses[i].value) }
            }
        } else {
            binding.rrLayoutadres.isVisible = false
        }

        // Check if About section should be visible
        val isAboutVisible = data.company.isNotEmpty() || data.websites.isNotEmpty() || 
                            data.events.isNotEmpty() || data.notes.isNotEmpty() || 
                            data.relations.isNotEmpty()
        
        binding.companyAddressView.isVisible = isAboutVisible

        if (isAboutVisible) {
            // Websites
            binding.containerWebsite.removeAllViews()
            if (data.websites.isNotEmpty()) {
                binding.linearLayoutWebsite.isVisible = true
                binding.txtWebsite.text = data.websites[0]
                binding.imgLink.setOnClickListener { openWebsite(data.websites[0]) }

                for (i in 1 until data.websites.size) {
                    addDetailEntry(
                        binding.containerWebsite,
                        data.websites[i],
                        getString(R.string.website),
                        null
                    ) { openWebsite(data.websites[i]) }
                }
            } else {
                binding.linearLayoutWebsite.isVisible = false
            }

            // Birthdays
            binding.containerBirthday.removeAllViews()
            if (data.events.isNotEmpty()) {
                binding.linearLayoutBirthday.isVisible = true
                binding.txtBirthday.text = data.events[0].value
                binding.txtBirthdayType.text = getEventTypeName(data.events[0].type, data.events[0].label)

                for (i in 1 until data.events.size) {
                    addDetailEntry(
                        binding.containerBirthday,
                        data.events[i].value,
                        getEventTypeName(data.events[i].type, data.events[i].label),
                        null
                    ) {}
                }
            } else {
                binding.linearLayoutBirthday.isVisible = false
            }

            // Related Persons
            binding.containerRelatedPerson.removeAllViews()
            if (data.relations.isNotEmpty()) {
                binding.linearLayoutRelatedperson.isVisible = true
                binding.txtRelatedperson.text = data.relations[0].value
                binding.txtTypeperson.text = getRelationTypeName(data.relations[0].type, data.relations[0].label)

                for (i in 1 until data.relations.size) {
                    addDetailEntry(
                        binding.containerRelatedPerson,
                        data.relations[i].value,
                        getRelationTypeName(data.relations[i].type, data.relations[i].label),
                        null
                    ) {}
                }
            } else {
                binding.linearLayoutRelatedperson.isVisible = false
            }

            // Notes
            if (data.notes.isNotEmpty()) {
                binding.linearLayoutNotes.isVisible = true
                binding.txtNotes.text = data.notes
            } else {
                binding.linearLayoutNotes.isVisible = false
            }
        }
    }

    private fun addDetailEntry(
        container: LinearLayout,
        value: String,
        type: String,
        iconRes: Int?,
        onClick: () -> Unit
    ) {
        val itemBinding = ItemDetailEntryBinding.inflate(
            LayoutInflater.from(this),
            container,
            false
        )
        itemBinding.tvValue.text = value
        itemBinding.tvType.text = type
        /*if (iconRes != null) {
            itemBinding.ivAction.isVisible = true
            itemBinding.ivAction.setImageResource(iconRes)
        } else {
            itemBinding.ivAction.isVisible = false
        }*/
        itemBinding.llMain.setOnClickListener { onClick() }
        container.addView(itemBinding.root)
    }

    private fun openMap(address: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW,
                "geo:0,0?q=${android.net.Uri.encode(address)}".toUri())
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW,
                    "geo:0,0?q=${android.net.Uri.encode(address)}".toUri())
                startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(this, "No map application found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openWebsite(url: String) {
        var website = url
        if (!website.startsWith("http://") && !website.startsWith("https://")) {
            website = "http://$website"
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, website.toUri())
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEmail(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri())
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No email application found", Toast.LENGTH_SHORT).show()
        }
    }

    // Type name helpers (Copied from NewContactActivity)
    private fun getPhoneTypeName(type: Int, label: String?) =
        if (type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) label
            ?: getString(R.string.custom) else getString(
            when (type) {
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> R.string.mobile
                ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> R.string.home
                ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> R.string.work
                ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> R.string.main
                ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> R.string.work_fax
                ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> R.string.home_fax
                ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> R.string.pager
                else -> R.string.other
            }
        )

    private fun getEmailTypeName(type: Int, label: String?) =
        if (type == ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM) label
            ?: getString(R.string.custom) else getString(
            when (type) {
                ContactsContract.CommonDataKinds.Email.TYPE_HOME -> R.string.home
                ContactsContract.CommonDataKinds.Email.TYPE_WORK -> R.string.work
                ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> R.string.mobile
                else -> R.string.other
            }
        )

    private fun getAddressTypeName(type: Int, label: String?) =
        if (type == ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM) label
            ?: getString(R.string.custom) else getString(
            when (type) {
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> R.string.home
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> R.string.work
                else -> R.string.other
            }
        )

    private fun getEventTypeName(type: Int, label: String?) =
        if (type == ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM) label
            ?: getString(R.string.custom) else getString(
            when (type) {
                ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY -> R.string.birthday
                ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY -> R.string.anniversary
                else -> R.string.other
            }
        )

    private fun getRelationTypeName(type: Int, label: String?) =
        if (type == ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM) label
            ?: getString(R.string.custom) else getString(
            when (type) {
                ContactsContract.CommonDataKinds.Relation.TYPE_ASSISTANT -> R.string.assistant
                ContactsContract.CommonDataKinds.Relation.TYPE_BROTHER -> R.string.brother
                ContactsContract.CommonDataKinds.Relation.TYPE_CHILD -> R.string.child
                ContactsContract.CommonDataKinds.Relation.TYPE_DOMESTIC_PARTNER -> R.string.domestic_partner
                ContactsContract.CommonDataKinds.Relation.TYPE_FATHER -> R.string.father
                ContactsContract.CommonDataKinds.Relation.TYPE_FRIEND -> R.string.friend
                ContactsContract.CommonDataKinds.Relation.TYPE_MANAGER -> R.string.manager
                ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER -> R.string.mother
                ContactsContract.CommonDataKinds.Relation.TYPE_PARENT -> R.string.parent
                ContactsContract.CommonDataKinds.Relation.TYPE_PARTNER -> R.string.partner
                ContactsContract.CommonDataKinds.Relation.TYPE_REFERRED_BY -> R.string.referred_by
                ContactsContract.CommonDataKinds.Relation.TYPE_RELATIVE -> R.string.relative
                ContactsContract.CommonDataKinds.Relation.TYPE_SISTER -> R.string.sister
                ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE -> R.string.spouse
                else -> R.string.other
            }
        )

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