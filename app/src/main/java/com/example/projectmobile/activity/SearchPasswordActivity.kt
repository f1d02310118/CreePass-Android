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
import com.google.android.gms.tasks.Tasks
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.Source
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SearchPasswordActivity : AppCompatActivity() {
    companion object {
        private var firestoreConfigured = false
    }

    private val db   = Firebase.firestore
    private val auth = Firebase.auth
    private lateinit var etSearch: EditText
    private lateinit var rvPasswords: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnAdd: FloatingActionButton
    private lateinit var btnClearSearch: ImageButton
    private lateinit var btnLogout: ImageButton
    private lateinit var btnMoreOptions: ImageButton
    private lateinit var layoutHeader: LinearLayout
    private lateinit var adapter: PasswordAdapter
    private var fullList: MutableList<PasswordItem> = mutableListOf()
    private var searchHandler = Handler(Looper.getMainLooper())
    private var biometricRetryHandler: Handler? = null
    private var searchRunnable: Runnable? = null
    private var inactivityHandler = Handler(Looper.getMainLooper())
    private var inactivityRunnable: Runnable? = null
    private var lastPauseTime = 0L
    private var forceServerFetch = false
    private var returningFromCrud = false

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) performExport(uri)
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) performImport(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_password)

        if (!firestoreConfigured) {
            db.firestoreSettings = firestoreSettings {
                setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            }
            firestoreConfigured = true
        }

        initViews()
        setupRecyclerView()
        setupSearch()

        if (savedInstanceState != null) {
            etSearch.setText(savedInstanceState.getString("searchText", ""))
        }

        setupButtons()
        setupInactivityTimer()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        rvPasswords = findViewById(R.id.rvPasswords)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
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
        layoutEmpty.visibility = View.GONE
        rvPasswords.visibility = View.GONE

        val query = db.collection("passwords")
            .whereEqualTo("userId", uid)

        val task = if (forceServerFetch) {
            forceServerFetch = false
            query.get(Source.SERVER)
        } else {
            query.get()
        }

        task.addOnSuccessListener { result ->
            forceServerFetch = false
            fullList = result.mapNotNull { doc ->
                val raw = doc.toObject(PasswordItem::class.java).copy(id = doc.id)
                val decrypted = CryptoUtil.decrypt(raw.password)
                raw.copy(password = decrypted)
            }.toMutableList()

            progressBar.visibility = View.GONE
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                filterPassword(query)
            } else {
                adapter.updateList(fullList)
                updateEmptyState(fullList)
            }
        }
        .addOnFailureListener {
            forceServerFetch = false
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
                forceServerFetch = true
                returningFromCrud = true
                val intent = Intent(this, AddEditActivity::class.java)
                intent.putExtra("id", item.id)
                intent.putExtra("title", item.title)
                intent.putExtra("username", item.username)
                intent.putExtra("password", item.password)
                intent.putExtra("url", item.url)
                intent.putExtra("category", item.category)
                intent.putExtra("createdAt", item.createdAt)
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
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
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
                progressBar.visibility = View.VISIBLE
                rvPasswords.visibility = View.GONE
                layoutEmpty.visibility = View.GONE
                db.collection("passwords")
                    .document(item.id)
                    .delete()
                    .addOnSuccessListener {
                        forceServerFetch = true
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.delete_success), Snackbar.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.delete_failed), Snackbar.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton(getString(R.string.cancel), null)
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
                forceServerFetch = true
                returningFromCrud = true
                startActivity(Intent(this@SearchPasswordActivity, AddEditActivity::class.java))
            }
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

        findViewById<View>(android.R.id.content).setOnTouchListener { _, _ ->
            resetInactivityTimer()
            false
        }
    }

    private fun setupInactivityTimer() {
        resetInactivityTimer()
    }

    private fun resetInactivityTimer() {
        inactivityRunnable?.let { inactivityHandler.removeCallbacks(it) }
        inactivityRunnable = Runnable {
            if (auth.currentUser != null) {
                showBiometricPrompt()
            }
        }
        inactivityHandler.postDelayed(inactivityRunnable!!, 300000L)
    }

    private fun showLogoutConfirm() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout))
            .setMessage(getString(R.string.logout_confirm))
            .setPositiveButton(getString(R.string.logout)) { _, _ ->
                auth.signOut()
                forceServerFetch = false
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.logout_success), Snackbar.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun exportData() {
        val uid = getCurrentUserId()
        if (uid == null) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.login_not_logged_in), Snackbar.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.export_title))
            .setMessage(getString(R.string.export_warning))
            .setPositiveButton(getString(R.string.export_confirm)) { _, _ ->
                exportLauncher.launch("CreePass_Backup.crp")
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.import_title))
            .setMessage(getString(R.string.import_confirm))
            .setPositiveButton(getString(R.string.import_confirm_button)) { _, _ ->
                importLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun performImport(uri: android.net.Uri) {
        val uid = getCurrentUserId() ?: return

        try {
            val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return

            val jsonArray = JSONArray(jsonString)
            progressBar.visibility = View.VISIBLE
            rvPasswords.visibility = View.GONE
            layoutEmpty.visibility = View.GONE

            db.collection("passwords")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { existingDocs ->
                    val existingKeys = existingDocs.map {
                        "${it.getString("title") ?: ""}|${it.getString("username") ?: ""}"
                    }.toSet()

                    val now = System.currentTimeMillis()
                    val addTasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()
                    var importedCount = 0

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val title = obj.optString("title", "")
                        val username = obj.optString("username", "")
                        val passwordPlain = obj.optString("password", "")
                        val url = obj.optString("url", "")
                        val category = obj.optString("category", "Lainnya")

                        if (!obj.has("title") && !obj.has("password")) continue
                        if (title.isEmpty() && passwordPlain.isEmpty()) continue
                        if ("$title|$username" in existingKeys) continue

                        val passwordEncrypted = CryptoUtil.encrypt(passwordPlain)
                        val item = PasswordItem(
                            title = title,
                            username = username,
                            password = passwordEncrypted,
                            url = url,
                            category = category,
                            userId = uid,
                            createdAt = now,
                            updatedAt = now
                        )

                        addTasks.add(db.collection("passwords").add(item))
                        importedCount++
                    }

                    if (addTasks.isEmpty()) {
                        progressBar.visibility = View.GONE
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.import_empty), Snackbar.LENGTH_SHORT).show()
                        loadData()
                        return@addOnSuccessListener
                    }

                    Tasks.whenAll(addTasks)
                        .addOnSuccessListener {
                            forceServerFetch = true
                            progressBar.visibility = View.GONE
                            Snackbar.make(findViewById(android.R.id.content), getString(R.string.import_success) + " ($importedCount)", Snackbar.LENGTH_SHORT).show()
                            loadData()
                        }
                        .addOnFailureListener {
                            progressBar.visibility = View.GONE
                            Snackbar.make(findViewById(android.R.id.content), getString(R.string.import_failed), Snackbar.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.import_failed), Snackbar.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
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
            layoutEmpty.visibility = View.VISIBLE
            rvPasswords.visibility = View.GONE

            val query = etSearch.text.toString().trim()
            tvEmptyState.text = if (query.isNotEmpty()) {
                getString(R.string.search_empty_query, query)
            } else {
                getString(R.string.search_empty_default)
            }
        } else {
            layoutEmpty.visibility = View.GONE
            rvPasswords.visibility = View.VISIBLE
        }
    }

    private fun showDetailDialog(item: PasswordItem) {
        val dialog = PasswordDetailDialog(this, item)
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        if (returningFromCrud) {
            returningFromCrud = false
            loadData()
            return
        }
        val sincePause = System.currentTimeMillis() - lastPauseTime
        if (sincePause > 5000 && auth.currentUser != null) {
            showBiometricPrompt()
        } else {
            loadData()
        }
        setupInactivityTimer()
    }

    override fun onPause() {
        super.onPause()
        lastPauseTime = System.currentTimeMillis()
        inactivityRunnable?.let { inactivityHandler.removeCallbacks(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("searchText", etSearch.text.toString())
    }

    override fun onDestroy() {
        super.onDestroy()
        searchHandler.removeCallbacksAndMessages(null)
        biometricRetryHandler?.removeCallbacksAndMessages(null)
        inactivityHandler.removeCallbacksAndMessages(null)
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                loadData()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    finish()
                } else if (errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE ||
                           errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                           errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT) {
                    loadData()
                } else {
                    biometricRetryHandler = Handler(Looper.getMainLooper())
                    biometricRetryHandler!!.postDelayed({
                        showBiometricPrompt()
                    }, 1000)
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.login_title))
            .build()

        biometricPrompt.authenticate(promptInfo)
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
