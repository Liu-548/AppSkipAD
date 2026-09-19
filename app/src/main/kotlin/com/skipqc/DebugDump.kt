package com.skipqc

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// R-80 / H4: debug builds only. Never called (and never writes anything) in release.
object DebugDump {

    private const val MAX_DEPTH = 30   // R-71
    private const val MAX_NODES = 500  // R-71

    fun shareLastWindow(activity: Activity) {
        if (!BuildConfig.DEBUG) return
        val file = write(activity) ?: run {
            Toast.makeText(activity, R.string.debug_dump_empty, Toast.LENGTH_LONG).show()
            return
        }
        val uri = Uri.parse("content://${activity.packageName}.dumps/${file.name}")
        activity.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                activity.getString(R.string.debug_dump)
            )
        )
    }

    private fun write(activity: Activity): File? {
        val root = SkipService.lastRoot ?: return null
        val dir = activity.getExternalFilesDir("dumps") ?: return null
        val out = StringBuilder("package=").append(root.packageName).append('\n')
        walk(root, 0, out, intArrayOf(0))
        val file = File(dir, "dump-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.txt")
        file.writeText(out.toString())
        return file
    }

    private fun walk(node: AccessibilityNodeInfo?, depth: Int, out: StringBuilder, seen: IntArray) {
        if (node == null || depth > MAX_DEPTH || seen[0] >= MAX_NODES) return
        seen[0]++
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        out.append("  ".repeat(depth))
            .append(node.className)
            .append(" | id=").append(node.viewIdResourceName)
            .append(" | text=").append(node.text)
            .append(" | desc=").append(node.contentDescription)
            .append(" | clickable=").append(node.isClickable)
            .append(" | enabled=").append(node.isEnabled)
            .append(" | visible=").append(node.isVisibleToUser)
            .append(" | ").append(bounds.toShortString())
            .append('\n')
        for (i in 0 until node.childCount) walk(node.getChild(i), depth + 1, out, seen)
    }
}
