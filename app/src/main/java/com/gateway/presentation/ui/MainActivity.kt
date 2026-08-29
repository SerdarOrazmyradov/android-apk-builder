package com.gateway.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gateway.data.db.DatabaseHelper
import com.example.sampleapp.R

class MainActivity : AppCompatActivity() {
    private lateinit var apiKeyInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var statusText: TextView
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)
        apiKeyInput = findViewById(R.id.api_key_input)
        phoneInput = findViewById(R.id.phone_input)
        statusText = findViewById(R.id.status_text)
        val saveApiKeyBtn = findViewById<Button>(R.id.save_api_key_btn)
        val addUserBtn = findViewById<Button>(R.id.add_user_btn)

        saveApiKeyBtn.setOnClickListener { saveApiKey() }
        addUserBtn.setOnClickListener { addUser() }

        requestPermissions()
        updateStatus()
    }

    private fun saveApiKey() {
        val apiKey = apiKeyInput.text.toString().trim()
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Enter API Key", Toast.LENGTH_SHORT).show()
            return
        }
        db.saveApiKey(apiKey)
        apiKeyInput.setText("")
        Toast.makeText(this, "API Key Saved", Toast.LENGTH_SHORT).show()
        updateStatus()
    }

    private fun addUser() {
        val phone = phoneInput.text.toString().trim()
        if (phone.isEmpty()) {
            Toast.makeText(this, "Enter Phone Number", Toast.LENGTH_SHORT).show()
            return
        }
        if (db.addUser(phone)) {
            phoneInput.setText("")
            Toast.makeText(this, "User Added: $phone", Toast.LENGTH_SHORT).show()
            updateStatus()
        } else {
            Toast.makeText(this, "User Already Exists", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatus() {
        val apiKey = db.getApiKey()
        val status = "API Key: " + if (apiKey.isEmpty()) "Not Set" else "✓ Configured"
        statusText.text = status
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.INTERNET
            )

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}
