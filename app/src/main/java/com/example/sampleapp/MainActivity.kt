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
import com.google.android.gms.security.ProviderInstaller

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            ProviderInstaller.installIfNeeded(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "TLS Provider install etmekde säwlik: ${e.message}", Toast.LENGTH_LONG).show()
        }

        setContentView(R.layout.activity_main)

        // Düwmä basylanda log dialogyny açmak
        val btnOpenLogs = findViewById<Button>(R.id.btnOpenLogs)
        btnOpenLogs.setOnClickListener {
            showLogDialog()
        }

        checkPermissionsAndStartService()
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

        // Fiziki Back (Yza) düwmesi basylsa dialog awtomatiki ýapylýar
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun checkPermissionsAndStartService() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val neededPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (neededPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), 100)
            } else {
                startGatewayService()
            }
        } else {
            startGatewayService()
        }
    }

    private fun startGatewayService() {
        try {
            val serviceIntent = Intent(this, GatewayService::class.java)
            startService(serviceIntent)
            LogManager.log(this, "MainActivity", "GatewayService üstünlikli başladyldy.")
        } catch (e: Exception) {
            e.printStackTrace()
            LogManager.log(this, "MainActivity", "Service säwligi: ${e.message}")
            Toast.makeText(this, "Service başlatmakda säwlik: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                startGatewayService()
            } else {
                LogManager.log(this, "MainActivity", "SMS rugsatlary ret edildi.")
                Toast.makeText(this, "SMS rugsatlary berilmedi!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}