package com.skipqc

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

// M0 placeholder so the scaffold compiles. Real screen: docs/SPEC.md R-30.
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = getString(R.string.app_name) })
    }
}
