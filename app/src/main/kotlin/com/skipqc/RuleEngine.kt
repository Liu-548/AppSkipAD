package com.skipqc

// R-02. Pure matching over a view abstraction so it can be unit-tested on the JVM.
interface NodeView {
    val viewId: String?
    val text: String?
    val description: String?
    val isVisible: Boolean
    val isEnabled: Boolean
    val isClickable: Boolean
    val parent: NodeView?
}

/** What the accessibility service offers: the framework lookups, plus a bounded walk. */
interface NodeFinder {
    fun byViewId(viewId: String): List<NodeView>
    fun byText(text: String): List<NodeView>

    /** R-71: the implementation caps depth and node count. */
    fun descendants(): Sequence<NodeView>
}

object RuleEngine {

    private const val MAX_ANCESTOR_STEPS = 3

    /** R-02 step 4: the part of a skip button's id that has outlived every YouTube redesign. */
    private const val SKIP_ID = "skip_ad"

    /**
     * The node to act on: the nearest clickable ancestor of a matching node, or the matching
     * node itself when nothing within [MAX_ANCESTOR_STEPS] is clickable (caller taps its centre).
     */
    fun findTarget(rules: Rules, pkg: String, finder: NodeFinder): NodeView? {
        val app = rules.forPackage(pkg)

        // 1. App rule viewIds.
        app?.viewIds?.forEach { id ->
            finder.byViewId(id).firstOrNull(::usable)?.let { return clickTarget(it) }
        }

        // 2. App rule texts, then generic texts. Exact match only (R-02).
        ((app?.texts ?: emptyList()) + rules.genericTexts).forEach { wanted ->
            finder.byText(wanted)
                .firstOrNull { usable(it) && matches(it, wanted) }
                ?.let { return clickTarget(it) }
        }

        // 3. Last resort: any visible node whose id still looks like a skip button, so a
        // renamed id (skip_ad_button_v2 and friends) does not need a new release. Deliberately
        // id-only: matching loose *text* here would hit a video titled "Skip the line".
        finder.descendants()
            .firstOrNull { usable(it) && it.viewId?.contains(SKIP_ID, ignoreCase = true) == true }
            ?.let { return clickTarget(it) }

        return null
    }

    // 3. Only nodes the user can actually see and press.
    private fun usable(node: NodeView) = node.isVisible && node.isEnabled

    private fun matches(node: NodeView, wanted: String): Boolean =
        node.text?.trim().equals(wanted, ignoreCase = true) ||
            node.description?.trim().equals(wanted, ignoreCase = true)

    private fun clickTarget(node: NodeView): NodeView {
        var current: NodeView? = node
        repeat(MAX_ANCESTOR_STEPS + 1) {
            if (current == null) return node
            if (current.isClickable) return current
            current = current.parent
        }
        return node
    }
}
