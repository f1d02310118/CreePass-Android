package com.example.projectmobile.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.Window
import android.widget.ImageButton
import android.widget.TextView
import com.example.projectmobile.R
import com.example.projectmobile.model.PasswordItem
import com.example.projectmobile.util.copyToClipboardWithTimeout
import com.example.projectmobile.util.togglePasswordVisibility
import com.google.android.material.snackbar.Snackbar

class PasswordDetailDialog(
    context: Context,
    private val item: PasswordItem
) : Dialog(context) {

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_password_detail)

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

        tvDialogTitle.text = item.title
        tvDetailCategory.text = item.category
        tvDetailUsername.text = item.username
        tvDetailPassword.text = item.password
        tvDetailUrl.text = if (item.url.isNotEmpty()) item.url else context.getString(R.string.detail_url_empty)

        tvDetailPassword.transformationMethod = PasswordTransformationMethod.getInstance()

        btnToggleDetail.setOnClickListener {
            isPasswordVisible = togglePasswordVisibility(
                isPasswordVisible,
                tvDetailPassword,
                btnToggleDetail,
                R.drawable.ic_eye_on,
                R.drawable.ic_eye_off
            )
        }

        btnCopyUsername.setOnClickListener {
            copyToClipboardWithTimeout(context, "username", item.username)
            Snackbar.make(btnCopyUsername, context.getString(R.string.detail_username_copied), Snackbar.LENGTH_SHORT).show()
        }

        btnCopyPassword.setOnClickListener {
            copyToClipboardWithTimeout(context, "password", item.password)
            Snackbar.make(btnCopyPassword, context.getString(R.string.detail_password_copied), Snackbar.LENGTH_SHORT).show()
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }
}
