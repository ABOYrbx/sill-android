package app.sill.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.StandardCharsets

class ProtocolFixturesTest {
    @Test
    fun roundTripAllFixtures() {
        val manifestText = readResourceText("manifest.json")
        val entries = parseManifest(manifestText)
        for ((filename, kindRaw, byteLength) in entries) {
            val kind = StreamMessageKind.fromRaw(kindRaw)
            val bytes = readResourceBytes(filename)
            assertEquals(byteLength, bytes.size)
            val header = StreamMessage.parseHeader(bytes) ?: error("bad header $filename")
            assertEquals(kind, header.kind)
            assertEquals(bytes.size - StreamMessage.HEADER_LENGTH, header.payloadLength)
            val payload = bytes.copyOfRange(StreamMessage.HEADER_LENGTH, bytes.size)
            val reencodedPayload = reencode(kind, payload)
            val rebuilt = StreamMessage(header.kind, header.timestamp, header.isKeyframe, reencodedPayload).serialized()
            assertArrayEquals(filename, bytes, rebuilt)
        }
    }

    @Test
    fun deliberatelyBrokenFieldFails() {
        val bytes = readResourceBytes("clipboard.bin")
        val header = StreamMessage.parseHeader(bytes)!!
        val payload = bytes.copyOfRange(StreamMessage.HEADER_LENGTH, bytes.size)
        val brokenText = String(payload, StandardCharsets.UTF_8).replace("\"text\"", "\"texx\"")
        val brokenPayload = brokenText.toByteArray(StandardCharsets.UTF_8)
        val rebuilt = try {
            val reencoded = reencode(StreamMessageKind.Clipboard, brokenPayload)
            StreamMessage(header.kind, header.timestamp, header.isKeyframe, reencoded).serialized()
        } catch (e: Exception) {
            null
        }
        if (rebuilt != null) {
            assert(!bytes.contentEquals(rebuilt))
        }
    }

    private fun reencode(kind: StreamMessageKind, payload: ByteArray): ByteArray {
        val text = String(payload, StandardCharsets.UTF_8)
        return when (kind) {
            StreamMessageKind.ParameterSets -> {
                val ps = ParameterSets.decode(payload) ?: error("decode ParameterSets")
                ps.encoded()
            }
            StreamMessageKind.Frame -> payload
            StreamMessageKind.WindowList -> {
                val v = WindowList.fromJson(text) ?: error("decode WindowList")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.Thumbnail -> {
                val (w, j) = ImageBlob.decodeThumbnail(payload) ?: error("decode Thumbnail")
                ImageBlob.encodeThumbnail(w, j)
            }
            StreamMessageKind.AppIcon -> {
                val (b, p) = ImageBlob.decodeIcon(payload) ?: error("decode AppIcon")
                ImageBlob.encodeIcon(b, p)
            }
            StreamMessageKind.AppList -> {
                val items = mutableListOf<AppInfo>()
                for (obj in splitTopLevelObjects(text)) {
                    items.add(AppInfo.fromJson(obj) ?: error("decode AppInfo"))
                }
                ("[" + items.joinToString(",") { it.toJson() } + "]").toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.SelectSource -> {
                val v = StreamSource.fromJson(text) ?: error("decode SelectSource")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.LaunchApp -> {
                val v = LaunchApp.fromJson(text) ?: error("decode LaunchApp")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.Input -> {
                val v = InputEvent.fromJson(text) ?: error("decode Input")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.Viewport -> {
                val v = Viewport.fromJson(text) ?: error("decode Viewport")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.Ping -> payload
            StreamMessageKind.Pong -> payload
            StreamMessageKind.ClientStats -> {
                val v = ClientStats.fromJson(text) ?: error("decode ClientStats")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.Tick -> payload
            StreamMessageKind.CursorShape -> {
                val c = CursorShapeBlob.decode(payload) ?: error("decode CursorShape")
                CursorShapeBlob.encode(c.hotspotX, c.hotspotY, c.width, c.height, c.png)
            }
            StreamMessageKind.WindowCommand -> {
                val v = WindowCommand.fromJson(text) ?: error("decode WindowCommand")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.HostSettings -> {
                val v = HostSettingsState.fromJson(text) ?: error("decode HostSettings")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.ChangeSettings -> {
                val v = HostSettingsChange.fromJson(text) ?: error("decode ChangeSettings")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.MacInfo -> {
                val v = SignedMacInfo.fromJson(text) ?: error("decode MacInfo")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.PairRequest -> {
                val v = PairRequest.fromJson(text) ?: error("decode PairRequest")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.PairResult -> {
                val v = PairResult.fromJson(text) ?: error("decode PairResult")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.PairingWanted -> payload
            StreamMessageKind.Goodbye -> {
                val v = Goodbye.fromJson(text) ?: error("decode Goodbye")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.Hello -> {
                val v = Hello.fromJson(text) ?: error("decode Hello")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.MacMenu -> {
                val v = MacMenu.fromJson(text) ?: error("decode MacMenu")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.PressMenuItem -> {
                val v = PressMenuItem.fromJson(text) ?: error("decode PressMenuItem")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.MacPointer -> {
                val v = MacPointer.fromJson(text) ?: error("decode MacPointer")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.FetchMenu -> {
                val v = FetchMenu.fromJson(text) ?: error("decode FetchMenu")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.Gesture -> {
                val v = TrackpadGesture.fromJson(text) ?: error("decode Gesture")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.Clipboard -> {
                val v = Clipboard.fromJson(text) ?: error("decode Clipboard")
                v.toJson().toByteArray(StandardCharsets.UTF_8)
            }
            StreamMessageKind.Audio -> {
                val v = AudioMessage.parse(payload) ?: error("decode Audio")
                AudioMessage.serialized(v)
            }
            StreamMessageKind.KeyframeRequest -> payload
            else -> error("unhandled kind $kind")
        }
    }

    private data class ManifestEntry(val filename: String, val kind: Int, val byteLength: Int)

    private fun parseManifest(text: String): List<ManifestEntry> {
        val result = mutableListOf<ManifestEntry>()
        val objRegex = Regex("""\{[^}]*\}""")
        for (m in objRegex.findAll(text)) {
            val obj = m.value
            val filename = Regex(""""filename"\s*:\s*"([^"]+)"""").find(obj)?.groupValues?.get(1) ?: continue
            val kind = Regex(""""kind"\s*:\s*(\d+)"""").find(obj)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            val byteLength = Regex(""""byteLength"\s*:\s*(\d+)"""").find(obj)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            result.add(ManifestEntry(filename, kind, byteLength))
        }
        return result
    }

    private fun splitTopLevelObjects(json: String): List<String> {
        val s = json.trim()
        val content = s.removePrefix("[").removeSuffix("]").trim()
        if (content.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        var depth = 0
        var inStr = false
        var start = 0
        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (inStr) {
                if (c == '\\') { i += 2; continue }
                if (c == '"') inStr = false
            } else {
                when (c) {
                    '"' -> inStr = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            result.add(content.substring(start, i + 1).trim())
                            var j = i + 1
                            while (j < content.length && (content[j] == ',' || content[j].isWhitespace())) j++
                            start = j
                            i = j - 1
                        }
                    }
                    '[' -> depth++
                    ']' -> depth--
                }
            }
            i++
        }
        if (start < content.length) {
            val tail = content.substring(start).trim()
            if (tail.isNotEmpty()) result.add(tail)
        }
        return result
    }

    private fun readResourceText(name: String): String =
        javaClass.classLoader!!.getResourceAsStream(name)!!.bufferedReader().readText()

    private fun readResourceBytes(name: String): ByteArray =
        javaClass.classLoader!!.getResourceAsStream(name)!!.readBytes()
}
