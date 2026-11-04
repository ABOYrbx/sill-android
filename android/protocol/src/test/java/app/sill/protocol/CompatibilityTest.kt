package app.sill.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityTest {
    @Test
    fun sillVersionParseAndCompare() {
        val a = SillVersion.parse("v0.4.0")!!
        val b = SillVersion.parse("0.4")!!
        assertEquals(a, b)
        assertTrue(SillVersion.parse("0.10")!! > SillVersion.parse("0.9")!!)
        assertEquals("0.4", SillVersion.parse("0.4.0")!!.toString())
        assertEquals("1.0", SillVersion.parse("1")!!.toString())
    }

    @Test
    fun helloRoundTrip() {
        val h = Hello(appVersion = "1.0", build = "42", protocol = 1, device = "iPad", audio = listOf("aac-eld"))
        assertEquals(h, Hello.fromJson(h.toJson()))
    }
}