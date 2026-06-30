package com.example.projectmobile.activity

import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.example.projectmobile.R
import com.example.projectmobile.model.PasswordItem
import com.example.projectmobile.util.CryptoUtil
import com.example.projectmobile.util.generatePassword
import com.example.projectmobile.util.togglePasswordVisibility
import com.example.projectmobile.util.evaluatePasswordStrength
import com.example.projectmobile.util.PasswordStrength
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
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var layoutHeader: LinearLayout
    private lateinit var tvPasswordStrength: TextView
    private var isEditMode = false
    private var existingCreatedAt: Long = 0L

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
        progressBar = findViewById(R.id.progressBarAddEdit)
        btnBack = findViewById(R.id.btnBack)
        btnTogglePassword = findViewById(R.id.btnToggleAddPassword)
        btnGeneratePassword = findViewById(R.id.btnGeneratePassword)
        layoutHeader = findViewById(R.id.layoutHeader)
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength)

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
            R.layout.spinner_item,
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

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
            existingCreatedAt = intent.getLongExtra("createdAt", System.currentTimeMillis())

            val category = intent.getStringExtra("category") ?: getString(R.string.category_other)
            @Suppress("UNCHECKED_CAST")
            val adapter = spCategory.adapter as ArrayAdapter<String>
            val index = adapter.getPosition(category)
            spCategory.setSelection(if (index >= 0) index else 0)

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
            isPasswordVisible = togglePasswordVisibility(
                isPasswordVisible,
                etPassword,
                btnTogglePassword,
                R.drawable.ic_eye_on,
                R.drawable.ic_eye_off
            )
        }

        etPassword.addTextChangedListener(updatePasswordStrengthTextWatcher())
    }

    private fun updatePasswordStrengthTextWatcher(): android.text.TextWatcher {
        return object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                val password = s?.toString() ?: ""
                if (password.isEmpty()) {
                    tvPasswordStrength.visibility = View.GONE
                    return
                }
                tvPasswordStrength.visibility = View.VISIBLE
                val strength = evaluatePasswordStrength(password)
                when (strength) {
                    PasswordStrength.WEAK -> {
                        tvPasswordStrength.text = getString(R.string.password_strength_weak)
                        tvPasswordStrength.setTextColor(ContextCompat.getColor(this@AddEditActivity, R.color.accent_red))
                    }
                    PasswordStrength.MEDIUM -> {
                        tvPasswordStrength.text = getString(R.string.password_strength_medium)
                        tvPasswordStrength.setTextColor(ContextCompat.getColor(this@AddEditActivity, R.color.accent_orange))
                    }
                    PasswordStrength.STRONG -> {
                        tvPasswordStrength.text = getString(R.string.password_strength_strong)
                        tvPasswordStrength.setTextColor(ContextCompat.getColor(this@AddEditActivity, R.color.accent_green))
                    }
                    else -> tvPasswordStrength.visibility = View.GONE
                }
            }
        }
    }

    private fun saveData() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.login_not_logged_in), Snackbar.LENGTH_SHORT).show()
            return
        }

        val title = etTitle.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val passwordRaw = etPassword.text.toString().trim()
        val password = CryptoUtil.encrypt(passwordRaw)
        val url = etUrl.text.toString().trim()
        val category = spCategory.selectedItem.toString()
        val now = System.currentTimeMillis()

        if (title.isEmpty()) {
            etTitle.error = getString(R.string.add_title_required)
            return
        }

        if (passwordRaw.isEmpty()) {
            etPassword.error = getString(R.string.add_password_required)
            return
        }

        if (url.isNotEmpty()) {
            if (!Patterns.WEB_URL.matcher(url).matches()) {
                etUrl.error = getString(R.string.add_url_invalid)
                return
            }
            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                etUrl.error = getString(R.string.add_url_invalid)
                return
            }
        }

        val data = PasswordItem(
            title = title,
            username = username,
            password = password,
            url = url,
            category = category,
            userId = uid,
            createdAt = if (isEditMode) existingCreatedAt else now,
            updatedAt = now
        )

        val docId = intent.getStringExtra("id")

        if (docId != null) {
            updateData(docId, data)
        } else {
            addData(data)
        }
    }

    private fun addData(data: PasswordItem) {
        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        db.collection("passwords")
            .add(data)
            .addOnSuccessListener {
                finish()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                val errorMsg = e.message ?: getString(R.string.save_error_default)
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.save_failed, errorMsg), Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun updateData(id: String, data: PasswordItem) {
        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        db.collection("passwords")
            .document(id)
            .set(data)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.update_success), Snackbar.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                val errorMsg = e.message ?: getString(R.string.save_error_default)
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.save_failed, errorMsg), Snackbar.LENGTH_SHORT).show()
            }
    }

    override fun finish() {
        super.finish()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION") overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
