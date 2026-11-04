package app.sill.network.framer

import app.sill.protocol.StreamMessage
import app.sill.protocol.StreamMessageKind
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MessageFramerTest {
    private fun readResourceBytes(name: String): ByteArray =
        javaClass.classLoader!!.getResourceAsStream(name)!!.readBytes()

    @Test
    fun readsGoldenFrame() {
        val bytes = readResourceBytes("frame.bin")
        val framer = MessageFramer(ByteArrayInputStream(bytes))
        val msg = framer.read()
        assertEquals(StreamMessageKind.Frame, msg.kind)
        assertEquals(bytes.size - StreamMessage.HEADER_LENGTH, msg.payload.size)
        val out = ByteArrayOutputStream()
        MessageFramer.write(out, msg)
        assertArrayEquals(bytes, out.toByteArray())
    }

    @Test
    fun roundTripConstructed() {
        val payload = byteArrayOf(10, 20, 30)
        val msg = StreamMessage(StreamMessageKind.Ping, 42.5, false, payload)
        val serialized = msg.serialized()
        val framer = MessageFramer(ByteArrayInputStream(serialized))
        val read = framer.readMessage()
        assertEquals(StreamMessageKind.Ping, read.kind)
        assertEquals(42.5, read.timestamp, 0.0)
        assertEquals(false, read.isKeyframe)
        assertArrayEquals(payload, read.payload)
        val out = ByteArrayOutputStream()
        MessageFramer(out).write(read)
        assertArrayEquals(serialized, out.toByteArray())
    }

    @Test
    fun writeThenReadGolden() {
        val bytes = readResourceBytes("clipboard.bin")
        val msg = MessageFramer.read(ByteArrayInputStream(bytes))
        val baos = ByteArrayOutputStream()
        MessageFramer(baos).write(msg)
        assertArrayEquals(bytes, baos.toByteArray())
    }
}
