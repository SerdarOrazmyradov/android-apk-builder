package io.turkmensms.aigateway.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import io.turkmensms.aigateway.R
import io.turkmensms.aigateway.service.BridgeForegroundService

class HomeActivity : AppCompatActivity() {
    private lateinit var viewModel: HomeViewModel
    private val handler = Handler(Looper.getMainLooper())
    private val refresher = object : Runnable {
        override fun run() {
            viewModel.refresh()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[HomeViewModel::class.java]

        val tvStatus = findViewById<TextView>(R.id.tvBridgeStatus)
        val tvPoll = findViewById<TextView>(R.id.tvLastPoll)
        val tvError = findViewById<TextView>(R.id.tvLastError)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        viewModel.status.observe(this) { status ->
            tvStatus.text = status.statusText
            tvPoll.text = status.lastPollText
            tvError.text = status.errorText
        }

        btnStart.setOnClickListener { startBridge() }
        btnStop.setOnClickListener { stopBridge() }
        findViewById<Button>(R.id.btnSetup).setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        requestSmsPermissions()
        BridgeForegroundService.ensureChannel(this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        handler.post(refresher)
    }

    override fun onPause() {
        handler.removeCallbacks(refresher)
        super.onPause()
    }

    private fun startBridge() {
        if (!hasSmsPermissions()) {
            Toast.makeText(this, getString(R.string.need_permissions), Toast.LENGTH_SHORT).show()
            requestSmsPermissions()
            return
        }
        if (!viewModel.canStart()) {
            Toast.makeText(this, getString(R.string.need_config), Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.markEnabled(true)
        BridgeForegroundService.ensureChannel(this)
        BridgeForegroundService.start(this)
        viewModel.refresh()
    }

    private fun stopBridge() {
        viewModel.markEnabled(false)
        BridgeForegroundService.stop(this)
        viewModel.refresh()
    }

    private fun hasSmsPermissions(): Boolean {
        return REQUIRED.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestSmsPermissions() {
        val missing = REQUIRED.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQ)
        }
    }

    companion object {
        private const val REQ = 101
        private val REQUIRED = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        )
    }
}
