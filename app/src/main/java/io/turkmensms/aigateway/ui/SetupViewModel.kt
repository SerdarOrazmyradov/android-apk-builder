package io.turkmensms.aigateway.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.turkmensms.aigateway.data.BridgeConfig
import io.turkmensms.aigateway.data.BridgeConfigStore
import io.turkmensms.aigateway.data.MailClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SetupViewModel(application: Application) : AndroidViewModel(application) {
    private val store = BridgeConfigStore(application)
    private val _config = MutableLiveData<BridgeConfig>()
    val config: LiveData<BridgeConfig> = _config
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun load() {
        _config.postValue(store.load())
    }

    fun save(config: BridgeConfig) {
        store.save(config)
        _config.postValue(store.load())
        _message.postValue("Saved")
    }

    fun testSmtp(config: BridgeConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = MailClient(config).testSmtp()
            _message.postValue(result.fold({ it }, { "SMTP failed: ${it.message}" }))
        }
    }

    fun testImap(config: BridgeConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = MailClient(config).testImap()
            _message.postValue(result.fold({ it }, { "IMAP failed: ${it.message}" }))
        }
    }
}
