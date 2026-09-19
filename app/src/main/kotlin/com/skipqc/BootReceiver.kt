package com.skipqc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// R-11: by default this leaves the app off; ModeLogic decides.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Prefs.get(context).onInput(ModeInput.Boot)
    }
}
