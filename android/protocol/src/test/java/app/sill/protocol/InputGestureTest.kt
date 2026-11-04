package app.sill.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class InputGestureTest {
    @Test
    fun inputRoundTripAllKinds() {
        val cases = listOf(
            InputEvent.Pointer(PointerAction.LeftDown, 0.25, 0.75),
            InputEvent.Scroll(0.1, 0.2, 0.3, -0.4),
            InputEvent.Text("hi\n"),
            InputEvent.ScrollGesture(ScrollPhase.MomentumBegan, 0.5, 0.5),
            InputEvent.Key(40, true, 65536L),
        )
        for (e in cases) {
            val json = e.toJson()
            val decoded = InputEvent.fromJson(json)
            assertEquals(e, decoded)
        }
    }

    @Test
    fun gestureRoundTrip() {
        val g = TrackpadGesture(TrackpadGesture.SwipeUp, 3)
        val decoded = TrackpadGesture.fromJson(g.toJson())
        assertEquals(g, decoded)
        val g2 = TrackpadGesture(TrackpadGesture.Pinch)
        assertEquals(g2, TrackpadGesture.fromJson(g2.toJson()))
    }

    @Test
    fun scrollDirectionFactor() {
        assertEquals(-1.0, ScrollDirection.factor(ScrollDirection.Source.Finger, true), 0.0)
        assertEquals(1.0, ScrollDirection.factor(ScrollDirection.Source.Trackpad, true), 0.0)
    }
}