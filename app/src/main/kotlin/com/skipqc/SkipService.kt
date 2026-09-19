package com.skipqc

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

// Events -> RuleEngine -> click (R-01..R-03). Modes arrive in M3; until then always active.
class SkipService : AccessibilityService() {

    private var rules: Rules? = null
    private val lastEventAt = HashMap<String, Long>()
    private val ignoreUntil = HashMap<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        // R-04 / H5: always an explicit list, never null (= every app).
        serviceInfo = serviceInfo.apply { packageNames = WATCHED }
        // R-50: rules are read once, here.
        rules = runCatching {
            Rules.parse(assets.open("rules.json").bufferedReader().use { it.readText() })
        }.getOrNull()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!active) return // R-70: cheapest possible exit.
        if (BuildConfig.DEBUG) captureTree(event)

        val rules = rules ?: return
        val pkg = event?.packageName?.toString() ?: return
        val now = SystemClock.uptimeMillis()
        if (now < (ignoreUntil[pkg] ?: 0L)) return                    // R-03 cooldown
        if (now - (lastEventAt[pkg] ?: 0L) < DEBOUNCE_MS) return      // R-03 debounce
        lastEventAt[pkg] = now

        val root = rootInActiveWindow ?: return                        // R-71
        if (root.packageName != pkg) return
        val target = RuleEngine.findTarget(rules, pkg, NodeInfoFinder(root)) ?: return
        if (click(target)) ignoreUntil[pkg] = now + COOLDOWN_MS
    }

    // R-02 step 3: press the node, or tap its centre when nothing around it is clickable.
    private fun click(target: NodeView): Boolean {
        val node = (target as? NodeInfoView)?.node ?: return false
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        if (bounds.isEmpty) return false
        val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
        val stroke = GestureDescription.StrokeDescription(path, 0, TAP_MS)
        return dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
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

        private const val DEBOUNCE_MS = 100L   // R-03
        private const val COOLDOWN_MS = 1500L  // R-03
        private const val TAP_MS = 50L
        private const val CAPTURE_INTERVAL_MS = 1000L

        private var lastCapture = 0L

        // M3.2 hands this over to ModeLogic (R-10..R-14).
        @Volatile
        var active = true

        // R-41: "enabled but not running" health check reads this.
        @Volatile
        var isRunning = false
            private set

        // R-80: debug only, read by DebugDump.
        @Volatile
        var lastTree: String? = null
            private set
    }
}

// Adapters between the framework tree and the pure RuleEngine (R-02).
private class NodeInfoView(val node: AccessibilityNodeInfo) : NodeView {
    override val viewId: String? get() = node.viewIdResourceName
    override val text: String? get() = node.text?.toString()
    override val description: String? get() = node.contentDescription?.toString()
    override val isVisible: Boolean get() = node.isVisibleToUser
    override val isEnabled: Boolean get() = node.isEnabled
    override val isClickable: Boolean get() = node.isClickable
    override val parent: NodeView? get() = node.parent?.let { NodeInfoView(it) }
}

private class NodeInfoFinder(private val root: AccessibilityNodeInfo) : NodeFinder {
    override fun byViewId(viewId: String): List<NodeView> =
        root.findAccessibilityNodeInfosByViewId(viewId).orEmpty().map { NodeInfoView(it) }

    override fun byText(text: String): List<NodeView> =
        root.findAccessibilityNodeInfosByText(text).orEmpty().map { NodeInfoView(it) }
}
