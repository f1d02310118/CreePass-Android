package com.example.projectmobile.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projectmobile.R
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    // KUNCI PERBAIKAN: Deklarasi variabel wajib ada di sini agar tidak error merah
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
                etRegEmail.error = "Email wajib diisi!"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etRegPassword.error = "Password wajib diisi!"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etRegPassword.error = "Password minimal 6 karakter!"
                return@setOnClickListener
            }

            // Proses daftar akun baru ke Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Pendaftaran Berhasil!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Gagal Daftar: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}