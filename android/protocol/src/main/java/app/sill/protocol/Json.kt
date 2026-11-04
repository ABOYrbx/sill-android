package app.sill.protocol

/** Shared JSON helpers matching Swift's JSONEncoder output. */
fun jsonNumber(x: Double): String {
    if (x.isFinite() && x == kotlin.math.floor(x) && x >= Int.MIN_VALUE && x <= Int.MAX_VALUE) {
        val i = x.toInt()
        if (i.toDouble() == x) return i.toString()
    }
    return x.toString()
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