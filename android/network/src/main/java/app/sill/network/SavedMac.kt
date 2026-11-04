package app.sill.network

import app.sill.protocol.MacAddress
import app.sill.protocol.jsonString
import app.sill.protocol.unescapeJson

/**
 * A Mac this device paired with (port of iOSClient/SavedMacs.swift). Persisted by [SavedMacsStore].
 */
data class SavedMac(
    var macID: String,
    var fingerprint: String = "",
    var name: String = "",
    var recognitionKey: String = "",
    var remotePort: Int = 0,
    var addresses: List<MacAddress> = emptyList(),
    var typedAddresses: List<MacAddress>? = null,
    var infoIssuedAt: Double = 0.0,
    var bonjourName: String? = null,
    var lastWorked: String? = null,
    var pairedAt: Long = 0L,
    var method: String = "qr",
    var lastConnectedAt: Long? = null,
    var lastRoute: String? = null,
    var homeTLS: Boolean? = null,
    var revoked: Boolean? = null,
    var newKey: Boolean? = null,
    var group: String? = null,
) {
    val id: String get() = macID

    fun toJson(): String {
        val parts = mutableListOf<String>()
        parts.add("\"addresses\":[${addresses.joinToString(",") { it.toJson() }}]")
        bonjourName?.let { parts.add("\"bonjourName\":${jsonString(it)}") }
        fingerprint.takeIf { it.isNotEmpty() }?.let { parts.add("\"fingerprint\":${jsonString(it)}") }
        group?.let { parts.add("\"group\":${jsonString(it)}") }
        homeTLS?.let { parts.add("\"homeTLS\":$it") }
        infoIssuedAt.takeIf { it != 0.0 }?.let { parts.add("\"infoIssuedAt\":${it}") }
        lastConnectedAt?.let { parts.add("\"lastConnectedAt\":$it") }
        lastRoute?.let { parts.add("\"lastRoute\":${jsonString(it)}") }
        lastWorked?.let { parts.add("\"lastWorked\":${jsonString(it)}") }
        parts.add("\"macID\":${jsonString(macID)}")
        parts.add("\"method\":${jsonString(method)}")
        name.takeIf { it.isNotEmpty() }?.let { parts.add("\"name\":${jsonString(it)}") }
        newKey?.let { parts.add("\"newKey\":$it") }
        pairedAt.takeIf { it != 0L }?.let { parts.add("\"pairedAt\":$it") }
        recognitionKey.takeIf { it.isNotEmpty() }?.let { parts.add("\"recognitionKey\":${jsonString(it)}") }
        remotePort.takeIf { it != 0 }?.let { parts.add("\"remotePort\":$it") }
        revoked?.let { parts.add("\"revoked\":$it") }
        typedAddresses?.let { list ->
            parts.add("\"typedAddresses\":[${list.joinToString(",") { it.toJson() }}]")
        }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): SavedMac? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val macID = regexString(s, "macID")?.let { unescapeJson(it) } ?: return null
            val m = SavedMac(macID = macID)
            m.fingerprint = regexString(s, "fingerprint")?.let { unescapeJson(it) } ?: m.fingerprint
            m.name = regexString(s, "name")?.let { unescapeJson(it) } ?: m.name
            m.recognitionKey = regexString(s, "recognitionKey")?.let { unescapeJson(it) } ?: m.recognitionKey
            m.remotePort = regexInt(s, "remotePort") ?: m.remotePort
            m.addresses = parseMacAddressList(extractArray(s, "addresses") ?: "")
            m.typedAddresses = if (s.contains("\"typedAddresses\"")) {
                parseMacAddressList(extractArray(s, "typedAddresses") ?: "")
            } else null
            m.infoIssuedAt = regexDouble(s, "infoIssuedAt") ?: m.infoIssuedAt
            m.bonjourName = regexString(s, "bonjourName")?.let { unescapeJson(it) }
            m.lastWorked = regexString(s, "lastWorked")?.let { unescapeJson(it) }
            m.pairedAt = regexLong(s, "pairedAt") ?: m.pairedAt
            m.method = regexString(s, "method")?.let { unescapeJson(it) } ?: m.method
            m.lastConnectedAt = regexLong(s, "lastConnectedAt")
            m.lastRoute = regexString(s, "lastRoute")?.let { unescapeJson(it) }
            m.homeTLS = regexBool(s, "homeTLS")
            m.revoked = regexBool(s, "revoked")
            m.newKey = regexBool(s, "newKey")
            m.group = regexString(s, "group")?.let { unescapeJson(it) }
            return m
        }

        fun decodeList(text: String?): List<SavedMac> {
            if (text == null) return emptyList()
            val t = text.trim()
            if (t.isEmpty()) return emptyList()
            if (!t.startsWith("[")) return emptyList()
            val content = t.substring(1, t.length - 1).trim()
            if (content.isEmpty()) return emptyList()
            val items = splitTopLevel(content)
            return items.mapNotNull { fromJson(it) }
        }

        fun encodeList(list: List<SavedMac>): String =
            "[" + list.joinToString(",") { it.toJson() } + "]"

        // --- helpers ---
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
        private fun regexDouble(s: String, key: String): Double? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
            return re.find(s)?.groupValues?.get(1)?.toDoubleOrNull()
        }
        private fun regexBool(s: String, key: String): Boolean? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(true|false)")
            return re.find(s)?.groupValues?.get(1)?.toBooleanStrictOrNull()
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
                    ']' -> {
                        depth--
                        if (depth == 0) return s.substring(start + 1, i)
                    }
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

        private fun parseMacAddressList(content: String): List<MacAddress> {
            if (content.isBlank()) return emptyList()
            val items = splitTopLevel(content)
            return items.mapNotNull { MacAddress.fromJson(it) }
        }

        private fun splitTopLevel(content: String): List<String> {
            val out = mutableListOf<String>()
            var depth = 0
            var start = 0
            var inStr = false
            var i = 0
            while (i < content.length) {
                val c = content[i]
                if (inStr) {
                    if (c == '\\') { i += 2; continue }
                    if (c == '"') inStr = false
                    i++
                    continue
                }
                when (c) {
                    '"' -> inStr = true
                    '{', '[' -> depth++
                    '}', ']' -> depth--
                    ',' -> if (depth == 0) {
                        val part = content.substring(start, i).trim()
                        if (part.isNotEmpty()) out.add(part)
                        start = i + 1
                    }
                }
                i++
            }
            val tail = content.substring(start).trim()
            if (tail.isNotEmpty()) out.add(tail)
            return out
        }
    }
}

object SavedMacs {
    const val cap = 16
    fun decode(text: String?): List<SavedMac> = SavedMac.decodeList(text)
    fun encode(list: List<SavedMac>): String = SavedMac.encodeList(list)
}
