package app.sill.protocol

/** Shared JSON helpers matching Swift's JSONEncoder output. */
fun jsonNumber(x: Double): String {
    if (x.isFinite() && x == kotlin.math.floor(x) && x >= Int.MIN_VALUE && x <= Int.MAX_VALUE) {
        val i = x.toInt()
        if (i.toDouble() == x) return i.toString()
    }
    return x.toString()
}

fun unescapeJson(v: String): String {
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

fun jsonString(v: String): String {
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