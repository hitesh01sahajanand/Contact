package com.example.contactmanager.fragments.keypad

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.activities.newContact.NewContactActivity
import com.example.contactmanager.activities.settings.SettingsActivity
import com.example.contactmanager.activities.speedDial.SpeedDialActivity
import com.example.contactmanager.adapters.SuggestionAdapter
import com.example.contactmanager.databinding.FragmentKeypadBinding
import com.example.contactmanager.models.ContactListItem
import com.example.contactmanager.models.ContactModel
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.NewCallManager
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.PermissionManager
import com.example.contactmanager.utils.SharedPreferenceManager
import com.example.contactmanager.viewmodels.ContactViewModel
import com.example.contactmanager.viewmodels.SpeedDialViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class KeypadFragment : Fragment(), OnClickHandler {
    private lateinit var binding: FragmentKeypadBinding
    private val viewModel: ContactViewModel by viewModels()
    private val speedDialViewModel: SpeedDialViewModel by viewModels()

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


    override fun onResume() {
        super.onResume()
        if (PermissionManager.hasContactPermissions(requireActivity())) {
            viewModel.loadAllContacts()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && PermissionManager.hasContactPermissions(requireActivity())) {
            viewModel.loadAllContacts()
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

        adapter = SuggestionAdapter(onClick = { model ->
            binding.edtDisplayNumber.setText(model.number)
        }, onFilterComplete = { count ->
            val query = binding.edtDisplayNumber.text.toString().trim()
            if (query.isEmpty()) {
                binding.rvSuggestions.isVisible = false
                binding.llOptionsSuggestions.isVisible = false
            } else {
                binding.rvSuggestions.isVisible = count > 0
                binding.llOptionsSuggestions.isVisible = true
            }
        })

        binding.rvSuggestions.adapter = adapter
        binding.rvSuggestions.layoutManager = LinearLayoutManager(requireActivity())

        if (PermissionManager.hasContactPermissions(requireActivity())) {
            viewModel.loadAllContacts()
        }

        viewModel.allContactList.observe(viewLifecycleOwner) { allContacts ->
            if (allContacts.isNotEmpty()) {
                val list = allContacts
                    .filterIsInstance<ContactListItem.Contact>()
                    .map { it.data }
                adapter.addAll(ArrayList(list))
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
            }
            adapter.filter(query)
        }

        binding.buttonDelete.setOnLongClickListener {
            clearNumber()
            true
        }
        setupDialPad()

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
                Toast.makeText(requireContext(),
                    getString(R.string.no_speed_dial_set_for, slot), Toast.LENGTH_SHORT)
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


    override fun onClick(view: View) {
        when (view.id) {
            binding.buttonCall.id -> {
                val number = binding.edtDisplayNumber.text.toString()

                if (number.isNotEmpty()) {
                    if (NewCallManager.isNumberActive(number)) {
                        Toast.makeText(
                            requireActivity(),
                            requireActivity().getString(R.string.number_already_in_a_call),
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

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
    }
}