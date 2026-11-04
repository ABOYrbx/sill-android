package app.sill.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class SwitcherPointerTest {
    @Test
    fun windowListRoundTrip() {
        val wl = WindowList(macName = "Mac", windows = listOf(WindowInfo(1u, "Title", "App", "com.app", 100, 200)), active = StreamSource.Window(1u), launchID = "abc", protocol = 1)
        assertEquals(wl, WindowList.fromJson(wl.toJson()))
    }

    @Test
    fun macPointerRoundTrip() {
        val mp = MacPointer(x = 0.5, y = 0.25, inside = true, seen = 3)
        assertEquals(mp, MacPointer.fromJson(mp.toJson()))
    }
}