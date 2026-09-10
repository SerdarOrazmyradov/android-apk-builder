package com.example.sampleapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.EditText

class MainActivity : AppCompatActivity() {

    private val SMS_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // val btnOpenLogs = findViewById<Button>(R.id.btnOpenLogs)
        // btnOpenLogs.setOnClickListener {
        //     showLogDialog()
        // }

        val etSenderEmail = findViewById<EditText>(R.id.etSenderEmail)
        val etAppPassword = findViewById<EditText>(R.id.etAppPassword)
        val etReceiverEmail = findViewById<EditText>(R.id.etReceiverEmail)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Öňki saklanan maglumatlary ýüklemek
        etSenderEmail.setText(SmtpPreferences.getSenderEmail(this))
        etAppPassword.setText(SmtpPreferences.getAppPassword(this))
        etReceiverEmail.setText(SmtpPreferences.getReceiverEmail(this))

        btnSave.setOnClickListener {
            val sender = etSenderEmail.text.toString().trim()
            val pass = etAppPassword.text.toString().trim()
            val receiver = etReceiverEmail.text.toString().trim()

            if (sender.isEmpty() || pass.isEmpty() || receiver.isEmpty()) {
                Toast.makeText(this, "Ähli meýdanlary dolduryň!", Toast.LENGTH_SHORT).show()
            } else {
                SmtpPreferences.saveSettings(this, sender, pass, receiver)
                Toast.makeText(this, "Sazlamalar ýatda saklandy!", Toast.LENGTH_SHORT).show()
                startBridgeService()
            }
        }

    }

    private fun startBridgeService() {
        // Sazlamalaryň doly doldurylandygyny barlap soň başlatýarys
        val sender = SmtpPreferences.getSenderEmail(this)
        val pass = SmtpPreferences.getAppPassword(this)

        if (!sender.isNullOrEmpty() && !pass.isNullOrEmpty()) {
            val service = SmsMailBridgeService(applicationContext)
            service.startListening()
        }
    }

    private fun showLogDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_log_viewer)

        val tvLogContent = dialog.findViewById<TextView>(R.id.tvLogContent)
        val btnClearLogs = dialog.findViewById<Button>(R.id.btnClearLogs)
        val scrollView = dialog.findViewById<ScrollView>(R.id.scrollView)

        tvLogContent.text = LogManager.readLogs(this)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }

        btnClearLogs.setOnClickListener {
            LogManager.clearLogs(this)
            tvLogContent.text = LogManager.readLogs(this)
        }

        dialog.setCancelable(true)
        dialog.show()
    }
}