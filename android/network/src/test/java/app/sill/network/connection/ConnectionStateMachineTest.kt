package app.sill.network.connection

import app.sill.network.framer.MessageFramer
import app.sill.network.socket.TcpClient
import java.net.ServerSocket
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateMachineTest {

    @Test
    fun initialStateIsDisconnected() {
        val sm = ConnectionStateMachine()
        assertEquals(ConnectionState.Disconnected, sm.state)
        assertTrue(sm.isDisconnected)
    }

    @Test
    fun connectTransitionsToConnecting() {
        val sm = ConnectionStateMachine()
        assertTrue(sm.connect())
        assertEquals(ConnectionState.Connecting, sm.state)
    }

    @Test
    fun markReadyTransitionsToReady() {
        val sm = ConnectionStateMachine()
        sm.connect()
        assertTrue(sm.markReady())
        assertEquals(ConnectionState.Ready, sm.state)
        assertTrue(sm.isReady)
    }

    @Test
    fun disconnectTransitionsToDisconnected() {
        val sm = ConnectionStateMachine()
        sm.connect()
        sm.markReady()
        assertTrue(sm.disconnect())
        assertEquals(ConnectionState.Disconnected, sm.state)
    }

    @Test
    fun reconnectTransitionsToConnecting() {
        val sm = ConnectionStateMachine()
        sm.connect()
        sm.markReady()
        assertTrue(sm.reconnect())
        assertEquals(ConnectionState.Connecting, sm.state)
    }

    @Test
    fun invalidTransitionReturnsFalse() {
        val sm = ConnectionStateMachine()
        assertFalse(sm.transitionTo(ConnectionState.Ready))
        assertEquals(ConnectionState.Disconnected, sm.state)
    }

    @Test
    fun tcpConnectIntegration() {
        val server = ServerSocket(0)
        val port = server.localPort
        val acceptThread = Thread {
            try {
                val c = server.accept()
                Thread.sleep(50)
                c.close()
            } catch (_: Exception) { }
        }
        acceptThread.isDaemon = true
        acceptThread.start()

        val client = TcpClient()
        val framer = MessageFramer()
        val sm = ConnectionStateMachine(client, framer)
        assertTrue(sm.connect("127.0.0.1", port, 2000))
        assertEquals(ConnectionState.Connecting, sm.state)
        assertTrue(sm.messageFramer?.input != null)
        sm.markReady()
        assertEquals(ConnectionState.Ready, sm.state)
        sm.disconnect()
        assertEquals(ConnectionState.Disconnected, sm.state)
        assertFalse(client.isConnected)

        acceptThread.join(2000)
        server.close()
    }
}
