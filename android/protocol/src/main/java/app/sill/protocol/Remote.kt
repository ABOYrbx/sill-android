package app.sill.protocol

/**
 * Port of `Sources/StreamProtocol/Remote.swift` (remote access payloads, kinds 18-22).
 */
data class MacAddress(
    var host: String,
    var port: Int? = null,
    var kind: String,
    var via: String,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        parts.add("\"host\":${jsonString(host)}")
        parts.add("\"kind\":${jsonString(kind)}")
        port?.let { parts.add("\"port\":$it") }
        parts.add("\"via\":${jsonString(via)}")
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        const val vpn = "vpn"; const val lan = "lan"; const val internet = "internet"

        fun fromJson(json: String): MacAddress? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val host = regexString(s, "host")?.let { unescape(it) } ?: return null
            val kind = regexString(s, "kind")?.let { unescape(it) } ?: return null
            val via = regexString(s, "via")?.let { unescape(it) } ?: return null
            val port = regexInt(s, "port")
            return MacAddress(host, port, kind, via)
        }
        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun regexInt(s: String, key: String): Int? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toIntOrNull()
        }
        private fun unescape(v: String): String = unescapeJson(v)
    }
}

data class MacInfo(
    var v: Int = 1,
    var macID: String,
    var name: String,
    var issuedAt: Double,
    var remoteAccess: Boolean,
    var remotePort: Int,
    var internet: Boolean,
    var addresses: List<MacAddress>,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        parts.add("\"addresses\":[" + addresses.joinToString(",") { it.toJson() } + "]")
        parts.add("\"internet\":$internet")
        parts.add("\"issuedAt\":${jsonNumber(issuedAt)}")
        parts.add("\"macID\":${jsonString(macID)}")
        parts.add("\"name\":${jsonString(name)}")
        parts.add("\"remoteAccess\":$remoteAccess")
        parts.add("\"remotePort\":$remotePort")
        parts.add("\"v\":$v")
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): MacInfo? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val addresses = parseAddressList(s, "addresses") ?: return null
            val internet = regexBool(s, "internet") ?: return null
            val issuedAt = regexDouble(s, "issuedAt") ?: return null
            val macID = regexString(s, "macID")?.let { unescape(it) } ?: return null
            val name = regexString(s, "name")?.let { unescape(it) } ?: return null
            val remoteAccess = regexBool(s, "remoteAccess") ?: return null
            val remotePort = regexInt(s, "remotePort") ?: return null
            val v = regexInt(s, "v") ?: 1
            return MacInfo(v, macID, name, issuedAt, remoteAccess, remotePort, internet, addresses)
        }
        private fun parseAddressList(s: String, key: String): List<MacAddress>? {
            val arr = extractArray(s, key) ?: return null
            if (arr.trim() == "null") return null
            val content = arr.trim().removeSurrounding("[", "]").trim()
            if (content.isEmpty()) return emptyList()
            val items = mutableListOf<MacAddress>()
            for (obj in splitTopLevelObjects(content)) {
                items.add(MacAddress.fromJson(obj) ?: return null)
            }
            return items
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
        private fun regexDouble(s: String, key: String): Double? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
            return re.find(s)?.groupValues?.get(1)?.toDoubleOrNull()
        }
        private fun unescape(v: String): String = unescapeJson(v)
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
    }
}

data class SignedMacInfo(
    var info: String,
    var key: String,
    var sig: String,
) {
    fun toJson(): String =
        "{\"info\":${jsonString(info)},\"key\":${jsonString(key)},\"sig\":${jsonString(sig)}}"

    companion object {
        fun fromJson(json: String): SignedMacInfo? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val info = regexString(s, "info")?.let { unescape(it) } ?: return null
            val key = regexString(s, "key")?.let { unescape(it) } ?: return null
            val sig = regexString(s, "sig")?.let { unescape(it) } ?: return null
            return SignedMacInfo(info, key, sig)
        }
        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun unescape(v: String): String = unescapeJson(v)
    }
}

data class PairRequest(
    var v: Int = 1,
    var method: String,
    var proof: String,
    var name: String,
    var model: String? = null,
    var cable: Boolean? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        cable?.let { parts.add("\"cable\":$it") }
        parts.add("\"method\":${jsonString(method)}")
        model?.let { parts.add("\"model\":${jsonString(it)}") }
        parts.add("\"name\":${jsonString(name)}")
        parts.add("\"proof\":${jsonString(proof)}")
        parts.add("\"v\":$v")
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        const val qr = "qr"; const val code = "code"; const val ask = "ask"; const val cancel = "cancel"
        const val version = 1

        fun fromJson(json: String): PairRequest? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val method = regexString(s, "method")?.let { unescape(it) } ?: return null
            val name = regexString(s, "name")?.let { unescape(it) } ?: return null
            val proof = regexString(s, "proof")?.let { unescape(it) } ?: return null
            val v = regexInt(s, "v") ?: 1
            val model = regexString(s, "model")?.let { unescape(it) }
            val cable = regexBool(s, "cable")
            return PairRequest(v, method, proof, name, model, cable)
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
        private fun unescape(v: String): String = unescapeJson(v)
    }
}

data class PairResult(
    var ok: Boolean,
    var proof: String? = null,
    var macID: String? = null,
    var name: String? = null,
    var recognitionKey: String? = null,
    var reason: String? = null,
    var triesLeft: Int? = null,
    var retryAfter: Double? = null,
    var method: String? = null,
    var message: String? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        macID?.let { parts.add("\"macID\":${jsonString(it)}") }
        message?.let { parts.add("\"message\":${jsonString(it)}") }
        method?.let { parts.add("\"method\":${jsonString(it)}") }
        name?.let { parts.add("\"name\":${jsonString(it)}") }
        parts.add("\"ok\":$ok")
        proof?.let { parts.add("\"proof\":${jsonString(it)}") }
        reason?.let { parts.add("\"reason\":${jsonString(it)}") }
        recognitionKey?.let { parts.add("\"recognitionKey\":${jsonString(it)}") }
        retryAfter?.let { parts.add("\"retryAfter\":${jsonNumber(it)}") }
        triesLeft?.let { parts.add("\"triesLeft\":$it") }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        const val code = "code"; const val closed = "closed"; const val expired = "expired"; const val stopped = "stopped"; const val busy = "busy"
        const val shown = "shown"; const val openOnMac = "openOnMac"; const val locked = "locked"
        const val cable = "cable"
        val knownReasons = setOf(code, closed, expired, stopped, busy, shown, openOnMac, locked)

        fun fromJson(json: String): PairResult? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val ok = regexBool(s, "ok") ?: return null
            val r = PairResult(ok)
            r.proof = regexString(s, "proof")?.let { unescape(it) }
            r.macID = regexString(s, "macID")?.let { unescape(it) }
            r.name = regexString(s, "name")?.let { unescape(it) }
            r.recognitionKey = regexString(s, "recognitionKey")?.let { unescape(it) }
            r.reason = regexString(s, "reason")?.let { unescape(it) }
            r.triesLeft = regexInt(s, "triesLeft")
            r.retryAfter = regexDouble(s, "retryAfter")
            r.method = regexString(s, "method")?.let { unescape(it) }
            r.message = regexString(s, "message")?.let { unescape(it) }
            return r
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
        private fun regexDouble(s: String, key: String): Double? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
            return re.find(s)?.groupValues?.get(1)?.toDoubleOrNull()
        }
        private fun unescape(v: String): String = unescapeJson(v)
    }
}

data class Goodbye(
    var reason: String,
    var message: String? = null,
    var minimumVersion: String? = null,
    var reconnect: Boolean? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        message?.let { parts.add("\"message\":${jsonString(it)}") }
        minimumVersion?.let { parts.add("\"minimumVersion\":${jsonString(it)}") }
        parts.add("\"reason\":${jsonString(reason)}")
        reconnect?.let { parts.add("\"reconnect\":$it") }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        const val removed = "removed"; const val remoteOff = "remoteOff"; const val internetOff = "internetOff"
        const val quit = "quit"; const val busy = "busy"; const val pairingRequired = "pairingRequired"
        const val update = "update"

        fun fromJson(json: String): Goodbye? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val reason = regexString(s, "reason")?.let { unescape(it) } ?: return null
            val g = Goodbye(reason)
            g.message = regexString(s, "message")?.let { unescape(it) }
            g.minimumVersion = regexString(s, "minimumVersion")?.let { unescape(it) }
            g.reconnect = regexBool(s, "reconnect")
            return g
        }
        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun regexBool(s: String, key: String): Boolean? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(true|false)")
            return re.find(s)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        }
        private fun unescape(v: String): String = unescapeJson(v)
    }
}
