package com.skipqc

import org.json.JSONArray
import org.json.JSONObject

// R-50. Pure logic: no android.* imports, so it runs in JVM tests.
data class AppRule(
    val pkg: String,
    val label: String,
    val verified: Boolean,
    val viewIds: List<String>,
    val texts: List<String>,
)

class Rules(val genericTexts: List<String>, private val apps: Map<String, AppRule>) {

    fun forPackage(pkg: String): AppRule? = apps[pkg]

    companion object {
        fun parse(json: String): Rules {
            val root = JSONObject(json)
            val generic = root.optJSONObject("generic")?.optJSONArray("texts").toList()
            val apps = root.optJSONArray("apps").objects().map { app ->
                val pkg = app.getString("package")
                AppRule(
                    pkg = pkg,
                    label = app.optString("label", pkg),
                    verified = app.optBoolean("verified", false),
                    // R-50: a bare id means "<package>:id/<id>".
                    viewIds = app.optJSONArray("viewIds").toList()
                        .map { if (it.contains(':')) it else "$pkg:id/$it" },
                    texts = app.optJSONArray("texts").toList(),
                )
            }.associateBy { it.pkg }
            return Rules(generic, apps)
        }

        private fun JSONArray?.toList(): List<String> =
            if (this == null) emptyList() else (0 until length()).map { getString(it) }

        private fun JSONArray?.objects(): List<JSONObject> =
            if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
    }
}
