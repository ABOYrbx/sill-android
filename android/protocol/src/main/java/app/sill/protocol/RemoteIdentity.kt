package app.sill.protocol

import java.util.Base64

/**
 * Port of parts of `Sources/StreamProtocol/RemoteIdentity.swift` needed by the protocol:
 * Base64URL and MacID. (SPKI/self-signed cert helpers are platform-specific.)
 */
object Base64URL {
    fun encode(data: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(data)

    fun decode(text: String): ByteArray? {
        if (text.isEmpty()) return ByteArray(0)
        if (!text.all { it.isAsciiLetterOrDigit() || it == '-' || it == '_' }) return null
        if (text.length % 4 == 1) return null
        return try {
            val padded = text + "=".repeat((4 - text.length % 4) % 4)
            val std = padded.replace('-', '+').replace('_', '/')
            val bytes = Base64.getDecoder().decode(std)
            if (encode(bytes) != text) null else bytes
        } catch (e: Exception) {
            null
        }
    }

    private fun Char.isAsciiLetterOrDigit(): Boolean =
        (this in 'a'..'z') || (this in 'A'..'Z') || (this in '0'..'9')
}

object MacID {
    const val alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    fun make(fingerprint: ByteArray): String {
        val bytes = fingerprint.copyOf(10)
        if (bytes.size < 10) return ""
        val out = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                bits -= 5
                out.append(alphabet[(buffer shr bits) and 31])
            }
            buffer = buffer and ((1 shl bits) - 1)
        }
        return out.toString()
    }

    fun isWellFormed(text: String): Boolean =
        text.length == 16 && text.all { alphabet.contains(it) }
}