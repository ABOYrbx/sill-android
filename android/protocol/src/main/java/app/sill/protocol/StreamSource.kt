package app.sill.protocol

/**
 * What the host streams.
 *
 * Port of `Sources/StreamProtocol/Switcher.swift` (StreamSource).
 */
sealed class StreamSource {
    object None : StreamSource()
    data class Window(val id: UInt) : StreamSource()
    object Desktop : StreamSource()

    fun toJson(): String = when (this) {
        is None -> """{"none":{}}"""
        is Window -> """{"window":{"_0":$id}}"""
        is Desktop -> """{"desktop":{}}"""
    }

    companion object {
        fun fromJson(json: String): StreamSource? {
            val s = json.trim()
            return when {
                s.startsWith("""{"none"""") -> None
                s.startsWith("""{"desktop"""") -> Desktop
                s.startsWith("""{"window"""") -> {
                    val re = Regex("\"_0\"\\s*:\\s*(\\d+)")
                    val id = re.find(s)?.groupValues?.get(1)?.toUIntOrNull() ?: return null
                    Window(id)
                }
                else -> null
            }
        }
    }
}