package com.example.projectmobile.activity

import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.EditText
import android.text.method.PasswordTransformationMethod
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.example.projectmobile.R
import com.example.projectmobile.model.PasswordItem
import com.example.projectmobile.util.CryptoUtil
import com.example.projectmobile.util.generatePassword
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class AddEditActivity : AppCompatActivity() {
    private val db   = Firebase.firestore
    private val auth = Firebase.auth
    private lateinit var etTitle: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etUrl: EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var btnGeneratePassword: ImageButton
    private lateinit var spCategory: Spinner
    private lateinit var tvHeaderTitle: TextView
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton
    private lateinit var layoutHeader: LinearLayout
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)

        initViews()
        setupSpinner()
        setupEditMode()
        setupActions()
    }

    private fun initViews() {
        etTitle   = findViewById(R.id.etTitle)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etUrl = findViewById(R.id.etUrl)
        spCategory = findViewById(R.id.spCategory)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
        btnTogglePassword = findViewById(R.id.btnToggleAddPassword)
        btnGeneratePassword = findViewById(R.id.btnGeneratePassword)
        layoutHeader = findViewById(R.id.layoutHeader)

        ViewCompat.setOnApplyWindowInsetsListener(layoutHeader) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(view.paddingLeft, top + 16, view.paddingRight, view.paddingBottom)
            insets
        }
    }

    private fun setupSpinner() {
        val categories = listOf(
            getString(R.string.category_other),
            getString(R.string.category_email),
            getString(R.string.category_social),
            getString(R.string.category_bank),
            getString(R.string.category_game)
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        spCategory.adapter = adapter
    }

    private fun setupEditMode() {
        val docId = intent.getStringExtra("id")
        isEditMode = docId != null

        if (isEditMode) {
            etTitle.setText(intent.getStringExtra("title"))
            etUsername.setText(intent.getStringExtra("username"))
            etPassword.setText(intent.getStringExtra("password"))
            etUrl.setText(intent.getStringExtra("url"))

            val category = intent.getStringExtra("category") ?: getString(R.string.category_other)
            @Suppress("UNCHECKED_CAST")
            val adapter = spCategory.adapter as ArrayAdapter<String>
            val index = adapter.getPosition(category)
            spCategory.setSelection(index)

            tvHeaderTitle.text = getString(R.string.edit_title)
            btnSave.text = getString(R.string.edit_save_button)
        }
    }

    private fun setupActions() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveData()
        }

        btnGeneratePassword.setOnClickListener {
            val generated = generatePassword()
            etPassword.setText(generated)
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.add_password_generated), Snackbar.LENGTH_SHORT).show()
        }

        var isPasswordVisible = false
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.transformationMethod = null
                btnTogglePassword.setImageResource(R.drawable.ic_eye_on)
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
        }
    }

    private fun saveData() {
        val title = etTitle.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val passwordRaw = etPassword.text.toString().trim()
        val password = CryptoUtil.encrypt(passwordRaw)
        val url = etUrl.text.toString().trim()
        val category = spCategory.selectedItem.toString()

        val uid = auth.currentUser?.uid ?: return

        if (title.isEmpty()) {
            etTitle.error = getString(R.string.add_title_required)
            return
        }

        if (password.isEmpty()) {
            etPassword.error = getString(R.string.add_password_required)
            return
        }

        if (url.isNotEmpty() && !Patterns.WEB_URL.matcher(url).matches()) {
            etUrl.error = getString(R.string.add_url_invalid)
            return
        }

        val data = PasswordItem(
            title = title,
            username = username,
            password = password,
            url = url,
            category = category,
            userId = uid
        )

        val docId = intent.getStringExtra("id")

        if (docId != null) {
            updateData(docId, data)
        } else {
            addData(data)
        }
    }

    private fun addData(data: PasswordItem) {
        db.collection("passwords")
            .add(data)
            .addOnSuccessListener {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.save_success), Snackbar.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.save_failed, "Gagal menyimpan data"), Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun updateData(id: String, data: PasswordItem) {
        db.collection("passwords")
            .document(id)
            .set(data)
            .addOnSuccessListener {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.update_success), Snackbar.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.save_failed, "Gagal memperbarui data"), Snackbar.LENGTH_SHORT).show()
            }
    }
}
