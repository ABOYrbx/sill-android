package app.sill.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTest {
    @Test
    fun audioFormatRoundTrip() {
        val f = AudioFormat(codec = "aac-eld", sampleRate = 48000, channels = 2, epoch = 5, cookie = byteArrayOf(1,2,3))
        assertEquals(f, AudioFormat.fromJson(f.toJson()))
    }

    @Test
    fun audioPacketsRoundTrip() {
        val p = AudioPackets(7, 3, true, mutableListOf(byteArrayOf(1,2), byteArrayOf(3)))
        val parsed = AudioPackets.parse(p.serialized())
        assertEquals(p.epoch, parsed!!.epoch)
        assertEquals(p.seq, parsed.seq)
        assertEquals(p.segmentStart, parsed.segmentStart)
        assertEquals(p.packets.size, parsed.packets.size)
        assertArrayEquals(p.packets[0], parsed.packets[0])
    }

    @Test
    fun audioMessageRoundTrip() {
        val f = AudioFormat(codec = "aac-eld")
        val msg = AudioMessage.Format(f)
        val parsed = AudioMessage.parse(AudioMessage.serialized(msg))
        assertTrue(parsed is AudioMessage.Format)
    }
}