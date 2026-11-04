package app.sill.protocol

/**
 * Port of `Sources/StreamProtocol/Compatibility.swift`.
 */
object SillProtocol {
    const val current = 1
}

class SillVersion(components: List<Int>) : Comparable<SillVersion> {
    val components: List<Int> = dropTrailingZeros(components)

    companion object {
        val zero = SillVersion(emptyList())

        fun parse(text: String): SillVersion? {
            var scalars = text.trim().toList()
            if (scalars.isNotEmpty() && (scalars[0] == 'v' || scalars[0] == 'V')) scalars = scalars.drop(1)
            val prefix = mutableListOf<Char>()
            for (s in scalars) {
                if (s.isDigit() || s == '.') prefix.add(s) else break
            }
            while (prefix.isNotEmpty() && prefix.last() == '.') prefix.removeAt(prefix.lastIndex)
            if (prefix.isEmpty() || !prefix[0].isDigit()) return null
            val split = String(prefix.toCharArray()).split('.')
            if (split.size > 8) return null
            val numbers = mutableListOf<Int>()
            for (part in split) {
                if (part.isEmpty()) return null
                if (part.length !in 1..9) return null
                if (!part.all { it.isDigit() }) return null
                val n = part.toIntOrNull() ?: return null
                numbers.add(n)
            }
            return SillVersion(numbers)
        }

        private fun dropTrailingZeros(c: List<Int>): List<Int> {
            val out = c.toMutableList()
            while (out.isNotEmpty() && out.last() == 0) out.removeAt(out.lastIndex)
            return out
        }
    }

    override fun compareTo(other: SillVersion): Int {
        val n = maxOf(components.size, other.components.size)
        for (i in 0 until n) {
            val x = if (i < components.size) components[i] else 0
            val y = if (i < other.components.size) other.components[i] else 0
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    override fun toString(): String {
        val c = components.toMutableList()
        while (c.size < 2) c.add(0)
        return c.joinToString(".")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SillVersion) return false
        return components == other.components
    }

    override fun hashCode(): Int = components.hashCode()
}

data class Hello(
    var appVersion: String? = null,
    var build: String? = null,
    var protocol: Int? = null,
    var device: String? = null,
    var audio: List<String>? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        audio?.let { parts.add("\"audio\":[" + it.joinToString(",") { jsonString(it) } + "]") }
        appVersion?.let { parts.add("\"appVersion\":${jsonString(it)}") }
        build?.let { parts.add("\"build\":${jsonString(it)}") }
        device?.let { parts.add("\"device\":${jsonString(it)}") }
        protocol?.let { parts.add("\"protocol\":$it") }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): Hello? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val h = Hello()
            h.appVersion = regexString(s, "appVersion")?.let { unescapeJson(it) }
            h.build = regexString(s, "build")?.let { unescapeJson(it) }
            h.protocol = regexInt(s, "protocol")
            h.device = regexString(s, "device")?.let { unescapeJson(it) }
            val arr = extractArray(s, "audio")
            if (arr != null) {
                val content = arr.trim().removeSurrounding("[", "]").trim()
                if (content.isEmpty()) h.audio = emptyList()
                else h.audio = content.split(",").map { unescapeJson(it.trim().removeSurrounding("\"")) }
            }
            return h
        }
        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun regexInt(s: String, key: String): Int? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toIntOrNull()
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
    }
}
