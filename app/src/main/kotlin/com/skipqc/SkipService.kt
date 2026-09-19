package com.skipqc

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
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
        // R-80 / H4: keep the last window for the dump button, debug builds only.
        if (BuildConfig.DEBUG) captureTree(event)
    }

    // R-80: render the tree now, while the window is alive — a node reference kept until
    // the user reaches the dump button is stale and hands back no children. Throttled, and
    // only for a root that really belongs to the watched app (events keep arriving after
    // the user left it, when the active window is someone else's).
    private fun captureTree(event: AccessibilityEvent?) {
        val now = SystemClock.uptimeMillis()
        if (now - lastCapture < CAPTURE_INTERVAL_MS) return
        val root = rootInActiveWindow ?: return
        if (root.packageName != event?.packageName) return
        lastCapture = now
        lastTree = DebugDump.render(root)
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

        private const val CAPTURE_INTERVAL_MS = 1000L
        private var lastCapture = 0L

        // R-80: debug only, read by DebugDump.
        @Volatile
        var lastTree: String? = null
            private set
    }
}
