package com.sampleapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sampleapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101
    private val API_BASE_URL = "http://10.0.2.2:8000"  // Default API endpoint

    private lateinit var etApiKey: EditText
    private lateinit var btnSave: Button
    private lateinit var btnTestApi: Button
    private lateinit var tvStatus: TextView

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etApiKey = findViewById(R.id.etApiKey)
        btnSave = findViewById(R.id.btnSave)
        btnTestApi = findViewById(R.id.btnTestApi)
        tvStatus = findViewById(R.id.tvStatus)

        loadSavedSettings()
        checkPermissions()

        btnSave.setOnClickListener {
            saveSettings()
        }

        btnTestApi.setOnClickListener {
            testApiConnection()
        }
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
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun loadSavedSettings() {
        val prefs = getSharedPreferences("GatewayPrefs", Context.MODE_PRIVATE)
        etApiKey.setText(prefs.getString("api_key", ""))
    }

    private fun saveSettings() {
        val key = etApiKey.text.toString().trim()

        if (key.isEmpty()) {
            Toast.makeText(this, "API Key boş bolmaly däl!", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("GatewayPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("api_key", key)
            .apply()

        Toast.makeText(this, "Sazlamalar ýatda saklandy!", Toast.LENGTH_SHORT).show()
        tvStatus.text = "Status: Sazlamalar girizildi"
    }

    private fun testApiConnection() {
        val key = etApiKey.text.toString().trim()

        if (key.isEmpty()) {
            Toast.makeText(this, "Ilki bilen URL we Key ýazyp saklaň!", Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text = "Status: Gemini AI barlaňýar..."

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$API_BASE_URL/api/ping")
                    .addHeader("X-API-KEY", key)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            tvStatus.text = "Status: Backend bilen baglanyşyk bar! (200 OK)"
                        } else {
                            tvStatus.text = "Status: Ýalňyşlyk kody: ${response.code}"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "Status: Baglanşyp bolmady (${e.localizedMessage})"
                }
            }
        }
    }
}
