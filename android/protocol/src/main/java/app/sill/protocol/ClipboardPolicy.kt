package app.sill.protocol

import java.util.Base64

/**
 * Port of `Sources/StreamProtocol/ClipboardPolicy.swift`.
 */
object ClipboardPolicy {
    const val generation = 1
    const val maxPayload = StreamMessage.MAX_CLIENT_PAYLOAD
    const val perSecond = 4
    const val lookInterval = 0.25
    const val deviceExpiry = 600.0
    const val chipLifetime = 20.0
    const val activationWindow = 15.0
    const val noteLifetime = 3.0
    const val settleWindow = 1.0

    const val remoteClipboardType = "com.apple.is-remote-clipboard"
    const val ownType = "me.saffer.sill.clipboard"

    fun payload(text: String): ByteArray? {
        if (text.isEmpty() || text.toByteArray(Charsets.UTF_8).size > Clipboard.maxTextBytes) return null
        val data = Clipboard(text = text).toJson().toByteArray(Charsets.UTF_8)
        if (data.isEmpty() || data.size > maxPayload) return null
        return data
    }

    val subscription: ByteArray get() = Clipboard().toJson().toByteArray(Charsets.UTF_8)
    val stopping: ByteArray get() = Clipboard(stop = true).toJson().toByteArray(Charsets.UTF_8)

    enum class HostSkip { Remote, Own, NoText, Empty, TooLong }
    enum class HostDecision { /* placeholder */ }
    // Use sealed? We'll use a simple data holder.
    sealed class Decision {
        data class Send(val data: ByteArray) : Decision()
        data class Skip(val skip: HostSkip) : Decision()
    }

    enum class MacAccess { Allowed, NotAsked, Asks, Denied }

    fun macAccess(raw: Int?): MacAccess {
        return when (raw) {
            null, 2 -> MacAccess.Allowed
            0 -> MacAccess.NotAsked
            3 -> MacAccess.Denied
            else -> MacAccess.Asks
        }
    }

    fun hostLooks(access: MacAccess): Boolean = access == MacAccess.Allowed

    enum class AccessStep { Look, TakeAsSeen, Wait }

    fun accessStep(was: MacAccess?, now: MacAccess): AccessStep {
        if (!hostLooks(now)) return AccessStep.Wait
        return if (was != null && !hostLooks(was)) AccessStep.TakeAsSeen else AccessStep.Look
    }

    enum class Refusal { Undecodable, TooLong, Rate }

    enum class FromDevice { Subscribe, Set, Stop, Refuse }

    // We'll represent FromDevice as sealed class instead.
    sealed class DeviceAsk {
        object Subscribe : DeviceAsk()
        data class Set(val text: String) : DeviceAsk()
        object Stop : DeviceAsk()
        data class Refuse(val refusal: Refusal) : DeviceAsk()
    }

    fun fromDevice(payload: ByteArray): DeviceAsk {
        val clip = Clipboard.fromJson(String(payload, Charsets.UTF_8)) ?: return DeviceAsk.Refuse(Refusal.Undecodable)
        if (clip.stop == true) return DeviceAsk.Stop
        val text = clip.text
        if (text == null || text.isEmpty()) return DeviceAsk.Subscribe
        if (text.toByteArray(Charsets.UTF_8).size > Clipboard.maxTextBytes) return DeviceAsk.Refuse(Refusal.TooLong)
        return DeviceAsk.Set(text)
    }

    fun <T> forward(subscribers: List<T>, sender: T): List<T> =
        subscribers.filter { it != sender }

    fun hostWaits(types: List<String>, firstLook: Double, now: Double): Boolean =
        types.isEmpty() && now - firstLook < settleWindow

    fun hostDecides(
        types: List<String>,
        changeCount: Int,
        ownChangeCount: Int?,
        launchID: String,
        ownMarker: () -> String?,
        text: () -> String?,
    ): Decision {
        if (ownChangeCount != null && ownChangeCount == changeCount) return Decision.Skip(HostSkip.Own)
        if (types.contains(remoteClipboardType)) return Decision.Skip(HostSkip.Remote)
        if (types.contains(ownType) && ownMarker() == launchID) return Decision.Skip(HostSkip.Own)
        val t = text() ?: return Decision.Skip(HostSkip.NoText)
        if (t.isEmpty()) return Decision.Skip(HostSkip.Empty)
        val data = payload(t) ?: return Decision.Skip(HostSkip.TooLong)
        return Decision.Send(data)
    }

    fun countsAgainstRate(ask: DeviceAsk): Boolean =
        ask is DeviceAsk.Subscribe || ask is DeviceAsk.Set

    fun admits(arrivals: List<Double>, now: Double): Pair<Boolean, List<Double>> {
        val recent = arrivals.filter { now - it < 1.0 }
        if (recent.size >= perSecond) return Pair(false, recent)
        return Pair(true, recent + now)
    }

    fun hostReceives(payload: ByteArray, arrivals: List<Double>, now: Double): Pair<DeviceAsk, List<Double>> {
        val ask = fromDevice(payload)
        if (!countsAgainstRate(ask)) return Pair(ask, arrivals)
        val admitted = admits(arrivals, now)
        return if (admitted.first) Pair(ask, admitted.second) else Pair(DeviceAsk.Refuse(Refusal.Rate), admitted.second)
    }

    fun hostTakes(hostClipboard: Int?): Boolean = (hostClipboard ?: 0) >= generation

    enum class Subscription { Subscribe, Stop, None }

    fun deviceSubscription(hostClipboard: Int?, switchOn: Boolean, subscribed: Boolean): Pair<Subscription, Boolean> {
        if (!hostTakes(hostClipboard)) return Pair(Subscription.None, false)
        if (switchOn && !subscribed) return Pair(Subscription.Subscribe, true)
        if (!switchOn && subscribed) return Pair(Subscription.Stop, false)
        return Pair(Subscription.None, subscribed)
    }

    data class Sent(val text: String, val changeCount: Int)

    fun deviceWrites(
        payload: ByteArray,
        switchOn: Boolean,
        hostClipboard: Int?,
        lastSent: Sent?,
        changeCount: Int,
    ): String? {
        if (!switchOn || !hostTakes(hostClipboard)) return null
        val clip = Clipboard.fromJson(String(payload, Charsets.UTF_8)) ?: return null
        val text = clip.text ?: return null
        if (text.isEmpty()) return null
        if (text.toByteArray(Charsets.UTF_8).size > Clipboard.maxTextBytes) return null
        if (lastSent != null && lastSent.text == text && lastSent.changeCount == changeCount) return null
        return text
    }

    fun offersChip(
        connected: Boolean,
        hostClipboard: Int?,
        switchOn: Boolean,
        hasStrings: Boolean,
        changeCount: Int,
        handled: Int?,
    ): Boolean =
        connected && hostTakes(hostClipboard) && switchOn && hasStrings && changeCount != handled

    fun chipCounts(onScreen: Boolean, active: Boolean, voiceOver: Boolean): Boolean =
        onScreen && active && !voiceOver

    fun looksAtSessionStart(activatedAt: Double?, now: Double): Boolean {
        if (activatedAt == null) return false
        return now >= (activatedAt) && now - activatedAt <= activationWindow
    }

    enum class DeviceSend { Send, TooLong, Nothing }

    fun deviceSends(strings: List<String>, connected: Boolean, hostClipboard: Int?, switchOn: Boolean): Pair<DeviceSend, ByteArray?> {
        if (!connected || !hostTakes(hostClipboard) || !switchOn) return Pair(DeviceSend.Nothing, null)
        val text = strings.firstOrNull { it.isNotEmpty() } ?: return Pair(DeviceSend.Nothing, null)
        val data = payload(text) ?: return Pair(DeviceSend.TooLong, null)
        return Pair(DeviceSend.Send, data)
    }

    const val receiptLength = 16

    fun receiptWait(worstRttMs: Int?): Double = maxOf(4.0, 4.0 * (worstRttMs ?: 0) / 1000.0)

    fun pastesOnReceipt(rightAway: Boolean, late: Boolean, streaming: Boolean, hostClipboard: Int?, tourShows: Boolean): Boolean =
        rightAway && !late && streaming && hostTakes(hostClipboard) && !tourShows

    enum class Corner { TopTrailing, BottomTrailing }
    fun chipCorner(landscape: Boolean, linkLineShown: Boolean): Corner =
        if (landscape && !linkLineShown) Corner.TopTrailing else Corner.BottomTrailing

    object Copy {
        val cap: String get() = "${Clipboard.maxTextBytes / 1024} KB"
        fun chipTitle(mac: String): String = "Paste on $mac"
        const val chipSubtitle = "Then press ⌘V there."
        fun chipSpoken(mac: String, device: String): String =
            "Paste on $mac. Sends the text you copied on this $device to $mac’s clipboard. Then press Command-V there."
        const val dismiss = "Dismiss"
        fun chipAnnouncement(mac: String): String = "Paste on $mac is available, near the bar."
        fun pasteHint(mac: String, device: String): String =
            "Sends the text you copied on this $device to $mac’s clipboard."
        fun sent(mac: String): String = "Sent to $mac’s clipboard. Press ⌘V there."
        fun sentSpoken(mac: String): String = "Sent to $mac’s clipboard. Press Command-V there."
        fun tooLong(mac: String): String = "Too long to send: $mac takes up to $cap of text."
        fun notSent(mac: String): String = "Didn’t reach $mac. Try Paste again."
        fun chipSubtitle(rightAway: Boolean): String = if (rightAway) "Where the cursor is." else chipSubtitle
        fun chipSpoken(mac: String, device: String, rightAway: Boolean): String =
            if (rightAway) "Paste on $mac. Pastes the text you copied on this $device where $mac’s cursor is."
            else chipSpoken(mac, device)
        fun pasteHint(mac: String, device: String, rightAway: Boolean): String =
            if (rightAway) "Pastes the text you copied on this $device where $mac’s cursor is." else pasteHint(mac, device)
        fun pasted(mac: String): String = "Pasted on $mac."
        const val switchTitle = "Share Clipboard"
        fun rowTitle(mac: String): String = "Paste on $mac"
        fun rightAwayTitle(mac: String): String = "Paste on $mac Right Away"
        fun footnote(on: Boolean, rightAway: Boolean, mac: String, device: String): String {
            if (on && rightAway) return "Text you copy on $mac is ready to paste on this $device. To paste on $mac, copy text here and tap Paste: it goes where $mac’s cursor is. With Paste on $mac Right Away off, Paste only puts it on $mac’s clipboard."
            return if (on) "Text you copy on $mac is ready to paste on this $device. To paste on $mac, copy text here, tap Paste, then press ⌘V there."
            else "Nothing you copy goes between this $device and $mac while this is off."
        }
        val macFootnote = "Text you copy here can be pasted on a connected iPhone or iPad, and text sent from one with Paste becomes this Mac’s."
        fun macAccessWarning(access: MacAccess): String? = when (access) {
            MacAccess.Allowed -> null
            MacAccess.NotAsked, MacAccess.Asks -> "macOS asks before Sill reads the clipboard, so what you copy here stays on this Mac."
            MacAccess.Denied -> "macOS doesn’t let Sill read the clipboard, so what you copy here stays on this Mac."
        }
        fun macAccessHint(access: MacAccess): String? = when (access) {
            MacAccess.Allowed -> null
            MacAccess.NotAsked -> "Click Allow in System Settings: macOS asks about Sill once, then Privacy & Security opens, where you set Sill to Allow under Paste from Other Apps. Text a device sends with Paste still arrives."
            MacAccess.Asks, MacAccess.Denied -> "Set Sill to Allow under Paste from Other Apps in Privacy & Security. Text a device sends with Paste still arrives."
        }
        const val macAccessButton = "Allow in System Settings…"
        fun macAccessLine(access: MacAccess): String = when (access) {
            MacAccess.Allowed -> "Clipboard: macOS lets Sill read the clipboard again; what is copied on this Mac from now on goes to devices."
            MacAccess.NotAsked, MacAccess.Asks -> "Clipboard: macOS asks before Sill reads the clipboard, so what is copied on this Mac stays here. To send it, allow Sill under Paste from Other Apps in System Settings › Privacy & Security (Sill’s Settings › General has the way there)."
            MacAccess.Denied -> "Clipboard: macOS does not let Sill read the clipboard, so what is copied on this Mac stays here. To send it, allow Sill under Paste from Other Apps in System Settings › Privacy & Security."
        }
        fun sharesLine(device: String): String = "Clipboard: $device shares the clipboard."
        fun stoppedLine(device: String): String = "Clipboard: $device stopped sharing the clipboard."
        fun ignoredLine(rate: Int, tooLong: Int, undecodable: Int): String {
            val parts = mutableListOf<String>()
            if (rate > 0) parts.add("$rate from devices sending more than $perSecond a second")
            if (tooLong > 0) parts.add("$tooLong longer than $cap")
            if (undecodable > 0) parts.add("$undecodable that did not decode")
            return "Clipboard from devices ignored in a minute: " + (if (parts.isEmpty()) "none" else parts.joinToString(", ")) + "."
        }
    }

    // accessLine uses Copy.macAccessLine
    fun accessLine(was: MacAccess?, now: MacAccess): String? {
        if (was == null && now == MacAccess.Allowed) return null
        val line = Copy.macAccessLine(now)
        return if (was != null && Copy.macAccessLine(was) == line) null else line
    }
}