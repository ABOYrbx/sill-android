package app.sill.protocol

/**
 * The Mac's menus (kinds 24, 25, 27).
 *
 * Port of `Sources/StreamProtocol/MacMenu.swift`.
 */
data class MacMenu(
    var version: Int? = null,
    var app: String? = null,
    var bundleID: String? = null,
    var menus: List<MacMenuItem>? = null,
    var answering: Int? = null,
    var menu: String? = null,
    var items: List<MacMenuItem>? = null,
    var more: Int? = null,
    var pressed: Boolean? = null,
    var stale: Boolean? = null,
    var note: String? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        version?.let { parts.add("\"version\":$it") }
        app?.let { parts.add("\"app\":${jsonString(it)}") }
        bundleID?.let { parts.add("\"bundleID\":${jsonString(it)}") }
        menus?.let { parts.add("\"menus\":${listToJson(it)}") }
        answering?.let { parts.add("\"answering\":$it") }
        menu?.let { parts.add("\"menu\":${jsonString(it)}") }
        items?.let { parts.add("\"items\":${listToJson(it)}") }
        more?.let { parts.add("\"more\":$it") }
        pressed?.let { parts.add("\"pressed\":$it") }
        stale?.let { parts.add("\"stale\":$it") }
        note?.let { parts.add("\"note\":${jsonString(it)}") }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): MacMenu? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val m = MacMenu()
            m.version = intField(s, "version")
            m.app = stringField(s, "app")
            m.bundleID = stringField(s, "bundleID")
            m.menus = listField(s, "menus")
            m.answering = intField(s, "answering")
            m.menu = stringField(s, "menu")
            m.items = listField(s, "items")
            m.more = intField(s, "more")
            m.pressed = boolField(s, "pressed")
            m.stale = boolField(s, "stale")
            m.note = stringField(s, "note")
            return m
        }

        private fun listToJson(list: List<MacMenuItem>): String =
            "[" + list.joinToString(",") { it.toJson() } + "]"

        private fun listField(s: String, key: String): List<MacMenuItem>? {
            val arr = extractArray(s, key) ?: return null
            if (arr == "null") return null
            val items = mutableListOf<MacMenuItem>()
            for (obj in splitTopLevelObjects(arr)) {
                items.add(MacMenuItem.fromJson(obj) ?: return null)
            }
            return items
        }

        private fun intField(s: String, key: String): Int? = regexLong(s, key)?.toInt()
        private fun boolField(s: String, key: String): Boolean? = regexBool(s, key)
        private fun stringField(s: String, key: String): String? = regexString(s, key)?.let { unescape(it) }

        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun regexLong(s: String, key: String): Long? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toLongOrNull()
        }
        private fun regexBool(s: String, key: String): Boolean? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(true|false)")
            return re.find(s)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        }

        private fun unescape(v: String): String {
            val sb = StringBuilder()
            var i = 0
            while (i < v.length) {
                val c = v[i]
                if (c == '\\') {
                    if (i + 1 >= v.length) break
                    when (v[i + 1]) {
                        '"' -> { sb.append('"'); i += 2; continue }
                        '\\' -> { sb.append('\\'); i += 2; continue }
                        '/' -> { sb.append('/'); i += 2; continue }
                        'n' -> { sb.append('\n'); i += 2; continue }
                        't' -> { sb.append('\t'); i += 2; continue }
                        'r' -> { sb.append('\r'); i += 2; continue }
                        'b' -> { sb.append('\b'); i += 2; continue }
                        'f' -> { sb.append('\u000C'); i += 2; continue }
                        'u' -> {
                            if (i + 5 < v.length) {
                                val hex = v.substring(i + 2, i + 6)
                                val cp = hex.toIntOrNull(16)
                                if (cp != null) { sb.append(cp.toChar()); i += 6; continue }
                            }
                        }
                    }
                }
                sb.append(c); i++
            }
            return sb.toString()
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

        private fun splitTopLevelObjects(arr: String): List<String> {
            val content = arr.trim().removeSurrounding("[", "]").trim()
            if (content.isEmpty() || content == "null") return emptyList()
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
                                // skip comma
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

        private fun jsonString(v: String): String {
            val sb = StringBuilder("\"")
            for (c in v) {
                when (c) {
                    '"' -> sb.append("\\\"")
                    '\\' -> sb.append("\\\\")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    '\b' -> sb.append("\\b")
                    '\u000C' -> sb.append("\\f")
                    else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
                }
            }
            sb.append("\"")
            return sb.toString()
        }
    }
}

data class MacMenuItem(
    var id: String? = null,
    var title: String? = null,
    var separator: Boolean? = null,
    var enabled: Boolean? = null,
    var mark: String? = null,
    var key: String? = null,
    var submenu: Boolean? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        id?.let { parts.add("\"id\":${jsonString(it)}") }
        title?.let { parts.add("\"title\":${jsonString(it)}") }
        separator?.let { parts.add("\"separator\":$it") }
        enabled?.let { parts.add("\"enabled\":$it") }
        mark?.let { parts.add("\"mark\":${jsonString(it)}") }
        key?.let { parts.add("\"key\":${jsonString(it)}") }
        submenu?.let { parts.add("\"submenu\":$it") }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): MacMenuItem? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val m = MacMenuItem()
            m.id = regexString(s, "id")?.let { unescape(it) }
            m.title = regexString(s, "title")?.let { unescape(it) }
            m.separator = regexBool(s, "separator")
            m.enabled = regexBool(s, "enabled")
            m.mark = regexString(s, "mark")?.let { unescape(it) }
            m.key = regexString(s, "key")?.let { unescape(it) }
            m.submenu = regexBool(s, "submenu")
            return m
        }

        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun regexBool(s: String, key: String): Boolean? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(true|false)")
            return re.find(s)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        }
        private fun unescape(v: String): String {
            // reuse MacMenu's? duplicate minimal
            val sb = StringBuilder()
            var i = 0
            while (i < v.length) {
                val c = v[i]
                if (c == '\\') {
                    if (i + 1 >= v.length) break
                    when (v[i + 1]) {
                        '"' -> { sb.append('"'); i += 2; continue }
                        '\\' -> { sb.append('\\'); i += 2; continue }
                        '/' -> { sb.append('/'); i += 2; continue }
                        'n' -> { sb.append('\n'); i += 2; continue }
                        't' -> { sb.append('\t'); i += 2; continue }
                        'r' -> { sb.append('\r'); i += 2; continue }
                        'b' -> { sb.append('\b'); i += 2; continue }
                        'f' -> { sb.append('\u000C'); i += 2; continue }
                        'u' -> {
                            if (i + 5 < v.length) {
                                val hex = v.substring(i + 2, i + 6)
                                val cp = hex.toIntOrNull(16)
                                if (cp != null) { sb.append(cp.toChar()); i += 6; continue }
                            }
                        }
                    }
                }
                sb.append(c); i++
            }
            return sb.toString()
        }
        private fun jsonString(v: String): String {
            val sb = StringBuilder("\"")
            for (c in v) {
                when (c) {
                    '"' -> sb.append("\\\"")
                    '\\' -> sb.append("\\\\")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    '\b' -> sb.append("\\b")
                    '\u000C' -> sb.append("\\f")
                    else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
                }
            }
            sb.append("\"")
            return sb.toString()
        }
    }
}

data class FetchMenu(
    var version: Int? = null,
    var id: String? = null,
    var title: String? = null,
    var token: Int? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        version?.let { parts.add("\"version\":$it") }
        id?.let { parts.add("\"id\":${jsonString(it)}") }
        title?.let { parts.add("\"title\":${jsonString(it)}") }
        token?.let { parts.add("\"token\":$it") }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): FetchMenu? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val m = FetchMenu()
            m.version = regexLong(s, "version")?.toInt()
            m.id = regexString(s, "id")?.let { unescape(it) }
            m.title = regexString(s, "title")?.let { unescape(it) }
            m.token = regexLong(s, "token")?.toInt()
            return m
        }

        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun regexLong(s: String, key: String): Long? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toLongOrNull()
        }
        private fun unescape(v: String): String {
            val sb = StringBuilder()
            var i = 0
            while (i < v.length) {
                val c = v[i]
                if (c == '\\') {
                    if (i + 1 >= v.length) break
                    when (v[i + 1]) {
                        '"' -> { sb.append('"'); i += 2; continue }
                        '\\' -> { sb.append('\\'); i += 2; continue }
                        '/' -> { sb.append('/'); i += 2; continue }
                        'n' -> { sb.append('\n'); i += 2; continue }
                        't' -> { sb.append('\t'); i += 2; continue }
                        'r' -> { sb.append('\r'); i += 2; continue }
                        'b' -> { sb.append('\b'); i += 2; continue }
                        'f' -> { sb.append('\u000C'); i += 2; continue }
                        'u' -> {
                            if (i + 5 < v.length) {
                                val hex = v.substring(i + 2, i + 6)
                                val cp = hex.toIntOrNull(16)
                                if (cp != null) { sb.append(cp.toChar()); i += 6; continue }
                            }
                        }
                    }
                }
                sb.append(c); i++
            }
            return sb.toString()
        }
        private fun jsonString(v: String): String {
            val sb = StringBuilder("\"")
            for (c in v) {
                when (c) {
                    '"' -> sb.append("\\\"")
                    '\\' -> sb.append("\\\\")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    '\b' -> sb.append("\\b")
                    '\u000C' -> sb.append("\\f")
                    else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
                }
            }
            sb.append("\"")
            return sb.toString()
        }
    }
}

data class PressMenuItem(
    var version: Int? = null,
    var id: String? = null,
    var title: String? = null,
    var token: Int? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        version?.let { parts.add("\"version\":$it") }
        id?.let { parts.add("\"id\":${jsonString(it)}") }
        title?.let { parts.add("\"title\":${jsonString(it)}") }
        token?.let { parts.add("\"token\":$it") }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): PressMenuItem? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val m = PressMenuItem()
            m.version = regexLong(s, "version")?.toInt()
            m.id = regexString(s, "id")?.let { unescape(it) }
            m.title = regexString(s, "title")?.let { unescape(it) }
            m.token = regexLong(s, "token")?.toInt()
            return m
        }

        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun regexLong(s: String, key: String): Long? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toLongOrNull()
        }
        private fun unescape(v: String): String {
            val sb = StringBuilder()
            var i = 0
            while (i < v.length) {
                val c = v[i]
                if (c == '\\') {
                    if (i + 1 >= v.length) break
                    when (v[i + 1]) {
                        '"' -> { sb.append('"'); i += 2; continue }
                        '\\' -> { sb.append('\\'); i += 2; continue }
                        '/' -> { sb.append('/'); i += 2; continue }
                        'n' -> { sb.append('\n'); i += 2; continue }
                        't' -> { sb.append('\t'); i += 2; continue }
                        'r' -> { sb.append('\r'); i += 2; continue }
                        'b' -> { sb.append('\b'); i += 2; continue }
                        'f' -> { sb.append('\u000C'); i += 2; continue }
                        'u' -> {
                            if (i + 5 < v.length) {
                                val hex = v.substring(i + 2, i + 6)
                                val cp = hex.toIntOrNull(16)
                                if (cp != null) { sb.append(cp.toChar()); i += 6; continue }
                            }
                        }
                    }
                }
                sb.append(c); i++
            }
            return sb.toString()
        }
        private fun jsonString(v: String): String {
            val sb = StringBuilder("\"")
            for (c in v) {
                when (c) {
                    '"' -> sb.append("\\\"")
                    '\\' -> sb.append("\\\\")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    '\b' -> sb.append("\\b")
                    '\u000C' -> sb.append("\\f")
                    else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
                }
            }
            sb.append("\"")
            return sb.toString()
        }
    }
}