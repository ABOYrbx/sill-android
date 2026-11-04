package app.sill.protocol

/**
 * Text a device supplies about itself.
 *
 * Port of `Sources/StreamProtocol/SafeText.swift`.
 */
object SafeText {
    const val labelLimit = 64

    fun label(text: String, limit: Int = labelLimit): String {
        val sb = StringBuilder()
        var pendingSpace = false
        for (cp in text.codePoints()) {
            val isWs = Character.isWhitespace(cp) // approximate
            // We need unicode property checks; Java's isWhitespace covers many but not all.
            // For exactness we treat control/format categories as removed.
            // Simplified: use Character.isWhitespace for whitespace.
            if (isWs) {
                if (sb.isNotEmpty()) pendingSpace = true
                continue
            }
            // Remove control/format/surrogate/private/unassigned
            val type = Character.getType(cp)
            if (type == Character.CONTROL.toInt() || type == Character.FORMAT.toInt() || type == Character.SURROGATE.toInt() ||
                type == Character.PRIVATE_USE.toInt() || type == Character.UNASSIGNED.toInt()) {
                continue
            }
            if (pendingSpace) {
                sb.append(' ')
                pendingSpace = false
            }
            sb.appendCodePoint(cp)
        }
        var out = sb.toString()
        if (out.length > limit) {
            out = out.substring(0, limit)
            while (out.endsWith(" ")) out = out.dropLast(1)
        }
        return out
    }

    private fun String.codePoints(): IntArray {
        val arr = IntArray(this.length) // upper bound
        var n = 0
        var i = 0
        while (i < this.length) {
            val cp = this.codePointAt(i)
            arr[n++] = cp
            i += Character.charCount(cp)
        }
        return arr.copyOf(n)
    }
}