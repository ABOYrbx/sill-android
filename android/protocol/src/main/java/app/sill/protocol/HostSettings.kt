package app.sill.protocol

/**
 * Port of `Sources/StreamProtocol/HostSettings.swift`.
 */
data class StreamSettings(
    var maxFPS: Int,
    var bitrate: Int,
    var captureScale: Double,
    var prioritizeSpeed: Boolean,
    var virtualDisplay: Boolean,
    var directWireless: Boolean? = null,
    var sendAudio: Boolean? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        parts.add("\"bitrate\":$bitrate")
        parts.add("\"captureScale\":${jsonNumber(captureScale)}")
        directWireless?.let { parts.add("\"directWireless\":$it") }
        parts.add("\"maxFPS\":$maxFPS")
        parts.add("\"prioritizeSpeed\":$prioritizeSpeed")
        sendAudio?.let { parts.add("\"sendAudio\":$it") }
        parts.add("\"virtualDisplay\":$virtualDisplay")
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): StreamSettings? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val maxFPS = regexInt(s, "maxFPS") ?: return null
            val bitrate = regexInt(s, "bitrate") ?: return null
            val captureScale = regexDouble(s, "captureScale") ?: return null
            val prioritizeSpeed = regexBool(s, "prioritizeSpeed") ?: return null
            val virtualDisplay = regexBool(s, "virtualDisplay") ?: return null
            val directWireless = regexBool(s, "directWireless")
            val sendAudio = regexBool(s, "sendAudio")
            return StreamSettings(maxFPS, bitrate, captureScale, prioritizeSpeed, virtualDisplay, directWireless, sendAudio)
        }
        private fun regexInt(s: String, key: String): Int? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toIntOrNull()
        }
        private fun regexDouble(s: String, key: String): Double? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
            return re.find(s)?.groupValues?.get(1)?.toDoubleOrNull()
        }
        private fun regexBool(s: String, key: String): Boolean? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(true|false)")
            return re.find(s)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        }
    }
}

data class RunningStream(
    var width: Int,
    var height: Int,
    var fps: Int,
    var mbps: Int,
    var onVirtualDisplay: Boolean,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        parts.add("\"fps\":$fps")
        parts.add("\"height\":$height")
        parts.add("\"mbps\":$mbps")
        parts.add("\"onVirtualDisplay\":$onVirtualDisplay")
        parts.add("\"width\":$width")
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): RunningStream? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val width = regexInt(s, "width") ?: return null
            val height = regexInt(s, "height") ?: return null
            val fps = regexInt(s, "fps") ?: return null
            val mbps = regexInt(s, "mbps") ?: return null
            val onVirtualDisplay = regexBool(s, "onVirtualDisplay") ?: return null
            return RunningStream(width, height, fps, mbps, onVirtualDisplay)
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

data class AwayQuality(
    var homeBitrate: Int,
    var homeCaptureScale: Double,
    var awayBitrate: Int,
    var awayCaptureScale: Double,
    var thisConnectionAway: Boolean,
    var awayRunning: Boolean,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        parts.add("\"awayBitrate\":$awayBitrate")
        parts.add("\"awayCaptureScale\":${jsonNumber(awayCaptureScale)}")
        parts.add("\"awayRunning\":$awayRunning")
        parts.add("\"homeBitrate\":$homeBitrate")
        parts.add("\"homeCaptureScale\":${jsonNumber(homeCaptureScale)}")
        parts.add("\"thisConnectionAway\":$thisConnectionAway")
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): AwayQuality? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val homeBitrate = regexInt(s, "homeBitrate") ?: return null
            val homeCaptureScale = regexDouble(s, "homeCaptureScale") ?: return null
            val awayBitrate = regexInt(s, "awayBitrate") ?: return null
            val awayCaptureScale = regexDouble(s, "awayCaptureScale") ?: return null
            val thisConnectionAway = regexBool(s, "thisConnectionAway") ?: return null
            val awayRunning = regexBool(s, "awayRunning") ?: return null
            return AwayQuality(homeBitrate, homeCaptureScale, awayBitrate, awayCaptureScale, thisConnectionAway, awayRunning)
        }
        private fun regexInt(s: String, key: String): Int? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toIntOrNull()
        }
        private fun regexDouble(s: String, key: String): Double? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
            return re.find(s)?.groupValues?.get(1)?.toDoubleOrNull()
        }
        private fun regexBool(s: String, key: String): Boolean? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(true|false)")
            return re.find(s)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        }
    }
}

data class LinkReport(
    var state: String,
    var withheldPerSecond: Int,
    var bitrate: Int,
    var carriedKbps: Int? = null,
    var suggestedBitrate: Int? = null,
    var suggestedCaptureScale: Double? = null,
) {
    val isBehind: Boolean get() = state == behind

    fun toJson(): String {
        val parts = mutableListOf<String>()
        parts.add("\"bitrate\":$bitrate")
        carriedKbps?.let { parts.add("\"carriedKbps\":$it") }
        parts.add("\"state\":${jsonString(state)}")
        suggestedBitrate?.let { parts.add("\"suggestedBitrate\":$it") }
        suggestedCaptureScale?.let { parts.add("\"suggestedCaptureScale\":${jsonNumber(it)}") }
        parts.add("\"withheldPerSecond\":$withheldPerSecond")
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        const val behind = "behind"
        const val stalled = "stalled"

        fun fromJson(json: String): LinkReport? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val state = regexString(s, "state")?.let { unescapeJson(it) } ?: return null
            val withheldPerSecond = regexInt(s, "withheldPerSecond") ?: return null
            val bitrate = regexInt(s, "bitrate") ?: return null
            return LinkReport(state, withheldPerSecond, bitrate,
                carriedKbps = regexInt(s, "carriedKbps"),
                suggestedBitrate = regexInt(s, "suggestedBitrate"),
                suggestedCaptureScale = regexDouble(s, "suggestedCaptureScale"))
        }
        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun regexInt(s: String, key: String): Int? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toIntOrNull()
        }
        private fun regexDouble(s: String, key: String): Double? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
            return re.find(s)?.groupValues?.get(1)?.toDoubleOrNull()
        }
    }
}

data class HostSettingsState(
    var settings: StreamSettings,
    var persistent: Boolean,
    var virtualDisplayAvailable: Boolean,
    var virtualDisplayNote: String? = null,
    var softwareEncoder: Boolean,
    var stream: RunningStream? = null,
    var answering: Int? = null,
    var away: AwayQuality? = null,
    var link: LinkReport? = null,
    var audioNote: String? = null,
    var sessionPaired: Boolean? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        answering?.let { parts.add("\"answering\":$it") }
        audioNote?.let { parts.add("\"audioNote\":${jsonString(it)}") }
        away?.let { parts.add("\"away\":" + it.toJson()) }
        link?.let { parts.add("\"link\":" + it.toJson()) }
        parts.add("\"persistent\":$persistent")
        sessionPaired?.let { parts.add("\"sessionPaired\":$it") }
        parts.add("\"settings\":" + settings.toJson())
        parts.add("\"softwareEncoder\":$softwareEncoder")
        stream?.let { parts.add("\"stream\":" + it.toJson()) }
        parts.add("\"virtualDisplayAvailable\":$virtualDisplayAvailable")
        virtualDisplayNote?.let { parts.add("\"virtualDisplayNote\":${jsonString(it)}") }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): HostSettingsState? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val settingsJson = extractObject(s, "settings") ?: return null
            val settings = StreamSettings.fromJson(settingsJson) ?: return null
            val persistent = regexBool(s, "persistent") ?: return null
            val virtualDisplayAvailable = regexBool(s, "virtualDisplayAvailable") ?: return null
            val softwareEncoder = regexBool(s, "softwareEncoder") ?: return null
            val h = HostSettingsState(settings, persistent, virtualDisplayAvailable, softwareEncoder = softwareEncoder)
            h.virtualDisplayNote = regexString(s, "virtualDisplayNote")?.let { unescapeJson(it) }
            h.stream = extractObject(s, "stream")?.let { RunningStream.fromJson(it) }
            h.answering = regexInt(s, "answering")
            h.away = extractObject(s, "away")?.let { AwayQuality.fromJson(it) }
            h.link = extractObject(s, "link")?.let { LinkReport.fromJson(it) }
            h.audioNote = regexString(s, "audioNote")?.let { unescapeJson(it) }
            h.sessionPaired = regexBool(s, "sessionPaired")
            return h
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
    }
}

data class HostSettingsChange(
    var token: Int? = null,
    var maxFPS: Int? = null,
    var bitrate: Int? = null,
    var captureScale: Double? = null,
    var prioritizeSpeed: Boolean? = null,
    var virtualDisplay: Boolean? = null,
    var directWireless: Boolean? = null,
    var sendAudio: Boolean? = null,
) {
    val isEmpty: Boolean
        get() = maxFPS == null && bitrate == null && captureScale == null && prioritizeSpeed == null &&
            virtualDisplay == null && directWireless == null && sendAudio == null

    fun appliedTo(s: StreamSettings): StreamSettings {
        val r = StreamSettings(s.maxFPS, s.bitrate, s.captureScale, s.prioritizeSpeed, s.virtualDisplay, s.directWireless, s.sendAudio)
        maxFPS?.let { r.maxFPS = it }
        bitrate?.let { r.bitrate = it }
        captureScale?.let { r.captureScale = it }
        prioritizeSpeed?.let { r.prioritizeSpeed = it }
        virtualDisplay?.let { r.virtualDisplay = it }
        directWireless?.let { r.directWireless = it }
        sendAudio?.let { r.sendAudio = it }
        return r
    }

    fun toJson(): String {
        val parts = mutableListOf<String>()
        bitrate?.let { parts.add("\"bitrate\":$it") }
        captureScale?.let { parts.add("\"captureScale\":${jsonNumber(it)}") }
        directWireless?.let { parts.add("\"directWireless\":$it") }
        maxFPS?.let { parts.add("\"maxFPS\":$it") }
        prioritizeSpeed?.let { parts.add("\"prioritizeSpeed\":$it") }
        sendAudio?.let { parts.add("\"sendAudio\":$it") }
        token?.let { parts.add("\"token\":$it") }
        virtualDisplay?.let { parts.add("\"virtualDisplay\":$it") }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): HostSettingsChange? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val c = HostSettingsChange()
            c.token = regexInt(s, "token")
            c.maxFPS = regexInt(s, "maxFPS")
            c.bitrate = regexInt(s, "bitrate")
            c.captureScale = regexDouble(s, "captureScale")
            c.prioritizeSpeed = regexBool(s, "prioritizeSpeed")
            c.virtualDisplay = regexBool(s, "virtualDisplay")
            c.directWireless = regexBool(s, "directWireless")
            c.sendAudio = regexBool(s, "sendAudio")
            return c
        }
        private fun regexInt(s: String, key: String): Int? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toIntOrNull()
        }
        private fun regexDouble(s: String, key: String): Double? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
            return re.find(s)?.groupValues?.get(1)?.toDoubleOrNull()
        }
        private fun regexBool(s: String, key: String): Boolean? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(true|false)")
            return re.find(s)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        }
    }
}

object SettingsChoices {
    val maxFPS = listOf(60, 120)
    val captureScale = listOf(2.0, 1.0)
    val bitrate: List<Int>
        get() = QualityPreset.entries.map { it.rawValue }
}

enum class QualityPreset(val rawValue: Int) {
    Low(4_000_000),
    Efficient(8_000_000),
    Balanced(15_000_000),
    High(25_000_000),
    Pro(40_000_000),
    Ultra(80_000_000),
    Extreme(150_000_000);

    val id: Int get() = rawValue

    val displayName: String
        get() = when (this) {
            Low -> "Low"; Efficient -> "Efficient"; Balanced -> "Balanced"; High -> "High"
            Pro -> "Pro"; Ultra -> "Ultra"; Extreme -> "Extreme"
        }

    val title: String get() = "$displayName — ${rawValue / 1_000_000} Mbps"

    companion object {
        val entries: List<QualityPreset> = values().toList()

        fun title(forBitrate: Int): String =
            values().firstOrNull { it.rawValue == forBitrate }?.title ?: "Custom — ${mbps(forBitrate)} Mbps"

        fun name(forBitrate: Int): String =
            values().firstOrNull { it.rawValue == forBitrate }?.displayName ?: "${mbps(forBitrate)} Mbps"

        fun shortTitle(bitrate: Int, captureScale: Double): String =
            "${name(bitrate)} · ${if (captureScale >= 1.5) "Retina" else "Standard"}"

        fun mbps(bitsPerSecond: Int): String =
            if (bitsPerSecond % 1_000_000 == 0) (bitsPerSecond / 1_000_000).toString()
            else "%.1f".format(bitsPerSecond.toDouble() / 1_000_000)

        const val fastLinkNote = "Ultra and Extreme need the USB cable or very fast Wi-Fi; if the picture lags, step down."
    }
}