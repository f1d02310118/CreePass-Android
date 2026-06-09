package com.example.projectmobile.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmobile.R
import com.example.projectmobile.adapter.PasswordAdapter
import com.example.projectmobile.model.PasswordItem
import com.example.projectmobile.dialog.PasswordDetailDialog

class SearchPasswordActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var rvPasswords: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnClearSearch: ImageButton

    private lateinit var adapter: PasswordAdapter
    private var fullList: MutableList<PasswordItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_password)

        initViews()
        loadData()
        setupRecyclerView()
        setupSearch()
        setupButtons()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        rvPasswords = findViewById(R.id.rvPasswords)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        btnBack = findViewById(R.id.btnBack)
        btnClearSearch = findViewById(R.id.btnClearSearch)
    }

    private fun loadData() {
        // =====================================================
        // DUMMY DATA — ganti dengan Room/SQLite dari tim nanti
        // =====================================================
        fullList = mutableListOf(
            PasswordItem(1, "Instagram",   "user@gmail.com",    "Insta@2024",    "instagram.com",  "Sosial Media"),
            PasswordItem(2, "Gmail",       "user@gmail.com",    "Gmail#Secure1", "gmail.com",      "Email"),
            PasswordItem(3, "Netflix",     "user@gmail.com",    "Netf!ix2024",   "netflix.com",    "Hiburan"),
            PasswordItem(4, "Bank BCA",    "1234567890",        "BCA@pass99",    "klikbca.com",    "Keuangan"),
            PasswordItem(5, "Tokopedia",   "user@gmail.com",    "Toped!2024",    "tokopedia.com",  "Belanja"),
            PasswordItem(6, "GitHub",      "devuser",           "G1tHub#Dev",    "github.com",     "Kerja"),
            PasswordItem(7, "Spotify",     "user@gmail.com",    "Spot!fy2024",   "spotify.com",    "Hiburan"),
            PasswordItem(8, "WhatsApp",    "+6281234567890",    "WA@pass123",    "whatsapp.com",   "Sosial Media"),
        )
    }

    private fun setupRecyclerView() {
        adapter = PasswordAdapter(this, fullList.toMutableList()) { item ->
            // Buka dialog detail saat item diklik
            showDetailDialog(item)
        }
        rvPasswords.layoutManager = LinearLayoutManager(this)
        rvPasswords.adapter = adapter
        updateEmptyState(fullList)
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()

                // Tampilkan/sembunyikan tombol clear
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                filterPassword(query)
            }
        })

        // Auto-fokus ke search bar saat masuk halaman
        etSearch.requestFocus()
    }

    private fun setupButtons() {
        btnBack.setOnClickListener {
            finish()
        }

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            etSearch.requestFocus()
        }
    }

    private fun filterPassword(query: String) {
        val filtered = if (query.isEmpty()) {
            fullList.toMutableList()
        } else {
            fullList.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                        item.username.contains(query, ignoreCase = true) ||
                        item.category.contains(query, ignoreCase = true) ||
                        item.url.contains(query, ignoreCase = true)
            }.toMutableList()
        }

        adapter.updateList(filtered)
        updateEmptyState(filtered)
    }

    private fun updateEmptyState(list: List<PasswordItem>) {
        if (list.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvPasswords.visibility = View.GONE

            val query = etSearch.text.toString().trim()
            tvEmptyState.text = if (query.isNotEmpty()) {
                "Tidak ada hasil untuk \"$query\""
            } else {
                "Belum ada password tersimpan"
            }
        } else {
            tvEmptyState.visibility = View.GONE
            rvPasswords.visibility = View.VISIBLE
        }
    }

    private fun showDetailDialog(item: PasswordItem) {
        val dialog = PasswordDetailDialog(this, item)
        dialog.show()
    }
}