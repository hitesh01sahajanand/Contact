package com.phonecall.dialcontacts.calldialer.activities.newContact

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSAppManage
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSBannerSmall
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSInterDisplayClick
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSNativeDisplay
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSUtilitis
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.adapters.AllAccountAdapter
import com.phonecall.dialcontacts.calldialer.databinding.ActivityNewContactBinding
import com.phonecall.dialcontacts.calldialer.databinding.DialogGoogleAccountsBinding
import com.phonecall.dialcontacts.calldialer.databinding.ItemAddContactFieldBinding
import com.phonecall.dialcontacts.calldialer.models.AccountModel
import com.phonecall.dialcontacts.calldialer.models.ContactDetail
import com.phonecall.dialcontacts.calldialer.models.FullContactData
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.viewmodels.NewContactViewModel
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlin.math.abs

@AndroidEntryPoint
class NewContactActivity : AppCompatActivity(), OnClickHandler {

    companion object {
        private const val MAX_FIELD_COUNT = 10
    }

    private lateinit var binding: ActivityNewContactBinding
    private val viewModel: NewContactViewModel by viewModels()
    private var newDisplayList: ArrayList<AccountModel> = ArrayList()

    private var selectedImageUri: Uri? = null
    private var accountModel: AccountModel? = null
    private var isContactSaved = false
    private var contactId: String? = null

    private var initialSnapshot: FullContactData? = null
    private var initialPhotoUri: Uri? = null
    private var initialAccountKey: String? = null
    private var isInitialSnapshotReady = false
    private var isContactDataLoaded = false

    // Tracking dynamic views
    private val phoneViews = mutableListOf<ItemAddContactFieldBinding>()
    private val emailViews = mutableListOf<ItemAddContactFieldBinding>()
    private val addressViews = mutableListOf<ItemAddContactFieldBinding>()
    private val birthdayViews = mutableListOf<ItemAddContactFieldBinding>()
    private val relationViews = mutableListOf<ItemAddContactFieldBinding>()
    private val websiteViews = mutableListOf<ItemAddContactFieldBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_contact)
        setupWindowInsets()
        setupScrollOnFocus()
        Common.hideSystemUI(this)
        initView()
        loadAds()
    }

    private fun setupWindowInsets() {
        var statusBarHeight = 0
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            if (systemBars.top > 0) {
                statusBarHeight = systemBars.top
            }
            v.setPadding(systemBars.left, statusBarHeight, systemBars.right, systemBars.bottom)
            binding.banner.visibility = if (insets.isVisible(WindowInsetsCompat.Type.ime())) View.GONE else View.VISIBLE
            binding.scrollView.updatePadding(bottom = imeInsets.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.main)
    }

    private val scrollOnFocusListener = View.OnFocusChangeListener { v, hasFocus ->
        if (hasFocus) {
            binding.scrollView.postDelayed({ scrollToView(v) }, 300)
        }
    }

    private fun setupScrollOnFocus() {
        applyEditTextFocusListener(binding.scrollView)
    }

    private fun applyEditTextFocusListener(view: View) {
        if (view is EditText) {
            view.onFocusChangeListener = scrollOnFocusListener
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyEditTextFocusListener(view.getChildAt(i))
            }
        }
    }

    private fun scrollToView(view: View) {
        val scrollView = binding.scrollView
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val scrollLocation = IntArray(2)
        scrollView.getLocationOnScreen(scrollLocation)
        val relativeTop = viewLocation[1] - scrollLocation[1]
        val scrollAmount = relativeTop - scrollView.height / 4
        if (scrollAmount > 0) {
            scrollView.smoothScrollBy(0, scrollAmount)
        }
    }

    private fun loadAds() {
        if (ADSMainClass.getOtherAdsShow()) {
            if (ADSMainClass.getOtherAdsType().equals("native")) {
                ADSNativeDisplay.loadAdmobNativeAdBig(
                    ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_NATIVE),
                    findViewById(R.id.flNativeSmallPlaceholder),
                    findViewById(R.id.shimmer_container_banner),
                    "small",
                    this
                )
            } else {
                ADSBannerSmall.loadAdMobBanner(
                    ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_BANNER),
                    findViewById(R.id.flBannerSmallPlaceholder),
                    findViewById(R.id.shimmer_container_banner),
                    this
                )
            }
        } else {
            findViewById<View>(R.id.shimmer_container_banner).visibility = View.GONE
            findViewById<View>(R.id.flNativeSmallPlaceholder).visibility = View.GONE
            findViewById<View>(R.id.flBannerSmallPlaceholder).visibility = View.GONE
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            startCrop(uri)
        } else {
            Toast.makeText(this, getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show()
        }
    }

    private val cropImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                selectedImageUri = resultUri
                binding.ivContactPhoto.setImageURI(resultUri)
                binding.ivPhoto.isVisible = false
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Log.e("TAG", "cropImageLauncher: $cropError")
        }
    }

    private fun startCrop(uri: Uri) {
        val destinationUri =
            Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        val uCrop = UCrop.of(uri, destinationUri)
        uCrop.withAspectRatio(1f, 1f)
        uCrop.withMaxResultSize(1000, 1000)
        val options = UCrop.Options()
        options.setToolbarColor(ContextCompat.getColor(this, R.color.grey_color))
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.grey_color))
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
        options.setCompressionQuality(90)
        options.setHideBottomControls(false)
        options.setFreeStyleCropEnabled(false)
        options.setToolbarWidgetColor(Color.WHITE)
        uCrop.withOptions(options)
        cropImageLauncher.launch(uCrop.getIntent(this))
    }

    private fun initView() {
        binding.onClickHandler = this

        isContactSaved = intent.getBooleanExtra(Constance.IS_CONTACT_SAVED, false)
        contactId = intent.getStringExtra(Constance.CONTACT_ID)

        if (isContactSaved) {
            binding.tvTitle.text = getString(R.string.edit_contact)
            contactId?.let { id ->
                viewModel.fetchFullContactData(id)
                viewModel.fetchContactAccountName(id)
            }
        } else {
            binding.tvTitle.text = getString(R.string.new_contact)
            val number = intent.getStringExtra(Constance.NUMBER)
            binding.etNumber.setText(number)
        }

        viewModel.getGoogleAccounts()
        viewModel.googleAccount.observe(this) { list ->
            newDisplayList.clear()
            newDisplayList.add(
                AccountModel(
                    name = getString(R.string.device_only),
                    email = "",
                    avtar = Common.generateAvatar(getString(R.string.device_only))
                )
            )
            list.forEach {
                newDisplayList.add(
                    AccountModel(
                        name = it.first,
                        email = it.second,
                        accountType = it.third,
                        avtar = Common.generateAvatar(it.first)
                    )
                )
            }

            if (newDisplayList.isNotEmpty() && !isContactSaved) {
                val defaultAccount =
                    newDisplayList.find { it.accountType == "com.google" } ?: newDisplayList[0]
                updateAccountUI(defaultAccount)
                accountModel = defaultAccount
            }
            trySaveInitialSnapshot()
        }

        viewModel.fullContactData.observe(this) { data ->
            data?.let { populateFields(it) }
        }

        viewModel.contactAccountInfo.observe(this) { accountInfo ->
            val accountName = accountInfo.first
            val accountType = accountInfo.second
            val accName =
                if (accountName != null && accountName.contains("@")) accountName.substringBefore("@") else getString(
                    R.string.device_only
                )
            val email = if (accountName != null && accountName.contains("@")) accountName else ""
            val itemData = AccountModel(
                name = accName,
                email = email,
                accountType = accountType,
                avtar = Common.generateAvatar(accName)
            )
            updateAccountUI(itemData)
            accountModel = itemData
            trySaveInitialSnapshot()
        }

        viewModel.newContactId.observe(this) { newId ->
            if (newId != null) {
                val resultIntent = Intent()
                resultIntent.putExtra(Constance.CONTACT_ID, newId)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }

        viewModel.savedContactMassage.observe(this) { msg ->
            if (msg.contains("System Error")) {
                Common.alertDialog(
                    this,
                    getString(R.string.local_storage_restricted),
                    getString(R.string.your_system_is_set_to_save),
                    getString(R.string.open_settings)
                ) {
                    try {
                        startActivity(Intent(Settings.ACTION_SYNC_SETTINGS))
                    } catch (_: Exception) {
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            } else {
                if (msg.contains("✅")) finish()
            }
        }

        binding.selectBirthday.setOnClickListener {
            showDatePicker { date -> binding.selectBirthday.text = date }
        }


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun trySaveInitialSnapshot() {
        if (isInitialSnapshotReady) return
        if (isContactSaved) {
            if (!isContactDataLoaded || accountModel == null) return
        } else if (accountModel == null) {
            return
        }
        saveInitialSnapshot()
    }

    private fun saveInitialSnapshot() {
        initialSnapshot = collectData()
        initialPhotoUri = selectedImageUri
        initialAccountKey = accountKey(accountModel)
        isInitialSnapshotReady = true
    }

    private fun accountKey(model: AccountModel?): String? =
        model?.let { "${it.email}|${it.name}|${it.accountType}" }

    private fun hasUnsavedChanges(): Boolean {
        if (!isInitialSnapshotReady) return false
        if (collectData() != initialSnapshot) return true
        if (selectedImageUri?.toString() != initialPhotoUri?.toString()) return true
        if (accountKey(accountModel) != initialAccountKey) return true
        return false
    }

    private fun handleBackPress() {
        if (hasUnsavedChanges()) {
            showExitDialog()
        } else {
            loadInterAd()
        }
    }

    fun loadInterAd() {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            this@NewContactActivity,
            ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME),
            { _ ->
                finish()
            })
    }

    private fun updateAccountUI(model: AccountModel) {
        binding.inAccountDesign.tvIdName.text = model.name
        binding.inAccountDesign.cvProfile.isVisible = true
        val color = Common.profileColors[abs(model.name.hashCode()) % Common.profileColors.size]
        binding.inAccountDesign.cvProfile.setCardBackgroundColor(
            ContextCompat.getColor(
                this,
                color
            )
        )
        binding.inAccountDesign.tvContactName.text = model.name.firstOrNull()?.uppercase() ?: ""
    }

    private fun populateFields(data: FullContactData) {
        binding.etName.setText(data.firstName)
        binding.etName1.setText(data.middleName)
        binding.etName2.setText(data.surname)
        binding.etCompany.setText(data.company)
        binding.etNote.setText(data.notes)

        // Photo
        val uriToLoad = data.photoUri?.toUri()

        binding.ivPhoto.isVisible = uriToLoad == null

        uriToLoad?.let { uri ->
            selectedImageUri = uri
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(binding.ivContactPhoto)
        }

        // Phones
        if (data.phones.isNotEmpty()) {
            binding.etNumber.setText(data.phones[0].value)
            binding.txtType.text = getPhoneTypeName(data.phones[0].type, data.phones[0].label)
            for (i in 1 until minOf(data.phones.size, MAX_FIELD_COUNT)) {
                addNewField(
                    binding.containerPhonetype,
                    phoneViews,
                    data.phones[i].value,
                    data.phones[i].type,
                    data.phones[i].label,
                    getString(R.string.number),
                    getString(R.string.phone_type)
                )
            }
        }

        // Emails
        if (data.emails.isNotEmpty()) {
            binding.etEmail.setText(data.emails[0].value)
            binding.txtEmailType.text = getEmailTypeName(data.emails[0].type, data.emails[0].label)
            for (i in 1 until minOf(data.emails.size, MAX_FIELD_COUNT)) {
                addNewField(
                    binding.containerEmail,
                    emailViews,
                    data.emails[i].value,
                    data.emails[i].type,
                    data.emails[i].label,
                    getString(R.string.email),
                    getString(R.string.email_type)
                )
            }
        }

        // Addresses
        if (data.addresses.isNotEmpty()) {
            binding.etAddress.setText(data.addresses[0].value)
            binding.txtAddressType.text =
                getAddressTypeName(data.addresses[0].type, data.addresses[0].label)
            for (i in 1 until minOf(data.addresses.size, MAX_FIELD_COUNT)) {
                addNewField(
                    binding.containerAddress,
                    addressViews,
                    data.addresses[i].value,
                    data.addresses[i].type,
                    data.addresses[i].label,
                    getString(R.string.address),
                    getString(R.string.address_type)
                )
            }
        }

        // Events
        if (data.events.isNotEmpty()) {
            binding.selectBirthday.text = data.events[0].value
            binding.txtBirthdayType.text =
                getEventTypeName(data.events[0].type, data.events[0].label)
            for (i in 1 until minOf(data.events.size, MAX_FIELD_COUNT)) {
                addNewField(
                    binding.containerBirthday,
                    birthdayViews,
                    data.events[i].value,
                    data.events[i].type,
                    data.events[i].label,
                    getString(R.string.date),
                    getString(R.string.birthday_type)
                )
            }
        }

        // Websites
        if (data.websites.isNotEmpty()) {
            binding.etWebsite.setText(data.websites[0])
            for (i in 1 until minOf(data.websites.size, MAX_FIELD_COUNT)) {
                addNewField(
                    binding.containerWebsite,
                    websiteViews,
                    data.websites[i],
                    hint = getString(R.string.website)
                )
            }
        }

        // Relations
        if (data.relations.isNotEmpty()) {
            binding.etRelationperson.setText(data.relations[0].value)
            binding.txtRelationtype.text =
                getRelationTypeName(data.relations[0].type, data.relations[0].label)
            for (i in 1 until minOf(data.relations.size, MAX_FIELD_COUNT)) {
                addNewField(
                    binding.containerReletion,
                    relationViews,
                    data.relations[i].value,
                    data.relations[i].type,
                    data.relations[i].label,
                    getString(R.string.related_person),
                    getString(R.string.relation_type)
                )
            }
        }
        isContactDataLoaded = true
        updateAddButtonVisibility()
        trySaveInitialSnapshot()
    }

    private fun addNewField(
        container: LinearLayout,
        viewList: MutableList<ItemAddContactFieldBinding>,
        value: String = "",
        type: Int = -1,
        label: String? = null,
        hint: String = "",
        typeTitle: String = ""
    ) {
        if (viewList.size >= MAX_FIELD_COUNT - 1) return

        val fieldBinding =
            ItemAddContactFieldBinding.inflate(LayoutInflater.from(this), container, false)
        fieldBinding.etValue.hint = hint
        fieldBinding.etValue.setText(value)

        if (typeTitle.isNotEmpty()) {
            fieldBinding.tvType.text = when (container.id) {
                R.id.container_phonetype -> getPhoneTypeName(
                    if (type == -1) ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE else type,
                    label
                )

                R.id.container_email -> getEmailTypeName(
                    if (type == -1) ContactsContract.CommonDataKinds.Email.TYPE_HOME else type,
                    label
                )

                R.id.container_address -> getAddressTypeName(
                    if (type == -1) ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME else type,
                    label
                )

                R.id.container_birthday -> getEventTypeName(
                    if (type == -1) ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY else type,
                    label
                )

                R.id.container_reletion -> getRelationTypeName(
                    if (type == -1) ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE else type,
                    label
                )

                else -> ""
            }
            fieldBinding.llType.setOnClickListener {
                showTypePopUpForField(
                    fieldBinding,
                    container.id,
                    typeTitle
                )
            }
        } else {
            fieldBinding.llType.isVisible = false
        }

        if (container.id == R.id.container_phonetype || container.id == R.id.container_reletion) {
            fieldBinding.etValue.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        if (container.id == R.id.container_birthday) {
            fieldBinding.etValue.isFocusable = false
            fieldBinding.etValue.setOnClickListener {
                showDatePicker { date -> fieldBinding.etValue.setText(date) }
            }
        }

        fieldBinding.ivRemove.setOnClickListener {
            container.removeView(fieldBinding.root)
            viewList.remove(fieldBinding)
            updateAddButtonVisibility()
        }

        applyEditTextFocusListener(fieldBinding.root)

        container.addView(fieldBinding.root)
        viewList.add(fieldBinding)
        updateAddButtonVisibility()
    }

    private fun updateAddButtonVisibility() {
        binding.newPhoneNumberAdd.isVisible = phoneViews.size < MAX_FIELD_COUNT - 1
        binding.newEmailNumberAdd.isVisible = emailViews.size < MAX_FIELD_COUNT - 1
        binding.newAddressAdd.isVisible = addressViews.size < MAX_FIELD_COUNT - 1
        binding.newBirthdayAdd.isVisible = birthdayViews.size < MAX_FIELD_COUNT - 1
        binding.newRelationAdd.isVisible = relationViews.size < MAX_FIELD_COUNT - 1
        binding.addMoreWebsite.isVisible = websiteViews.size < MAX_FIELD_COUNT - 1
    }

    private fun showTypePopUpForField(
        fieldBinding: ItemAddContactFieldBinding,
        containerId: Int,
        title: String
    ) {
        val types = when (containerId) {
            R.id.container_phonetype -> getPhoneTypeArray()
            R.id.container_email -> getEmailTypeArray()
            R.id.container_address -> getAddressTypeArray()
            R.id.container_birthday -> getBirthdayTypeArray()
            R.id.container_reletion -> getRelationTypeArray()
            else -> emptyArray()
        }
        Common.typePopUp(
            context = this,
            anchorView = fieldBinding.llType,
            title = title,
            typeArray = types,
            selectedType = fieldBinding.tvType.text.toString(),
            onItemClick = { fieldBinding.tvType.text = it }
        )
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        when (view.id) {
            binding.llAccounts.id -> showAccountPopup(binding.inAccountDesign.root, newDisplayList)
            binding.cvAddPhoto.id -> showImagePickerDialog()
            binding.cvSave.id -> saveContact()
            binding.rrnumberType.id -> Common.typePopUp(
                context = this,
                anchorView = binding.rrnumberType,
                title = getString(R.string.phone_type),
                typeArray = getPhoneTypeArray(),
                selectedType = binding.txtType.text.toString(),
                onItemClick = { binding.txtType.text = it }
            )

            binding.rrAdrestype.id -> Common.typePopUp(
                context = this,
                anchorView = binding.rrAdrestype,
                title = getString(R.string.address_type),
                typeArray = getAddressTypeArray(),
                selectedType = binding.txtAddressType.text.toString(),
                onItemClick = { binding.txtAddressType.text = it }
            )

            binding.rremailType.id -> Common.typePopUp(
                context = this,
                anchorView = binding.rremailType,
                title = getString(R.string.email_type),
                typeArray = getEmailTypeArray(),
                selectedType = binding.txtEmailType.text.toString(),
                onItemClick = { binding.txtEmailType.text = it }
            )

            binding.rrRelatedperson.id -> Common.typePopUp(
                context = this,
                anchorView = binding.rrRelatedperson,
                title = getString(R.string.relation_type),
                typeArray = getRelationTypeArray(),
                selectedType = binding.txtRelationtype.text.toString(),
                onItemClick = { binding.txtRelationtype.text = it }
            )

            binding.rvBday.id -> Common.typePopUp(
                context = this,
                anchorView = binding.rvBday,
                title = getString(R.string.birthday_type),
                typeArray = getBirthdayTypeArray(),
                selectedType = binding.txtBirthdayType.text.toString(),
                onItemClick = { binding.txtBirthdayType.text = it }
            )

            binding.newPhoneNumberAdd.id -> addNewField(
                binding.containerPhonetype,
                phoneViews,
                hint = getString(R.string.number),
                typeTitle = getString(R.string.phone_type)
            )

            binding.newEmailNumberAdd.id -> addNewField(
                binding.containerEmail,
                emailViews,
                hint = getString(R.string.email),
                typeTitle = getString(R.string.email_type)
            )

            binding.newAddressAdd.id -> addNewField(
                binding.containerAddress,
                addressViews,
                hint = getString(R.string.address),
                typeTitle = getString(R.string.address_type)
            )

            binding.newBirthdayAdd.id -> addNewField(
                binding.containerBirthday,
                birthdayViews,
                hint = getString(R.string.date),
                typeTitle = getString(R.string.birthday_type)
            )

            binding.newRelationAdd.id -> addNewField(
                binding.containerReletion,
                relationViews,
                hint = getString(R.string.related_person),
                typeTitle = getString(R.string.relation_type)
            )

            binding.addMoreWebsite.id -> addNewField(
                binding.containerWebsite,
                websiteViews,
                hint = getString(R.string.website)
            )

            binding.ivBack.id -> handleBackPress()
        }
    }

    private fun showExitDialog() {
        Common.alertDialog(
            this,
            getString(R.string.leave_page),
            getString(R.string.your_changes_won_t_be_saved),
            getString(R.string.okay)
        ) {
            loadInterAd()
        }
    }

    private fun saveContact() {
        val name = binding.etName.text.toString().trim()
        val number = binding.etNumber.text.toString().trim()
        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.enter_first_name); return
        }
        if (number.isEmpty()) {
            binding.etNumber.error = getString(R.string.enter_number); return
        }
        if (accountModel == null) {
            Toast.makeText(
                this,
                getString(R.string.please_wait_fetching_accounts),
                Toast.LENGTH_SHORT
            ).show(); return
        }

        val contactData = collectData()
        viewModel.saveOrUpdateContact(
            contactData,
            selectedImageUri,
            accountModel!!,
            isContactSaved,
            contactId
        )
    }

    private fun collectData(): FullContactData {
        val phones = mutableListOf<ContactDetail>()
        if (binding.etNumber.text.isNotEmpty()) phones.add(
            ContactDetail(
                binding.etNumber.text.toString(),
                getPhoneTypeInt(binding.txtType.text.toString())
            )
        )
        phoneViews.forEach {
            if (it.etValue.text.isNotEmpty()) phones.add(
                ContactDetail(
                    it.etValue.text.toString(),
                    getPhoneTypeInt(it.tvType.text.toString())
                )
            )
        }

        val emails = mutableListOf<ContactDetail>()
        if (binding.etEmail.text.isNotEmpty()) emails.add(
            ContactDetail(
                binding.etEmail.text.toString(),
                getEmailTypeInt(binding.txtEmailType.text.toString())
            )
        )
        emailViews.forEach {
            if (it.etValue.text.isNotEmpty()) emails.add(
                ContactDetail(
                    it.etValue.text.toString(),
                    getEmailTypeInt(it.tvType.text.toString())
                )
            )
        }

        val addresses = mutableListOf<ContactDetail>()
        if (binding.etAddress.text.isNotEmpty()) addresses.add(
            ContactDetail(
                binding.etAddress.text.toString(),
                getAddressTypeInt(binding.txtAddressType.text.toString())
            )
        )
        addressViews.forEach {
            if (it.etValue.text.isNotEmpty()) addresses.add(
                ContactDetail(
                    it.etValue.text.toString(),
                    getAddressTypeInt(it.tvType.text.toString())
                )
            )
        }

        val events = mutableListOf<ContactDetail>()
        if (binding.selectBirthday.text.isNotEmpty()) events.add(
            ContactDetail(
                binding.selectBirthday.text.toString(),
                getEventTypeInt(binding.txtBirthdayType.text.toString())
            )
        )
        birthdayViews.forEach {
            if (it.etValue.text.isNotEmpty()) events.add(
                ContactDetail(
                    it.etValue.text.toString(),
                    getEventTypeInt(it.tvType.text.toString())
                )
            )
        }

        val websites = mutableListOf<String>()
        if (binding.etWebsite.text.isNotEmpty()) websites.add(binding.etWebsite.text.toString())
        websiteViews.forEach { if (it.etValue.text.isNotEmpty()) websites.add(it.etValue.text.toString()) }

        val relations = mutableListOf<ContactDetail>()
        if (binding.etRelationperson.text.isNotEmpty()) relations.add(
            ContactDetail(
                binding.etRelationperson.text.toString(),
                getRelationTypeInt(binding.txtRelationtype.text.toString())
            )
        )
        relationViews.forEach {
            if (it.etValue.text.isNotEmpty()) relations.add(
                ContactDetail(
                    it.etValue.text.toString(),
                    getRelationTypeInt(it.tvType.text.toString())
                )
            )
        }

        return FullContactData(
            firstName = binding.etName.text.toString(),
            middleName = binding.etName1.text.toString(),
            surname = binding.etName2.text.toString(),
            company = binding.etCompany.text.toString(),
            phones = phones,
            emails = emails,
            addresses = addresses,
            events = events,
            websites = websites,
            relations = relations,
            notes = binding.etNote.text.toString()
        )
    }

    // Type mapping helpers
    private fun getPhoneTypeArray() = arrayOf(
        getString(R.string.mobile),
        getString(R.string.home),
        getString(R.string.work),
        getString(R.string.main),
        getString(R.string.work_fax),
        getString(R.string.home_fax),
        getString(R.string.pager),
        getString(R.string.other)
    )

    private fun getEmailTypeArray() = arrayOf(
        getString(R.string.home),
        getString(R.string.work),
        getString(R.string.other),
        getString(R.string.mobile)
    )

    private fun getAddressTypeArray() =
        arrayOf(getString(R.string.home), getString(R.string.work), getString(R.string.other))

    private fun getBirthdayTypeArray() = arrayOf(
        getString(R.string.birthday),
        getString(R.string.anniversary),
        getString(R.string.other)
    )

    private fun getRelationTypeArray() = arrayOf(
        getString(R.string.assistant),
        getString(R.string.brother),
        getString(R.string.child),
        getString(R.string.domestic_partner),
        getString(R.string.father),
        getString(R.string.friend),
        getString(R.string.manager),
        getString(R.string.mother),
        getString(R.string.partner),
        getString(R.string.parent),
        getString(R.string.referred_by),
        getString(R.string.relative),
        getString(R.string.sister),
        getString(R.string.spouse)
    )

    private fun getPhoneTypeInt(type: String) = when (type) {
        getString(R.string.mobile) -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
        getString(R.string.home) -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
        getString(R.string.work) -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
        getString(R.string.main) -> ContactsContract.CommonDataKinds.Phone.TYPE_MAIN
        getString(R.string.work_fax) -> ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK
        getString(R.string.home_fax) -> ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME
        getString(R.string.pager) -> ContactsContract.CommonDataKinds.Phone.TYPE_PAGER
        else -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
    }

    private fun getEmailTypeInt(type: String) = when (type) {
        getString(R.string.home) -> ContactsContract.CommonDataKinds.Email.TYPE_HOME
        getString(R.string.work) -> ContactsContract.CommonDataKinds.Email.TYPE_WORK
        getString(R.string.mobile) -> ContactsContract.CommonDataKinds.Email.TYPE_MOBILE
        else -> ContactsContract.CommonDataKinds.Email.TYPE_OTHER
    }

    private fun getAddressTypeInt(type: String) = when (type) {
        getString(R.string.home) -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
        getString(R.string.work) -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK
        else -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER
    }

    private fun getEventTypeInt(type: String) = when (type) {
        getString(R.string.birthday) -> ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
        getString(R.string.anniversary) -> ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY
        else -> ContactsContract.CommonDataKinds.Event.TYPE_OTHER
    }

    private fun getRelationTypeInt(type: String) = when (type) {
        getString(R.string.assistant) -> ContactsContract.CommonDataKinds.Relation.TYPE_ASSISTANT
        getString(R.string.brother) -> ContactsContract.CommonDataKinds.Relation.TYPE_BROTHER
        getString(R.string.child) -> ContactsContract.CommonDataKinds.Relation.TYPE_CHILD
        getString(R.string.domestic_partner) -> ContactsContract.CommonDataKinds.Relation.TYPE_DOMESTIC_PARTNER
        getString(R.string.father) -> ContactsContract.CommonDataKinds.Relation.TYPE_FATHER
        getString(R.string.friend) -> ContactsContract.CommonDataKinds.Relation.TYPE_FRIEND
        getString(R.string.manager) -> ContactsContract.CommonDataKinds.Relation.TYPE_MANAGER
        getString(R.string.mother) -> ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER
        getString(R.string.parent) -> ContactsContract.CommonDataKinds.Relation.TYPE_PARENT
        getString(R.string.partner) -> ContactsContract.CommonDataKinds.Relation.TYPE_PARTNER
        getString(R.string.referred_by) -> ContactsContract.CommonDataKinds.Relation.TYPE_REFERRED_BY
        getString(R.string.relative) -> ContactsContract.CommonDataKinds.Relation.TYPE_RELATIVE
        getString(R.string.sister) -> ContactsContract.CommonDataKinds.Relation.TYPE_SISTER
        getString(R.string.spouse) -> ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE
        else -> ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE
    }

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
                else -> R.string.spouse
            }
        )

    private fun showAccountPopup(anchorView: View, list: List<AccountModel>) {
        val popupBinding = DialogGoogleAccountsBinding.inflate(layoutInflater)
        val popupWindow = PopupWindow(
            popupBinding.root,
            anchorView.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popupWindow.elevation = 10f
        val adapter = AllAccountAdapter(onClick = { accountModel ->
            this.accountModel = accountModel
            updateAccountUI(accountModel)
            popupWindow.dismiss()
        })
        adapter.addAll(list)
        popupBinding.rvAccounts.adapter = adapter
        popupBinding.rvAccounts.layoutManager = LinearLayoutManager(this)
        popupWindow.showAsDropDown(anchorView)
    }

    private fun showImagePickerDialog() {
        ADSAppManage.isAppOpenBlocked = true
        pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = java.util.Calendar.getInstance()
        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(date)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
}