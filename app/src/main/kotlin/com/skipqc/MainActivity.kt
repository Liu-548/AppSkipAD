package com.skipqc

import android.app.Activity
import android.app.StatusBarManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

// R-30: the only screen.
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private val status by lazy { findViewById<LinearLayout>(R.id.status) }
    private val appList by lazy { findViewById<LinearLayout>(R.id.apps) }
    private val activeSwitch by lazy { findViewById<Switch>(R.id.active) }
    private val autoWithApps by lazy { findViewById<CheckBox>(R.id.auto_with_apps) }

    /** Keeps the tile's own updates from bouncing back as user input. */
    private var syncing = false
    private val onState: (ModeState) -> Unit = { runOnUiThread { syncControls() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        prefs = Prefs.get(this)

        activeSwitch.setOnCheckedChangeListener(userChange { prefs.setActive(it) })
        autoWithApps.setOnCheckedChangeListener(userChange { prefs.setAutoWithApps(it) })
        setUpTileButton()
        buildAppList()

        findViewById<Button>(R.id.dump).apply {
            if (!BuildConfig.DEBUG) return@apply // R-80
            visibility = View.VISIBLE
            setOnClickListener { DebugDump.shareLastWindow(this@MainActivity) }
        }
        askForNotifications()
    }

    override fun onResume() {
        super.onResume()
        buildStatus()
        prefs.addListener(onState) // also syncs the controls
    }

    override fun onPause() {
        prefs.removeListener(onState)
        super.onPause()
    }

    private fun userChange(apply: (Boolean) -> Unit) =
        CompoundButton.OnCheckedChangeListener { _, checked -> if (!syncing) apply(checked) }

    private fun syncControls() {
        syncing = true
        activeSwitch.isChecked = prefs.state.active
        autoWithApps.isChecked = prefs.state.autoWithApps
        syncing = false
    }

    // R-41 + R-43
    private fun buildStatus() {
        status.removeAllViews()
        Oem.checks(this).forEach { check ->
            val row = layoutInflater.inflate(R.layout.row_health, status, false)
            row.findViewById<TextView>(R.id.mark).apply {
                text = if (check.ok == null) "?" else if (check.ok) "✓" else "✗"
                setTextColor(
                    getColor(
                        when (check.ok) {
                            true -> R.color.ok
                            false -> R.color.bad
                            null -> R.color.unknown
                        }
                    )
                )
            }
            row.findViewById<TextView>(R.id.label).text = check.label
            row.findViewById<TextView>(R.id.detail).text = check.detail
            row.setOnClickListener { check.open(this) }
            status.addView(row)
        }
        findViewById<TextView>(R.id.restricted_hint).apply {
            visibility = if (Oem.needsRestrictedSettingsHint(this@MainActivity)) View.VISIBLE else View.GONE
            setOnClickListener { Oem.openAppDetails(this@MainActivity) }
        }
    }

    // R-30.4: apps with a rule first, then alphabetical.
    private fun buildAppList() {
        val rules = runCatching {
            Rules.parse(assets.open("rules.json").bufferedReader().use { it.readText() })
        }.getOrNull()
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != packageName }
            .map { Triple(it.packageName, packageManager.getApplicationLabel(it).toString(), it) }
            .sortedWith(compareBy({ rules?.forPackage(it.first) == null }, { it.second.lowercase() }))
            .forEach { (pkg, label, info) ->
                val row = layoutInflater.inflate(R.layout.row_app, appList, false) as CheckBox
                row.text = label
                row.isChecked = pkg in prefs.watched
                // Launcher icons are far bigger than this row: pin them to the text size.
                val icon = packageManager.getApplicationIcon(info)
                val size = (32 * resources.displayMetrics.density).toInt()
                icon.setBounds(0, 0, size, size)
                row.setCompoundDrawablesRelative(icon, null, null, null)
                // A click is always the user: setOnCheckedChangeListener would also fire
                // when this code sets the box, and could wipe the saved list.
                row.setOnClickListener {
                    prefs.watched = if (row.isChecked) prefs.watched + pkg else prefs.watched - pkg
                }
                appList.addView(row)
            }
    }

    // R-22
    private fun setUpTileButton() {
        val button = findViewById<Button>(R.id.add_tile)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            button.visibility = View.GONE
            findViewById<TextView>(R.id.add_tile_hint).visibility = View.VISIBLE
            return
        }
        button.setOnClickListener {
            getSystemService(StatusBarManager::class.java)?.requestAddTileService(
                android.content.ComponentName(this, SkipTile::class.java),
                getString(R.string.app_name),
                Icon.createWithResource(this, R.drawable.ic_logo),
                {},
                {},
            )
        }
    }

    // R-20: the status icon needs this on Android 13+; the app works without it.
    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = android.Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 1)
        }
    }
}
