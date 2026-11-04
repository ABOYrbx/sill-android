package app.sill.protocol

/**
 * Port of `Sources/StreamProtocol/Switcher.swift` (catalog types) and `Pointer.swift`.
 */
data class WindowInfo(
    var id: UInt,
    var title: String,
    var appName: String,
    var bundleID: String,
    var width: Int,
    var height: Int,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        parts.add("\"appName\":${jsonString(appName)}")
        parts.add("\"bundleID\":${jsonString(bundleID)}")
        parts.add("\"height\":$height")
        parts.add("\"id\":$id")
        parts.add("\"title\":${jsonString(title)}")
        parts.add("\"width\":$width")
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): WindowInfo? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val id = regexLong(s, "id")?.toUInt() ?: return null
            val title = regexString(s, "title")?.let { unescapeJson(it) } ?: return null
            val appName = regexString(s, "appName")?.let { unescapeJson(it) } ?: return null
            val bundleID = regexString(s, "bundleID")?.let { unescapeJson(it) } ?: return null
            val width = regexInt(s, "width") ?: return null
            val height = regexInt(s, "height") ?: return null
            return WindowInfo(id, title, appName, bundleID, width, height)
        }
        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun regexInt(s: String, key: String): Int? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toIntOrNull()
        }
        private fun regexLong(s: String, key: String): Long? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toLongOrNull()
        }
    }
}

data class AppInfo(
    var name: String,
    var bundleID: String,
) {
    val id: String get() = bundleID

    fun toJson(): String =
        "{\"bundleID\":${jsonString(bundleID)},\"name\":${jsonString(name)}}"

    companion object {
        fun fromJson(json: String): AppInfo? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val name = regexString(s, "name")?.let { unescapeJson(it) } ?: return null
            val bundleID = regexString(s, "bundleID")?.let { unescapeJson(it) } ?: return null
            return AppInfo(name, bundleID)
        }
        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
    }
}

data class WindowList(
    var macName: String,
    var windows: List<WindowInfo>,
    var active: StreamSource,
    var launchID: String? = null,
    var hostVersion: String? = null,
    var protocol: Int? = null,
    var gestures: Int? = null,
    var clipboard: Int? = null,
    var keyRepeat: Int? = null,
    var locked: Boolean? = null,
    var offConsole: Boolean? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        parts.add("\"active\":${active.toJson()}")
        clipboard?.let { parts.add("\"clipboard\":$it") }
        gestures?.let { parts.add("\"gestures\":$it") }
        hostVersion?.let { parts.add("\"hostVersion\":${jsonString(it)}") }
        keyRepeat?.let { parts.add("\"keyRepeat\":$it") }
        launchID?.let { parts.add("\"launchID\":${jsonString(it)}") }
        locked?.let { parts.add("\"locked\":$it") }
        macName.let { parts.add("\"macName\":${jsonString(it)}") }
        offConsole?.let { parts.add("\"offConsole\":$it") }
        protocol?.let { parts.add("\"protocol\":$it") }
        parts.add("\"windows\":[" + windows.joinToString(",") { it.toJson() } + "]")
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): WindowList? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val macName = regexString(s, "macName")?.let { unescapeJson(it) } ?: return null
            val windows = parseWindowList(s, "windows") ?: return null
            val activeJson = extractObject(s, "active") ?: return null
            val active = StreamSource.fromJson(activeJson) ?: return null
            val wl = WindowList(macName, windows, active)
            wl.launchID = regexString(s, "launchID")?.let { unescapeJson(it) }
            wl.hostVersion = regexString(s, "hostVersion")?.let { unescapeJson(it) }
            wl.protocol = regexInt(s, "protocol")
            wl.gestures = regexInt(s, "gestures")
            wl.clipboard = regexInt(s, "clipboard")
            wl.keyRepeat = regexInt(s, "keyRepeat")
            wl.locked = regexBool(s, "locked")
            wl.offConsole = regexBool(s, "offConsole")
            return wl
        }
        private fun parseWindowList(s: String, key: String): List<WindowInfo>? {
            val arr = extractArray(s, key) ?: return null
            val content = arr.trim().removeSurrounding("[", "]").trim()
            if (content.isEmpty()) return emptyList()
            val items = mutableListOf<WindowInfo>()
            for (obj in splitTopLevelObjects(content)) {
                items.add(WindowInfo.fromJson(obj) ?: return null)
            }
            return items
        }
        private fun extractArray(s: String, key: String): String? {
            val idx = s.indexOf("\"$key\"")
            if (idx < 0) return null
            var i = s.indexOf(':', idx)
            if (i < 0) return null
            i++
            while (i < s.length && s[i].isWhitespace()) i++
            if (i >= s.length || s[i] != '[') return null
            var depth = 0
            val start = i
            while (i < s.length) {
                when (s[i]) {
                    '[' -> depth++
                    ']' -> { depth--; if (depth == 0) return s.substring(start, i + 1) }
                    '"' -> {
                        i++
                        while (i < s.length) {
                            if (s[i] == '\\') { i += 2; continue }
                            if (s[i] == '"') break
                            i++
                        }
                    }
                }
                i++
            }
            return null
        }
        private fun extractObject(s: String, key: String): String? {
            val idx = s.indexOf("\"$key\"")
            if (idx < 0) return null
            var i = s.indexOf(':', idx)
            if (i < 0) return null
            i++
            while (i < s.length && s[i].isWhitespace()) i++
            if (i >= s.length || s[i] != '{') return null
            var depth = 0
            val start = i
            while (i < s.length) {
                when (s[i]) {
                    '{' -> depth++
                    '}' -> { depth--; if (depth == 0) return s.substring(start, i + 1) }
                    '"' -> {
                        i++
                        while (i < s.length) {
                            if (s[i] == '\\') { i += 2; continue }
                            if (s[i] == '"') break
                            i++
                        }
                    }
                }
                i++
            }
            return null
        }
        private fun splitTopLevelObjects(content: String): List<String> {
            val result = mutableListOf<String>()
            var depth = 0
            var inStr = false
            var start = 0
            var i = 0
            while (i < content.length) {
                val c = content[i]
                if (inStr) {
                    if (c == '\\') { i += 2; continue }
                    if (c == '"') inStr = false
                } else {
                    when (c) {
                        '"' -> inStr = true
                        '{' -> depth++
                        '}' -> {
                            depth--
                            if (depth == 0) {
                                result.add(content.substring(start, i + 1).trim())
                                var j = i + 1
                                while (j < content.length && (content[j] == ',' || content[j].isWhitespace())) j++
                                start = j
                                i = j - 1
                            }
                        }
                        '[' -> depth++
                        ']' -> depth--
                    }
                }
                i++
            }
            if (start < content.length) {
                val tail = content.substring(start).trim()
                if (tail.isNotEmpty()) result.add(tail)
            }
            return result
        }
        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun regexInt(s: String, key: String): Int? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toIntOrNull()
        }
        private fun regexBool(s: String, key: String): Boolean? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(true|false)")
            return re.find(s)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        }
    }
}

data class MacPointer(
    var x: Double? = null,
    var y: Double? = null,
    var inside: Boolean? = null,
    var seen: Int? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        inside?.let { parts.add("\"inside\":$it") }
        seen?.let { parts.add("\"seen\":$it") }
        x?.let { parts.add("\"x\":${jsonNumber(it)}") }
        y?.let { parts.add("\"y\":${jsonNumber(it)}") }
        return "{" + parts.joinToString(",") + "}"
    }

    val position: Pair<Double, Double>?
        get() {
            if (inside != true) return null
            val xx = x ?: return null
            val yy = y ?: return null
            if (!xx.isFinite() || !yy.isFinite()) return null
            return Pair(xx, yy)
        }

    companion object {
        fun rounded(v: Double): Double = Math.round(v * 10000.0).toDouble() / 10000.0

        fun fromJson(json: String): MacPointer? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val m = MacPointer()
            m.x = regexDouble(s, "x")
            m.y = regexDouble(s, "y")
            m.inside = regexBool(s, "inside")
            m.seen = regexInt(s, "seen")
            return m
        }
        private fun regexDouble(s: String, key: String): Double? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
            return re.find(s)?.groupValues?.get(1)?.toDoubleOrNull()
        }
        private fun regexBool(s: String, key: String): Boolean? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(true|false)")
            return re.find(s)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        }
        private fun regexInt(s: String, key: String): Int? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toIntOrNull()
        }
    }
}