package app.sill.network.connection

import app.sill.network.framer.MessageFramer
import app.sill.network.socket.TcpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.SocketAddress

/**
 * Connection lifecycle states shared by the Android network layer.
 */
enum class ConnectionState {
    Disconnected,
    Connecting,
    Ready,
    Disconnecting;

    companion object {
        fun fromName(name: String): ConnectionState? =
            values().firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

/** Alias for [ConnectionState], for callers that prefer the shorter name. */
typealias State = ConnectionState

class ConnectionStateMachine(
    private var client: TcpClient? = null,
    private var framer: MessageFramer? = null,
    host: String? = null,
    port: Int? = null,
    connectTimeoutMs: Int = TcpClient.DEFAULT_CONNECT_TIMEOUT_MS,
) {

    constructor() : this(null, null, null, null, TcpClient.DEFAULT_CONNECT_TIMEOUT_MS)

    constructor(client: TcpClient) : this(client, null, null, null, TcpClient.DEFAULT_CONNECT_TIMEOUT_MS)

    constructor(client: TcpClient, framer: MessageFramer) :
        this(client, framer, null, null, TcpClient.DEFAULT_CONNECT_TIMEOUT_MS)

    constructor(host: String, port: Int) : this(null, null, host, port, TcpClient.DEFAULT_CONNECT_TIMEOUT_MS)

    constructor(host: String, port: Int, connectTimeoutMs: Int) : this(null, null, host, port, connectTimeoutMs)

    constructor(framer: MessageFramer) : this(null, framer, null, null, TcpClient.DEFAULT_CONNECT_TIMEOUT_MS)

    init {
        if (client == null && host != null && port != null) {
            client = TcpClient(host, port, connectTimeoutMs)
        } else if (client != null && host != null) {
            client!!.configure(host, port, connectTimeoutMs)
        }
    }

    private val _state = MutableStateFlow(ConnectionState.Disconnected)

    val state: ConnectionState
        get() = _state.value

    val currentState: ConnectionState
        get() = state

    val connectionState: ConnectionState
        get() = state

    val status: ConnectionState
        get() = state

    val stateFlow: StateFlow<ConnectionState> = _state.asStateFlow()

    val states: StateFlow<ConnectionState>
        get() = stateFlow

    private val listeners = mutableListOf<(ConnectionState) -> Unit>()

    fun addStateListener(listener: (ConnectionState) -> Unit) {
        listeners += listener
        listener(state)
    }

    fun removeStateListener(listener: (ConnectionState) -> Unit) {
        listeners.remove(listener)
    }

    fun isTransitionAllowed(from: ConnectionState, to: ConnectionState): Boolean {
        if (from == to) return true
        return when (from) {
            ConnectionState.Disconnected ->
                to == ConnectionState.Connecting ||
                    to == ConnectionState.Disconnecting
            ConnectionState.Connecting ->
                to == ConnectionState.Ready ||
                    to == ConnectionState.Disconnecting ||
                    to == ConnectionState.Connecting
            ConnectionState.Ready ->
                to == ConnectionState.Connecting ||
                    to == ConnectionState.Disconnecting
            ConnectionState.Disconnecting ->
                to == ConnectionState.Disconnected ||
                    to == ConnectionState.Connecting
        }
    }

    fun canTransition(from: ConnectionState, to: ConnectionState): Boolean = isTransitionAllowed(from, to)

    fun transitionTo(target: ConnectionState): Boolean {
        val from = _state.value
        if (from == target) return true
        if (!isTransitionAllowed(from, target)) return false
        _state.value = target
        listeners.forEach { runCatching { it(target) } }
        return true
    }

    fun moveTo(target: ConnectionState): Boolean = transitionTo(target)

    fun goto(target: ConnectionState): Boolean = transitionTo(target)

    fun setState(target: ConnectionState): Boolean = transitionTo(target)

    val tcpClient: TcpClient?
        get() = client

    val messageFramer: MessageFramer?
        get() = framer

    fun configure(host: String, port: Int, connectTimeoutMs: Int = client?.connectTimeoutMs ?: TcpClient.DEFAULT_CONNECT_TIMEOUT_MS) {
        if (client == null) {
            client = TcpClient(host, port, connectTimeoutMs)
        } else {
            client!!.configure(host, port, connectTimeoutMs)
        }
        framer = framer ?: MessageFramer()
    }

    fun connect(): Boolean {
        val c = client
        val h = c?.host
        val p = c?.port
        if (h != null && p != null) {
            return connect(h, p, c!!.connectTimeoutMs)
        }
        transitionTo(ConnectionState.Connecting)
        return state == ConnectionState.Connecting
    }

    fun connect(host: String, port: Int): Boolean =
        connect(host, port, client?.connectTimeoutMs ?: TcpClient.DEFAULT_CONNECT_TIMEOUT_MS)

    fun connect(host: String, port: Int, timeoutMs: Int): Boolean {
        if (client == null) client = TcpClient(host, port, timeoutMs)
        else client!!.configure(host, port, timeoutMs)
        if (framer == null) framer = MessageFramer()
        transitionTo(ConnectionState.Connecting)
        return try {
            client!!.connect(host, port, timeoutMs)
            framer!!.input = client!!.inputStream
            framer!!.output = client!!.outputStream
            true
        } catch (e: Exception) {
            transitionTo(ConnectionState.Disconnected)
            false
        }
    }

    fun connect(address: SocketAddress): Boolean =
        connect(address, client?.connectTimeoutMs ?: TcpClient.DEFAULT_CONNECT_TIMEOUT_MS)

    fun connect(address: SocketAddress, timeoutMs: Int): Boolean {
        if (client == null) client = TcpClient()
        if (framer == null) framer = MessageFramer()
        transitionTo(ConnectionState.Connecting)
        return try {
            client!!.connect(address, timeoutMs)
            framer!!.input = client!!.inputStream
            framer!!.output = client!!.outputStream
            true
        } catch (e: Exception) {
            transitionTo(ConnectionState.Disconnected)
            false
        }
    }

    fun connect(tcpClient: TcpClient): Boolean {
        client = tcpClient
        if (framer == null) framer = MessageFramer()
        transitionTo(ConnectionState.Connecting)
        return true
    }

    fun markReady(): Boolean {
        if (state == ConnectionState.Ready) return true
        return transitionTo(ConnectionState.Ready)
    }

    fun ready(): Boolean = markReady()

    fun setReady(): Boolean = markReady()

    fun onReady(): Boolean = markReady()

    fun disconnecting(): Boolean = transitionTo(ConnectionState.Disconnecting)

    fun beginDisconnect(): Boolean = disconnecting()

    fun startDisconnect(): Boolean = disconnecting()

    fun disconnect(): Boolean {
        runCatching { client?.disconnect() }
        framer?.input = null
        framer?.output = null
        transitionTo(ConnectionState.Disconnecting)
        transitionTo(ConnectionState.Disconnected)
        return state == ConnectionState.Disconnected
    }

    fun close(): Boolean = disconnect()

    fun shutdown(): Boolean = disconnect()

    fun disconnected(): Boolean {
        runCatching { client?.disconnect() }
        framer?.input = null
        framer?.output = null
        return transitionTo(ConnectionState.Disconnected)
    }

    fun finishDisconnect(): Boolean = disconnected()

    fun completeDisconnect(): Boolean = disconnected()

    fun onDisconnected(): Boolean = disconnected()

    fun reconnect(): Boolean {
        disconnect()
        return connect()
    }

    fun reconnect(host: String, port: Int): Boolean {
        disconnect()
        return connect(host, port)
    }

    fun reconnect(host: String, port: Int, timeoutMs: Int): Boolean {
        disconnect()
        return connect(host, port, timeoutMs)
    }

    fun reset() {
        runCatching { client?.disconnect() }
        framer?.input = null
        framer?.output = null
        _state.value = ConnectionState.Disconnected
        listeners.forEach { runCatching { it(ConnectionState.Disconnected) } }
    }

    val isReady: Boolean
        get() = state == ConnectionState.Ready

    val isConnected: Boolean
        get() = state == ConnectionState.Ready || (client?.isConnected == true)

    val isDisconnected: Boolean
        get() = state == ConnectionState.Disconnected

    companion object {
        typealias ConnectionState = app.sill.network.connection.ConnectionState

        typealias State = app.sill.network.connection.ConnectionState
    }
}
