package app.sill.protocol

import java.util.Base64

/**
 * Port of `Sources/StreamProtocol/Clipboard.swift`.
 */
data class Clipboard(
    var text: String? = null,
    var image: ByteArray? = null,
    var stop: Boolean? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Clipboard) return false
        return text == other.text && stop == other.stop &&
            (image?.contentEquals(other.image) ?: (other.image == null))
    }
    override fun hashCode(): Int {
        var r = text?.hashCode() ?: 0
        r = 31 * r + (image?.contentHashCode() ?: 0)
        r = 31 * r + (stop?.hashCode() ?: 0)
        return r
    }

    fun toJson(): String {
        val parts = mutableListOf<String>()
        image?.let { parts.add("\"image\":${jsonString(Base64.getEncoder().encodeToString(it))}") }
        stop?.let { parts.add("\"stop\":$it") }
        text?.let { parts.add("\"text\":${jsonString(it)}") }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        const val maxTextBytes = 256 * 1024

        fun fromJson(json: String): Clipboard? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val c = Clipboard()
            regexString(s, "text")?.let { c.text = unescapeJson(it) }
            regexString(s, "image")?.let { b64 ->
                try { c.image = Base64.getDecoder().decode(b64) } catch (e: Exception) { c.image = null }
            }
            c.stop = regexBool(s, "stop")
            return c
        }
        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun regexBool(s: String, key: String): Boolean? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(true|false)")
            return re.find(s)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        }
    }
}