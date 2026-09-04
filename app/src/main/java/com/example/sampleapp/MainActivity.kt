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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val btnOpenLogs = findViewById<Button>(R.id.btnOpenLogs)
        btnOpenLogs.setOnClickListener {
            showLogDialog()
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