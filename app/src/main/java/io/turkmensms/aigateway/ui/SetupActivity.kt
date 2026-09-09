package io.turkmensms.aigateway.ui

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import io.turkmensms.aigateway.R
import io.turkmensms.aigateway.data.BridgeConfig

class SetupActivity : AppCompatActivity() {
    private lateinit var viewModel: SetupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[SetupViewModel::class.java]

        val etSmtpHost = findViewById<EditText>(R.id.etSmtpHost)
        val etSmtpPort = findViewById<EditText>(R.id.etSmtpPort)
        val etSmtpUser = findViewById<EditText>(R.id.etSmtpUser)
        val etSmtpPassword = findViewById<EditText>(R.id.etSmtpPassword)
        val cbSmtpStartTls = findViewById<CheckBox>(R.id.cbSmtpStartTls)
        val etImapHost = findViewById<EditText>(R.id.etImapHost)
        val etImapPort = findViewById<EditText>(R.id.etImapPort)
        val etImapUser = findViewById<EditText>(R.id.etImapUser)
        val etImapPassword = findViewById<EditText>(R.id.etImapPassword)
        val cbImapSsl = findViewById<CheckBox>(R.id.cbImapSsl)
        val etImapFolder = findViewById<EditText>(R.id.etImapFolder)
        val etForwardTo = findViewById<EditText>(R.id.etForwardTo)
        val etAllowed = findViewById<EditText>(R.id.etAllowedSenders)
        val etPoll = findViewById<EditText>(R.id.etPollInterval)
        val tvStatus = findViewById<TextView>(R.id.tvSetupStatus)

        viewModel.config.observe(this) { cfg ->
            etSmtpHost.setText(cfg.smtpHost)
            etSmtpPort.setText(cfg.smtpPort.toString())
            etSmtpUser.setText(cfg.smtpUser)
            etSmtpPassword.setText(cfg.smtpPassword)
            cbSmtpStartTls.isChecked = cfg.smtpStartTls
            etImapHost.setText(cfg.imapHost)
            etImapPort.setText(cfg.imapPort.toString())
            etImapUser.setText(cfg.imapUser)
            etImapPassword.setText(cfg.imapPassword)
            cbImapSsl.isChecked = cfg.imapSsl
            etImapFolder.setText(cfg.imapFolder)
            etForwardTo.setText(cfg.forwardTo)
            etAllowed.setText(cfg.allowedSenders.joinToString("\n"))
            etPoll.setText(cfg.pollIntervalSeconds.toString())
        }
        viewModel.message.observe(this) { msg ->
            tvStatus.text = msg
            if (msg == "Saved") {
                Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
            }
        }

        fun readConfig(): BridgeConfig {
            return BridgeConfig(
                smtpHost = etSmtpHost.text.toString(),
                smtpPort = etSmtpPort.text.toString().toIntOrNull() ?: 587,
                smtpUser = etSmtpUser.text.toString(),
                smtpPassword = etSmtpPassword.text.toString(),
                smtpStartTls = cbSmtpStartTls.isChecked,
                imapHost = etImapHost.text.toString(),
                imapPort = etImapPort.text.toString().toIntOrNull() ?: 993,
                imapUser = etImapUser.text.toString(),
                imapPassword = etImapPassword.text.toString(),
                imapSsl = cbImapSsl.isChecked,
                imapFolder = etImapFolder.text.toString().ifBlank { "INBOX" },
                forwardTo = etForwardTo.text.toString(),
                allowedSenders = etAllowed.text.toString().split('\n').map { it.trim() }.filter { it.isNotEmpty() },
                pollIntervalSeconds = etPoll.text.toString().toIntOrNull() ?: 30,
                bridgeEnabled = viewModel.config.value?.bridgeEnabled ?: false,
                lastImapUid = viewModel.config.value?.lastImapUid ?: 0L
            )
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener { viewModel.save(readConfig()) }
        findViewById<Button>(R.id.btnTestSmtp).setOnClickListener { viewModel.testSmtp(readConfig()) }
        findViewById<Button>(R.id.btnTestImap).setOnClickListener { viewModel.testImap(readConfig()) }

        viewModel.load()
    }
}
