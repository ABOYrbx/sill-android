package app.sill.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewportTextScaleTest {
    @Test
    fun viewportRoundTrip() {
        val v = Viewport(100.0, 200.5, 1.0, true, 60)
        assertEquals(v, Viewport.fromJson(v.toJson()))
    }

    @Test
    fun clientStatsRoundTrip() {
        val c = ClientStats(60, 10, 5, "iPad", 12, 8, 100, 2)
        assertEquals(c, ClientStats.fromJson(c.toJson()))
    }

    @Test
    fun viewportBoundsHeld() {
        val v = Viewport(0.5, 10.0, 5.0, null, null)
        val h = ViewportBounds.held(v)
        assertEquals(1.0, h!!.width, 0.0)
        assertEquals(4.0, h.scale!!, 0.0)
    }

    @Test
    fun textScaleSnap() {
        assertEquals(1.0, TextScale.snap(0.9, true), 0.0)
        assertEquals(TextScale.off, TextScale.snap(0.1, true), 0.0)
    }

    @Test
    fun streamSourceRoundTrip() {
        val s = StreamSource.Window(42u)
        assertEquals(s, StreamSource.fromJson(s.toJson()))
        assertEquals(StreamSource.None, StreamSource.fromJson(StreamSource.None.toJson()))
        assertEquals(StreamSource.Desktop, StreamSource.fromJson(StreamSource.Desktop.toJson()))
    }
}