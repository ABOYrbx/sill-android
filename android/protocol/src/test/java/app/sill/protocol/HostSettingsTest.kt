package app.sill.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class HostSettingsTest {
    @Test
    fun streamSettingsRoundTrip() {
        val s = StreamSettings(60, 15000000, 2.0, false, true, true, true)
        assertEquals(s, StreamSettings.fromJson(s.toJson()))
    }

    @Test
    fun hostSettingsStateRoundTrip() {
        val st = StreamSettings(60, 15000000, 2.0, false, true, true, true)
        val h = HostSettingsState(settings = st, persistent = true, virtualDisplayAvailable = true, softwareEncoder = false, stream = RunningStream(100, 200, 60, 15, true), sessionPaired = true)
        assertEquals(h, HostSettingsState.fromJson(h.toJson()))
    }

    @Test
    fun hostSettingsChangeRoundTrip() {
        val c = HostSettingsChange(token = 5, maxFPS = 60, bitrate = 15000000, virtualDisplay = true)
        assertEquals(c, HostSettingsChange.fromJson(c.toJson()))
        assertEquals(false, c.isEmpty)
    }

    @Test
    fun qualityPreset() {
        assertEquals("Balanced — 15 Mbps", QualityPreset.Balanced.title)
        assertEquals("Custom — 12 Mbps", QualityPreset.title(forBitrate = 12000000))
    }
}