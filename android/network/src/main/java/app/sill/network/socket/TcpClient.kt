package app.sill.network.socket

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress

/**
 * Plain TCP client built on [java.net.Socket].
 *
 * Port of the plain (non-TLS) transport leg used to reach a `SillHost` in dev mode.
 * Provides connect with a timeout and access to the socket's input/output streams,
 * plus a clean disconnect. Thread-safe for connect/disconnect.
 */
class TcpClient(
    host: String? = null,
    port: Int? = null,
    connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
) {
    private val lock = Any()

    /** Configured default host, if any. */
    var host: String? = host
        private set

    /** Configured default port, if any. */
    var port: Int? = port
        private set

    /** Default connect timeout in milliseconds. */
    var connectTimeoutMs: Int = connectTimeoutMs
        private set

    private var _socket: Socket? = null

    /** The underlying socket, or null when not connected. */
    val rawSocket: Socket?
        get() = synchronized(lock) { _socket }

    /** Alias for [rawSocket]. */
    val socket: Socket?
        get() = rawSocket

    /** True when the socket is connected and not closed. */
    val isConnected: Boolean
        get() = synchronized(lock) {
            val s = _socket
            s != null && s.isConnected && !s.isClosed
        }

    /** Alias for [isConnected]. */
    val connected: Boolean
        get() = isConnected

    /** The socket's input stream. Throws if not connected. */
    val inputStream: InputStream
        get() = synchronized(lock) {
            _socket?.getInputStream() ?: throw IllegalStateException("TcpClient is not connected")
        }

    /** Alias for [inputStream]. */
    val input: InputStream
        get() = inputStream

    /** Alias for [inputStream]. */
    val inStream: InputStream
        get() = inputStream

    /** The socket's output stream. Throws if not connected. */
    val outputStream: OutputStream
        get() = synchronized(lock) {
            _socket?.getOutputStream() ?: throw IllegalStateException("TcpClient is not connected")
        }

    /** Alias for [outputStream]. */
    val output: OutputStream
        get() = outputStream

    /** Alias for [outputStream]. */
    val out: OutputStream
        get() = outputStream

    /** Alias for [outputStream]. */
    val outStream: OutputStream
        get() = outputStream

    /**
     * Configure defaults (host/port/timeout) without connecting.
     */
    fun configure(host: String? = this.host, port: Int? = this.port, connectTimeoutMs: Int = this.connectTimeoutMs) {
        synchronized(lock) {
            this.host = host
            this.port = port
            this.connectTimeoutMs = connectTimeoutMs
        }
    }

    /**
     * Connect using the configured host/port (must have been set).
     */
    fun connect() {
        val h = host ?: throw IllegalStateException("host is not set")
        val p = port ?: throw IllegalStateException("port is not set")
        connect(h, p, connectTimeoutMs)
    }

    /**
     * Connect to [host]:[port] with the default timeout.
     */
    fun connect(host: String, port: Int) {
        connect(host, port, connectTimeoutMs)
    }

    /**
     * Connect to [host]:[port] with the given timeout in milliseconds.
     */
    fun connect(host: String, port: Int, timeoutMs: Int) {
        synchronized(lock) {
            closeLocked()
            val s = Socket()
            s.connect(InetSocketAddress(host, port), timeoutMs)
            _socket = s
            this.host = host
            this.port = port
            this.connectTimeoutMs = timeoutMs
        }
    }

    /**
     * Connect to the given socket address with the default timeout.
     */
    fun connect(address: SocketAddress) {
        connect(address, connectTimeoutMs)
    }

    /**
     * Connect to the given socket address with the given timeout in milliseconds.
     */
    fun connect(address: SocketAddress, timeoutMs: Int) {
        synchronized(lock) {
            closeLocked()
            val s = Socket()
            s.connect(address, timeoutMs)
            _socket = s
        }
    }

    /**
     * Disconnect and release resources. Idempotent.
     */
    fun disconnect() {
        synchronized(lock) { closeLocked() }
    }

    /** Alias for [disconnect]. */
    fun close() {
        disconnect()
    }

    private fun closeLocked() {
        val s = _socket
        _socket = null
        try {
            s?.close()
        } catch (_: Exception) {
            // ignore
        }
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MS = 5000
    }
}
