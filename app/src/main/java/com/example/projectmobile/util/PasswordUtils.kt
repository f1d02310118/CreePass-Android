package com.example.projectmobile.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageButton

fun togglePasswordVisibility(
    isVisible: Boolean,
    editText: EditText?,
    button: ImageButton?,
    eyeOnIcon: Int,
    eyeOffIcon: Int
): Boolean {
    val newVisibility = !isVisible
    if (newVisibility) {
        editText?.transformationMethod = null
        button?.setImageResource(eyeOnIcon)
    } else {
        editText?.transformationMethod = PasswordTransformationMethod.getInstance()
        button?.setImageResource(eyeOffIcon)
    }
    return newVisibility
}

fun copyToClipboardWithTimeout(
    context: Context,
    label: String,
    text: String,
    timeoutMs: Long = 30000L
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)

    Handler(Looper.getMainLooper()).postDelayed({
        val currentClip = clipboard.primaryClip
        if (currentClip != null && currentClip.getItemAt(0)?.text.toString() == text) {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }, timeoutMs)
}

fun generatePassword(length: Int = 16): String {
    val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lowercase = "abcdefghijklmnopqrstuvwxyz"
    val digits = "0123456789"
    val special = "!@#\$%^&*()_+-=[]{}|;:,.<>?"
    val all = uppercase + lowercase + digits + special
    return (1..length)
        .map { all.random() }
        .joinToString("")
}
