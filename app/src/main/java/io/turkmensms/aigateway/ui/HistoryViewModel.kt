package io.turkmensms.aigateway.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.turkmensms.aigateway.data.BridgeLog
import io.turkmensms.aigateway.data.LogRepository

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val logs = LogRepository(application)
    private val _items = MutableLiveData<List<BridgeLog>>()
    val items: LiveData<List<BridgeLog>> = _items

    fun refresh() {
        _items.postValue(logs.latest())
    }
}
