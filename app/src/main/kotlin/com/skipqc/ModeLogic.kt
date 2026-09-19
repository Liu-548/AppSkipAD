package com.skipqc

// R-13: pure state machine for R-10..R-12. No android.* imports, so JVM tests can run it.

enum class OffReason { MANUAL, AUTO }

data class ModeState(
    val active: Boolean = false,                 // R-10: a fresh install is off
    val offReason: OffReason = OffReason.AUTO,
    val autoWithApps: Boolean = false,
    val lastWatchedEventAt: Long = 0L,
    /** Only an automatic activation may time out again (R-12, last bullet). */
    val autoActivated: Boolean = false,
)

sealed interface ModeInput {
    data class ManualSet(val on: Boolean) : ModeInput
    data object Boot : ModeInput
    data class WatchedEvent(val isWindowStateChange: Boolean) : ModeInput
    data object IdleTick : ModeInput
}

object ModeLogic {

    /** R-12: a window state change after this long means the user re-opened the app. */
    const val REOPEN_GAP_MS = 10_000L

    /** R-12: auto-activation gives up after this long without a watched-app event. */
    const val IDLE_TIMEOUT_MS = 10 * 60 * 1000L

    fun next(state: ModeState, input: ModeInput, nowMs: Long): ModeState = when (input) {
        // R-10: the manual switch always wins, and a manual off must stay off.
        is ModeInput.ManualSet ->
            state.copy(active = input.on, offReason = OffReason.MANUAL, autoActivated = false)

        // R-11: a reboot always leaves the app off. Turning it on at boot is postponed (SPEC §11).
        ModeInput.Boot ->
            state.copy(active = false, offReason = OffReason.AUTO, autoActivated = false)

        is ModeInput.WatchedEvent -> onWatchedEvent(state, input.isWindowStateChange, nowMs)

        ModeInput.IdleTick ->
            if (state.active && state.autoActivated &&
                nowMs - state.lastWatchedEventAt >= IDLE_TIMEOUT_MS
            ) {
                state.copy(active = false, offReason = OffReason.AUTO, autoActivated = false)
            } else {
                state
            }
    }

    /** R-12, last bullet: switching the mode on re-arms automatic activation. */
    fun onAutoWithAppsChanged(state: ModeState, on: Boolean): ModeState =
        state.copy(autoWithApps = on, offReason = if (on) OffReason.AUTO else state.offReason)

    private fun onWatchedEvent(state: ModeState, isWindowStateChange: Boolean, nowMs: Long): ModeState {
        val seen = state.copy(lastWatchedEventAt = nowMs)
        if (state.active || !state.autoWithApps) return seen
        val turnOn = when (state.offReason) {
            OffReason.AUTO -> true
            // A manual off is respected until the user leaves and re-opens the app.
            OffReason.MANUAL ->
                isWindowStateChange && nowMs - state.lastWatchedEventAt >= REOPEN_GAP_MS
        }
        return if (turnOn) seen.copy(active = true, offReason = OffReason.AUTO, autoActivated = true) else seen
    }
}
