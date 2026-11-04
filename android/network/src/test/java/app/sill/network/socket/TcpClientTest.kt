package app.sill.network.socket

import java.net.ServerSocket
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TcpClientTest {
    private var server: ServerSocket? = null

    @After
    fun tearDown() {
        try { server?.close() } catch (_: Exception) { }
    }

    @Test
    fun connectAndDisconnect() {
        server = ServerSocket(0)
        val port = server!!.localPort
        val acceptThread = Thread {
            try {
                val client = server!!.accept()
                // echo one byte
                val b = client.getInputStream().read()
                client.getOutputStream().write(b)
                client.getOutputStream().flush()
                client.close()
            } catch (_: Exception) { }
        }
        acceptThread.isDaemon = true
        acceptThread.start()

        val tcp = TcpClient()
        tcp.connect("127.0.0.1", port, 2000)
        assertTrue(tcp.isConnected)
        assertTrue(tcp.connected)

        val out = tcp.outputStream
        out.write(42)
        out.flush()
        val input = tcp.inputStream
        assertEquals(42, input.read())

        tcp.disconnect()
        assertFalse(tcp.isConnected)
        assertFalse(tcp.connected)

        acceptThread.join(2000)
    }

    @Test
    fun constructorDefaultsAndClose() {
        server = ServerSocket(0)
        val port = server!!.localPort
        Thread {
            try { server!!.accept().close() } catch (_: Exception) { }
        }.apply { isDaemon = true; start() }

        val tcp = TcpClient("127.0.0.1", port, 2000)
        tcp.connect()
        assertTrue(tcp.isConnected)
        tcp.close()
        assertFalse(tcp.isConnected)
    }

    @Test
    fun connectSocketAddress() {
        server = ServerSocket(0)
        val port = server!!.localPort
        Thread {
            try { server!!.accept().close() } catch (_: Exception) { }
        }.apply { isDaemon = true; start() }

        val tcp = TcpClient()
        tcp.connect(java.net.InetSocketAddress("127.0.0.1", port), 2000)
        assertTrue(tcp.isConnected)
        tcp.disconnect()
    }
}