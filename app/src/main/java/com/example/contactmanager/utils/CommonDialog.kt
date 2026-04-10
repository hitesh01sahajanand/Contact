package com.example.contactmanager.utils

import android.content.Context
import android.view.LayoutInflater
import com.example.contactmanager.databinding.CommonDialogDesignBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

object CommonDialog {

    fun showDefaultDialerDialog(context: Context, msg: String, onclick: () -> Unit) {

        val binding = CommonDialogDesignBinding.inflate(LayoutInflater.from(context))

        val bottomSheetDialog = BottomSheetDialog(context)
        bottomSheetDialog.setContentView(binding.root)
        bottomSheetDialog.setCancelable(false)

        binding.tvTitle.text = msg

        binding.btnOkay.setOnClickListener {
            onclick()
            bottomSheetDialog.dismiss()
        }

        binding.btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }
}