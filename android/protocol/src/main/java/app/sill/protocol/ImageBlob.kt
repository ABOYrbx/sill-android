package app.sill.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Binary image payloads (kinds 3, 4): a key, then the image bytes.
 *
 * Port of `Sources/StreamProtocol/Switcher.swift` (`ImageBlob`).
 */
object ImageBlob {
    fun encodeThumbnail(windowID: UInt, jpeg: ByteArray): ByteArray {
        val buf = ByteBuffer.allocate(4 + jpeg.size).order(ByteOrder.BIG_ENDIAN)
        buf.putInt(windowID.toInt())
        buf.put(jpeg)
        return buf.array()
    }

    fun decodeThumbnail(data: ByteArray): Pair<UInt, ByteArray>? {
        if (data.size <= 4) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
        val wid = buf.int.toUInt() and 0xFFFFFFFFu
        val jpeg = ByteArray(data.size - 4)
        buf.get(jpeg)
        return Pair(wid, jpeg)
    }

    fun encodeIcon(bundleID: String, png: ByteArray): ByteArray {
        val id = bundleID.toByteArray(Charsets.UTF_8)
        val buf = ByteBuffer.allocate(2 + id.size + png.size).order(ByteOrder.BIG_ENDIAN)
        buf.putShort(id.size.toShort())
        buf.put(id)
        buf.put(png)
        return buf.array()
    }

    fun decodeIcon(data: ByteArray): Pair<String, ByteArray>? {
        if (data.size <= 2) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
        val len = buf.short.toInt() and 0xFFFF
        if (data.size < 2 + len) return null
        val idBytes = ByteArray(len)
        buf.get(idBytes)
        val id = String(idBytes, Charsets.UTF_8)
        val png = ByteArray(data.size - 2 - len)
        buf.get(png)
        return Pair(id, png)
    }
}
