package com.example.contactmanager.activities.newContact

import android.Manifest
import android.app.AlertDialog
import android.content.ContentProviderOperation
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.contactmanager.R
import com.example.contactmanager.adapters.AllAccountAdapter
import com.example.contactmanager.databinding.ActivityNewContactBinding
import com.example.contactmanager.databinding.DialogGoogleAccountsBinding
import com.example.contactmanager.databinding.ItemEmailDesignBinding
import com.example.contactmanager.databinding.ItemPhoneDesignBinding
import com.example.contactmanager.models.AccountModel
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.SendData
import com.example.contactmanager.viewmodels.ContactDetailsViewModel
import com.example.contactmanager.viewmodels.HomeViewModel
import com.example.contactmanager.viewmodels.NewContactViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.getValue
import kotlin.text.get
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.graphics.drawable.toDrawable
import kotlin.math.abs
import com.yalantis.ucrop.UCrop


@AndroidEntryPoint
class NewContactActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityNewContactBinding
    private val viewModel: NewContactViewModel by viewModels()
    private val contactDetailViewModel: ContactDetailsViewModel by viewModels()
    private var newDisplayList: ArrayList<AccountModel> = ArrayList()

    var selectedImageUri: Uri? = null
    var accountModel: AccountModel? = null
    var isContactSaved = false
    var contactId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_contact)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
    }


    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val uri = bitmapToUri(it)
            startCrop(uri)
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            startCrop(uri)
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
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
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Log.e("TAG", "cropImageLauncher: $cropError")
        }
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        val uCrop = UCrop.of(uri, destinationUri)
        uCrop.withAspectRatio(1f, 1f)
        uCrop.withMaxResultSize(1000, 1000)
        val options = UCrop.Options()
        options.setToolbarColor(ContextCompat.getColor(this, R.color.grey_color))
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.grey_color))
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
        options.setCompressionQuality(90)
        options.setHideBottomControls(false)
        options.setFreeStyleCropEnabled(true)
        options.setToolbarWidgetColor(Color.WHITE)
        uCrop.withOptions(options)
        cropImageLauncher.launch(uCrop.getIntent(this))
    }

    private fun initView() {
        binding.onClickHandler = this

        isContactSaved = intent.getBooleanExtra(Constance.IS_CONTACT_SAVED, false)
        if (isContactSaved) {
            binding.tvTitle.text = getString(R.string.edit_contact)
        } else {
            binding.tvTitle.text = getString(R.string.new_contact)
        }


        viewModel.getGoogleAccounts()
        viewModel.googleAccount.observe(this) { list ->
            newDisplayList.clear()
            newDisplayList.add(
                AccountModel(
                    name = "Device Only",
                    email = "",
                    avtar = Common.generateAvatar("Device Only")
                )
            )

            list.forEach {
                newDisplayList.add(
                    AccountModel(
                        name = it.first,
                        email = it.second,
                        avtar = Common.generateAvatar(it.first)
                    )
                )
            }

            if (newDisplayList.isNotEmpty() && !isContactSaved) {
                val itemData = newDisplayList[0]
                binding.inAccountDesign.tvIdName.text = itemData.name
                binding.inAccountDesign.cvProfile.isVisible = true
                val color = Common.profileColors[1 % Common.profileColors.size]
                binding.inAccountDesign.cvProfile.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, color)
                )
                val firstChar = itemData.name.firstOrNull()?.uppercase() ?: ""
                binding.inAccountDesign.tvContactName.text = firstChar

                accountModel = itemData
            }
        }

        isContactSaved = intent.getBooleanExtra(Constance.IS_CONTACT_SAVED, false)
        contactId = intent.getStringExtra(Constance.CONTACT_ID)

        if (isContactSaved) {
            contactId?.let { id ->
                contactDetailViewModel.getUpdatedContact(id)
                contactDetailViewModel.contactData.observe(this) { contact ->
                    contact?.let {
                        binding.edtFirstName.setText(it.stringCallName)
                        binding.edtPhone.setText(it.stringNumber)
                        it.stringPhotoUri?.let { uri ->
                            selectedImageUri = uri.toUri()
                            Glide.with(this).load(uri).into(binding.ivContactPhoto)
                        }

                        // Fetch email
                        it.contactId?.let { id ->
                            viewModel.fetchContactEmail(id)
                            viewModel.fetchContactAccountName(id)
                        }
                    }
                }
            }

        } else {
            val number = intent.getStringExtra("Number")
            binding.edtPhone.setText(number)
        }

        viewModel.contactEmail.observe(this) { email ->
            binding.edtEmail.setText(email)
        }

        viewModel.contactAccountName.observe(this) { accountName ->
            val accName =
                if (accountName != null && accountName.contains("@")) accountName.substringBefore("@") else "Device Only"
            val email = if (accountName != null && accountName.contains("@")) accountName else ""
            val itemData = AccountModel(
                name = accName,
                email = email,
                avtar = Common.generateAvatar(accName)
            )
            binding.inAccountDesign.tvIdName.text = itemData.name
            binding.inAccountDesign.cvProfile.isVisible = true
            val color = Common.profileColors[1 % Common.profileColors.size]
            binding.inAccountDesign.cvProfile.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, color)
            )
            val firstChar = itemData.name.firstOrNull()?.uppercase() ?: ""
            binding.inAccountDesign.tvContactName.text = firstChar

            accountModel = itemData
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
            if (msg.contains("✅")) {
                finish()
            }
        }
    }


    override fun onClick(view: View) {
        when (view.id) {

            binding.llAccounts.id -> {
                showAccountPopup(binding.inAccountDesign.root, newDisplayList)
            }

            binding.cvAddPhoto.id -> {
                if (hasPermissions()) {
                    showImagePickerDialog()
                } else {
                    requestCameraGalleryPermission()
                }
            }

            binding.cvSave.id -> {

                isContactSaved = intent.getBooleanExtra(Constance.IS_CONTACT_SAVED, false)
                val account = accountModel
                val name = binding.edtFirstName.text.toString().trim()
                val email = binding.edtEmail.text.toString().trim()
                val number = binding.edtPhone.text.toString().trim()
                val imageUrl = selectedImageUri
                val isValidEmail = !Common.isValidEmail(email)



                Log.e(
                    "TAG",
                    "onClick: $isContactSaved  $account  $name  $email  $number  $imageUrl  $isValidEmail",
                )

                when {
                    name.isEmpty() -> {
                        binding.edtFirstName.error = "Enter first name"
                    }

                    number.isEmpty() -> {
                        binding.edtPhone.error = "Enter number"
                    }

                    email.isNotEmpty() && isValidEmail -> {
                        binding.edtEmail.error = "Enter valid email"
                    }

                    else -> {
                        viewModel.saveOrUpdateContact(
                            name = name,
                            number = number,
                            email = email,
                            selectedImageUri = imageUrl,
                            accountModel = accountModel!!,
                            isContactSaved = isContactSaved,
                            contactId = contactId
                        )
                    }
                }

            }


            binding.cvCancel.id -> {
                finish()
            }

            binding.ivBack.id -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

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
            binding.inAccountDesign.tvIdName.text = accountModel.name
            val color =
                Common.profileColors[abs(accountModel.name.hashCode()) % Common.profileColors.size]
            binding.inAccountDesign.cvProfile.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, color)
            )
            val firstChar = accountModel.name.firstOrNull()?.uppercase() ?: ""
            binding.inAccountDesign.tvContactName.text = firstChar
            popupWindow.dismiss()
        })

        adapter.addAll(list)

        popupBinding.rvAccounts.adapter = adapter
        popupBinding.rvAccounts.layoutManager = LinearLayoutManager(this)

        popupWindow.showAsDropDown(anchorView)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val cameraGranted = permissions[Manifest.permission.CAMERA] == true

        if (cameraGranted) {
            showImagePickerDialog()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImagePickerDialog() {
        /*val options = arrayOf("Camera", "Gallery")

        AlertDialog.Builder(this)
            .setTitle("Select Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> pickImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
            .show()*/
        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun hasPermissions(): Boolean {

        val cameraGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        return cameraGranted
    }

    private fun requestCameraGalleryPermission() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA
        )
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val file = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")

        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.flush()
        stream.close()

        return Uri.fromFile(file)
    }

}