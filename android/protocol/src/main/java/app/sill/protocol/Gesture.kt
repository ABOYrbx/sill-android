package app.sill.protocol

/**
 * Device -> host (kind 28): a three- or four-finger gesture.
 *
 * Port of `Sources/StreamProtocol/Gesture.swift`.
 */
data class TrackpadGesture(
    var gesture: String,
    var fingers: Int? = null,
) {
    fun toJson(): String {
        return if (fingers == null) {
            """{"gesture":"$gesture"}"""
        } else {
            """{"fingers":$fingers,"gesture":"$gesture"}"""
        }
    }

    companion object {
        const val SwipeUp = "swipeUp"
        const val SwipeDown = "swipeDown"
        const val SwipeLeft = "swipeLeft"
        const val SwipeRight = "swipeRight"
        const val Pinch = "pinch"
        const val Spread = "spread"

        val Names = listOf(SwipeUp, SwipeDown, SwipeLeft, SwipeRight, Pinch, Spread)

        const val Generation = 1

        fun fromJson(json: String): TrackpadGesture? {
            val s = json.trim()
            val gesture = regexString(s, "gesture") ?: return null
            val fingers = regexInt(s, "fingers")
            return TrackpadGesture(gesture, fingers)
        }

        private fun regexString(s: String, key: String): String? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            return re.find(s)?.groupValues?.get(1)
        }

        private fun regexInt(s: String, key: String): Int? {
            val re = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)")
            return re.find(s)?.groupValues?.get(1)?.toIntOrNull()
        }
    }
}