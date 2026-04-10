package com.example.contactmanager.activities.details

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.contactmanager.R
import com.example.contactmanager.activities.history.HistoryActivity
import com.example.contactmanager.activities.newContact.NewContactActivity
import com.example.contactmanager.databinding.ActivityContactsDetailsBinding
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.SendData
import com.example.contactmanager.viewmodels.ContactDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContactsDetailsActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityContactsDetailsBinding
    private val viewModel: ContactDetailsViewModel by viewModels()
    private var contactDetail: CallLogEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_contacts_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
    }

    private fun initView() {
        SendData.contactDetails?.let {
            contactDetail = it
        }

        binding.onClickHandler = this

        contactDetail?.let {
            val name = if (it.stringCallName.isNullOrEmpty()) it.stringNumber else it.stringCallName
            binding.tvName.text = name

            val image =
                if (it.stringPhotoUri.isNullOrEmpty()) Common.generateAvatar(name!!) else it.stringPhotoUri
            Glide.with(this).load(image).into(binding.ivContactPhoto)
            Log.e("TAG", "initView:sizegggggg ", )
            viewModel.getNumberToHistory(it.stringNumber.toString(), 0, 1000)
            viewModel.contactHistory.observe(this) { list ->
                Log.e("TAG", "initView:size ${list.size}", )
                if (list.isNotEmpty()) {
                    val itemData = list[0]

//                    binding.inHistory.tvTime.text = Common.formatSmartDate(itemData.date)

                    val tag = Common.getCallType(itemData.intType)

//                    val duration = Common.formatDuration(itemData.duration)
                    binding.inHistory.tvInOutCallsWithDuration.text = "$tag"
                }
            }
        }

    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.cvMessage.id -> {
                contactDetail?.let {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "smsto:${it.stringNumber}".toUri()
                    }
                    startActivity(intent)
                }
            }

            binding.cvCall.id -> {

            }

            binding.cvVideo.id -> {

            }

            binding.cvMail.id -> {

            }


            binding.tvShowHistory.id -> {
                contactDetail?.let {
                    val intent = Intent(this, HistoryActivity::class.java)
                    intent.putExtra("Number", it.stringNumber)
                    startActivity(intent)
                }
            }

            binding.tvNewContact.id -> {
                contactDetail?.let {
                    val intent = Intent(this, NewContactActivity::class.java)
                    intent.putExtra("Number", it.stringNumber)
                    startActivity(intent)
                }

            }

            binding.tvShareContact.id -> {

            }

            binding.cvBlockContact.id -> {

            }

        }
    }
}