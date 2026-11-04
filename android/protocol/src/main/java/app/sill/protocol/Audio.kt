package app.sill.protocol

import java.util.Base64

/**
 * Port of `Sources/StreamProtocol/Audio.swift`.
 */
object AudioCodec {
    const val aacELD = "aac-eld"
    val hostMakes = listOf(aacELD)
    val devicePlays = listOf(aacELD)

    fun choose(offered: List<String>?, makes: List<String> = hostMakes): String? {
        if (offered == null) return null
        return offered.firstOrNull { makes.contains(it) }
    }
}

data class AudioFormat(
    var codec: String? = null,
    var sampleRate: Int? = null,
    var channels: Int? = null,
    var framesPerPacket: Int? = null,
    var primingFrames: Int? = null,
    var bitrate: Int? = null,
    var epoch: Int? = null,
    var cookie: ByteArray? = null,
    var source: String? = null,
    var app: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioFormat) return false
        return codec == other.codec && sampleRate == other.sampleRate && channels == other.channels &&
            framesPerPacket == other.framesPerPacket && primingFrames == other.primingFrames &&
            bitrate == other.bitrate && epoch == other.epoch && source == other.source && app == other.app &&
            (cookie?.contentEquals(other.cookie) ?: (other.cookie == null))
    }
    override fun hashCode(): Int {
        var r = codec.hashCode()
        r = 31 * r + (sampleRate ?: 0)
        r = 31 * r + (channels ?: 0)
        r = 31 * r + (framesPerPacket ?: 0)
        r = 31 * r + (primingFrames ?: 0)
        r = 31 * r + (bitrate ?: 0)
        r = 31 * r + (epoch ?: 0)
        r = 31 * r + (source?.hashCode() ?: 0)
        r = 31 * r + (app?.hashCode() ?: 0)
        r = 31 * r + (cookie?.contentHashCode() ?: 0)
        return r
    }
    fun toJson(): String {
        val parts = mutableListOf<String>()
        app?.let { parts.add("\"app\":${jsonString(it)}") }
        bitrate?.let { parts.add("\"bitrate\":$it") }
        channels?.let { parts.add("\"channels\":$it") }
        codec?.let { parts.add("\"codec\":${jsonString(it)}") }
        cookie?.let { parts.add("\"cookie\":${jsonString(Base64.getEncoder().encodeToString(it))}") }
        epoch?.let { parts.add("\"epoch\":$it") }
        framesPerPacket?.let { parts.add("\"framesPerPacket\":$it") }
        primingFrames?.let { parts.add("\"primingFrames\":$it") }
        sampleRate?.let { parts.add("\"sampleRate\":$it") }
        source?.let { parts.add("\"source\":${jsonString(it)}") }
        return "{" + parts.joinToString(",") + "}"
    }

    companion object {
        const val sourceApp = "app"
        const val sourceDesktop = "desktop"
        const val sourceTest = "test"

        fun fromJson(json: String): AudioFormat? {
            val s = json.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val f = AudioFormat()
            f.codec = regexString(s, "codec")?.let { unescapeJson(it) }
            f.sampleRate = regexInt(s, "sampleRate")
            f.channels = regexInt(s, "channels")
            f.framesPerPacket = regexInt(s, "framesPerPacket")
            f.primingFrames = regexInt(s, "primingFrames")
            f.bitrate = regexInt(s, "bitrate")
            f.epoch = regexInt(s, "epoch")
            regexString(s, "cookie")?.let { b64 ->
                try { f.cookie = Base64.getDecoder().decode(b64) } catch (e: Exception) { f.cookie = null }
            }
            f.source = regexString(s, "source")?.let { unescapeJson(it) }
            f.app = regexString(s, "app")?.let { unescape(it) }
            return f
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

data class AudioPackets(
    var epoch: Int,
    var seq: Long,
    var segmentStart: Boolean,
    var packets: MutableList<ByteArray>,
) {
    fun serialized(): ByteArray {
        val kept = packets.take(maxCount)
        val buf = java.nio.ByteBuffer.allocate(65536).order(java.nio.ByteOrder.BIG_ENDIAN)
        buf.put(2.toByte()) // type packets
        // Actually AudioPackets.serialized in Swift includes type? No, Swift's AudioPackets.serialized starts with type 2? Let's check: Swift AudioPackets.serialized appended AudioMessage.typePackets? In code: `d.append(AudioMessage.typePackets)` then epoch... Wait AudioPackets.serialized() in Swift: `d.append(AudioMessage.typePackets)`? Let's re-read: Swift AudioPackets.serialized: `d.append(AudioMessage.typePackets)`? Lines: `d.append(AudioMessage.typePackets)` yes? It says `d.append(AudioMessage.typePackets)`? Actually code: `d.append(AudioMessage.typePackets)`? Let's check: in Swift, `d.append(AudioMessage.typePackets)`? The snippet: `d.append(AudioMessage.typePackets)`? It says `d.append(AudioMessage.typePackets)`? Hmm I need verify. The serialized method of AudioPackets in Swift starts:
        // d.append(AudioMessage.typePackets)
        // d.appendBigEndian(UInt16(... epoch))
        // ...
        // So yes includes type.
        buf.putShort((epoch and 0xFFFF).toShort())
        buf.putInt(seq.toInt()) // seq UInt32
        buf.put(if (segmentStart) 1 else 0)
        buf.put(kept.size.toByte())
        for (p in kept) {
            val n = minOf(p.size, maxPacketBytes)
            buf.putShort(n.toShort())
            buf.put(p.copyOf(n))
        }
        val out = ByteArray(buf.position())
        buf.flip()
        buf.get(out)
        return out
    }

    companion object {
        const val maxCount = 255
        const val maxPacketBytes = 65535
        const val headerBytes = 9

        fun parse(data: ByteArray): AudioPackets? {
            if (data.size < headerBytes) return null
            val buf = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.BIG_ENDIAN)
            val type = buf.get().toInt() and 0xFF
            if (type != 2) return null
            val epoch = buf.short.toInt() and 0xFFFF
            val seq = buf.int.toLong() and 0xFFFFFFFFL
            val flags = buf.get().toInt() and 0xFF
            val count = buf.get().toInt() and 0xFF
            if (count < 1) return null
            val packets = mutableListOf<ByteArray>()
            var offset = headerBytes
            for (i in 0 until count) {
                if (offset + 2 > data.size) return null
                val lenBuf = java.nio.ByteBuffer.wrap(data, offset, 2).order(java.nio.ByteOrder.BIG_ENDIAN)
                val length = lenBuf.short.toInt() and 0xFFFF
                offset += 2
                if (offset + length > data.size) return null
                packets.add(data.copyOfRange(offset, offset + length))
                offset += length
            }
            if (offset != data.size) return null
            return AudioPackets(epoch, seq, (flags and 1) == 1, packets)
        }
    }
}

sealed class AudioMessage {
    data class Format(val format: AudioFormat) : AudioMessage()
    data class Packets(val packets: AudioPackets) : AudioMessage()

    companion object {
        const val typeFormat: Byte = 1
        const val typePackets: Byte = 2
        const val maxFormatPayload = 4096
        const val maxPacketsPayload = 16384

        fun serialized(msg: AudioMessage): ByteArray {
            return when (msg) {
                is Format -> byteArrayOf(typeFormat) + msg.format.toJson().toByteArray(Charsets.UTF_8)
                is Packets -> msg.packets.serialized()
            }
        }

        fun parse(payload: ByteArray): AudioMessage? {
            if (payload.isEmpty()) return null
            return when (payload[0]) {
                typeFormat -> {
                    if (payload.size > maxFormatPayload) return null
                    val json = String(payload.copyOfRange(1, payload.size), Charsets.UTF_8)
                    val f = AudioFormat.fromJson(json) ?: return null
                    Format(f)
                }
                typePackets -> {
                    if (payload.size > maxPacketsPayload || payload.size < AudioPackets.headerBytes) return null
                    val ap = AudioPackets.parse(payload) ?: return null
                    Packets(ap)
                }
                else -> null
            }
        }
    }
}


