package app.sill.protocol

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

/**
 * Port of `Sources/StreamProtocol/Pairing.swift`.
 */
object PairingCode {
    private val table = arrayOf(
        intArrayOf(0, 3, 1, 7, 5, 9, 8, 6, 4, 2),
        intArrayOf(7, 0, 9, 2, 1, 5, 4, 8, 6, 3),
        intArrayOf(4, 2, 0, 6, 8, 7, 1, 3, 5, 9),
        intArrayOf(1, 7, 5, 0, 9, 8, 3, 4, 2, 6),
        intArrayOf(6, 1, 2, 3, 0, 4, 5, 9, 7, 8),
        intArrayOf(3, 6, 7, 4, 2, 0, 9, 5, 8, 1),
        intArrayOf(5, 8, 6, 9, 7, 2, 0, 1, 3, 4),
        intArrayOf(8, 9, 4, 5, 3, 6, 2, 0, 1, 7),
        intArrayOf(9, 4, 3, 8, 6, 1, 7, 2, 0, 5),
        intArrayOf(2, 5, 8, 1, 4, 3, 6, 7, 9, 0),
    )

    const val length = 12

    fun checkDigit(digits: List<Int>): Int {
        var interim = 0
        for (d in digits) interim = table[interim][d]
        return interim
    }

    fun generate(): String? {
        val digits = mutableListOf<Int>()
        val rng = SecureRandom()
        while (digits.size < length - 1) {
            val b = rng.nextInt(256)
            if (b < 250 && digits.size < length - 1) digits.add(b % 10)
        }
        return (digits + checkDigit(digits)).joinToString("")
    }

    enum class Problem { Length, Typo }

    fun check(input: String): Pair<String, Problem?> {
        val kept = input.filter { it != ' ' && it != '-' && it != '\u00A0' }
        if (kept.length != length || !kept.all { it.isDigit() }) return Pair("", Problem.Length)
        val digits = kept.map { it.digitToInt() }
        if (checkDigit(digits) != 0) return Pair("", Problem.Typo)
        return Pair(kept, null)
    }

    fun grouped(code: String): String =
        code.chunked(4).joinToString(" ")
}

object PairingProof {
    const val rounds: Long = 600_000
    private const val label = "sill-pair-v1"

    fun codeKey(code: String, macFingerprint: ByteArray): ByteArray? {
        if (code.length != PairingCode.length || !code.all { it.isDigit() }) return null
        val salt = (label + "").toByteArray() + macFingerprint // label bytes + macFingerprint
        // Actually salt = Data(label.utf8) + macFingerprint
        val saltBytes = label.toByteArray(Charsets.UTF_8) + macFingerprint
        val password = code.toByteArray(Charsets.UTF_8)
        return try {
            val spec = PBEKeySpec(String(password, Charsets.UTF_8).toCharArray(), saltBytes, rounds.toInt(), 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val key = factory.generateSecret(spec)
            key.encoded
        } catch (e: Exception) {
            null
        }
    }

    fun qrKey(secret: ByteArray): ByteArray = secret

    fun deviceMessage(deviceFingerprint: ByteArray, macFingerprint: ByteArray): ByteArray {
        val m = (label + " device").toByteArray(Charsets.UTF_8)
        return m + byteArrayOf(0) + deviceFingerprint + macFingerprint
    }

    fun macMessage(macFingerprint: ByteArray, deviceFingerprint: ByteArray): ByteArray {
        val m = (label + " mac").toByteArray(Charsets.UTF_8)
        return m + byteArrayOf(0) + macFingerprint + deviceFingerprint
    }

    fun deviceProof(key: ByteArray, deviceFingerprint: ByteArray, macFingerprint: ByteArray): ByteArray =
        hmacSha256(key, deviceMessage(deviceFingerprint, macFingerprint))

    fun macProof(key: ByteArray, macFingerprint: ByteArray, deviceFingerprint: ByteArray): ByteArray =
        hmacSha256(key, macMessage(macFingerprint, deviceFingerprint))

    fun isValidDeviceProof(proof: ByteArray, key: ByteArray, deviceFingerprint: ByteArray, macFingerprint: ByteArray): Boolean =
        constantTimeEquals(proof, deviceProof(key, deviceFingerprint, macFingerprint))

    fun isValidMacProof(proof: ByteArray, key: ByteArray, macFingerprint: ByteArray, deviceFingerprint: ByteArray): Boolean =
        constantTimeEquals(proof, macProof(key, macFingerprint, deviceFingerprint))

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}

object RecognitionTag {
    const val txtKey = "r"
    private const val label = "sill-tag-v1"

    fun make(recognitionKey: ByteArray, nonce: ByteArray? = null): String? {
        val p = if (nonce != null && nonce.size == 6) nonce.copyOf() else {
            val bytes = ByteArray(6)
            SecureRandom().nextBytes(bytes)
            bytes
        }
        val mac = hmacSha256(recognitionKey, label.toByteArray(Charsets.UTF_8) + p)
        val tag = p + mac.copyOf(6)
        return Base64URL.encode(tag)
    }

    fun matches(tag: String, recognitionKey: ByteArray): Boolean {
        val bytes = Base64URL.decode(tag) ?: return false
        if (bytes.size != 12) return false
        val nonce = bytes.copyOf(6)
        val expected = hmacSha256(recognitionKey, label.toByteArray(Charsets.UTF_8) + nonce)
        var diff: Byte = 0
        for (i in 0 until 6) diff = (diff.toInt() or (expected[i].toInt() xor bytes[6 + i].toInt())).toByte()
        return diff.toInt() == 0
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}

object HomeDoorTXT {
    const val key = "p"

    enum class Door { Plain, PairingRequired, Open }

    fun value(requirePairing: Boolean): String = if (requirePairing) "1" else "0"

    fun door(txt: Map<String, String>): Door {
        val value = txt[key] ?: txt[key.uppercase()]
        return if (value == null) Door.Plain else if (value == "0") Door.Open else Door.PairingRequired
    }
}

data class PairLink(
    var macID: String,
    var fingerprint: ByteArray,
    var secret: ByteArray,
    var name: String,
    var port: Int,
    var addresses: List<ParsedAddress>,
) {
    init {
        // macID set by caller? Swift sets macID = MacID.make(fingerprint). We'll enforce.
        if (macID.isEmpty()) macID = MacID.make(fingerprint)
        val clean = SafeText.label(name, nameLimit)
        if (clean.isEmpty()) name = "Mac" else name = clean
        if (addresses.size > maxAddresses) addresses = addresses.take(maxAddresses)
    }

    enum class Failure { NotALink, Version, Field, MacIDMismatch, Address }

    val url: String
        get() {
            val items = mutableListOf<Pair<String, String>>()
            items.add("v" to "1")
            items.add("m" to macID)
            items.add("k" to Base64URL.encode(fingerprint))
            items.add("s" to Base64URL.encode(secret))
            items.add("n" to name)
            items.add("p" to port.toString())
            for (a in addresses.take(maxAddresses)) items.add("a" to a.text)
            return "sill://pair?" + items.joinToString("&") { "${it.first}=${escape(it.second)}" }
        }

    private fun escape(s: String): String {
        val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:"
        val sb = StringBuilder()
        for (c in s) {
            if (allowed.contains(c)) sb.append(c)
            else {
                for (b in c.toString().toByteArray(Charsets.UTF_8)) {
                    sb.append("%%%02X".format(b.toInt() and 0xFF))
                }
            }
        }
        return sb.toString()
    }

    companion object {
        const val maxAddresses = 5
        const val nameLimit = 64

        fun parse(text: String): Pair<PairLink?, Failure?> {
            val trimmed = text.trim()
            // Minimal URL parsing
            if (!trimmed.lowercase().startsWith("sill://pair")) return Pair(null, Failure.NotALink)
            val queryStart = trimmed.indexOf('?')
            if (queryStart < 0) return Pair(null, Failure.NotALink)
            val query = trimmed.substring(queryStart + 1)
            val items = parseQuery(query)
            fun single(key: String): String? {
                val vals = items.filter { it.first == key }.map { it.second }
                if (vals.size != 1 || vals[0].isEmpty()) return null
                return vals[0]
            }
            val v = single("v") ?: return Pair(null, Failure.Version)
            if (v != "1") return Pair(null, Failure.Version)
            val m = single("m") ?: return Pair(null, Failure.Field)
            if (!MacID.isWellFormed(m)) return Pair(null, Failure.Field)
            val k = single("k") ?: return Pair(null, Failure.Field)
            val fp = Base64URL.decode(k) ?: return Pair(null, Failure.Field)
            if (fp.size != 32) return Pair(null, Failure.Field)
            val s = single("s") ?: return Pair(null, Failure.Field)
            val secret = Base64URL.decode(s) ?: return Pair(null, Failure.Field)
            if (secret.size != 16) return Pair(null, Failure.Field)
            val p = single("p") ?: return Pair(null, Failure.Field)
            if (p.length > 5 || !p.all { it.isDigit() }) return Pair(null, Failure.Field)
            val port = p.toIntOrNull() ?: return Pair(null, Failure.Field)
            if (port !in 1..65535) return Pair(null, Failure.Field)
            if (m != MacID.make(fp)) return Pair(null, Failure.MacIDMismatch)
            val addresses = mutableListOf<ParsedAddress>()
            for (item in items) {
                if (item.first == "a") {
                    val raw = item.second
                    val res = AddressParser.parse(raw)
                    when (res) {
                        is AddressResult.Success -> addresses.add(res.value)
                        is AddressResult.Failure -> return Pair(null, Failure.Address)
                    }
                }
            }
            val names = items.filter { it.first == "n" }
            val name = names.firstOrNull()?.second ?: ""
            val link = PairLink(macID = m, fingerprint = fp, secret = secret, name = name, port = port, addresses = addresses.take(maxAddresses))
            return Pair(link, null)
        }

        private fun parseQuery(q: String): List<Pair<String, String>> {
            val result = mutableListOf<Pair<String, String>>()
            for (part in q.split("&")) {
                if (part.isEmpty()) continue
                val idx = part.indexOf('=')
                if (idx < 0) {
                    result.add(part to "")
                } else {
                    val key = part.substring(0, idx)
                    val value = percentDecode(part.substring(idx + 1))
                    result.add(key to value)
                }
            }
            return result
        }

        private fun percentDecode(s: String): String {
            val sb = StringBuilder()
            var i = 0
            while (i < s.length) {
                if (s[i] == '%' && i + 2 < s.length) {
                    val hex = s.substring(i + 1, i + 3)
                    val b = hex.toIntOrNull(16)
                    if (b != null) {
                        sb.append(b.toChar())
                        i += 3
                        continue
                    }
                }
                sb.append(s[i])
                i++
            }
            return sb.toString()
        }
    }
}