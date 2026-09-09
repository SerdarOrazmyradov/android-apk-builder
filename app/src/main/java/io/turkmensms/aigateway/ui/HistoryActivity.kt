package io.turkmensms.aigateway.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.turkmensms.aigateway.R

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[HistoryViewModel::class.java]
        val adapter = LogAdapter()
        val rv = findViewById<RecyclerView>(R.id.rvLogs)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        viewModel.items.observe(this) { adapter.submit(it) }
        viewModel.refresh()
    }
}
