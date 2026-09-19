package com.skipqc

import android.content.Context

/**
 * All persisted state, and the only writer of it (R-14): callers hand in an input, the pure
 * [ModeLogic] decides, and every listener (tile, notification, service) hears about the result.
 */
class Prefs private constructor(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("skipqc", Context.MODE_PRIVATE)
    private val listeners = mutableListOf<(ModeState) -> Unit>()

    var state: ModeState = read()
        private set

    var watched: Set<String> = sp.getStringSet(KEY_WATCHED, null) ?: emptySet()
        set(value) {
            field = value
            sp.edit().putStringSet(KEY_WATCHED, value).apply()
            notifyListeners()
        }

    /** R-05: only ever fills in a default when the owner has never chosen a list. */
    fun initWatchedIfUnset(default: Set<String>) {
        if (!sp.contains(KEY_WATCHED)) watched = default
    }

    fun addListener(listener: (ModeState) -> Unit) {
        listeners += listener
        listener(state)
    }

    fun removeListener(listener: (ModeState) -> Unit) {
        listeners -= listener
    }

    /**
     * R-11: a reboot puts the app back to off. Realme/Redmi never deliver BOOT_COMPLETED to a
     * sideloaded app, so the service recognises a fresh boot itself. [bootAtMs] is its start.
     */
    fun applyBootIfNewSession(bootAtMs: Long) {
        if (Math.abs(bootAtMs - sp.getLong(KEY_BOOT_AT, 0L)) < BOOT_SLACK_MS) return
        sp.edit().putLong(KEY_BOOT_AT, bootAtMs).apply()
        onInput(ModeInput.Boot)
    }

    /** R-10/R-14: the switch and the tile both land here. */
    fun setActive(on: Boolean) = onInput(ModeInput.ManualSet(on))

    fun setAutoWithApps(on: Boolean) = write(ModeLogic.onAutoWithAppsChanged(state, on))

    fun onInput(input: ModeInput, nowMs: Long = System.currentTimeMillis()) =
        write(ModeLogic.next(state, input, nowMs))

    private fun write(new: ModeState) {
        if (new == state) return
        // lastWatchedEventAt moves on every single event: keep it in memory, off the disk.
        val durableChange = new.copy(lastWatchedEventAt = 0L) != state.copy(lastWatchedEventAt = 0L)
        state = new
        if (!durableChange) return
        sp.edit()
            .putBoolean(KEY_ACTIVE, new.active)
            .putBoolean(KEY_OFF_MANUAL, new.offReason == OffReason.MANUAL)
            .putBoolean(KEY_AUTO_APPS, new.autoWithApps)
            .putBoolean(KEY_AUTO_ACTIVATED, new.autoActivated)
            .apply()
        notifyListeners()
    }

    private fun notifyListeners() = listeners.toList().forEach { it(state) }

    private fun read() = ModeState(
        active = sp.getBoolean(KEY_ACTIVE, false),
        offReason = if (sp.getBoolean(KEY_OFF_MANUAL, false)) OffReason.MANUAL else OffReason.AUTO,
        autoWithApps = sp.getBoolean(KEY_AUTO_APPS, false),
        autoActivated = sp.getBoolean(KEY_AUTO_ACTIVATED, false),
    )

    companion object {
        private const val KEY_ACTIVE = "active"
        private const val KEY_OFF_MANUAL = "offManual"
        private const val KEY_AUTO_APPS = "autoWithApps"
        private const val KEY_AUTO_ACTIVATED = "autoActivated"
        private const val KEY_WATCHED = "watched"
        private const val KEY_BOOT_AT = "bootAt"

        /** Clock corrections move the computed boot time by seconds, a reboot by minutes. */
        private const val BOOT_SLACK_MS = 30_000L

        @Volatile
        private var instance: Prefs? = null

        fun get(context: Context): Prefs =
            instance ?: synchronized(this) { instance ?: Prefs(context).also { instance = it } }
    }
}
