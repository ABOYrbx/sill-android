package app.sill.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamMessageTest {
    @Test
    fun roundTripHeader() {
        val payload = byteArrayOf(1, 2, 3, 4)
        val msg = StreamMessage(StreamMessageKind.Frame, 123.25, true, payload)
        val bytes = msg.serialized()
        val header = StreamMessage.parseHeader(bytes)!!
        assertEquals(StreamMessageKind.Frame, header.kind)
        assertEquals(123.25, header.timestamp, 0.0)
        assertEquals(true, header.isKeyframe)
        assertEquals(payload.size, header.payloadLength)
        assertArrayEquals(payload, bytes.copyOfRange(StreamMessage.HEADER_LENGTH, bytes.size))
    }

    @Test
    fun unknownKindMapsToUnknown() {
        val data = ByteArray(14)
        data[0] = 42
        assertEquals(StreamMessageKind.Unknown, StreamMessage.parseHeader(data)!!.kind)
    }

    @Test
    fun parameterSetsRoundTrip() {
        val sets = mutableListOf(byteArrayOf(0x01, 0x02), byteArrayOf(0x03, 0x04, 0x05))
        val ps = ParameterSets(4, sets)
        val decoded = ParameterSets.decode(ps.encoded())!!
        assertEquals(4, decoded.nalUnitHeaderLength)
        assertEquals(2, decoded.sets.size)
        assertArrayEquals(sets[0], decoded.sets[0])
        assertArrayEquals(sets[1], decoded.sets[1])
    }
}