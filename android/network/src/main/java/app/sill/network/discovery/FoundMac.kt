package app.sill.network.discovery

/**
 * Port of `iOSClient/StreamClient.swift`'s `FoundMac`, adapted to Android: the browser's endpoint
 * becomes host/port and NWInterface becomes an interface name string.
 */
data class FoundMac(
    val name: String,
    val host: String? = null,
    val port: Int = 0,
    val route: FoundMac.Route = FoundMac.Route.network,
    val macID: String? = null,
    val savedByName: String? = null,
    val carriesTag: Boolean = false,
    val method: DiscoveryPolicy.Method? = null,
    val wired: String? = null,
    val wifi: String? = null,
    val door: DiscoveryPolicy.HomeDoor = DiscoveryPolicy.HomeDoor.plain,
    val homeWord: DiscoveryPolicy.RowWord? = null,
) {
    enum class Route { network, direct, remote;
        companion object {
            val Network: Route get() = Route.network
            val Direct: Route get() = Route.direct
            val Remote: Route get() = Route.remote
        }
    }

    val direct: Boolean get() = route == Route.direct
    val savedID: String? get() = macID ?: savedByName
    val id: String get() = if (route == Route.remote) "remote:${macID ?: name}" else "${route.name}:${name}"
    val word: String? get() = if (route == Route.remote) "Remote" else homeWord?.word
        ?: method?.let { m ->
            when (m) {
                DiscoveryPolicy.Method.wired -> "Wired"
                DiscoveryPolicy.Method.wifi -> "Wi\u2011Fi"
                DiscoveryPolicy.Method.direct -> "Direct"
            }
        }
}