package io.turkmensms.aigateway.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.turkmensms.aigateway.R
import io.turkmensms.aigateway.data.model.AllowedUser
import io.turkmensms.aigateway.data.repository.UserRepository

class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 101

    private lateinit var etApiKey: EditText
    private lateinit var btnSave: Button
    private lateinit var etPhoneNumber: EditText
    private lateinit var btnTestApi: Button
    private lateinit var tvStatus: TextView
    private lateinit var lvUsers: ListView

    private lateinit var userRepository: UserRepository
    private var userAdapter: SimpleAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize repository
        userRepository = UserRepository(this)

        // Bind views
        etApiKey = findViewById(R.id.etApiKey)
        btnSave = findViewById(R.id.btnSave)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnTestApi = findViewById(R.id.btnTestApi)
        tvStatus = findViewById(R.id.tvStatus)
        lvUsers = findViewById(R.id.lvUsers)

        // Set up listeners
        btnSave.setOnClickListener { saveApiKey() }
        btnTestApi.setOnClickListener { addUser() }
        lvUsers.setOnItemLongClickListener { parent, _, position, _ ->
            val user = (parent.adapter.getItem(position) as? Map<*, *>)?.get("phone") as? String
            if (user != null) {
                deleteUser(user)
            }
            true
        }

        // Check permissions and load UI
        checkPermissions()
        loadSavedApiKey()
        refreshUserList()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.INTERNET
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            updateStatus("All permissions granted")
        }
    }

    private fun loadSavedApiKey() {
        val savedKey = userRepository.getGeminiApiKey()
        if (!savedKey.isNullOrBlank()) {
            etApiKey.setText(savedKey.take(10) + "...")
            updateStatus("API Key loaded")
        } else {
            updateStatus("No API Key set")
        }
    }

    private fun saveApiKey() {
        val key = etApiKey.text.toString().trim()

        if (key.isEmpty() || key.length < 10) {
            Toast.makeText(this, "Invalid API Key", Toast.LENGTH_SHORT).show()
            return
        }

        userRepository.saveGeminiApiKey(key)
        etApiKey.text.clear()
        etApiKey.setText(key.take(10) + "...")
        updateStatus("API Key saved successfully")
        Toast.makeText(this, "API Key saved", Toast.LENGTH_SHORT).show()
    }

    private fun addUser() {
        val phoneNumber = etPhoneNumber.text.toString().trim()

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate phone format (basic validation)
        if (!phoneNumber.matches(Regex("^\\+?[0-9]{10,15}$"))) {
            Toast.makeText(this, "Invalid phone number format", Toast.LENGTH_SHORT).show()
            return
        }

        val user = AllowedUser(
            phoneNumber = phoneNumber,
            geminiApiKey = "", // Uses main API key
            isActive = true
        )

        if (userRepository.addUser(user)) {
            etPhoneNumber.text.clear()
            refreshUserList()
            updateStatus("User added: $phoneNumber")
            Toast.makeText(this, "User added", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to add user", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteUser(phoneNumber: String) {
        if (userRepository.removeUser(phoneNumber)) {
            refreshUserList()
            updateStatus("User deleted: $phoneNumber")
            Toast.makeText(this, "User removed", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to delete user", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshUserList() {
        val users = userRepository.getAllUsers()
        val userList = users.map { user ->
            mapOf(
                "phone" to user.phoneNumber,
                "status" to (if (user.isActive) "Active" else "Inactive"),
                "date" to android.text.format.DateFormat.format("MM/dd/yyyy", user.addedAt)
            )
        }

        val from = arrayOf("phone", "status", "date")
        val to = intArrayOf(
            android.R.id.text1,
            android.R.id.text2,
            android.R.id.text3
        )

        userAdapter = SimpleAdapter(this, userList, android.R.layout.simple_list_item_2, from, to)
        lvUsers.adapter = userAdapter

        updateStatus("Registered users: ${users.size}")
    }

    private fun updateStatus(message: String) {
        tvStatus.text = "Status: $message"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                updateStatus("All permissions granted")
            } else {
                updateStatus("Some permissions denied")
                Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
