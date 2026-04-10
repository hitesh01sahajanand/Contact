package com.example.contactmanager.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.example.contactmanager.R
import com.example.contactmanager.databinding.PermissionDialogDesignBinding
import androidx.core.graphics.drawable.toDrawable
import javax.security.auth.callback.Callback

object PermissionDialog {

    fun showDefaultDialerDialog(context: Context, onclick: (Int) -> Unit) {

        val binding =
            PermissionDialogDesignBinding.inflate(LayoutInflater.from(context), null, false)

        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())


        binding.tvContinue.setOnClickListener {
            dialog.dismiss()
            onclick(1)
        }

        dialog.show()
    }
}