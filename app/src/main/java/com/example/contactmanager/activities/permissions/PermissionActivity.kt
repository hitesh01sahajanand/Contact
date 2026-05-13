package com.example.contactmanager.activities.permissions

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.contactmanager.R
import com.example.contactmanager.activities.language.LanguageActivity
import com.example.contactmanager.databinding.ActivityPermissionBinding
import com.example.contactmanager.utils.Common.isValidClick
import com.example.contactmanager.utils.OnClickHandler
import com.example.contactmanager.utils.ThemeManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityPermissionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyAppTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_permission)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        intView()
    }

    private val defaultDialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            when (result.resultCode) {
                RESULT_OK -> {
                    goNextActivity()
                }

                RESULT_CANCELED -> {
                    goNextActivity()
                }
            }
        }

    private fun intView() {
        binding.onClickHandler = this

        binding.lottiPermission.setAnimation(R.raw.permission_light)
        binding.lottiPermission.playAnimation()

        binding.lottiPermissionBtn.setAnimation(R.raw.permission_btn)
        binding.lottiPermissionBtn.playAnimation()

        manageTextViews()

    }

    fun manageTextViews() {

        val text = getString(R.string.we_don_t_collect_personal)
        val spannable = SpannableString(text)

        val termsStart = text.indexOf("Terms of Service")
        val termsEnd = termsStart + "Terms of Service".length

        val termsClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(widget.context, "Terms clicked", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@PermissionActivity, R.color.main_color)
                ds.isUnderlineText = true
            }
        }

        spannable.setSpan(termsClickable, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val privacyStart = text.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length

        val privacyClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {

            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@PermissionActivity, R.color.main_color)
                ds.isUnderlineText = true
            }
        }

        spannable.setSpan(
            privacyClickable,
            privacyStart,
            privacyEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvPrivacyPolicy.text = spannable
        binding.tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()
        binding.tvPrivacyPolicy.highlightColor = Color.TRANSPARENT
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        when (view.id) {
            binding.lottiPermissionBtn.id -> {
                openDefaultAppDialog(this)
            }

            binding.tvCancel.id -> {
                goNextActivity()
            }
        }
    }

    fun openDefaultAppDialog(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val roleManager = context.getSystemService(ROLE_SERVICE) as RoleManager
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                defaultDialerLauncher.launch(intent)
            } else {
                val telecomManager =
                    context.getSystemService(TELECOM_SERVICE) as TelecomManager
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
            Log.e("TAG", "openDefaultAppDialog:${e.message} ")
        }
    }

    fun goNextActivity() {
        startActivity(Intent(this, LanguageActivity::class.java))
        finish()
    }
}