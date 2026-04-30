package com.example.contactmanager.fragments.keypad

import android.Manifest
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.activities.newContact.NewContactActivity
import com.example.contactmanager.adapters.SuggestionAdapter
import com.example.contactmanager.databinding.FragmentKeypadBinding
import com.example.contactmanager.models.ContactListItem
import com.example.contactmanager.models.ContactModel
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.NewCallManager
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager
import com.example.contactmanager.utils.PermissionManager.isDefaultDialer
import com.example.contactmanager.viewmodels.ContactViewModel
import com.example.contactmanager.viewmodels.SpeedDialViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.media.AudioManager
import android.media.ToneGenerator
import com.example.contactmanager.activities.settings.SettingsActivity
import com.example.contactmanager.activities.speedDial.SpeedDialActivity
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.SharedPreferenceManager

@AndroidEntryPoint
class KeypadFragment : Fragment(), OnClickHandler {
    private lateinit var binding: FragmentKeypadBinding
    private val viewModel: ContactViewModel by viewModels()
    private val speedDialViewModel: SpeedDialViewModel by viewModels()
    private var contactList: ArrayList<ContactModel> = ArrayList()

    private lateinit var adapter: SuggestionAdapter

    private val keyMap = mapOf(
        R.id.linear1 to "1",
        R.id.linear2 to "2",
        R.id.linear3 to "3",
        R.id.linear4 to "4",
        R.id.linear5 to "5",
        R.id.linear6 to "6",
        R.id.linear7 to "7",
        R.id.linear8 to "8",
        R.id.linear9 to "9",
        R.id.linear10 to "*",
        R.id.linear11 to "0",
        R.id.linear12 to "#"
    )

    private val toneMap = mapOf(
        R.id.linear1 to ToneGenerator.TONE_DTMF_1,
        R.id.linear2 to ToneGenerator.TONE_DTMF_2,
        R.id.linear3 to ToneGenerator.TONE_DTMF_3,
        R.id.linear4 to ToneGenerator.TONE_DTMF_4,
        R.id.linear5 to ToneGenerator.TONE_DTMF_5,
        R.id.linear6 to ToneGenerator.TONE_DTMF_6,
        R.id.linear7 to ToneGenerator.TONE_DTMF_7,
        R.id.linear8 to ToneGenerator.TONE_DTMF_8,
        R.id.linear9 to ToneGenerator.TONE_DTMF_9,
        R.id.linear10 to ToneGenerator.TONE_DTMF_S,
        R.id.linear11 to ToneGenerator.TONE_DTMF_0,
        R.id.linear12 to ToneGenerator.TONE_DTMF_P
    )

    private var toneGenerator: ToneGenerator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentKeypadBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val allGranted = permissions.values.all { it }

            if (allGranted) {
                checkOverlayPermission()
                Toast.makeText(requireActivity(), "Permissions Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireActivity(), "Permissions Denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val overlayLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(requireActivity())) {
                Toast.makeText(requireActivity(), "Overlay permission granted", Toast.LENGTH_SHORT)
                    .show()
                allPermissionGranted()
            } else {
                Toast.makeText(requireActivity(), "Overlay permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private val defaultDialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            when (result.resultCode) {
                RESULT_OK -> {
                    checkOverlayPermission()
                }

                RESULT_CANCELED -> {

                }
            }
        }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(requireActivity())) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${requireActivity().packageName}".toUri()
            )
            overlayLauncher.launch(intent)
        } else {
            allPermissionGranted()
            Toast.makeText(requireActivity(), "Overlay permission ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        if (!PermissionManager.hasPermissions(requireActivity())) {

            val permissionsList = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(
                    requireActivity(), Manifest.permission.READ_CALL_LOG
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsList.add(Manifest.permission.READ_CALL_LOG)
            }

            if (ContextCompat.checkSelfPermission(
                    requireActivity(), Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsList.add(Manifest.permission.READ_CONTACTS)
            }

            if (permissionsList.isNotEmpty()) {
                permissionLauncher.launch(permissionsList.toTypedArray())
            }
        } else {
            checkOverlayPermission()
            allPermissionGranted()
            Toast.makeText(requireActivity(), "Already granted", Toast.LENGTH_SHORT).show()
        }
    }


    private fun initView() {
        binding.onClickHandler = this
        binding.inHeader.onClickHandler = this
        binding.inHeader.tvTitle.text = requireActivity().getString(R.string.phone)
        binding.inHeader.cvMore.isVisible = true
        binding.edtDisplayNumber.apply {
            showSoftInputOnFocus = false
            isFocusable = true
            isFocusableInTouchMode = true
        }

        allPermissionGranted()

        adapter = SuggestionAdapter(onClick = { model ->
            binding.edtDisplayNumber.setText(model.number)
        })

        binding.rvSuggestions.adapter = adapter
        binding.rvSuggestions.layoutManager = LinearLayoutManager(requireActivity())

        if (PermissionManager.hasPermissions(requireActivity())) {
            viewModel.loadAllContacts()
        }

        viewModel.allContactList.observe(requireActivity()) { allContacts ->
            if (allContacts.isNotEmpty()) {
                val list = allContacts
                    .filterIsInstance<ContactListItem.Contact>()
                    .map { it.data }
                contactList.addAll(ArrayList(list))
                adapter.addAll(contactList)
            } else {
                binding.rvSuggestions.isVisible = false
                binding.llOptionsSuggestions.isVisible = false
            }
        }

        binding.edtDisplayNumber.addTextChangedListener { editable ->

            val query = editable.toString().trim()

            if (query.isEmpty()) {
                binding.rvSuggestions.isVisible = false
                binding.llOptionsSuggestions.isVisible = false
                return@addTextChangedListener
            }

            val result = adapter.filter(query)

            if (result.isEmpty()) {
                binding.rvSuggestions.isVisible = false
            } else {
                binding.rvSuggestions.isVisible = true
            }
            binding.llOptionsSuggestions.isVisible = true
        }

        binding.buttonDelete.setOnLongClickListener {
            clearNumber()
            true
        }
        setupDialPad()

        allPermissionGranted()

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 80)
        } catch (e: Exception) {
            Log.e("KeypadFragment", "Exception while creating ToneGenerator: $e")
        }
    }

    private fun appendDigit(viewId: Int) {
        val value = keyMap[viewId] ?: return

        if (SharedPreferenceManager.getBoolean(requireContext(), Constance.DIAL_PAD_SOUND, false)) {
            playTone(viewId)
        }

        binding.edtDisplayNumber.append(value)

        if (binding.edtDisplayNumber.text.isNotEmpty()) {
            binding.buttonDelete.visibility = View.VISIBLE
        }
    }

    private fun playTone(viewId: Int) {
        val tone = toneMap[viewId] ?: return
        toneGenerator?.startTone(tone, 150)
    }

    private fun setupDialPad() {
        val buttons = listOf(
            binding.linear1,
            binding.linear2,
            binding.linear3,
            binding.linear4,
            binding.linear5,
            binding.linear6,
            binding.linear7,
            binding.linear8,
            binding.linear9,
            binding.linear10,
            binding.linear11,
            binding.linear12
        )

        buttons.forEach { view ->
            view.setOnClickListener {
                appendDigit(view.id)
            }
            val digitStr = keyMap[view.id]
            if (digitStr != null && digitStr.matches(Regex("[1-9]"))) {
                view.setOnLongClickListener {
                    val slot = digitStr.toInt()
                    handleSpeedDial(slot)
                    true
                }
            }
        }
    }

    private fun handleSpeedDial(slot: Int) {
        lifecycleScope.launch {
            val speedDial = speedDialViewModel.getSpeedDialBySlot(slot)
            if (speedDial != null && speedDial.contactNumber.isNotEmpty()) {
                Common.actionCall(speedDial.contactNumber, requireActivity())
            } else {
                Toast.makeText(requireContext(), "No speed dial set for $slot", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun deleteLastDigit() {
        val text = binding.edtDisplayNumber.text.toString()

        if (text.isNotEmpty()) {
            val updated = text.dropLast(1)
            binding.edtDisplayNumber.setText(updated)
            binding.edtDisplayNumber.setSelection(updated.length)
        }

        if (binding.edtDisplayNumber.text.isEmpty()) {
            binding.buttonDelete.visibility = View.INVISIBLE
        }
    }

    private fun clearNumber() {
        binding.edtDisplayNumber.setText("")
        binding.buttonDelete.visibility = View.INVISIBLE
    }

    fun allPermissionGranted() {
       /* if (PermissionManager.hasPermissions(requireActivity()) && Settings.canDrawOverlays(
                requireActivity()
            )
        ) {
            binding.llDefaultUi.visibility = View.GONE
            binding.llKeypadUi.visibility = View.VISIBLE
        } else {
            binding.llDefaultUi.visibility = View.VISIBLE
            binding.llKeypadUi.visibility = View.GONE
        }*/

        binding.llDefaultUi.visibility = View.GONE
        binding.llKeypadUi.visibility = View.VISIBLE
    }

    fun openDefaultAppDialog(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                defaultDialerLauncher.launch(intent)
            } else {
                val telecomManager =
                    context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                if (context.packageName != telecomManager.defaultDialerPackage) {
                    val intent = Intent("android.telecom.action.CHANGE_DEFAULT_DIALER").apply {
                        putExtra(
                            "android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME",
                            context.packageName
                        )
                    }
                    defaultDialerLauncher.launch(intent)
                }
            }
        } catch (e: Exception) {
            // Handle exception if needed
        }
    }


    override fun onClick(view: View) {
        when (view.id) {
            binding.cvSetDefaultApp.id -> {
                openDefaultAppDialog(requireContext())
            }

            binding.buttonCall.id -> {
                val number = binding.edtDisplayNumber.text.toString()

                if (number.isNotEmpty()) {
                    if (NewCallManager.isNumberActive(number)) {
                        Toast.makeText(
                            requireActivity(),
                            "Number already in a call",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    Common.actionCall(number, requireActivity())
                }
            }

            binding.buttonDelete.id -> {
                deleteLastDigit()
            }

            binding.inHeader.cvMore.id -> {
                val speedDial = requireActivity().getString(R.string.speed_dial_number)
                val settings = requireActivity().getString(R.string.setting)
                Common.popUpMenu(
                    requireActivity(),
                    binding.inHeader.cvMore,
                    speedDial,
                    settings,
                    option1Click = {
                        requireActivity().startActivity(
                            Intent(
                                requireActivity(),
                                SpeedDialActivity::class.java
                            )
                        )
                    },
                    option2Click = {
                        requireActivity().startActivity(
                            Intent(
                                requireActivity(),
                                SettingsActivity::class.java
                            )
                        )
                    })
            }

            binding.llCreateNewContact.id -> {
                if (binding.edtDisplayNumber.text.isNotEmpty()) {
                    val intent = Intent(requireActivity(), NewContactActivity::class.java)
                    intent.putExtra("Number", binding.edtDisplayNumber.text.toString())
                    requireActivity().startActivity(intent)
                } else {
                    Toast.makeText(
                        requireActivity(),
                        requireActivity().getString(R.string.enter_number),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            binding.llVideoCall.id -> {
                if (binding.edtDisplayNumber.text.isNotEmpty()) {
                    Common.showVideoAppChooser(
                        requireActivity(),
                        binding.edtDisplayNumber.text.toString()
                    )
                } else {
                    Toast.makeText(
                        requireActivity(),
                        requireActivity().getString(R.string.enter_number),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            binding.llSendMessage.id -> {
                if (binding.edtDisplayNumber.text.isNotEmpty()) {
                    Common.showMessageAppChooser(
                        requireActivity(),
                        binding.edtDisplayNumber.text.toString()
                    )
                } else {
                    Toast.makeText(
                        requireActivity(),
                        requireActivity().getString(R.string.enter_number),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /*fun actionCall(phoneNumber: String, context: Context) {
        if (phoneNumber.isEmpty()) return

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: return

        val callUri = Uri.fromParts("tel", phoneNumber, null)
        val callBundle = Bundle().apply {
            putBoolean("android.telecom.extra.START_CALL_WITH_SPEAKERPHONE", false)
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            val subscriptionManager =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val activeSimList = subscriptionManager?.activeSubscriptionInfoList

            if (!activeSimList.isNullOrEmpty() && activeSimList.size > 1) {

                val simNames = Array(activeSimList.size) { i ->
                    "SIM ${i + 1}"
                }

                val builder = MaterialAlertDialogBuilder(context)

                builder.setTitle("Select SIM")
                    .setItems(simNames) { _, which ->

                        val selectedSim = activeSimList[which]

                        val callBundle2 = Bundle().apply {
                            putParcelable(
                                TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                                Common.getHandleForSubId(
                                    selectedSim.subscriptionId,
                                    context
                                )
                            )
                        }

                        val callUri2 = Uri.fromParts("tel", phoneNumber, null)
                        telecomManager.placeCall(callUri2, callBundle2)
                    }

                val dialog = builder.create()
                dialog.show()

                dialog.getButton(Dialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)

            } else {
                // Single SIM
                telecomManager.placeCall(callUri, callBundle)
            }

        }
    }*/

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
    }
}