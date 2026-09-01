package com.sampleapp.gateway

import android.app.Application
import org.conscrypt.Conscrypt
import java.security.Security

class GatewayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }
}