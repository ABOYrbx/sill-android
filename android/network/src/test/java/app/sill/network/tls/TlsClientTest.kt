package app.sill.network.tls

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket

class TlsClientTest {
    private var server: SSLServerSocket? = null

    @After
    fun tearDown() {
        try { server?.close() } catch (_: Exception) { }
    }

    private fun loadServerKeyStore(): KeyStore {
        val ks = KeyStore.getInstance("PKCS12")
        val stream = TlsClientTest::class.java.getResourceAsStream("/app/sill/network/tls/server.p12")
            ?: throw IllegalStateException("test keystore resource missing")
        stream.use { ks.load(it, "changeit".toCharArray()) }
        return ks
    }

    private fun startServer(): Int {
        val ks = loadServerKeyStore()
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, "changeit".toCharArray())
        val ctx = SSLContext.getInstance("TLSv1.3")
        ctx.init(kmf.keyManagers, null, SecureRandom())
        val ssf = ctx.serverSocketFactory
        val srv = ssf.createServerSocket(0) as SSLServerSocket
        srv.enabledProtocols = arrayOf("TLSv1.3")
        server = srv
        val acceptThread = Thread {
            try {
                val c = srv.accept()
                val b = c.inputStream.read()
                c.outputStream.write(b)
                c.outputStream.flush()
                c.close()
            } catch (_: Exception) { }
        }
        acceptThread.isDaemon = true
        acceptThread.start()
        return srv.localPort
    }

    private fun serverPin(): ByteArray {
        val ks = loadServerKeyStore()
        val cert = ks.getCertificate("server")
        return MessageDigest.getInstance("SHA-256").digest(cert.publicKey.encoded)
    }

    @Test
    fun connectsWithPinnedKey() {
        val port = startServer()
        val pin = serverPin()
        val client = TlsClient("127.0.0.1", port, pin, 5000)
        client.connect()
        assertTrue(client.isConnected)
        assertTrue(client.connected)

        val out = client.outputStream
        out.write(42)
        out.flush()
        assertEquals(42, client.inputStream.read())

        client.disconnect()
        assertFalse(client.isConnected)
        assertFalse(client.connected)
    }

    @Test
    fun wrongPinFails() {
        val port = startServer()
        val wrongPin = ByteArray(32) { 1 }
        val client = TlsClient("127.0.0.1", port, wrongPin, 5000)
        try {
            client.connect()
            throw AssertionError("expected TLS handshake failure")
        } catch (e: Exception) {
            // expected
        }
        assertFalse(client.isConnected)
    }
}