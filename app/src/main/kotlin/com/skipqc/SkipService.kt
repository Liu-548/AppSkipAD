package com.skipqc

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

// Events -> ModeLogic -> RuleEngine -> click (R-01..R-04, R-12, R-70..R-72).
class SkipService : AccessibilityService() {

    private var rules: Rules? = null
    private val lastEventAt = HashMap<String, Long>()
    private val ignoreUntil = HashMap<String, Long>()

    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())
    private var watchedInUse: Set<String>? = null

    // R-72: one tick a minute, and only while an automatic activation can still time out.
    private val idleTick = Runnable { prefs.onInput(ModeInput.IdleTick) }

    private val onStateChanged: (ModeState) -> Unit = { state ->
        applyWatched()
        handler.removeCallbacks(idleTick)
        if (state.active && state.autoActivated) handler.postDelayed(idleTick, IDLE_TICK_MS)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        prefs = Prefs.get(this)
        // R-05: YouTube is the default list, but only until the owner picks one.
        prefs.initWatchedIfUnset(DEFAULT_WATCHED.filter(::isInstalled).toSet())
        prefs.addListener(onStateChanged)
        // R-50: rules are read once, here.
        rules = runCatching {
            Rules.parse(assets.open("rules.json").bufferedReader().use { it.readText() })
        }.getOrNull()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val now = SystemClock.uptimeMillis()
        if (now - (lastEventAt[pkg] ?: 0L) < DEBOUNCE_MS) return      // R-03 debounce
        lastEventAt[pkg] = now

        // R-12: the service only ever sees watched apps, so every event feeds the mode machine.
        prefs.onInput(ModeInput.WatchedEvent(event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED))
        if (!prefs.state.active) return                                // R-70: before touching any node
        if (BuildConfig.DEBUG) captureTree(event)

        val rules = rules ?: return
        if (now < (ignoreUntil[pkg] ?: 0L)) return                     // R-03 cooldown
        val root = rootInActiveWindow ?: return                        // R-71
        if (root.packageName != pkg) return
        val target = RuleEngine.findTarget(rules, pkg, NodeInfoFinder(root)) ?: return
        if (click(target)) ignoreUntil[pkg] = now + COOLDOWN_MS
    }

    // R-04 / H5: an explicit list, always — never null, which would mean every app.
    private fun applyWatched() {
        val watched = prefs.watched
        if (watched == watchedInUse) return
        watchedInUse = watched
        serviceInfo = serviceInfo.apply { packageNames = watched.toTypedArray() }
    }

    private fun isInstalled(pkg: String) = packageManager.getLaunchIntentForPackage(pkg) != null

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
        stop()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }

    private fun stop() {
        isRunning = false
        handler.removeCallbacks(idleTick)
        if (::prefs.isInitialized) prefs.removeListener(onStateChanged)
    }

    companion object {
        // R-05: the default list, used until the owner picks one.
        private val DEFAULT_WATCHED = listOf("com.google.android.youtube")

        private const val IDLE_TICK_MS = 60_000L // R-72
        private const val DEBOUNCE_MS = 100L   // R-03
        private const val COOLDOWN_MS = 1500L  // R-03
        private const val TAP_MS = 50L
        private const val CAPTURE_INTERVAL_MS = 1000L

        private var lastCapture = 0L

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
