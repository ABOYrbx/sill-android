package app.sill.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Client -> host viewport (kind 9) and related types.
 *
 * Port of `Sources/StreamProtocol/Viewport.swift`.
 */
data class Viewport(
    var width: Double,
    var height: Double,
    var scale: Double? = null,
    var localCursor: Boolean? = null,
    var fps: Int? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        fps?.let { parts.add("\"fps\":$it") }
        height.let { parts.add("\"height\":${jsonNumber(it)}") }
        localCursor?.let { parts.add("\"localCursor\":$it") }
        scale?.let { parts.add("\"scale\":${jsonNumber(it)}") }
        width.let { parts.add("\"width\":${jsonNumber(it)}") }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): Viewport? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val width = regexDouble(s, "width") ?: return null
            val height = regexDouble(s, "height") ?: return null
            val scale = regexDoubleOrNull(s, "scale")
            val localCursor = regexBool(s, "localCursor")
            val fps = regexInt(s, "fps")
            return Viewport(width, height, scale, localCursor, fps)
        }

        private fun regexDouble(s: String, key: String): Double? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
            return re.find(s)?.groupValues?.get(1)?.toDoubleOrNull()
        }
        private fun regexDoubleOrNull(s: String, key: String): Double? {
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

data class ClientStats(
    var fps: Int,
    var frameAgeMs: Int,
    var rttMs: Int,
    var device: String,
    var frameAgeMaxMs: Int? = null,
    var rttMaxMs: Int? = null,
    var audioBehindMs: Int? = null,
    var audioLate: Int? = null,
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        audioBehindMs?.let { parts.add("\"audioBehindMs\":$it") }
        audioLate?.let { parts.add("\"audioLate\":$it") }
        parts.add("\"device\":${jsonString(device)}")
        frameAgeMaxMs?.let { parts.add("\"frameAgeMaxMs\":$it") }
        parts.add("\"frameAgeMs\":$frameAgeMs")
        parts.add("\"fps\":$fps")
        rttMaxMs?.let { parts.add("\"rttMaxMs\":$it") }
        parts.add("\"rttMs\":$rttMs")
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        fun fromJson(json: String): ClientStats? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val fps = regexInt(s, "fps") ?: return null
            val frameAgeMs = regexInt(s, "frameAgeMs") ?: return null
            val rttMs = regexInt(s, "rttMs") ?: return null
            val device = regexString(s, "device") ?: return null
            return ClientStats(
                fps = fps,
                frameAgeMs = frameAgeMs,
                rttMs = rttMs,
                device = unescape(device),
                frameAgeMaxMs = regexInt(s, "frameAgeMaxMs"),
                rttMaxMs = regexInt(s, "rttMaxMs"),
                audioBehindMs = regexInt(s, "audioBehindMs"),
                audioLate = regexInt(s, "audioLate"),
            )
        }

        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }
        private fun regexInt(s: String, key: String): Int? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toIntOrNull()
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

        fun lateSince(count: Int, adding: Int?): Int {
            val more = maxOf(0, adding ?: 0)
            return if (count > Int.MAX_VALUE - more) Int.MAX_VALUE else count + more
        }
    }
}

object ViewportBounds {
    val sides: ClosedFloatingPointRange<Double> = 1.0..16384.0
    val scales: ClosedFloatingPointRange<Double> = 0.25..4.0

    fun refusal(v: Viewport): String? {
        val points = "(${whole(sides.start)} to ${whole(sides.endInclusive)} points)"
        val why = mutableListOf<String>()
        if (v.width !in sides) why.add("width ${number(v.width)} $points")
        if (v.height !in sides) why.add("height ${number(v.height)} $points")
        v.scale?.let { s ->
            if (s !in scales) why.add("scale ${number(s)} (${number(scales.start)} to ${number(scales.endInclusive)})")
        }
        return if (why.isEmpty()) null else why.joinToString(", ")
    }

    fun held(v: Viewport): Viewport? {
        if (!v.width.isFinite() || !v.height.isFinite() || v.width <= 0 || v.height <= 0) return null
        val held = Viewport(v.width, v.height, v.scale, v.localCursor, v.fps)
        held.width = minOf(maxOf(v.width, sides.start), sides.endInclusive)
        held.height = minOf(maxOf(v.height, sides.start), sides.endInclusive)
        held.scale = v.scale?.let { s ->
            if (s.isFinite() && s > 0) minOf(maxOf(s, scales.start), scales.endInclusive) else null
        }
        return held
    }

    fun whole(x: Double): String {
        val r = kotlin.math.floor(x) // toward zero approx
        val i = r.toInt()
        return if (i.toDouble() == x) i.toString() else x.toString()
    }

    fun number(x: Double): String {
        if (x == kotlin.math.floor(x)) {
            val i = x.toInt()
            if (i.toDouble() == x) return i.toString()
        }
        return x.toString()
    }
}

object TextScale {
    const val off = 0.25
    val scales: List<Double> = listOf(0.5, 0.75, 1.0, 1.25, 1.5)

    fun detents(offered: Boolean): List<Double> =
        if (offered) listOf(off) + scales else scales

    fun positionOf(scale: Double?, offered: Boolean): Double =
        scale ?: if (offered) off else 1.0

    fun scaleAt(position: Double): Double? =
        if (position == off) null else position

    fun snap(value: Double, offered: Boolean): Double {
        val d = detents(offered)
        val clamped = minOf(maxOf(value, d.first()), d.last())
        return d.minByOrNull { kotlin.math.abs(it - clamped) } ?: 1.0
    }

    fun number(scale: Double): String {
        var text = "%.2f".format(scale)
        while (text.endsWith("0")) text = text.dropLast(1).toString()
        if (text.endsWith(".")) text = text.dropLast(1).toString()
        return text
    }

    fun title(position: Double): String =
        if (position == off) "Off" else number(position) + "×"

    fun spoken(position: Double): String =
        if (position == off) "Off" else number(position) + " times"

    fun step(from: Double, up: Boolean, offered: Boolean): Double {
        val d = detents(offered)
        val i = d.indexOf(from).let { if (it >= 0) it else d.indexOf(1.0).coerceAtLeast(0) }
        val ni = if (up) minOf(i + 1, d.size - 1) else maxOf(i - 1, 0)
        return d[ni]
    }

    fun endsFit(previous: Double?, now: Double?): Boolean =
        previous != null && now == null

    fun offersOff(virtualDisplay: Boolean?, source: StreamSource, onVirtualDisplay: Boolean?): Boolean {
        if (source is StreamSource.Window && onVirtualDisplay != null) return !onVirtualDisplay
        return virtualDisplay != true
    }

    fun selectable(source: StreamSource): Boolean = source is StreamSource.Window

    const val unavailableHint = "Available for a window."
}

data class CursorShape(val hotspotX: Float, val hotspotY: Float, val width: Float, val height: Float, val png: ByteArray)

object CursorShapeBlob {
    fun encode(hotspotX: Float, hotspotY: Float, width: Float, height: Float, png: ByteArray): ByteArray {
        val buf = ByteBuffer.allocate(16 + png.size).order(ByteOrder.BIG_ENDIAN)
        buf.putFloat(hotspotX)
        buf.putFloat(hotspotY)
        buf.putFloat(width)
        buf.putFloat(height)
        buf.put(png)
        return buf.array()
    }

    fun decode(data: ByteArray): CursorShape? {
        if (data.size <= 16) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
        val hx = buf.float
        val hy = buf.float
        val w = buf.float
        val h = buf.float
        val png = ByteArray(data.size - 16)
        buf.get(png)
        return CursorShape(hx, hy, w, h, png)
    }
}
