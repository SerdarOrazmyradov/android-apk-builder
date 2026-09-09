package io.turkmensms.aigateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.turkmensms.aigateway.data.BridgeConfigStore
import io.turkmensms.aigateway.service.BridgeForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        val store = BridgeConfigStore(context)
        val config = store.load()
        if (config.bridgeEnabled && config.isReady()) {
            BridgeForegroundService.ensureChannel(context)
            BridgeForegroundService.start(context)
        }
    }
}
