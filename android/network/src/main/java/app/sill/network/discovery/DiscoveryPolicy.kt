package app.sill.network.discovery

/**
 * Port of the pure discovery logic in `iOSClient/DiscoveryPolicy.swift`.
 *
 * Only the discovery decisions that do not need Apple frameworks are ported here:
 * rows, sightings, direct-wait (`reconnectRow`), and browser rebuild. Constants mirror the Swift
 * source. KDoc cites the Swift file.
 */
object DiscoveryPolicy {

    // ---- constants ---------------------------------------------------------------

    const val networkFirst: Double = 3.0
    const val memoryCap: Int = 16

    const val directWait: Double = 6.0
    const val networkGrace: Double = 10.0
    const val quitWait: Double = 3.0
    const val moveAfter: Double = 2.0
    const val moveRetry: Double = 10.0

    val browserRetry: List<Double> = listOf(1.0, 2.0, 4.0, 8.0, 16.0, 30.0)
    const val browserStable: Double = 30.0
    const val rebuildSettle: Double = networkFirst

    const val pathHysteresis: Double = 5.0
    const val upBackoffCap: Double = 60.0
    const val cableSettle: Double = 2.0
    const val wifiFresh: Double = 5.0
    const val pongSilence: Double = 1.0

    // ---- nested types -----------------------------------------------------------

    data class Interface(val name: String, val type: Kind) {
        enum class Kind { wifi, wiredEthernet, cellular, loopback, other;
            companion object {
                val Wifi: Kind get() = Kind.wifi
                val WiredEthernet: Kind get() = Kind.wiredEthernet
                val Cellular: Kind get() = Kind.cellular
                val Loopback: Kind get() = Kind.loopback
                val Other: Kind get() = Kind.other
            }
        }
    }

    enum class Method(val word: String) {
        wired("Wired"), wifi("Wi\u2011Fi"), direct("Direct");
        val rank: Int get() = when (this) { wired -> 2; wifi -> 1; direct -> 0 }
        companion object {
            val Wired: Method get() = Method.wired
            val Wifi: Method get() = Method.wifi
            val Direct: Method get() = Method.direct
        }
    }

    // PascalCase aliases are not possible for enum entries; keep lowercase. Provide companion
    // constants? Tests in Kotlin convention use PascalCase enum names usually, but Swift uses
    // lowercase. We keep lowercase to match Swift case names exactly.

    enum class HomeDoor { plain, pairingRequired, open;
        companion object {
            val Plain: HomeDoor get() = HomeDoor.plain
            val PairingRequired: HomeDoor get() = HomeDoor.pairingRequired
            val Open: HomeDoor get() = HomeDoor.open
        }
    }

    sealed class RowWord {
        object NotPaired : RowWord()
        object PairsOverCable : RowWord()
        object OpenDoor : RowWord()
        object UpdateSill : RowWord()
        data class Method(val method: DiscoveryPolicy.Method?) : RowWord()

        companion object {
            val notPaired: RowWord get() = NotPaired
            val pairsOverCable: RowWord get() = PairsOverCable
            val openDoor: RowWord get() = OpenDoor
            val updateSill: RowWord get() = UpdateSill
        }

        val word: String?
            get() = when (this) {
                is NotPaired -> "Not paired"
                is PairsOverCable -> "Wired"
                is OpenDoor -> "Not paired"
                is UpdateSill -> "Update Sill"
                is Method -> method?.let { m ->
                    when (m) {
                        DiscoveryPolicy.Method.wired -> "Wired"
                        DiscoveryPolicy.Method.wifi -> "Wi\u2011Fi"
                        DiscoveryPolicy.Method.direct -> "Direct"
                    }
                }
            }

        fun label(name: String): String = when (this) {
            is NotPaired -> "$name, not paired"
            is PairsOverCable -> "$name, Wired"
            is OpenDoor -> "$name, not paired"
            is UpdateSill -> "$name, Update Sill"
            is Method -> if (method == null) name else "$name, ${method.word}"
        }

        fun hint(name: String, device: String, direct: Boolean): String = when (this) {
            is NotPaired -> "Pairs with a code $name shows, then connects."
            is PairsOverCable -> "Pairs over the USB cable, then connects."
            is OpenDoor -> "Connects without pairing: $name lets any device in."
            is UpdateSill -> "$name\u2019s Sill is too old for this $device."
            is Method -> if (method == DiscoveryPolicy.Method.direct && direct)
                "Connects without a shared Wi\u2011Fi network" else ""
        }
    }

    // ---- rows ------------------------------------------------------------------

    data class Row(val name: String, val direct: Boolean) {
        val first: String get() = name
        val second: Boolean get() = direct
    }

    data class Nearby(val name: String, val interfaces: List<String>)

    fun isPeerToPeer(interfaceName: String): Boolean =
        interfaceName.startsWith("awdl") || interfaceName.startsWith("llw")

    @JvmName("rowsNearby")
    fun rows(network: List<String>, nearby: List<Nearby>): List<Row> =
        rows(network, nearby.map { it.name to it.interfaces })

    @JvmName("rowsPairs")
    fun rows(network: List<String>, nearby: List<Pair<String, List<String>>>): List<Row> {
        val seen = mutableSetOf<String>()
        val out = mutableListOf<Row>()
        for (name in network) {
            if (seen.add(name)) out.add(Row(name, false))
        }
        for (n in nearby) {
            val (name, interfaces) = n
            if (interfaces.isNotEmpty() && interfaces.all { isPeerToPeer(it) } && seen.add(name)) {
                out.add(Row(name, true))
            }
        }
        return out
    }

    // ---- sightings -------------------------------------------------------------

    data class NetworkSightings(
        var since: MutableMap<String, Double> = mutableMapOf(),
        var leftAt: MutableMap<String, Double> = mutableMapOf(),
    )

    fun sightings(previous: NetworkSightings, listed: Set<String>, now: Double): NetworkSightings {
        return sightings(previous, listed as Collection<String>, now)
    }

    fun sightings(previous: NetworkSightings, listed: Collection<String>, now: Double): NetworkSightings {
        val listedSet = listed.toSet()
        val next = NetworkSightings()
        for (name in listedSet) {
            next.since[name] = previous.since[name] ?: now
        }
        for (name in previous.since.keys) {
            if (!listedSet.contains(name)) next.leftAt[name] = now
        }
        for ((name, at) in previous.leftAt) {
            if (!listedSet.contains(name) && !next.leftAt.containsKey(name) && now - at < networkGrace) {
                next.leftAt[name] = at
            }
        }
        return next
    }

    @JvmName("directSinceRows")
    fun directSince(previous: Map<String, Double>, rows: List<Row>, now: Double): Map<String, Double> {
        val next = mutableMapOf<String, Double>()
        for (row in rows) {
            if (row.direct) next[row.name] = previous[row.name] ?: now
        }
        return next
    }

    @JvmName("directSincePairs")
    fun directSince(previous: Map<String, Double>, rows: List<Pair<String, Boolean>>, now: Double): Map<String, Double> {
        val next = mutableMapOf<String, Double>()
        for ((name, direct) in rows) {
            if (direct) next[name] = previous[name] ?: now
        }
        return next
    }

    // ---- direct-wait (reconnectRow) -------------------------------------------

    data class ReconnectResult<out T>(val take: T?, val recheckAt: Double?) {
        val first: T? get() = take
        val second: Double? get() = recheckAt
    }

    fun <T> reconnectRow(
        network: T?,
        networkSince: Double? = null,
        direct: T?,
        directSince: Double?,
        networkLeftAt: Double?,
        quitAt: Double? = null,
        now: Double,
    ): ReconnectResult<T> {
        if (quitAt != null && now < quitAt + quitWait) {
            fun fresh(since: Double?): Boolean = since?.let { it > quitAt } ?: false
            val nFresh = fresh(networkSince)
            val dFresh = fresh(directSince)
            val choice = reconnectRow(
                network = if (nFresh) network else null,
                networkSince = networkSince,
                direct = if (dFresh) direct else null,
                directSince = directSince,
                networkLeftAt = networkLeftAt,
                now = now,
            )
            val heldBack = (network != null && !nFresh) || (direct != null && !dFresh)
            if (choice.take == null && heldBack) {
                val rc = minOf(choice.recheckAt ?: Double.POSITIVE_INFINITY, quitAt + quitWait)
                return ReconnectResult(null, rc)
            }
            return choice
        }
        if (network != null) return ReconnectResult(network, null)
        if (direct == null || directSince == null) return ReconnectResult(null, null)
        var due = directSince + directWait
        if (networkLeftAt != null) due = maxOf(due, networkLeftAt + networkGrace)
        return if (now >= due) ReconnectResult(direct, null) else ReconnectResult(null, due)
    }

    // ---- browser rebuild -------------------------------------------------------

    data class RebuildResult(val rebuild: Boolean, val recheckAt: Double?) {
        val first: Boolean get() = rebuild
        val second: Double? get() = recheckAt
    }

    fun browserRetryDelay(afterFailures: Int): Double {
        val idx = minOf(maxOf(afterFailures, 1), browserRetry.size) - 1
        return browserRetry[idx]
    }

    fun browserFailures(previous: Int, startedAt: Double, now: Double): Int =
        if (now - startedAt >= browserStable) 1 else maxOf(previous, 0) + 1

    fun rebuildBrowser(
        failedAt: Double?,
        failures: Int,
        cameForward: Boolean,
        foreground: Boolean,
        now: Double,
    ): RebuildResult {
        if (cameForward) return RebuildResult(true, null)
        if (!foreground || failedAt == null) return RebuildResult(false, null)
        val due = failedAt + browserRetryDelay(afterFailures = failures)
        return if (now >= due) RebuildResult(true, null) else RebuildResult(false, due)
    }

    fun <R> browserResults(
        kept: List<R>,
        fresh: List<R>,
        name: (R) -> String,
        settleEnds: Double?,
        now: Double,
    ): List<R> {
        if (settleEnds == null || now >= settleEnds) return fresh
        val listed = fresh.map(name).toSet()
        return fresh + kept.filter { !listed.contains(name(it)) }
    }

    // ---- move helpers ----------------------------------------------------------

    data class MoveResult(val move: Boolean, val recheckAt: Double?) {
        val first: Boolean get() = move
        val second: Double? get() = recheckAt
    }

    fun moveToNetwork(
        listedSince: Double?,
        lastAttempt: Double?,
        refusedListing: Double?,
        now: Double,
    ): MoveResult {
        if (listedSince == null || listedSince == refusedListing) return MoveResult(false, null)
        var due = listedSince + moveAfter
        if (lastAttempt != null) due = maxOf(due, lastAttempt + moveRetry)
        return if (now >= due) MoveResult(true, null) else MoveResult(false, due)
    }

    fun moveHome(
        listedSince: Double?,
        lastAttempt: Double?,
        failures: Int,
        now: Double,
    ): MoveResult {
        if (listedSince == null) return MoveResult(false, null)
        var due = listedSince + moveAfter
        if (lastAttempt != null && failures > 0) due = maxOf(due, lastAttempt + upWait(failures = failures))
        return if (now >= due) MoveResult(true, null) else MoveResult(false, due)
    }

    fun upWait(failures: Int): Double =
        minOf(pathHysteresis * (1 shl minOf(maxOf(failures, 0), 4)).toDouble(), upBackoffCap)

    // ---- path sightings (extra) ------------------------------------------------

    data class PathSightings(
        val wired: MutableMap<String, String> = mutableMapOf(),
        val wiredSince: MutableMap<String, Double> = mutableMapOf(),
        val wifi: MutableMap<String, String> = mutableMapOf(),
        val wifiLeft: MutableMap<String, WifiLeft> = mutableMapOf(),
    ) {
        data class WifiLeft(val interfaceName: String, val at: Double)
    }

    // additional alias for Swift WifiLeft interface field
    // (above uses interfaceName; provide compatibility via type alias? no)
}