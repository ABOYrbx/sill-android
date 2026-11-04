package app.sill.protocol

/**
 * Reverse Scrolling helper (device side).
 *
 * Port of `Sources/StreamProtocol/ScrollDirection.swift`.
 */
object ScrollDirection {
    enum class Source(val raw: String) {
        Finger("finger"),
        Pad("pad"),
        Trackpad("trackpad"),
        Wheel("wheel");
    }

    fun reversible(source: Source): Boolean =
        source == Source.Finger || source == Source.Pad

    fun factor(source: Source, reversed: Boolean): Double =
        if (reversed && reversible(source)) -1.0 else 1.0
}