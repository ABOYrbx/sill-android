package app.sill.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardTest {
    @Test
    fun clipboardRoundTrip() {
        val c = Clipboard(text = "hello", stop = true)
        assertEquals(c, Clipboard.fromJson(c.toJson()))
    }

    @Test
    fun capEnforced() {
        val longText = "a".repeat(Clipboard.maxTextBytes + 1)
        assertEquals(null, ClipboardPolicy.payload(longText))
    }

    @Test
    fun deviceWritesEcho() {
        val payload = Clipboard(text = "hi").toJson().toByteArray()
        val sent = ClipboardPolicy.Sent("hi", 5)
        assertEquals(null, ClipboardPolicy.deviceWrites(payload, true, 1, sent, 5))
        assertEquals("hi", ClipboardPolicy.deviceWrites(payload, true, 1, sent, 6))
    }
}