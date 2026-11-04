package app.sill.network.framer

import app.sill.protocol.StreamMessage
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

class MessageFramer {
    var input: InputStream? = null
    var output: OutputStream? = null

    var inputStream: InputStream?
        get() = input
        set(value) { input = value }

    var outputStream: OutputStream?
        get() = output
        set(value) { output = value }

    constructor()
    constructor(input: InputStream) { this.input = input }
    constructor(output: OutputStream) { this.output = output }
    constructor(input: InputStream, output: OutputStream) {
        this.input = input
        this.output = output
    }

    private fun requireInput(): InputStream =
        input ?: throw IllegalStateException("MessageFramer has no input stream")

    private fun requireOutput(): OutputStream =
        output ?: throw IllegalStateException("MessageFramer has no output stream")

    fun read(): StreamMessage = readFrom(requireInput())
    fun read(input: InputStream): StreamMessage = readFrom(input)
    fun readMessage(): StreamMessage = read()
    fun readMessage(input: InputStream): StreamMessage = readFrom(input)
    fun readFrame(): StreamMessage = read()
    fun readFrame(input: InputStream): StreamMessage = readFrom(input)
    fun readNext(): StreamMessage = read()
    fun readNext(input: InputStream): StreamMessage = readFrom(input)
    fun readOne(): StreamMessage = read()
    fun readOne(input: InputStream): StreamMessage = readFrom(input)

    fun readOrNull(): StreamMessage? = try { readFrom(requireInput()) } catch (_: EOFException) { null } catch (_: IllegalStateException) { null }
    fun readOrNull(input: InputStream): StreamMessage? = try { readFrom(input) } catch (_: EOFException) { null }

    fun write(message: StreamMessage) { writeTo(requireOutput(), message) }
    fun write(output: OutputStream, message: StreamMessage) { writeTo(output, message) }
    fun write(message: StreamMessage, output: OutputStream) { writeTo(output, message) }

    fun writeMessage(message: StreamMessage) = write(message)
    fun writeMessage(output: OutputStream, message: StreamMessage) = writeTo(output, message)
    fun writeMessage(message: StreamMessage, output: OutputStream) = writeTo(output, message)

    fun writeFrame(message: StreamMessage) = write(message)
    fun writeFrame(output: OutputStream, message: StreamMessage) = writeTo(output, message)
    fun writeFrame(message: StreamMessage, output: OutputStream) = writeTo(output, message)

    fun send(message: StreamMessage) = write(message)
    fun send(output: OutputStream, message: StreamMessage) = writeTo(output, message)
    fun send(message: StreamMessage, output: OutputStream) = writeTo(output, message)

    fun sendMessage(message: StreamMessage) = write(message)
    fun sendMessage(output: OutputStream, message: StreamMessage) = writeTo(output, message)
    fun sendMessage(message: StreamMessage, output: OutputStream) = writeTo(output, message)

    fun put(message: StreamMessage) = write(message)
    fun put(output: OutputStream, message: StreamMessage) = writeTo(output, message)
    fun put(message: StreamMessage, output: OutputStream) = writeTo(output, message)

    fun encode(message: StreamMessage): ByteArray = message.serialized()
    fun decode(data: ByteArray): StreamMessage = readFrom(data.inputStream())
    fun toBytes(message: StreamMessage): ByteArray = message.serialized()
    fun fromBytes(data: ByteArray): StreamMessage = readFrom(data.inputStream())

    companion object {
        fun read(input: InputStream): StreamMessage = readFrom(input)
        fun readMessage(input: InputStream): StreamMessage = readFrom(input)
        fun readFrame(input: InputStream): StreamMessage = readFrom(input)
        fun readNext(input: InputStream): StreamMessage = readFrom(input)
        fun readOne(input: InputStream): StreamMessage = readFrom(input)

        fun write(output: OutputStream, message: StreamMessage) = writeTo(output, message)
        fun write(message: StreamMessage, output: OutputStream) = writeTo(output, message)
        fun writeMessage(output: OutputStream, message: StreamMessage) = writeTo(output, message)
        fun writeMessage(message: StreamMessage, output: OutputStream) = writeTo(output, message)
        fun writeFrame(output: OutputStream, message: StreamMessage) = writeTo(output, message)
        fun writeFrame(message: StreamMessage, output: OutputStream) = writeTo(output, message)
        fun send(output: OutputStream, message: StreamMessage) = writeTo(output, message)
        fun send(message: StreamMessage, output: OutputStream) = writeTo(output, message)
        fun sendMessage(output: OutputStream, message: StreamMessage) = writeTo(output, message)
        fun sendMessage(message: StreamMessage, output: OutputStream) = writeTo(output, message)
        fun put(output: OutputStream, message: StreamMessage) = writeTo(output, message)
        fun put(message: StreamMessage, output: OutputStream) = writeTo(output, message)

        fun encode(message: StreamMessage): ByteArray = message.serialized()
        fun toBytes(message: StreamMessage): ByteArray = message.serialized()
        fun decode(data: ByteArray): StreamMessage = readFrom(data.inputStream())
        fun fromBytes(data: ByteArray): StreamMessage = readFrom(data.inputStream())

        fun create(input: InputStream? = null, output: OutputStream? = null): MessageFramer =
            MessageFramer().apply {
                this.input = input
                this.output = output
            }
    }
}

private fun readFrom(input: InputStream): StreamMessage {
    val headerBytes = readFully(input, StreamMessage.HEADER_LENGTH)
    val header = StreamMessage.parseHeader(headerBytes)
        ?: throw java.io.IOException("invalid StreamMessage header")
    if (header.payloadLength < 0) throw java.io.IOException("negative payload length")
    val payload = readFully(input, header.payloadLength)
    return StreamMessage(header.kind, header.timestamp, header.isKeyframe, payload)
}

private fun writeTo(output: OutputStream, message: StreamMessage) {
    output.write(message.serialized())
    output.flush()
}

private fun readFully(input: InputStream, count: Int): ByteArray {
    val buf = ByteArray(count)
    var off = 0
    while (off < count) {
        val r = input.read(buf, off, count - off)
        if (r < 0) throw EOFException("unexpected end of stream after $off of $count bytes")
        if (r == 0) continue
        off += r
    }
    return buf
}
