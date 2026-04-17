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

@AndroidEntryPoint
class NewContactActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityNewContactBinding
    private val viewModel: NewContactViewModel by viewModels()
    private val contactDetailViewModel: ContactDetailsViewModel by viewModels()
    private var newDisplayList: ArrayList<AccountModel> = ArrayList()

    var selectedImageUri: Uri? = null
    var accountModel: AccountModel? = null


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
            selectedImageUri = uri
            binding.ivContactPhoto.setImageBitmap(it)
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivContactPhoto.setImageURI(uri)
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initView() {
        binding.onClickHandler = this


        viewModel.getGoogleAccounts()
        viewModel.googleAccount.observe(this) { list ->
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

            if (newDisplayList.isNotEmpty()) {
                val itemData = newDisplayList[0]
                binding.inAccountDesign.tvIdName.text = itemData.name
                val color = Common.profileColors[1 % Common.profileColors.size]
                binding.inAccountDesign.cvProfile.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, color)
                )
                val firstChar = itemData.name.firstOrNull()?.uppercase() ?: ""
                binding.inAccountDesign.tvContactName.text = firstChar

                accountModel = itemData
            }
        }

        val isContactSaved = intent.getBooleanExtra(Constance.IS_CONTACT_SAVED, false)
        val contactId = intent.getStringExtra(Constance.CONTACT_ID)

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

                val isContactSaved = intent.getBooleanExtra(Constance.IS_CONTACT_SAVED, false)
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

                    isValidEmail -> {
                        binding.edtEmail.error = "Enter valid email"
                    }

                    else -> {

                        viewModel.savedContactMassage.observe(this) { msg ->
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                            val resultIntent = Intent().apply {
                                putExtra(Constance.DATA_FETCH, true)
                            }
                            setResult(RESULT_OK, resultIntent)

                            finish()
                        }

                        viewModel.saveOrUpdateContact(
                            name = name,
                            number = number,
                            email = email,
                            selectedImageUri = imageUrl,
                            accountModel = accountModel!!,
                            isContactSaved = isContactSaved,
                            contactId = intent.getStringExtra(Constance.CONTACT_ID)
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

    /*private fun addPhoneRow(number: String = "") {
        val bindingItem = ItemPhoneDesignBinding.inflate(layoutInflater, null, false)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, phoneTypes)
        bindingItem.spinnerType.adapter = adapter

        bindingItem.ivDelete.setOnClickListener {
            if (binding.llPhoneContainer.childCount > 1) {
                binding.llPhoneContainer.removeView(bindingItem.root)
            } else {
                Toast.makeText(this, "At least one phone required", Toast.LENGTH_SHORT).show()
            }
        }
        if (number.isNotEmpty()) {
            bindingItem.etPhone.setText(number)
        }

        binding.llPhoneContainer.addView(bindingItem.root)
    }*/

    /*fun getPhoneList(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()

        for (i in 0 until binding.llPhoneContainer.childCount) {
            val view = binding.llPhoneContainer.getChildAt(i)

            val phone = view.findViewById<EditText>(R.id.etPhone).text.toString()
            val type = view.findViewById<Spinner>(R.id.spinnerType).selectedItem.toString()

            if (phone.isNotEmpty()) {
                list.add(Pair(phone, type))
            }
        }

        return list
    }*/

    /*private fun addEmailRow() {
        val bindingItem = ItemEmailDesignBinding.inflate(layoutInflater, null, false)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, emailTypes)
        bindingItem.spinnerEmailType.adapter = adapter


        bindingItem.ivDelete.setOnClickListener {
            if (binding.llEmailContainer.childCount > 1) {
                binding.llEmailContainer.removeView(bindingItem.root)
            } else {
                Toast.makeText(this, "At least one email required", Toast.LENGTH_SHORT).show()
            }
        }

        binding.llEmailContainer.addView(bindingItem.root)
    }*/

    /*fun getEmailList(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()

        for (i in 0 until binding.llEmailContainer.childCount) {
            val view = binding.llEmailContainer.getChildAt(i)

            val email = view.findViewById<EditText>(R.id.etEmail).text.toString()
            val type = view.findViewById<Spinner>(R.id.spinnerEmailType).selectedItem.toString()

            if (email.isNotEmpty()) {
                list.add(Pair(email, type))
            }
        }

        return list
    }*/

    private fun showAccountPopup(anchorView: View, list: List<AccountModel>) {

        val popupBinding = DialogGoogleAccountsBinding.inflate(layoutInflater)

        val popupWindow = PopupWindow(
            popupBinding.root,
            anchorView.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.isOutsideTouchable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.elevation = 10f

        val adapter = AllAccountAdapter(onClick = { accountModel ->
            this.accountModel = accountModel
            binding.inAccountDesign.tvIdName.text = accountModel.name
            val color = Common.profileColors[1 % Common.profileColors.size]
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
        val options = arrayOf("Camera", "Gallery")

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
            .show()
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