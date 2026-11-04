package app.sill.network.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryPolicyTest {

    @Test
    fun peerToPeer() {
        assertTrue(DiscoveryPolicy.isPeerToPeer("awdl0"))
        assertTrue(DiscoveryPolicy.isPeerToPeer("llw0"))
        assertFalse(DiscoveryPolicy.isPeerToPeer("en0"))
    }

    @Test
    fun rowsBasic() {
        val network = listOf("Mac mini", "Studio")
        val nearby = listOf(
            DiscoveryPolicy.Nearby("Mac mini", listOf("awdl0")),
            DiscoveryPolicy.Nearby("Air", listOf("awdl0")),
            DiscoveryPolicy.Nearby("LanOnly", listOf("en0")),
        )
        val result = DiscoveryPolicy.rows(network, nearby)
        assertEquals(3, result.size)
        assertEquals("Mac mini", result[0].name)
        assertFalse(result[0].direct)
        assertEquals("Studio", result[1].name)
        assertFalse(result[1].direct)
        assertEquals("Air", result[2].name)
        assertTrue(result[2].direct)
    }

    @Test
    fun rowsEmptyInterfacesExcluded() {
        val nearby = listOf(DiscoveryPolicy.Nearby("X", emptyList()))
        val result = DiscoveryPolicy.rows(emptyList(), nearby)
        assertTrue(result.isEmpty())
    }

    @Test
    fun sightingsBasic() {
        val prev = DiscoveryPolicy.NetworkSightings()
        prev.since["A"] = 1.0
        val now = 5.0
        val next = DiscoveryPolicy.sightings(prev, setOf("B"), now)
        assertEquals(5.0, next.since["B"]!!, 0.0)
        assertEquals(5.0, next.leftAt["A"]!!, 0.0)
        assertFalse(next.leftAt.containsKey("B"))
    }

    @Test
    fun sightingsKeepsLeftWithinGrace() {
        val prev = DiscoveryPolicy.NetworkSightings()
        prev.leftAt["A"] = 2.0
        val now = 5.0
        val next = DiscoveryPolicy.sightings(prev, emptySet(), now)
        assertEquals(2.0, next.leftAt["A"]!!, 0.0)
    }

    @Test
    fun directSinceBasic() {
        val prev = mapOf("A" to 1.0)
        val rows = listOf(DiscoveryPolicy.Row("A", true), DiscoveryPolicy.Row("B", false), DiscoveryPolicy.Row("C", true))
        val next = DiscoveryPolicy.directSince(prev, rows, 9.0)
        assertEquals(1.0, next["A"]!!, 0.0)
        assertEquals(9.0, next["C"]!!, 0.0)
        assertFalse(next.containsKey("B"))
    }

    @Test
    fun reconnectRowNetworkFirst() {
        val r = DiscoveryPolicy.reconnectRow(network = "net", networkSince = 1.0, direct = "dir", directSince = 2.0, networkLeftAt = null, now = 5.0)
        assertEquals("net", r.take)
    }

    @Test
    fun reconnectRowDirectWait() {
        val r = DiscoveryPolicy.reconnectRow(network = null, direct = "dir", directSince = 0.0, networkLeftAt = null, now = 3.0)
        assertEquals(null, r.take)
        assertEquals(6.0, r.recheckAt!!, 0.0)
    }

    @Test
    fun reconnectRowDirectReady() {
        val r = DiscoveryPolicy.reconnectRow(network = null, direct = "dir", directSince = 0.0, networkLeftAt = null, now = 7.0)
        assertEquals("dir", r.take)
    }

    @Test
    fun reconnectRowNetworkGrace() {
        val r = DiscoveryPolicy.reconnectRow(network = null, direct = "dir", directSince = 0.0, networkLeftAt = 5.0, now = 7.0)
        assertEquals(null, r.take)
        assertEquals(15.0, r.recheckAt!!, 0.0)
    }

    @Test
    fun rebuildBrowserCameForward() {
        val r = DiscoveryPolicy.rebuildBrowser(failedAt = null, failures = 0, cameForward = true, foreground = false, now = 1.0)
        assertTrue(r.rebuild)
    }

    @Test
    fun rebuildBrowserFailedDelay() {
        val r = DiscoveryPolicy.rebuildBrowser(failedAt = 0.0, failures = 1, cameForward = false, foreground = true, now = 0.5)
        assertFalse(r.rebuild)
        assertEquals(1.0, r.recheckAt!!, 0.0)
    }

    @Test
    fun rebuildBrowserFailedReady() {
        val r = DiscoveryPolicy.rebuildBrowser(failedAt = 0.0, failures = 1, cameForward = false, foreground = true, now = 2.0)
        assertTrue(r.rebuild)
    }

    @Test
    fun browserResultsSettle() {
        val kept = listOf("old")
        val fresh = listOf("new")
        val res = DiscoveryPolicy.browserResults(kept, fresh, { it }, settleEnds = 10.0, now = 5.0)
        assertEquals(listOf("new", "old"), res)
    }

    @Test
    fun browserResultsAfterSettle() {
        val kept = listOf("old")
        val fresh = listOf("new")
        val res = DiscoveryPolicy.browserResults(kept, fresh, { it }, settleEnds = 10.0, now = 15.0)
        assertEquals(listOf("new"), res)
    }

    @Test
    fun browserFailuresAndDelay() {
        assertEquals(1, DiscoveryPolicy.browserFailures(5, 0.0, 31.0))
        assertEquals(6, DiscoveryPolicy.browserFailures(5, 0.0, 5.0))
        assertEquals(1.0, DiscoveryPolicy.browserRetryDelay(1), 0.0)
        assertEquals(30.0, DiscoveryPolicy.browserRetryDelay(100), 0.0)
    }

    @Test
    fun upWaitBasic() {
        assertEquals(5.0, DiscoveryPolicy.upWait(0), 0.0)
        assertEquals(10.0, DiscoveryPolicy.upWait(1), 0.0)
        assertEquals(60.0, DiscoveryPolicy.upWait(10), 0.0)
    }

    @Test
    fun moveToNetworkBasic() {
        val r = DiscoveryPolicy.moveToNetwork(listedSince = 0.0, lastAttempt = null, refusedListing = null, now = 3.0)
        assertTrue(r.move)
    }

    @Test
    fun moveHomeBasic() {
        val r = DiscoveryPolicy.moveHome(listedSince = 0.0, lastAttempt = null, failures = 0, now = 3.0)
        assertTrue(r.move)
    }

    @Test
    fun constants() {
        assertEquals(6.0, DiscoveryPolicy.directWait, 0.0)
        assertEquals(3.0, DiscoveryPolicy.networkFirst, 0.0)
    }
}