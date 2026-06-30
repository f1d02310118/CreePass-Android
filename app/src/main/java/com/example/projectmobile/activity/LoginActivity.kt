package com.example.projectmobile.activity

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.example.projectmobile.R
import com.example.projectmobile.activity.SearchPasswordActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvToRegister: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBarLogin)
        tvToRegister = findViewById(R.id.tvToRegister)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            navigateToHome()
            return
        }

        tvToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = getString(R.string.login_email_required)
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = getString(R.string.login_password_required)
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = getString(R.string.login_email_invalid)
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false
            tvToRegister.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    tvToRegister.isEnabled = true

                    if (task.isSuccessful) {
                        navigateToHome()
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.login_failed), Snackbar.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, SearchPasswordActivity::class.java)
        startActivity(intent)
        finish()
    }
}
