package io.turkmensms.aigateway.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.turkmensms.aigateway.data.BridgeConfigStore
import io.turkmensms.aigateway.service.BridgeForegroundService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = BridgeConfigStore(application)
    private val _status = MutableLiveData<HomeStatus>()
    val status: LiveData<HomeStatus> = _status

    fun refresh() {
        val running = BridgeForegroundService.isRunning()
        val pollAt = BridgeForegroundService.lastPollMillis()
        val error = BridgeForegroundService.lastErrorMessage()
        val pollText = if (pollAt == 0L) {
            "Last poll: never"
        } else {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            "Last poll: ${fmt.format(Date(pollAt))}"
        }
        _status.postValue(
            HomeStatus(
                running = running,
                statusText = if (running) "Running" else "Stopped",
                lastPollText = pollText,
                errorText = error
            )
        )
    }

    fun canStart(): Boolean = store.load().isReady()

    fun markEnabled(enabled: Boolean) {
        store.setEnabled(enabled)
    }

    data class HomeStatus(
        val running: Boolean,
        val statusText: String,
        val lastPollText: String,
        val errorText: String
    )
}
