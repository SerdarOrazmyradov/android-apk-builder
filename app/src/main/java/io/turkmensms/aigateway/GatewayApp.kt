package io.turkmensms.aigateway

import android.app.Application
import org.conscrypt.Conscrypt
import java.security.Security

class GatewayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        } catch (_: Exception) {
        }
    }
}
