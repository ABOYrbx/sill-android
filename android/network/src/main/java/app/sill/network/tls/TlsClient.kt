package app.sill.network.tls

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * TLS 1.3 client built on [javax.net.ssl.SSLSocket], with a [TrustManager] that pins the
 * peer's (the Mac's) SPKI SHA-256 fingerprint instead of evaluating a certificate chain.
 *
 * Port of the TLS leg of `Sources/StreamProtocol/RemoteTLS.swift`: TLS 1.3 only, no session
 * tickets/resumption, trust decided entirely by the pinned key.
 */
class TlsClient(
    host: String? = null,
    port: Int? = null,
    pinnedSpkiSha256: ByteArray? = null,
    connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
) {
    private val lock = Any()

    /** Configured default host, if any. */
    var host: String? = host
        private set

    /** Configured default port, if any. */
    var port: Int? = port
        private set

    /** Pinned SHA-256 of the peer's SubjectPublicKeyInfo. */
    var pinnedSpkiSha256: ByteArray? = pinnedSpkiSha256
        private set

    /** Alias for [pinnedSpkiSha256]. */
    var pin: ByteArray?
        get() = pinnedSpkiSha256
        set(value) { pinnedSpkiSha256 = value }

    /** Alias for [pinnedSpkiSha256]. */
    var pinnedKey: ByteArray?
        get() = pinnedSpkiSha256
        set(value) { pinnedSpkiSha256 = value }

    /** Default connect timeout in milliseconds. */
    var connectTimeoutMs: Int = connectTimeoutMs
        private set

    private var _socket: SSLSocket? = null

    /** The underlying socket, or null when not connected. */
    val rawSocket: SSLSocket?
        get() = synchronized(lock) { _socket }

    /** Alias for [rawSocket]. */
    val socket: SSLSocket?
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
            _socket?.inputStream ?: throw IllegalStateException("TlsClient is not connected")
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
            _socket?.outputStream ?: throw IllegalStateException("TlsClient is not connected")
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

    // ---- secondary constructors ------------------------------------------------

    constructor(pinnedSpkiSha256: ByteArray, connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS) :
        this(null, null, pinnedSpkiSha256, connectTimeoutMs)

    constructor(host: String?, port: Int?, pinnedPin: String, connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS) :
        this(host, port, decodePin(pinnedPin), connectTimeoutMs)

    constructor(pinnedPin: String, connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS) :
        this(null, null, decodePin(pinnedPin), connectTimeoutMs)

    /**
     * Configure defaults (host/port/pin/timeout) without connecting.
     */
    fun configure(
        host: String? = this.host,
        port: Int? = this.port,
        pinnedSpkiSha256: ByteArray? = this.pinnedSpkiSha256,
        connectTimeoutMs: Int = this.connectTimeoutMs,
    ) {
        synchronized(lock) {
            this.host = host
            this.port = port
            this.pinnedSpkiSha256 = pinnedSpkiSha256
            this.connectTimeoutMs = connectTimeoutMs
        }
    }

    /** Alias-style configure accepting a string pin. */
    fun configure(
        host: String? = this.host,
        port: Int? = this.port,
        pinnedPin: String,
        connectTimeoutMs: Int = this.connectTimeoutMs,
    ) {
        configure(host, port, decodePin(pinnedPin), connectTimeoutMs)
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
            val pin = pinnedSpkiSha256
                ?: throw IllegalStateException("pinned SPKI is not set")
            val factory = createFactory(pin)
            val s = factory.createSocket() as SSLSocket
            try {
                s.connect(InetSocketAddress(host, port), timeoutMs)
                val params = s.sslParameters
                params.endpointIdentificationAlgorithm = null
                s.sslParameters = params
                s.enabledProtocols = arrayOf("TLSv1.3")
                s.startHandshake()
                _socket = s
                this.host = host
                this.port = port
                this.connectTimeoutMs = timeoutMs
                this.pinnedSpkiSha256 = pin
            } catch (e: Exception) {
                try { s.close() } catch (_: Exception) { }
                throw e
            }
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
            val pin = pinnedSpkiSha256
                ?: throw IllegalStateException("pinned SPKI is not set")
            val factory = createFactory(pin)
            val s = factory.createSocket() as SSLSocket
            try {
                s.connect(address, timeoutMs)
                val params = s.sslParameters
                params.endpointIdentificationAlgorithm = null
                s.sslParameters = params
                s.enabledProtocols = arrayOf("TLSv1.3")
                s.startHandshake()
                _socket = s
                this.connectTimeoutMs = timeoutMs
                this.pinnedSpkiSha256 = pin
            } catch (e: Exception) {
                try { s.close() } catch (_: Exception) { }
                throw e
            }
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

    private fun createFactory(pin: ByteArray): SSLSocketFactory {
        val tm = PinnedTrustManager(pin)
        val ctx = SSLContext.getInstance("TLSv1.3")
        ctx.init(null, arrayOf<TrustManager>(tm), SecureRandom())
        return ctx.socketFactory
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MS = 5000

        /** SHA-256 of an X.509 certificate's SPKI (its public key's encoded form). */
        fun spkiSha256(certificate: java.security.cert.X509Certificate): ByteArray =
            sha256(certificate.publicKey.encoded)

        /** SHA-256 of the given bytes. */
        fun sha256(data: ByteArray): ByteArray {
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(data)
        }

        /** Constant-time comparison. */
        fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
            if (a.size != b.size) return false
            var diff = 0
            for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
            return diff == 0
        }

        fun decodePin(text: String): ByteArray {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) throw IllegalArgumentException("empty pin")
            // Hex if all hex chars and even length.
            if (trimmed.length % 2 == 0 && trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                val out = ByteArray(trimmed.length / 2)
                for (i in out.indices) {
                    out[i] = trimmed.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                }
                return out
            }
            // Otherwise try Base64 (standard or url-safe).
            return try {
                val normalized = trimmed.replace('-', '+').replace('_', '/')
                val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
                java.util.Base64.getDecoder().decode(padded)
            } catch (e: Exception) {
                throw IllegalArgumentException("invalid pin", e)
            }
        }

        private class PinnedTrustManager(private val pin: ByteArray) : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                // Client-side trust manager: server authentication only.
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                if (chain.isEmpty()) throw CertificateException("empty certificate chain")
                val fp = sha256(chain[0].publicKey.encoded)
                if (!constantTimeEquals(fp, pin)) {
                    throw CertificateException("SPKI pin mismatch")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
    }
}
