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
            status.addView(
                TextView(this).apply {
                    val mark = when (check.ok) {
                        true -> "✓"
                        false -> "✗"
                        null -> "?"
                    }
                    text = "$mark ${check.label}\n${check.detail}"
                    setPadding(0, 24, 0, 24)
                    setOnClickListener { check.open(this@MainActivity) }
                }
            )
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
                appList.addView(
                    CheckBox(this).apply {
                        text = label
                        isChecked = pkg in prefs.watched
                        compoundDrawablePadding = 24
                        setCompoundDrawablesRelativeWithIntrinsicBounds(
                            packageManager.getApplicationIcon(info), null, null, null,
                        )
                        // A click is always the user: setOnCheckedChangeListener would also
                        // fire when this code sets the box, and could wipe the saved list.
                        setOnClickListener {
                            prefs.watched =
                                if (isChecked) prefs.watched + pkg else prefs.watched - pkg
                        }
                    }
                )
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
