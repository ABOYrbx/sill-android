package app.sill.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Wire format shared by the Mac host and the (iOS/Android) client.
 *
 * Port of `Sources/StreamProtocol/StreamMessage.swift`.
 *
 * Header (14 bytes, big endian): kind(1) timestamp(8, Double) isKeyframe(1) payloadLength(4).
 */
enum class StreamMessageKind(val raw: UByte) {
    ParameterSets(0u),
    Frame(1u),
    WindowList(2u),
    Thumbnail(3u),
    AppIcon(4u),
    AppList(5u),
    SelectSource(6u),
    LaunchApp(7u),
    Input(8u),
    Viewport(9u),
    Ping(10u),
    Pong(11u),
    ClientStats(12u),
    Tick(13u),
    CursorShape(14u),
    WindowCommand(15u),
    HostSettings(16u),
    ChangeSettings(17u),
    MacInfo(18u),
    PairRequest(19u),
    PairResult(20u),
    PairingWanted(21u),
    Goodbye(22u),
    Hello(23u),
    MacMenu(24u),
    PressMenuItem(25u),
    MacPointer(26u),
    FetchMenu(27u),
    Gesture(28u),
    Clipboard(32u),
    Audio(43u),
    KeyframeRequest(44u),
    Unknown(255u);

    companion object {
        fun fromRaw(raw: Int): StreamMessageKind =
            entries.firstOrNull { it.raw.toInt() == (raw and 0xFF) } ?: Unknown
    }
}

data class StreamHeader(
    val kind: StreamMessageKind,
    val timestamp: Double,
    val isKeyframe: Boolean,
    val payloadLength: Int,
)

data class StreamMessage(
    var kind: StreamMessageKind,
    var timestamp: Double,
    var isKeyframe: Boolean,
    var payload: ByteArray,
) {
    fun serialized(): ByteArray {
        val buf = ByteBuffer.allocate(HEADER_LENGTH + payload.size).order(ByteOrder.BIG_ENDIAN)
        buf.put(kind.raw.toByte())
        buf.putDouble(timestamp)
        buf.put(if (isKeyframe) 1 else 0)
        buf.putInt(payload.size)
        buf.put(payload)
        return buf.array()
    }

    companion object {
        const val HEADER_LENGTH = 14

        const val MAX_CLIENT_PAYLOAD = 1 shl 20
        const val MAX_PAIRING_PAYLOAD = 4096
        const val MAX_FRAME_PAYLOAD = 32 shl 20
        const val MAX_OTHER_HOST_PAYLOAD = 4 shl 20

        fun parseHeader(data: ByteArray): StreamHeader? {
            if (data.size < HEADER_LENGTH) return null
            val buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            val kind = StreamMessageKind.fromRaw(buf.get().toInt())
            val ts = buf.double
            val key = buf.get() == 1.toByte()
            val len = buf.int
            return StreamHeader(kind, ts, key, len)
        }
    }
}

data class ParameterSets(
    var nalUnitHeaderLength: Int,
    var sets: MutableList<ByteArray>,
) {
    fun encoded(): ByteArray {
        val total = 2 + sets.sumOf { 2 + it.size }
        val buf = ByteBuffer.allocate(total).order(ByteOrder.BIG_ENDIAN)
        buf.put(nalUnitHeaderLength.toByte())
        buf.put(sets.size.toByte())
        for (s in sets) {
            buf.putShort(s.size.toShort())
            buf.put(s)
        }
        return buf.array()
    }

    companion object {
        fun decode(data: ByteArray): ParameterSets? {
            if (data.size < 2) return null
            val buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            val nal = buf.get().toInt() and 0xFF
            val count = buf.get().toInt() and 0xFF
            val sets = mutableListOf<ByteArray>()
            for (i in 0 until count) {
                if (buf.remaining() < 2) return null
                val len = buf.short.toInt() and 0xFFFF
                if (buf.remaining() < len) return null
                val arr = ByteArray(len)
                buf.get(arr)
                sets.add(arr)
            }
            return ParameterSets(nal, sets)
        }
    }
}