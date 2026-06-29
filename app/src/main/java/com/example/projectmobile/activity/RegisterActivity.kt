package com.example.projectmobile.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.example.projectmobile.R
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var etRegEmail: EditText
    private lateinit var etRegPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etRegEmail = findViewById(R.id.etRegEmail)
        etRegPassword = findViewById(R.id.etRegPassword)
        btnRegister = findViewById(R.id.btnRegister)

        auth = FirebaseAuth.getInstance()

        btnRegister.setOnClickListener {
            val email = etRegEmail.text.toString().trim()
            val password = etRegPassword.text.toString().trim()

            if (email.isEmpty()) {
                etRegEmail.error = getString(R.string.register_email_required)
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etRegPassword.error = getString(R.string.register_password_required)
                return@setOnClickListener
            }
            if (password.length < 6) {
                etRegPassword.error = getString(R.string.register_password_min_length)
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.register_success), Snackbar.LENGTH_SHORT).show()

                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.register_failed, "Gagal mendaftar, coba lagi"), Snackbar.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
