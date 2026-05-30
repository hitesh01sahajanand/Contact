package com.phonecall.dialcontacts.calldialer.fragments.keypad

import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class DialPadDisplayEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    var onAfterPaste: (() -> Unit)? = null

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.paste) {
            pasteFromClipboard()
            return true
        }
        return super.onTextContextMenuItem(id)
    }

    private fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = clipboard?.primaryClip
        if (clipData == null || clipData.itemCount == 0) {
            return
        }

        val pastedText = sanitizePhoneInput(
            clipData.getItemAt(0).coerceToText(context)?.toString().orEmpty()
        )
        if (pastedText.isEmpty()) {
            return
        }

        val editable = text ?: return
        val start = selectionStart.coerceIn(0, editable.length)
        val end = selectionEnd.coerceIn(start, editable.length)
        editable.replace(start, end, pastedText)

        val newCursorPosition = start + pastedText.length
        post {
            setSelection(newCursorPosition)
            isCursorVisible = true
            onAfterPaste?.invoke()
        }
    }

    private fun sanitizePhoneInput(input: String): String {
        val cleaned = StringBuilder()
        for (char in input) {
            when {
                char.isDigit() || char == '+' || char == '*' || char == '#' -> cleaned.append(char)
            }
        }
        return cleaned.toString()
    }
}
