package app.sill.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingTest {
    @Test
    fun pairingCodeCheck() {
        val code = PairingCode.generate()!!
        val (kept, problem) = PairingCode.check(code)
        assertEquals(code, kept)
        assertEquals(null, problem)
    }

    @Test
    fun homeDoorTxt() {
        assertEquals(HomeDoorTXT.Door.Open, HomeDoorTXT.door(mapOf("p" to "0")))
        assertEquals(HomeDoorTXT.Door.PairingRequired, HomeDoorTXT.door(mapOf("p" to "1")))
        assertEquals(HomeDoorTXT.Door.Plain, HomeDoorTXT.door(emptyMap()))
    }

    @Test
    fun pairLinkRoundTrip() {
        val fp = ByteArray(32) { it.toByte() }
        val secret = ByteArray(16) { (it + 1).toByte() }
        val link = PairLink(macID = MacID.make(fp), fingerprint = fp, secret = secret, name = "Mac", port = 7455, addresses = emptyList())
        val parsed = PairLink.parse(link.url)
        assertTrue(parsed.first != null)
        assertEquals(link.macID, parsed.first!!.macID)
    }
}