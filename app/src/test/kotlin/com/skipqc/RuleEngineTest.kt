package com.skipqc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeNode(
    override val viewId: String? = null,
    override val text: String? = null,
    override val description: String? = null,
    override val isVisible: Boolean = true,
    override val isEnabled: Boolean = true,
    override val isClickable: Boolean = false,
    override val parent: NodeView? = null,
) : NodeView

private class FakeFinder(
    private val byViewId: Map<String, List<NodeView>> = emptyMap(),
    private val byText: Map<String, List<NodeView>> = emptyMap(),
    private val tree: List<NodeView> = emptyList(),
) : NodeFinder {
    override fun byViewId(viewId: String) = byViewId[viewId].orEmpty()
    override fun byText(text: String) = byText[text].orEmpty()
    override fun descendants() = tree.asSequence()
}

private const val YT = "com.google.android.youtube"

private val RULES = Rules.parse(
    """
    { "schema": 1,
      "generic": { "texts": ["Skip"] },
      "apps": [ { "package": "$YT", "label": "YouTube", "verified": true,
                  "viewIds": ["skip_ad_button"], "texts": ["Bỏ qua"] } ] }
    """.trimIndent()
)

class RulesTest {
    @Test
    fun `bare view ids are expanded with the package`() {
        assertEquals(listOf("$YT:id/skip_ad_button"), RULES.forPackage(YT)!!.viewIds)
    }

    @Test
    fun `packages without a rule fall back to generic texts`() {
        assertNull(RULES.forPackage("com.other.app"))
        assertEquals(listOf("Skip"), RULES.genericTexts)
    }
}

class RuleEngineTest {

    @Test
    fun `view id hit wins`() {
        val button = FakeNode(viewId = "$YT:id/skip_ad_button", isClickable = true)
        val finder = FakeFinder(byViewId = mapOf("$YT:id/skip_ad_button" to listOf(button)))
        assertSame(button, RuleEngine.findTarget(RULES, YT, finder))
    }

    @Test
    fun `exact text hit`() {
        val button = FakeNode(text = " Bỏ qua ", isClickable = true)
        val finder = FakeFinder(byText = mapOf("Bỏ qua" to listOf(button)))
        assertSame(button, RuleEngine.findTarget(RULES, YT, finder))
    }

    @Test
    fun `content description counts as a hit`() {
        val button = FakeNode(description = "Skip", isClickable = true)
        val finder = FakeFinder(byText = mapOf("Skip" to listOf(button)))
        assertSame(button, RuleEngine.findTarget(RULES, YT, finder))
    }

    @Test
    fun `substring is rejected`() {
        val countdown = FakeNode(text = "Skip in 5", isClickable = true)
        val finder = FakeFinder(byText = mapOf("Skip" to listOf(countdown)))
        assertNull(RuleEngine.findTarget(RULES, YT, finder))
    }

    @Test
    fun `invisible or disabled nodes are rejected`() {
        val invisible = FakeNode(viewId = "$YT:id/skip_ad_button", isVisible = false, isClickable = true)
        val disabled = FakeNode(text = "Bỏ qua", isEnabled = false, isClickable = true)
        val finder = FakeFinder(
            byViewId = mapOf("$YT:id/skip_ad_button" to listOf(invisible)),
            byText = mapOf("Bỏ qua" to listOf(disabled)),
        )
        assertNull(RuleEngine.findTarget(RULES, YT, finder))
    }

    @Test
    fun `walks up to the nearest clickable ancestor`() {
        val clickable = FakeNode(isClickable = true)
        val middle = FakeNode(parent = clickable)
        val label = FakeNode(text = "Bỏ qua", parent = middle)
        val finder = FakeFinder(byText = mapOf("Bỏ qua" to listOf(label)))
        assertSame(clickable, RuleEngine.findTarget(RULES, YT, finder))
    }

    @Test
    fun `stops after three levels and returns the matched node`() {
        val clickable = FakeNode(isClickable = true)
        val tooFar = generateSequence(clickable) { FakeNode(parent = it) }.take(6).last()
        val label = FakeNode(text = "Bỏ qua", parent = tooFar)
        val finder = FakeFinder(byText = mapOf("Bỏ qua" to listOf(label)))
        val target = RuleEngine.findTarget(RULES, YT, finder)
        assertSame(label, target)
        assertTrue(!target!!.isClickable)
    }

    // R-02 step 3: survives a renamed id.
    @Test
    fun `a renamed skip id is still found by the fallback walk`() {
        val renamed = FakeNode(viewId = "$YT:id/skip_ad_button_v2", isClickable = true)
        val noise = FakeNode(viewId = "$YT:id/watch_player", isClickable = true)
        assertSame(renamed, RuleEngine.findTarget(RULES, YT, FakeFinder(tree = listOf(noise, renamed))))
    }

    @Test
    fun `the fallback walk ignores unrelated and invisible nodes`() {
        val title = FakeNode(text = "Skip the line", isClickable = true)
        val hidden = FakeNode(viewId = "$YT:id/skip_ad_button_v2", isVisible = false, isClickable = true)
        assertNull(RuleEngine.findTarget(RULES, YT, FakeFinder(tree = listOf(title, hidden))))
    }

    @Test
    fun `nothing on screen means no target`() {
        assertNull(RuleEngine.findTarget(RULES, YT, FakeFinder()))
    }
}
