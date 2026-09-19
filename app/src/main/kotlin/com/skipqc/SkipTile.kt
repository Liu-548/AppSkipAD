package com.skipqc

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

// R-21: the main way to switch the app on and off.
class SkipTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val enabled = SkipService.isEnabled(this)
        val active = enabled && Prefs.get(this).state.active
        tile.icon = Icon.createWithResource(this, R.drawable.ic_logo)
        tile.label = getString(R.string.app_name)
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                when {
                    !enabled -> R.string.tile_no_permission
                    active -> R.string.tile_on
                    else -> R.string.tile_off
                }
            )
        }
        tile.updateTile()
    }

    override fun onClick() {
        if (!SkipService.isEnabled(this)) {
            openAccessibilitySettings()
            return
        }
        Prefs.get(this).setActive(!Prefs.get(this).state.active)
        onStartListening()
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated") // minSdk 26: the PendingIntent overload is API 34+
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        /** R-21: whoever changes the state asks the tile to refresh. */
        fun refresh(context: Context) = runCatching {
            requestListeningState(context, ComponentName(context, SkipTile::class.java))
        }
    }
}
