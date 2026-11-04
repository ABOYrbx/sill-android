package app.sill.protocol

/**
 * Client -> host input.
 *
 * Port of `Sources/StreamProtocol/Input.swift`.
 *
 * Positions are fractions of the streamed video frame (0..1). JSON on the wire uses
 * Swift's synthesized Codable shape (compact, declaration order).
 */
enum class PointerAction(val raw: String) {
    Move("move"),
    LeftDown("leftDown"),
    LeftUp("leftUp"),
    RightDown("rightDown"),
    RightUp("rightUp");

    companion object {
        fun fromRaw(raw: String): PointerAction? = entries.firstOrNull { it.raw == raw }
    }
}

enum class ScrollPhase(val raw: String) {
    Began("began"),
    Ended("ended"),
    MomentumBegan("momentumBegan"),
    MomentumEnded("momentumEnded");

    companion object {
        fun fromRaw(raw: String): ScrollPhase? = entries.firstOrNull { it.raw == raw }
    }
}

sealed class InputEvent {
    data class Pointer(val action: PointerAction, val x: Double, val y: Double) : InputEvent()
    data class Scroll(val x: Double, val y: Double, val dx: Double, val dy: Double) : InputEvent()
    data class Text(val value: String) : InputEvent()
    data class ScrollGesture(val phase: ScrollPhase, val x: Double, val y: Double) : InputEvent()
    data class Key(val hidUsage: Int, val down: Boolean, val modifiers: Long) : InputEvent()

    fun toJson(): String = when (this) {
        is Pointer -> """{"pointer":{"_0":"${action.raw}","x":$x,"y":$y}}"""
        is Scroll -> """{"scroll":{"dx":$dx,"dy":$dy,"x":$x,"y":$y}}"""
        is Text -> """{"text":{"_0":${jsonString(value)}}}"""
        is ScrollGesture -> """{"scrollGesture":{"_0":"${phase.raw}","x":$x,"y":$y}}"""
        is Key -> """{"key":{"down":$down,"hidUsage":$hidUsage,"modifiers":$modifiers}}"""
    }

    companion object {
        fun fromJson(json: String): InputEvent? {
            val s = json.trim()
            return when {
                s.startsWith("""{"pointer":""") -> parsePointer(s)
                s.startsWith("""{"scroll":""") && !s.contains("Gesture") -> parseScroll(s)
                s.startsWith("""{"text":""") -> parseText(s)
                s.startsWith("""{"scrollGesture":""") -> parseScrollGesture(s)
                s.startsWith("""{"key":""") -> parseKey(s)
                else -> null
            }
        }

        private fun parsePointer(s: String): Pointer? {
            val action = regexString(s, "_0") ?: return null
            val pa = PointerAction.fromRaw(action) ?: return null
            val x = regexDouble(s, "x") ?: return null
            val y = regexDouble(s, "y") ?: return null
            return Pointer(pa, x, y)
        }

        private fun parseScroll(s: String): Scroll? {
            val x = regexDouble(s, "x") ?: return null
            val y = regexDouble(s, "y") ?: return null
            val dx = regexDouble(s, "dx") ?: return null
            val dy = regexDouble(s, "dy") ?: return null
            return Scroll(x, y, dx, dy)
        }

        private fun parseText(s: String): Text? {
            // text:{"_0":"..."}
            val idx = s.indexOf("\"_0\":")
            if (idx < 0) return null
            val rest = s.substring(idx + 5).trimStart()
            if (!rest.startsWith("\"")) return null
            val value = parseJsonString(rest) ?: return null
            return Text(value)
        }

        private fun parseScrollGesture(s: String): ScrollGesture? {
            val phaseRaw = regexString(s, "_0") ?: return null
            val phase = ScrollPhase.fromRaw(phaseRaw) ?: return null
            val x = regexDouble(s, "x") ?: return null
            val y = regexDouble(s, "y") ?: return null
            return ScrollGesture(phase, x, y)
        }

        private fun parseKey(s: String): Key? {
            val down = regexBoolean(s, "down") ?: return null
            val hid = regexLong(s, "hidUsage")?.toInt() ?: return null
            val mods = regexLong(s, "modifiers") ?: return null
            return Key(hid, down, mods)
        }

        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            val m = re.find(s) ?: return null
            return unescapeJson(m.groupValues[1])
        }

        private fun regexDouble(s: String, key: String): Double? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
            return re.find(s)?.groupValues?.get(1)?.toDoubleOrNull()
        }

        private fun regexLong(s: String, key: String): Long? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toLongOrNull()
        }

        private fun regexBoolean(s: String, key: String): Boolean? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(true|false)")
            return re.find(s)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        }

        private fun parseJsonString(s: String): String? {
            // s starts with quote
            val sb = StringBuilder()
            var i = 1
            while (i < s.length) {
                val c = s[i]
                if (c == '"') return sb.toString()
                if (c == '\\') {
                    if (i + 1 >= s.length) return null
                    val n = s[i + 1]
                    when (n) {
                        '"' -> { sb.append('"'); i += 2; continue }
                        '\\' -> { sb.append('\\'); i += 2; continue }
                        '/' -> { sb.append('/'); i += 2; continue }
                        'n' -> { sb.append('\n'); i += 2; continue }
                        't' -> { sb.append('\t'); i += 2; continue }
                        'r' -> { sb.append('\r'); i += 2; continue }
                        'b' -> { sb.append('\b'); i += 2; continue }
                        'f' -> { sb.append('\u000C'); i += 2; continue }
                        'u' -> {
                            if (i + 5 >= s.length) return null
                            val hex = s.substring(i + 2, i + 6)
                            val cp = hex.toIntOrNull(16) ?: return null
                            sb.append(cp.toChar())
                            i += 6
                            continue
                        }
                        else -> return null
                    }
                }
                sb.append(c)
                i++
            }
            return null
        }

        private fun jsonString(value: String): String {
            val sb = StringBuilder("\"")
            for (c in value) {
                when (c) {
                    '"' -> sb.append("\\\"")
                    '\\' -> sb.append("\\\\")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    '\b' -> sb.append("\\b")
                    '\u000C' -> sb.append("\\f")
                    else -> {
                        if (c.code < 0x20) sb.append("\\u%04x".format(c.code))
                        else sb.append(c)
                    }
                }
            }
            sb.append("\"")
            return sb.toString()
        }

        private fun unescapeJson(value: String): String {
            // value is already unquoted content with escapes
            val sb = StringBuilder()
            var i = 0
            while (i < value.length) {
                val c = value[i]
                if (c == '\\') {
                    if (i + 1 >= value.length) return value
                    when (value[i + 1]) {
                        '"' -> { sb.append('"'); i += 2; continue }
                        '\\' -> { sb.append('\\'); i += 2; continue }
                        '/' -> { sb.append('/'); i += 2; continue }
                        'n' -> { sb.append('\n'); i += 2; continue }
                        't' -> { sb.append('\t'); i += 2; continue }
                        'r' -> { sb.append('\r'); i += 2; continue }
                        'b' -> { sb.append('\b'); i += 2; continue }
                        'f' -> { sb.append('\u000C'); i += 2; continue }
                        'u' -> {
                            if (i + 5 >= value.length) return value
                            val hex = value.substring(i + 2, i + 6)
                            val cp = hex.toIntOrNull(16)
                            if (cp == null) return value
                            sb.append(cp.toChar())
                            i += 6
                            continue
                        }
                        else -> { sb.append(c); i++; continue }
                    }
                }
                sb.append(c)
                i++
            }
            return sb.toString()
        }
    }
}
