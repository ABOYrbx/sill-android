package app.sill.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TxtRecordParserTest {
    @Test
    fun plainNoP() {
        assertEquals(HomeDoorTXT.Door.Plain, TxtRecordParser.door(emptyMap<String, String>()))
        assertEquals(HomeDoorTXT.Door.Plain, TxtRecordParser.door(mapOf("r" to "tag")))
        assertEquals(HomeDoorTXT.Door.Plain, TxtRecordParser.parse(mapOf("r" to "tag")).door)
    }

    @Test
    fun pairingRequiredP1() {
        assertEquals(HomeDoorTXT.Door.PairingRequired, TxtRecordParser.door(mapOf("p" to "1")))
    }

    @Test
    fun pairingRequiredOther() {
        for (v in listOf("", "2", "yes", "true", "00", "O")) {
            assertEquals(HomeDoorTXT.Door.PairingRequired, TxtRecordParser.door(mapOf("p" to v)))
        }
    }

    @Test
    fun openP0() {
        assertEquals(HomeDoorTXT.Door.Open, TxtRecordParser.door(mapOf("p" to "0")))
    }

    @Test
    fun caseInsensitiveKeys() {
        assertEquals(HomeDoorTXT.Door.Open, TxtRecordParser.door(mapOf("P" to "0")))
        assertEquals(HomeDoorTXT.Door.PairingRequired, TxtRecordParser.door(mapOf("P" to "1")))
    }

    @Test
    fun missingValuePairingRequired() {
        // p present with empty/missing value reads as pairing required
        assertEquals(HomeDoorTXT.Door.PairingRequired, TxtRecordParser.door(mapOf("p" to "")))
        val bytes: Map<String, ByteArray> = mapOf("p" to ByteArray(0))
        assertEquals(HomeDoorTXT.Door.PairingRequired, TxtRecordParser.door(bytes))
    }

    @Test
    fun recognitionTagParsed() {
        val rec = TxtRecordParser.parse(mapOf("r" to "abc", "p" to "0"))
        assertEquals("abc", rec.tag)
        assertEquals("abc", rec.recognitionTag)
        assertEquals(HomeDoorTXT.Door.Open, rec.door)
    }

    @Test
    fun recognitionTagMissing() {
        assertNull(TxtRecordParser.recognitionTag(emptyMap<String, String>()))
        assertNull(TxtRecordParser.parse(mapOf("p" to "1")).tag)
    }

    @Test
    fun bytesAttributes() {
        val attrs: Map<String, ByteArray> = mapOf(
            "r" to "tag".toByteArray(),
            "p" to "0".toByteArray(),
        )
        val rec = TxtRecordParser.parse(attrs)
        assertEquals("tag", rec.tag)
        assertEquals(HomeDoorTXT.Door.Open, rec.door)
    }

    @Test
    fun barePBytesPairingRequired() {
        val attrs: Map<String, ByteArray> = mapOf("r" to "x".toByteArray(), "p" to ByteArray(0))
        assertEquals(HomeDoorTXT.Door.PairingRequired, TxtRecordParser.door(attrs))
        // bare p not in map value form: simulate missing value by empty string already covered
    }
}