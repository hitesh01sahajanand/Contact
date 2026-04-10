package com.example.contactmanager.fragments.keypad

import android.Manifest
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.contactmanager.databinding.FragmentKeypadBinding
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionDialog
import com.example.contactmanager.utils.PermissionManager
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.example.contactmanager.R
import com.example.contactmanager.activities.newContact.NewContactActivity
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class KeypadFragment : Fragment(), OnClickHandler {
    private lateinit var binding: FragmentKeypadBinding

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
                    checkOverlayPermission()/* PermissionDialog.showDefaultDialerDialog(requireActivity(), onclick = { int ->
                         checkAndRequestPermissions()
 //                        Toast.makeText(requireActivity(), "Success", Toast.LENGTH_SHORT).show()
                     })*/
//                    "User accepted request to become default dialer"
                }

                RESULT_CANCELED -> {
//                    "User declined request to become default dialer"
                }

                else -> {
//                    "Unexpected result code ${result.resultCode}"
                }
            }

//            Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
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

        binding.buttonDelete.setOnLongClickListener {
            clearNumber()
            true
        }
        setupDialPad()

        allPermissionGranted()
    }

    private fun appendDigit(viewId: Int) {
        val value = keyMap[viewId] ?: return

        binding.edtDisplayNumber.append(value)

        if (binding.edtDisplayNumber.text.isNotEmpty()) {
            binding.buttonDelete.visibility = View.VISIBLE
        }
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
        if (PermissionManager.hasPermissions(requireActivity()) && Settings.canDrawOverlays(
                requireActivity()
            )
        ) {
            Log.e("TAG", "allPermissionGranted: false")
            binding.llDefaultUi.visibility = View.GONE
            binding.llKeypadUi.visibility = View.VISIBLE
        } else {
            Log.e("TAG", "allPermissionGranted: true")
            binding.llDefaultUi.visibility = View.VISIBLE
            binding.llKeypadUi.visibility = View.GONE
        }
    }

    /* private fun setupKeypad() {

         val buttons: List<Button> = listOf(
             binding.btn1, binding.btn2, binding.btn3,
             binding.btn4, binding.btn5, binding.btn6,
             binding.btn7, binding.btn8, binding.btn9,
             binding.btn0, binding.btnStar, binding.btnHasTag
         )

         buttons.forEach { btn ->
             btn.setOnClickListener {
                 number += btn.text.toString()
                 binding.tvNumber.text = number
             }
         }


         binding.btnClear.setOnLongClickListener {
             number = ""
             binding.tvNumber.text = ""
             true
         }
     }*/

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

                if (number.isNotEmpty()) {/* val intent = Intent(Intent.ACTION_DIAL)
                     intent.data = Uri.parse("tel:$number")
                     startActivity(intent)
                     makeCall(number)*/
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
                        Toast.makeText(requireActivity(), speedDial, Toast.LENGTH_SHORT).show()
                    },
                    option2Click = {
                        Toast.makeText(requireActivity(), settings, Toast.LENGTH_SHORT).show()
                    })
            }

            binding.llCreateNewContact.id -> {
                if (binding.edtDisplayNumber.text.isNotEmpty()) {
                    val intent = Intent(requireActivity(), NewContactActivity::class.java)
                    intent.putExtra("Number", binding.edtDisplayNumber.text.toString())
                    requireActivity().startActivity(intent)
                } else {
                    Toast.makeText(requireActivity(), "enter number", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    fun makeCall(number: String) {

        val telecomManager =
            requireContext().getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        val uri = Uri.fromParts("tel", number, null)

        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            telecomManager.placeCall(uri, Bundle())
        } else {
            Toast.makeText(requireContext(), "Permission required", Toast.LENGTH_SHORT).show()
        }
    }
}