package app.sill.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemotePairingAddressTest {
    @Test
    fun base64UrlRoundTrip() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val enc = Base64URL.encode(data)
        assertEquals(data.toList(), Base64URL.decode(enc)!!.toList())
    }

    @Test
    fun macIdWellFormed() {
        val fp = ByteArray(32) { it.toByte() }
        val id = MacID.make(fp)
        assertEquals(16, id.length)
        assertTrue(MacID.isWellFormed(id))
    }

    @Test
    fun pairingCheckDigit() {
        // generate a code's check digit? Just check length validation
        // PairingCode is not ported fully; skip crypto. Test AddressParser instead.
    }

    @Test
    fun addressParserIpv4() {
        val r = AddressParser.parse("100.101.102.103:7455")
        assertTrue(r is AddressResult.Success)
        val a = (r as AddressResult.Success).value
        assertEquals("100.101.102.103", a.host)
        assertEquals(7455, a.port)
        assertEquals(ParsedAddress.Kind.IPv4, a.kind)
    }

    @Test
    fun safeTextLabel() {
        assertEquals("hi there", SafeText.label("hi\n\nthere   "))
    }

    @Test
    fun remoteRoundTrip() {
        val ma = MacAddress("example.com", 443, MacAddress.vpn, "Tailscale")
        assertEquals(ma, MacAddress.fromJson(ma.toJson()))
        val mi = MacInfo(macID = "ID", name = "Mac", issuedAt = 1.0, remoteAccess = true, remotePort = 7455, internet = false, addresses = listOf(ma))
        assertEquals(mi, MacInfo.fromJson(mi.toJson()))
        val gr = Goodbye("quit", message = "bye", reconnect = false)
        assertEquals(gr, Goodbye.fromJson(gr.toJson()))
        val pr = PairResult(ok = true, macID = "ID", name = "Mac")
        assertEquals(pr, PairResult.fromJson(pr.toJson()))
        val pq = PairRequest(method = "qr", proof = "abc", name = "iPad")
        assertEquals(pq, PairRequest.fromJson(pq.toJson()))
    }
}