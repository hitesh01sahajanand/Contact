package com.phonecall.dialcontacts.calldialer.fragments.keypad

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.newContact.NewContactActivity
import com.phonecall.dialcontacts.calldialer.activities.settings.SettingsActivity
import com.phonecall.dialcontacts.calldialer.activities.speedDial.SpeedDialActivity
import com.phonecall.dialcontacts.calldialer.adapters.SuggestionAdapter
import com.phonecall.dialcontacts.calldialer.databinding.FragmentKeypadBinding
import com.phonecall.dialcontacts.calldialer.models.ContactListItem
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.NewCallManager
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.utils.PermissionManager
import com.phonecall.dialcontacts.calldialer.utils.SharedPreferenceManager
import com.phonecall.dialcontacts.calldialer.viewmodels.ContactViewModel
import com.phonecall.dialcontacts.calldialer.viewmodels.SpeedDialViewModel
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
    private var isDialPadInput = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentKeypadBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }


    override fun onResume() {
        super.onResume()
        hideKeypadKeyboard()
        if (PermissionManager.hasContactPermissions(requireActivity())) {
            viewModel.loadAllContacts()
        }
    }

    override fun onPause() {
        if (::binding.isInitialized) {
            binding.edtDisplayNumber.clearFocus()
        }
        super.onPause()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            if (::binding.isInitialized) {
                binding.edtDisplayNumber.clearFocus()
            }
        } else {
            hideKeypadKeyboard()
            if (PermissionManager.hasContactPermissions(requireActivity())) {
                viewModel.loadAllContacts()
            }
        }
    }

    private fun hideKeypadKeyboard() {
        if (!::binding.isInitialized) return
        binding.edtDisplayNumber.clearFocus()
        binding.root.apply {
            isFocusableInTouchMode = true
            requestFocus()
        }
        val decorView = requireActivity().window.decorView
        Common.hideKeyboard(requireContext(), decorView)
        decorView.post {
            binding.edtDisplayNumber.clearFocus()
            Common.hideKeyboard(requireContext(), decorView)
        }
    }


    private fun initView() {
        binding.onClickHandler = this
        binding.inHeader.onClickHandler = this
        binding.inHeader.tvTitle.text = requireActivity().getString(R.string.phone)
        binding.inHeader.cvMore.isVisible = true
        binding.edtDisplayNumber.apply {
            inputType = InputType.TYPE_CLASS_TEXT
            showSoftInputOnFocus = false
            isFocusable = true
            isFocusableInTouchMode = true
            isCursorVisible = true
            setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    Common.hideKeyboard(requireContext(), view)
                    isCursorVisible = true
                }
            }
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    Common.hideKeyboard(requireContext(), view)
                    isCursorVisible = true
                }
                false
            }
        }

        adapter = SuggestionAdapter(onClick = { model ->
            val number = model.number.orEmpty()
            binding.edtDisplayNumber.setText(number)
            safeSetSelection(number.length)
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
                val currentQuery = binding.edtDisplayNumber.text.toString().trim()
                // addAllAndFilter: if query is present, skips intermediate notify to avoid blink
                adapter.addAllAndFilter(ArrayList(list), currentQuery)
            } else {
                binding.rvSuggestions.isVisible = false
                binding.llOptionsSuggestions.isVisible = false
            }
        }

        var textLengthBeforeChange = 0
        binding.edtDisplayNumber.addTextChangedListener(
            beforeTextChanged = { text, _, _, _ ->
                textLengthBeforeChange = text?.length ?: 0
            },
            afterTextChanged = { editable ->
                val query = editable.toString().trim()
                binding.buttonDelete.visibility =
                    if (editable.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
                if (query.isEmpty()) {
                    binding.rvSuggestions.isVisible = false
                    binding.llOptionsSuggestions.isVisible = false
                }
                adapter.filter(query)

                if (!isDialPadInput) {
                    val newLength = editable?.length ?: 0
                    if (newLength - textLengthBeforeChange > 1) {
                        scheduleCursorToEnd()
                    }
                }
                isDialPadInput = false
            }
        )

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

    private fun scheduleCursorToEnd() {
        binding.edtDisplayNumber.post {
            binding.edtDisplayNumber.post {
                moveCursorToEnd()
            }
        }
    }

    private fun moveCursorToEnd() {
        val edt = binding.edtDisplayNumber
        val length = edt.text?.length ?: 0
        if (length == 0) return
        if (!edt.hasFocus()) {
            edt.requestFocus()
        }
        edt.isCursorVisible = true
        edt.setSelection(length)
    }

    private fun appendDigit(viewId: Int) {
        val value = keyMap[viewId] ?: return

        if (SharedPreferenceManager.getBoolean(requireContext(), Constance.DIAL_PAD_SOUND, false)) {
            playTone(viewId)
        }

        insertAtCursor(value)
    }

    private fun insertAtCursor(value: String) {
        val edt = binding.edtDisplayNumber
        if (!edt.hasFocus()) {
            edt.requestFocus()
        }
        edt.isCursorVisible = true
        val text = edt.text ?: return
        val currentLength = text.length
        val start = edt.selectionStart.coerceIn(0, currentLength)
        val end = edt.selectionEnd.coerceIn(start, currentLength)
        isDialPadInput = true
        text.replace(start, end, value)
        safeSetSelection(start + value.length)
    }

    private fun safeSetSelection(index: Int) {
        val edt = binding.edtDisplayNumber
        val length = edt.text?.length ?: 0
        if (length == 0) return
        edt.isCursorVisible = true
        edt.setSelection(index.coerceIn(0, length))
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
            if (digitStr == "0") {
                view.setOnLongClickListener {
                    insertAtCursor("+")
                    true
                }
            } else if (digitStr != null && digitStr.matches(Regex("[1-9]"))) {
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
                    requireActivity().getString(R.string.no_speed_dial_set_for, slot), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun deleteLastDigit() {
        val edt = binding.edtDisplayNumber
        val text = edt.text ?: return
        val currentLength = text.length
        val start = edt.selectionStart.coerceIn(0, currentLength)
        val end = edt.selectionEnd.coerceIn(start, currentLength)

        if (start != end) {
            text.delete(start, end)
            safeSetSelection(start)
        } else if (start > 0) {
            text.delete(start - 1, start)
            safeSetSelection(start - 1)
        }
    }

    private fun clearNumber() {
        binding.edtDisplayNumber.setText("")
    }


    override fun onClick(view: View) {
        if (view.id != binding.buttonDelete.id && !isValidClick()) return
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
                binding.edtDisplayNumber.text?.let {
                    if (it.isNotEmpty()) {
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
            }

            binding.llVideoCall.id -> {
                binding.edtDisplayNumber.text?.let {
                    if (it.isNotEmpty()) {
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
            }

            binding.llSendMessage.id -> {
                binding.edtDisplayNumber.text?.let {
                    if (it.isNotEmpty()) {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
    }
}