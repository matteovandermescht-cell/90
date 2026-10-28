package com.smith.screenbot

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Status-only AccessibilityService.
 * It does not inspect window content or perform gestures.
 */
class TouchService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        sendStatus("Accessibility-Dienst verbunden (Statusmodus).")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally no UI inspection or automated actions.
    }

    override fun onInterrupt() {
        sendStatus("Accessibility-Dienst unterbrochen.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        sendStatus("Accessibility-Dienst getrennt.")
        return super.onUnbind(intent)
    }

    private fun sendStatus(message: String) {
        sendBroadcast(
            Intent(AppEvents.ACTION_FEEDBACK)
                .setPackage(packageName)
                .putExtra(AppEvents.EXTRA_MESSAGE, message)
        )
    }
}
