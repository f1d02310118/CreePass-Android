package com.example.projectmobile.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.method.PasswordTransformationMethod
import android.widget.ImageButton
import android.widget.TextView
import java.security.SecureRandom

fun togglePasswordVisibility(
    isVisible: Boolean,
    textView: TextView?,
    button: ImageButton?,
    eyeOnIcon: Int,
    eyeOffIcon: Int
): Boolean {
    val newVisibility = !isVisible
    if (newVisibility) {
        textView?.transformationMethod = null
        button?.setImageResource(eyeOnIcon)
    } else {
        textView?.transformationMethod = PasswordTransformationMethod.getInstance()
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
        if (currentClip != null && currentClip.itemCount > 0) {
            val currentText = currentClip.getItemAt(0)?.text?.toString()
            if (currentText == text) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }, timeoutMs)
}

fun generatePassword(length: Int = 16): String {
    val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lowercase = "abcdefghijklmnopqrstuvwxyz"
    val digits = "0123456789"
    val special = "!@#\$%^&*()_+-=[]{}"
    val all = uppercase + lowercase + digits + special
    val secureRandom = SecureRandom()
    return CharArray(length) { all[secureRandom.nextInt(all.length)] }.concatToString()
}

fun evaluatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.EMPTY

    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when {
        score <= 2 -> PasswordStrength.WEAK
        score <= 4 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.STRONG
    }
}

enum class PasswordStrength {
    EMPTY, WEAK, MEDIUM, STRONG
}
