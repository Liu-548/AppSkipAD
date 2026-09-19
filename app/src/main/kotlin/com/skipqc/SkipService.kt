package com.skipqc

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

// M1.1: connect and scope the service to the watched apps. No clicking yet (R-01..R-03 = M2).
class SkipService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        // R-04 / H5: always an explicit list, never null (= every app).
        serviceInfo = serviceInfo.apply { packageNames = WATCHED }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // R-70: nothing to do before M2 wires ModeLogic + RuleEngine.
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        isRunning = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    companion object {
        // M1: hardcoded. Comes from Prefs in M3.2 (R-05).
        private val WATCHED = arrayOf("com.google.android.youtube")

        // R-41: "enabled but not running" health check reads this.
        @Volatile
        var isRunning = false
            private set
    }
}
