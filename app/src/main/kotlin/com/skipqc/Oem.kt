package com.skipqc

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

// R-40
enum class Brand { XIAOMI, OPLUS, OTHER }

/** One line of the status block (R-41). [ok] is null when the system will not tell us. */
data class Health(
    val label: String,
    val detail: String,
    val ok: Boolean?,
    val open: (Context) -> Unit,
)

object Oem {

    val brand: Brand = Build.MANUFACTURER.lowercase().let { maker ->
        when {
            listOf("xiaomi", "redmi", "poco").any(maker::contains) -> Brand.XIAOMI
            listOf("realme", "oppo", "oneplus").any(maker::contains) -> Brand.OPLUS
            else -> Brand.OTHER
        }
    }

    // R-42: none of these are documented, so every one of them may be missing.
    private val autostartScreens = mapOf(
        Brand.XIAOMI to listOf(
            "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",
        ),
        Brand.OPLUS to listOf(
            "com.coloros.safecenter/.startupapp.StartupAppListActivity",
            "com.coloros.safecenter/.permission.startup.StartupAppListActivity",
            "com.oplus.safecenter/.startupapp.StartupAppListActivity",
        ),
    )

    fun checks(context: Context): List<Health> = buildList {
        add(accessibility(context))
        add(battery(context))
        add(notifications(context))
        if (brand != Brand.OTHER) add(autostart(context))
    }

    /** R-43: on Android 13+ a sideloaded app is blocked from Accessibility until unlocked. */
    fun needsRestrictedSettingsHint(context: Context) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !SkipService.isEnabled(context)

    fun openAppDetails(context: Context) = start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
    )

    private fun accessibility(context: Context): Health {
        val enabled = SkipService.isEnabled(context)
        val running = SkipService.isRunning
        return Health(
            label = context.getString(R.string.health_accessibility),
            detail = context.getString(
                when {
                    enabled && running -> R.string.health_on
                    enabled -> R.string.health_enabled_not_running
                    else -> R.string.health_off
                }
            ),
            ok = enabled && running,
            open = { start(it, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
        )
    }

    private fun battery(context: Context): Health {
        val power = context.getSystemService(PowerManager::class.java)
        val ignoring = power?.isIgnoringBatteryOptimizations(context.packageName) == true
        return Health(
            label = context.getString(R.string.health_battery),
            detail = context.getString(if (ignoring) R.string.health_battery_free else R.string.health_battery_limited),
            ok = ignoring,
            open = {
                start(
                    it,
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${it.packageName}"),
                    ),
                )
            },
        )
    }

    private fun notifications(context: Context): Health {
        val allowed = context.getSystemService(NotificationManager::class.java)
            ?.areNotificationsEnabled() == true
        return Health(
            label = context.getString(R.string.health_notifications),
            detail = context.getString(if (allowed) R.string.health_on else R.string.health_off),
            ok = allowed,
            open = {
                start(
                    it,
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, it.packageName),
                )
            },
        )
    }

    private fun autostart(context: Context) = Health(
        label = context.getString(R.string.health_autostart),
        detail = context.getString(R.string.health_check_yourself),
        ok = null, // R-41: this one cannot be read back.
        open = { startFirst(it, autostartScreens[brand].orEmpty()) },
    )

    private fun startFirst(context: Context, components: List<String>) {
        for (name in components) {
            val component = ComponentName.unflattenFromString(name) ?: continue
            if (start(context, Intent().setComponent(component))) return
        }
        openAppDetails(context)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
