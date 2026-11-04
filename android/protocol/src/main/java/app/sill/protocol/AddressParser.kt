package app.sill.protocol

import java.net.InetAddress
import java.net.Inet6Address

data class ParsedAddress(
    var host: String,
    var port: Int?,
    var kind: Kind,
) {
    enum class Kind(val raw: String) { IPv4("ipv4"), IPv6("ipv6"), Name("name") }

    val text: String
        get() {
            val h = if (kind == Kind.IPv6) "[$host]" else host
            return if (port != null) "$h:$port" else h
        }
}

sealed class AddressResult {
    data class Success(val value: ParsedAddress) : AddressResult()
    data class Failure(val error: AddressParser.Failure) : AddressResult()
}

object AddressParser {
    enum class Failure { Empty, Malformed, Zone, IPv4, IPv6, Name, Port, PairingLink }

    fun parse(raw: String): AddressResult {
        val s = raw.trim()
        if (s.isEmpty()) return AddressResult.Failure(Failure.Empty)
        if (s.lowercase().startsWith("sill:")) return AddressResult.Failure(Failure.PairingLink)
        if (s.any { it.isWhitespace() || it.code < 0x20 || it.code == 0x7F }) return AddressResult.Failure(Failure.Malformed)
        if (s.contains("%")) return AddressResult.Failure(Failure.Zone)

        // bracketed IPv6
        if (s.startsWith("[")) {
            val close = s.indexOf(']')
            if (close < 0) return AddressResult.Failure(Failure.IPv6)
            val hostPart = s.substring(1, close)
            val rest = s.substring(close + 1)
            var portPart: String? = null
            if (rest.isNotEmpty()) {
                if (!rest.startsWith(":")) return AddressResult.Failure(Failure.Malformed)
                portPart = rest.substring(1)
            }
            val v6 = ipv6(hostPart) ?: return AddressResult.Failure(Failure.IPv6)
            val addr = if (portPart != null) {
                val p = parsePort(portPart) ?: return AddressResult.Failure(Failure.Port)
                ParsedAddress(v6.host, p, v6.kind)
            } else {
                v6
            }
            return AddressResult.Success(addr)
        }

        val colons = s.count { it == ':' }
        if (colons >= 2) {
            val v6 = ipv6(s) ?: return AddressResult.Failure(Failure.IPv6)
            return AddressResult.Success(v6)
        }

        val hostPart: String
        var portPart: String? = null
        if (colons == 1) {
            val idx = s.indexOf(':')
            hostPart = s.substring(0, idx)
            portPart = s.substring(idx + 1)
        } else {
            hostPart = s
        }

        val hostRes = hostOnly(hostPart)
        val host = when (hostRes) {
            is AddressResult.Success -> hostRes.value
            is AddressResult.Failure -> return hostRes
        }
        val result = if (portPart != null) {
            val p = parsePort(portPart) ?: return AddressResult.Failure(Failure.Port)
            ParsedAddress(host.host, p, host.kind)
        } else {
            host
        }
        return AddressResult.Success(result)
    }

    private fun hostOnly(h: String): AddressResult {
        if (h.isEmpty()) return AddressResult.Failure(Failure.Malformed)
        val labels = if (h.endsWith(".")) h.dropLast(1).split(".") else h.split(".")
        val allNumeric = labels.isNotEmpty() && labels.all { it.isNotEmpty() && it.all { ch -> ch.isDigit() } }
        if (allNumeric) {
            val ok = !h.endsWith(".") && labels.size == 4 &&
                labels.all { (it == "0" || it[0] != '0') && it.length <= 3 && (it.toIntOrNull() ?: 1000) <= 255 }
            if (!ok) return AddressResult.Failure(Failure.IPv4)
            return AddressResult.Success(ParsedAddress(h, null, ParsedAddress.Kind.IPv4))
        }
        if (acceptedByInetAton(h)) return AddressResult.Failure(Failure.IPv4)
        if (!isName(h)) return AddressResult.Failure(Failure.Name)
        return AddressResult.Success(ParsedAddress(h, null, ParsedAddress.Kind.Name))
    }

    private fun isName(h: String): Boolean {
        val body = if (h.endsWith(".")) h.dropLast(1) else h
        if (body.isEmpty() || body.length > 253) return false
        for (label in body.split(".")) {
            if (label.isEmpty()) return false
            if (label.length !in 1..63) return false
            if (label.startsWith("-") || label.endsWith("-")) return false
            if (!label.all { it.isAsciiLetterOrDigit() || it == '-' }) return false
        }
        return true
    }

    private fun ipv6(h: String): ParsedAddress? {
        if (h.isEmpty() || h.contains("%")) return null
        return try {
            val addr = InetAddress.getByName(h)
            if (addr !is Inet6Address) return null
            val bytes = addr.address
            if (bytes.size == 16 && bytes.copyOfRange(0, 10).all { it == 0.toByte() } &&
                bytes[10] == 0xFF.toByte() && bytes[11] == 0xFF.toByte()) {
                val ipv4 = "${bytes[12].toUByte()}.${bytes[13].toUByte()}.${bytes[14].toUByte()}.${bytes[15].toUByte()}"
                return ParsedAddress(ipv4, null, ParsedAddress.Kind.IPv4)
            }
            ParsedAddress(addr.hostAddress, null, ParsedAddress.Kind.IPv6)
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePort(p: String): Int? {
        if (p.isEmpty() || p.length > 5 || !p.all { it.isDigit() }) return null
        val v = p.toIntOrNull() ?: return null
        if (v !in 1..65535) return null
        return v
    }

    private fun acceptedByInetAton(h: String): Boolean {
        return h.matches(Regex("^[0-9.]+$")) || h.matches(Regex("^0[xX][0-9a-fA-F.]+$"))
    }
}

private fun Char.isAsciiLetterOrDigit(): Boolean =
    (this in 'a'..'z') || (this in 'A'..'Z') || (this in '0'..'9')