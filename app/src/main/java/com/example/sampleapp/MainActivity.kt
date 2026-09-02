package com.example.sampleapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Funksiýany indi arkaýyn çagyryp bilersiňiz
        checkPermissionsAndStartService()
    }

    private fun checkPermissionsAndStartService() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        )

        // Android 6.0 (API 23) we ondan ýokary bolsa Runtime Permission soralýar
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
            // Android 5.1.1 we ondan pes ulgamlarda rugsattlar eýýäm berlen diýip hasaplanýar
            startGatewayService()
        }
    }

    private fun startGatewayService() {
        try {
            val serviceIntent = Intent(this, GatewayService::class.java)
            startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
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
            // Ähli rugsatlaryň berlendigini takyk barlamak
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                startGatewayService()
            } else {
                Toast.makeText(this, "SMS rugsatlary berilmedi!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}