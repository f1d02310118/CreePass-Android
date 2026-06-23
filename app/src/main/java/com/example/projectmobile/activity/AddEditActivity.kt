package com.example.projectmobile.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projectmobile.R
import com.example.projectmobile.model.PasswordItem
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
    private lateinit var spCategory: Spinner
    private lateinit var tvHeaderTitle: TextView
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton

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
    }

    private fun setupSpinner() {
        val categories = listOf("Lainnya", "Email", "Sosial Media", "Bank", "Game")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        spCategory.adapter = adapter
    }

    private fun setupEditMode() {
        val docId = intent.getStringExtra("id")
        val isEdit = docId != null

        if (isEdit) {
            etTitle.setText(intent.getStringExtra("title"))
            etUsername.setText(intent.getStringExtra("username"))
            etPassword.setText(intent.getStringExtra("password"))
            etUrl.setText(intent.getStringExtra("url"))

            val category = intent.getStringExtra("category") ?: "Lainnya"
            val index = (spCategory.adapter as ArrayAdapter<String>).getPosition(category)
            spCategory.setSelection(index)

            tvHeaderTitle.text = "Edit Password"
            btnSave.text = "Simpan Perubahan"
        }
    }

    private fun setupActions() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveData()
        }
    }

    private fun saveData() {
        val title = etTitle.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val url = etUrl.text.toString().trim()
        val category = spCategory.selectedItem.toString()

        // val uid = auth.currentUser?.uid ?: return
        val uid = "HN81h5r5ajebYqueoMTsFiKP2Vi1"

        if (title.isEmpty()) {
            etTitle.error = "Nama aplikasi wajib diisi"
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
                Toast.makeText(this, "Data disimpan", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateData(id: String, data: PasswordItem) {
        db.collection("passwords")
            .document(id)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Data diperbarui", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}