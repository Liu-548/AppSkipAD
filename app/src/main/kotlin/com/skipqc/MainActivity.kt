package com.skipqc

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

// M0/M1 placeholder screen. Real layout: docs/SPEC.md R-30 (M5.2).
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            addView(TextView(this@MainActivity).apply { text = getString(R.string.app_name) })
        }
        // R-80: temporary debug button, dropped in M5.2.
        if (BuildConfig.DEBUG) {
            root.addView(Button(this).apply {
                text = getString(R.string.debug_dump)
                setOnClickListener { DebugDump.shareLastWindow(this@MainActivity) }
            })
        }
        setContentView(root)
    }
}
