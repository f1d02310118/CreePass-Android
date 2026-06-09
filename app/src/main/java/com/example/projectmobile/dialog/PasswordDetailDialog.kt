package com.example.projectmobile.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.Window
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.example.projectmobile.R
import com.example.projectmobile.model.PasswordItem

class PasswordDetailDialog(
    context: Context,
    private val item: PasswordItem
) : Dialog(context) {

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_password_detail)

        // Atur background dialog transparan (biar rounded corners keliatan)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupViews()
    }

    private fun setupViews() {
        val tvDialogTitle: TextView = findViewById(R.id.tvDialogTitle)
        val tvDetailCategory: TextView = findViewById(R.id.tvDetailCategory)
        val tvDetailUsername: TextView = findViewById(R.id.tvDetailUsername)
        val tvDetailPassword: TextView = findViewById(R.id.tvDetailPassword)
        val tvDetailUrl: TextView = findViewById(R.id.tvDetailUrl)
        val btnToggleDetail: ImageButton = findViewById(R.id.btnToggleDetail)
        val btnCopyUsername: ImageButton = findViewById(R.id.btnCopyUsername)
        val btnCopyPassword: ImageButton = findViewById(R.id.btnCopyDetailPassword)
        val btnClose: ImageButton = findViewById(R.id.btnCloseDialog)

        // Isi data
        tvDialogTitle.text = item.title
        tvDetailCategory.text = item.category
        tvDetailUsername.text = item.username
        tvDetailPassword.text = item.password
        tvDetailUrl.text = if (item.url.isNotEmpty()) item.url else "-"

        // Default password tersembunyi
        tvDetailPassword.transformationMethod = PasswordTransformationMethod.getInstance()

        // Toggle show/hide password
        btnToggleDetail.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                tvDetailPassword.transformationMethod = null
                btnToggleDetail.setImageResource(R.drawable.ic_eye_on)
            } else {
                tvDetailPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnToggleDetail.setImageResource(R.drawable.ic_eye_off)
            }
        }

        // Copy username
        btnCopyUsername.setOnClickListener {
            copyToClipboard("username", item.username)
            Toast.makeText(context, "Username disalin!", Toast.LENGTH_SHORT).show()
        }

        // Copy password
        btnCopyPassword.setOnClickListener {
            copyToClipboard("password", item.password)
            Toast.makeText(context, "Password disalin!", Toast.LENGTH_SHORT).show()
        }

        // Tutup dialog
        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}