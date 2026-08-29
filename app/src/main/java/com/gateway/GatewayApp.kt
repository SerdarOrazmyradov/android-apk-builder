package com.gateway

import android.app.Application
import com.google.android.gms.security.ProviderInstaller
import org.conscrypt.Conscrypt
import java.security.Security

class GatewayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        enableTlsSupport()
    }

    private fun enableTlsSupport() {
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (e: Exception) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }
    }
}
