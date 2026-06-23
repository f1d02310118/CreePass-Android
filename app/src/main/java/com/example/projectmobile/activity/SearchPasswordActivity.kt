package com.example.projectmobile.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmobile.R
import com.example.projectmobile.adapter.PasswordAdapter
import com.example.projectmobile.model.PasswordItem
import com.example.projectmobile.dialog.PasswordDetailDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class SearchPasswordActivity : AppCompatActivity() {
    private val db   = Firebase.firestore
    private val auth = Firebase.auth
    private lateinit var etSearch: EditText
    private lateinit var rvPasswords: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnAdd: ImageButton
    private lateinit var btnClearSearch: ImageButton
    private lateinit var adapter: PasswordAdapter
    private var fullList: MutableList<PasswordItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_password)

        initViews()
        setupRecyclerView()
        setupSearch()
        setupButtons()
        loadData()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        rvPasswords = findViewById(R.id.rvPasswords)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        btnBack = findViewById(R.id.btnBack)
        btnAdd = findViewById(R.id.btnAdd)
        btnClearSearch = findViewById(R.id.btnClearSearch)
    }

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun loadData() {
        // val uid = getCurrentUserId()
        val uid = "HN81h5r5ajebYqueoMTsFiKP2Vi1"

        if (uid == null) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("passwords")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->

                fullList.clear()

                for (doc in result) {
                    val item = doc.toObject(PasswordItem::class.java).copy(
                        id = doc.id
                    )
                    fullList.add(item)
                }

                adapter.updateList(fullList)
                updateEmptyState(fullList)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal load data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        adapter = PasswordAdapter(this, fullList.toMutableList(),
            onItemClick = { item ->
                // Buka dialog detail saat item diklik
                showDetailDialog(item)
            },
            onEditClick = { item ->
                // nanti arahkan ke edit activity
                val intent = Intent(this, AddEditActivity::class.java)
                intent.putExtra("id", item.id)
                startActivity(intent)
            },
            onDeleteClick = { item ->
                db.collection("passwords")
                    .document(item.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal hapus", Toast.LENGTH_SHORT).show()
                    }
            }
        )
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

        btnAdd.setOnClickListener {
            btnAdd.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).withEndAction {
                btnAdd.animate().scaleX(1f).scaleY(1f).duration = 80
            }

            startActivity(Intent(this, AddEditActivity::class.java))
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

    override fun onResume() {
        super.onResume()
        loadData()
    }
}