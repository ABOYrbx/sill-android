package app.sill.protocol

import android.net.nsd.NsdServiceInfo

data class TxtRecord(
    val door: HomeDoorTXT.Door,
    val tag: String? = null,
) {
    val recognitionTag: String? get() = tag
    val r: String? get() = tag
    val doorMode: HomeDoorTXT.Door get() = door
    val homeDoor: HomeDoorTXT.Door get() = door
}

object TxtRecordParser {
    typealias Result = TxtRecord
    typealias Parsed = TxtRecord
    typealias Record = TxtRecord

    // ------------------------------------------------------------------ Map<String, String>

    fun parse(txt: Map<String, String>): TxtRecord =
        TxtRecord(door = door(txt), tag = tag(txt))

    fun door(txt: Map<String, String>): HomeDoorTXT.Door =
        HomeDoorTXT.door(txt)

    fun recognitionTag(txt: Map<String, String>): String? = tag(txt)

    fun tag(txt: Map<String, String>): String? {
        val value = txt["r"] ?: txt["R"]
        return value?.takeIf { it.isNotEmpty() }
    }

    fun r(txt: Map<String, String>): String? = tag(txt)

    fun parseDoor(txt: Map<String, String>): HomeDoorTXT.Door = door(txt)
    fun toDoor(txt: Map<String, String>): HomeDoorTXT.Door = door(txt)
    fun toDoorMode(txt: Map<String, String>): HomeDoorTXT.Door = door(txt)
    fun doorMode(txt: Map<String, String>): HomeDoorTXT.Door = door(txt)
    fun parseTag(txt: Map<String, String>): String? = tag(txt)
    fun parseRecognitionTag(txt: Map<String, String>): String? = tag(txt)

    // ------------------------------------------------------------------ byte maps

    @JvmName("parseBytes")
    fun parse(attributes: Map<String, ByteArray?>): TxtRecord =
        TxtRecord(door = door(attributes), tag = tag(attributes))

    @JvmName("doorBytes")
    fun door(attributes: Map<String, ByteArray?>): HomeDoorTXT.Door =
        HomeDoorTXT.door(toStringMap(attributes))

    @JvmName("recognitionTagBytes")
    fun recognitionTag(attributes: Map<String, ByteArray?>): String? = tag(attributes)

    @JvmName("tagBytes")
    fun tag(attributes: Map<String, ByteArray?>): String? {
        val bytes = attributes["r"] ?: attributes["R"]
        if (bytes == null || bytes.isEmpty()) return null
        val value = String(bytes, Charsets.UTF_8)
        return value.takeIf { it.isNotEmpty() }
    }

    @JvmName("rBytes")
    fun r(attributes: Map<String, ByteArray?>): String? = tag(attributes)

    @JvmName("parseDoorBytes")
    fun parseDoor(attributes: Map<String, ByteArray?>): HomeDoorTXT.Door = door(attributes)
    @JvmName("toDoorBytes")
    fun toDoor(attributes: Map<String, ByteArray?>): HomeDoorTXT.Door = door(attributes)
    @JvmName("toDoorModeBytes")
    fun toDoorMode(attributes: Map<String, ByteArray?>): HomeDoorTXT.Door = door(attributes)
    @JvmName("doorModeBytes")
    fun doorMode(attributes: Map<String, ByteArray?>): HomeDoorTXT.Door = door(attributes)
    @JvmName("parseTagBytes")
    fun parseTag(attributes: Map<String, ByteArray?>): String? = tag(attributes)
    @JvmName("parseRecognitionTagBytes")
    fun parseRecognitionTag(attributes: Map<String, ByteArray?>): String? = tag(attributes)

    // ------------------------------------------------------------------ NsdServiceInfo

    fun parse(serviceInfo: NsdServiceInfo): TxtRecord =
        parse(serviceInfo.attributes ?: emptyMap<String, ByteArray>())

    fun door(serviceInfo: NsdServiceInfo): HomeDoorTXT.Door =
        door(serviceInfo.attributes ?: emptyMap<String, ByteArray>())

    fun recognitionTag(serviceInfo: NsdServiceInfo): String? =
        tag(serviceInfo.attributes ?: emptyMap<String, ByteArray>())

    fun tag(serviceInfo: NsdServiceInfo): String? =
        tag(serviceInfo.attributes ?: emptyMap<String, ByteArray>())

    fun parseDoor(serviceInfo: NsdServiceInfo): HomeDoorTXT.Door = door(serviceInfo)
    fun toDoor(serviceInfo: NsdServiceInfo): HomeDoorTXT.Door = door(serviceInfo)
    fun toDoorMode(serviceInfo: NsdServiceInfo): HomeDoorTXT.Door = door(serviceInfo)
    fun doorMode(serviceInfo: NsdServiceInfo): HomeDoorTXT.Door = door(serviceInfo)
    fun parseTag(serviceInfo: NsdServiceInfo): String? = tag(serviceInfo)
    fun parseRecognitionTag(serviceInfo: NsdServiceInfo): String? = tag(serviceInfo)

    // ------------------------------------------------------------------ helpers

    fun toStringMap(attributes: Map<String, ByteArray?>): Map<String, String> =
        attributes.mapValues { (_, v) ->
            if (v == null || v.isEmpty()) "" else String(v, Charsets.UTF_8)
        }
}