package com.skipqc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun ModeState.after(input: ModeInput, nowMs: Long = 0L) = ModeLogic.next(this, input, nowMs)

private val contentEvent = ModeInput.WatchedEvent(isWindowStateChange = false)
private val openEvent = ModeInput.WatchedEvent(isWindowStateChange = true)

class ModeLogicTest {

    // R-10
    @Test
    fun `fresh state is off`() {
        assertFalse(ModeState().active)
    }

    @Test
    fun `manual on and off, off is remembered as manual`() {
        val on = ModeState().after(ModeInput.ManualSet(true))
        assertTrue(on.active)
        val off = on.after(ModeInput.ManualSet(false))
        assertFalse(off.active)
        assertEquals(OffReason.MANUAL, off.offReason)
    }

    // R-11
    @Test
    fun `boot leaves the app off by default`() {
        val state = ModeState(active = true).after(ModeInput.Boot)
        assertFalse(state.active)
        assertEquals(OffReason.AUTO, state.offReason)
    }

    @Test
    fun `boot turns the app on when auto on boot is set`() {
        assertTrue(ModeState(autoOnBoot = true).after(ModeInput.Boot).active)
    }

    // R-12, bullet 1
    @Test
    fun `any watched event activates after an automatic off`() {
        val state = ModeState(autoWithApps = true, offReason = OffReason.AUTO)
        assertTrue(state.after(contentEvent, nowMs = 5_000).active)
    }

    @Test
    fun `nothing happens while the mode is off`() {
        val state = ModeState(autoWithApps = false, offReason = OffReason.AUTO)
        assertFalse(state.after(contentEvent, nowMs = 5_000).active)
    }

    // R-12, bullet 2
    @Test
    fun `a manual off survives while the user stays in the app`() {
        val state = ModeState(autoWithApps = true, offReason = OffReason.MANUAL, lastWatchedEventAt = 1_000)
        assertFalse(state.after(contentEvent, nowMs = 2_000).active)
        assertFalse(state.after(openEvent, nowMs = 2_000).active)
        assertFalse(state.after(openEvent, nowMs = 1_000 + ModeLogic.REOPEN_GAP_MS - 1).active)
    }

    @Test
    fun `re-opening the app after ten seconds clears a manual off`() {
        val state = ModeState(autoWithApps = true, offReason = OffReason.MANUAL, lastWatchedEventAt = 1_000)
        val reopened = state.after(openEvent, nowMs = 1_000 + ModeLogic.REOPEN_GAP_MS)
        assertTrue(reopened.active)
        assertTrue(reopened.autoActivated)
    }

    // R-12, bullet 3
    @Test
    fun `an automatic activation times out after ten idle minutes`() {
        val state = ModeState(active = true, autoActivated = true, lastWatchedEventAt = 0)
        assertTrue(state.after(ModeInput.IdleTick, nowMs = ModeLogic.IDLE_TIMEOUT_MS - 1).active)
        val timedOut = state.after(ModeInput.IdleTick, nowMs = ModeLogic.IDLE_TIMEOUT_MS)
        assertFalse(timedOut.active)
        assertEquals(OffReason.AUTO, timedOut.offReason)
    }

    @Test
    fun `a manual on never times out`() {
        val state = ModeState(active = true, autoActivated = false, lastWatchedEventAt = 0)
        assertTrue(state.after(ModeInput.IdleTick, nowMs = 10 * ModeLogic.IDLE_TIMEOUT_MS).active)
    }

    @Test
    fun `watched events keep an automatic activation alive`() {
        val state = ModeState(active = true, autoWithApps = true, autoActivated = true)
            .after(contentEvent, nowMs = ModeLogic.IDLE_TIMEOUT_MS)
        assertTrue(state.after(ModeInput.IdleTick, nowMs = ModeLogic.IDLE_TIMEOUT_MS + 1).active)
    }

    // R-12, bullet 4
    @Test
    fun `switching the mode on re-arms automatic activation`() {
        val state = ModeLogic.onAutoWithAppsChanged(ModeState(offReason = OffReason.MANUAL), on = true)
        assertTrue(state.autoWithApps)
        assertEquals(OffReason.AUTO, state.offReason)
    }
}
