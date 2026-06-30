package com.example.projectmobile.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmobile.R
import com.example.projectmobile.adapter.PasswordAdapter
import com.example.projectmobile.model.PasswordItem
import com.example.projectmobile.dialog.PasswordDetailDialog
import com.example.projectmobile.util.CryptoUtil
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings

class SearchPasswordActivity : AppCompatActivity() {
    private val db   = Firebase.firestore
    private val auth = Firebase.auth
    private lateinit var etSearch: EditText
    private lateinit var rvPasswords: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAdd: ImageButton
    private lateinit var btnClearSearch: ImageButton
    private lateinit var btnLogout: ImageButton
    private lateinit var btnMoreOptions: ImageButton
    private lateinit var layoutHeader: LinearLayout
    private lateinit var adapter: PasswordAdapter
    private var fullList: MutableList<PasswordItem> = mutableListOf()
    private var searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) performExport(uri)
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) performImport(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_password)

        db.firestoreSettings = firestoreSettings {
            setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
        }
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
        progressBar = findViewById(R.id.progressBar)
        btnAdd = findViewById(R.id.btnAdd)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        btnLogout = findViewById(R.id.btnLogout)
        btnMoreOptions = findViewById(R.id.btnMoreOptions)
        layoutHeader = findViewById(R.id.layoutHeader)

        ViewCompat.setOnApplyWindowInsetsListener(layoutHeader) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(view.paddingLeft, top + 16, view.paddingRight, view.paddingBottom)
            insets
        }
    }

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun loadData() {
        val uid = getCurrentUserId()

        if (uid == null) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.login_not_logged_in), Snackbar.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        rvPasswords.visibility = View.GONE

        db.collection("passwords")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->

                fullList.clear()

                for (doc in result) {
                    val raw = doc.toObject(PasswordItem::class.java).copy(
                        id = doc.id
                    )
                    val item = raw.copy(password = CryptoUtil.decrypt(raw.password))
                    fullList.add(item)
                }

                progressBar.visibility = View.GONE
                adapter.updateList(fullList)
                updateEmptyState(fullList)
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.load_failed), Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.load_retry)) { loadData() }
                    .show()
            }
    }

    private fun setupRecyclerView() {
        adapter = PasswordAdapter(this, fullList.toMutableList(),
            onItemClick = { item ->
                showDetailDialog(item)
            },
            onEditClick = { item ->
                val intent = Intent(this, AddEditActivity::class.java)
                intent.putExtra("id", item.id)
                intent.putExtra("title", item.title)
                intent.putExtra("username", item.username)
                intent.putExtra("password", item.password)
                intent.putExtra("url", item.url)
                intent.putExtra("category", item.category)
                startActivity(intent)
            },
            onDeleteClick = { item ->
                showDeleteConfirmation(item)
            }
        )
        rvPasswords.layoutManager = LinearLayoutManager(this)
        rvPasswords.adapter = adapter
        rvPasswords.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    currentFocus?.windowToken?.let { imm.hideSoftInputFromWindow(it, 0) }
                }
            }
        })
        updateEmptyState(fullList)
    }

    private fun showDeleteConfirmation(item: PasswordItem) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_title))
            .setMessage(getString(R.string.delete_message, item.title))
            .setPositiveButton(getString(R.string.delete_confirm)) { _, _ ->
                db.collection("passwords")
                    .document(item.id)
                    .delete()
                    .addOnSuccessListener {
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.delete_success), Snackbar.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener {
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.delete_failed), Snackbar.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton(getString(R.string.delete_cancel), null)
            .show()
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()

                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    filterPassword(query)
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })
    }

    private fun setupButtons() {
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

        btnMoreOptions.setOnClickListener { view ->
            PopupMenu(this, view).apply {
                menu.add(0, 1, 0, getString(R.string.export_title))
                menu.add(0, 2, 0, getString(R.string.import_title))
                menu.add(0, 3, 0, getString(R.string.logout))
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> exportData()
                        2 -> importData()
                        3 -> showLogoutConfirm()
                    }
                    true
                }
                show()
            }
        }

        btnLogout.setOnClickListener {
            showLogoutConfirm()
        }
    }

    private fun showLogoutConfirm() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout))
            .setMessage(getString(R.string.logout_confirm))
            .setPositiveButton(getString(R.string.logout)) { _, _ ->
                auth.signOut()
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.logout_success), Snackbar.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton(getString(R.string.delete_cancel), null)
            .show()
    }

    private fun exportData() {
        val uid = getCurrentUserId()
        if (uid == null) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.login_not_logged_in), Snackbar.LENGTH_SHORT).show()
            return
        }
        exportLauncher.launch("CreePass_Backup.json")
    }

    private fun performExport(uri: android.net.Uri) {
        val uid = getCurrentUserId() ?: return
        progressBar.visibility = View.VISIBLE

        db.collection("passwords")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->
                val jsonArray = JSONArray()
                for (doc in result) {
                    val raw = doc.toObject(PasswordItem::class.java).copy(id = doc.id)
                    val item = JSONObject().apply {
                        put("title", raw.title)
                        put("username", raw.username)
                        put("password", CryptoUtil.decrypt(raw.password))
                        put("url", raw.url)
                        put("category", raw.category)
                    }
                    jsonArray.put(item)
                }

                try {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonArray.toString(2).toByteArray(Charsets.UTF_8))
                    }
                    progressBar.visibility = View.GONE
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.export_success), Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    progressBar.visibility = View.GONE
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.export_failed), Snackbar.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.export_failed), Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun importData() {
        val uid = getCurrentUserId()
        if (uid == null) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.login_not_logged_in), Snackbar.LENGTH_SHORT).show()
            return
        }
        importLauncher.launch(arrayOf("application/json", "*/*"))
    }

    private fun performImport(uri: android.net.Uri) {
        val uid = getCurrentUserId() ?: return

        try {
            val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return

            val jsonArray = JSONArray(jsonString)
            var imported = 0

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", "")
                val username = obj.optString("username", "")
                val passwordPlain = obj.optString("password", "")
                val url = obj.optString("url", "")
                val category = obj.optString("category", "Lainnya")

                if (title.isEmpty() && passwordPlain.isEmpty()) continue

                val passwordEncrypted = CryptoUtil.encrypt(passwordPlain)
                val item = PasswordItem(
                    title = title,
                    username = username,
                    password = passwordEncrypted,
                    url = url,
                    category = category,
                    userId = uid
                )

                db.collection("passwords").add(item)
                imported++
            }

            Snackbar.make(findViewById(android.R.id.content), getString(R.string.import_success) + " ($imported)", Snackbar.LENGTH_SHORT).show()
            loadData()
        } catch (e: Exception) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.import_invalid_format), Snackbar.LENGTH_SHORT).show()
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
                getString(R.string.search_empty_query, query)
            } else {
                getString(R.string.search_empty_default)
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

    override fun onDestroy() {
        super.onDestroy()
        searchHandler.removeCallbacksAndMessages(null)
    }
}
