package com.phonecall.dialcontacts.calldialer.activities.storageLocation

import android.provider.ContactsContract
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.adapters.StorageLocationAdapter
import com.phonecall.dialcontacts.calldialer.databinding.ActivityStorageLocationBinding
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler

class StorageLocationActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityStorageLocationBinding
    private lateinit var adapter: StorageLocationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_storage_location)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
    }

    private fun initView() {
        binding.onClickHandler = this

        val contactId = intent.getStringExtra("contact_id")
        val contactName = intent.getStringExtra("contact_name") ?: ""
        val contactNumber = intent.getStringExtra("contact_number") ?: ""
        val contactPhotoUri = intent.getStringExtra("contact_photo_uri")

        binding.tvName.text = contactName
        binding.tvNumber.text = contactNumber

        if (contactPhotoUri.isNullOrEmpty()) {
            binding.tvContactName.isVisible = true
            binding.ivContactPhoto.isVisible = false
            val color = Common.profileColors[1 % Common.profileColors.size]
            binding.cvProfile.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, color)
            )
            val firstChar = contactName.firstOrNull()?.uppercase() ?: ""
            binding.tvContactName.text = firstChar

        } else {
            binding.tvContactName.isVisible = false
            binding.ivContactPhoto.isVisible = true
            Glide.with(this).load(contactPhotoUri)
                .into(binding.ivContactPhoto)
        }

        adapter = StorageLocationAdapter()
        binding.rvStorageLocation.adapter = adapter
        binding.rvStorageLocation.layoutManager = LinearLayoutManager(this)

        adapter.addAll(getAccountInfoDetails(contactId))
    }


    private fun getAccountInfoDetails(contactId: String?): List<Pair<String?, String?>> {
        val result = mutableListOf<Pair<String?, String?>>()

        if (contactId.isNullOrEmpty()) return result

        val cursor = contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(
                ContactsContract.RawContacts.ACCOUNT_NAME,
                ContactsContract.RawContacts.ACCOUNT_TYPE
            ),
            "${ContactsContract.RawContacts.CONTACT_ID}=?",
            arrayOf(contactId),
            null
        )

        val uniqueAccounts = mutableSetOf<Pair<String, String>>()
        cursor?.use {
            val accountNameIndex = it.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
            val accountTypeIndex = it.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
            while (it.moveToNext()) {
                val accountName = if (accountNameIndex != -1) it.getString(accountNameIndex) ?: "" else ""
                val accountType = if (accountTypeIndex != -1) it.getString(accountTypeIndex) ?: "" else ""
                uniqueAccounts.add(Pair(accountName, accountType))
            }
        }

        var simCount = 0
        for (account in uniqueAccounts) {
            val accountName = account.first
            val accountType = account.second

            val isGoogle = accountType == "com.google"
            val isWhatsApp = accountType.contains("whatsapp", ignoreCase = true)
            val isTelegram = accountType.contains("telegram", ignoreCase = true)
            val isEmail =
                accountName.contains("@") && accountType.contains("exchange", ignoreCase = true)
            val isSim = accountType.contains("sim", ignoreCase = true) || accountType.contains(
                "adn",
                ignoreCase = true
            )

            val displayName = when {
                isGoogle -> accountName
                isWhatsApp -> "WhatsApp"
                isTelegram -> "Telegram"
                isSim -> {
                    simCount++
                    if (accountName.contains(
                            "sim",
                            ignoreCase = true
                        ) && accountName.any { it.isDigit() }
                    ) {
                        accountName.uppercase()
                    } else {
                        "SIM $simCount"
                    }
                }

                !isGoogle && !isWhatsApp && !isTelegram && !isEmail && !isSim -> "Device"
                else -> accountName.ifEmpty { "Device" }
            }

            result.add(Pair(displayName, accountType))
        }

        // 🔹 Sorting logic
        return result.sortedWith(compareBy { (name, type) ->
            val isGoogle = type == "com.google"
            val isWhatsApp = type?.contains("whatsapp", ignoreCase = true) == true
            val isTelegram = type?.contains("telegram", ignoreCase = true) == true
            val isEmail =
                name?.contains("@") == true && type?.contains("exchange", ignoreCase = true) == true
            val isSim = type?.contains("sim", ignoreCase = true) == true || type?.contains(
                "adn",
                ignoreCase = true
            ) == true
            val isDevice = name == "Device"

            when {
                isDevice -> 0
                isSim -> 1
                isGoogle || isEmail -> 2
                isWhatsApp || isTelegram -> 3
                else -> 4
            }
        })
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