package app.sill.network.discovery

import android.net.nsd.NsdServiceInfo

/**
 * A Sill service resolved from an NSD browse result: its Bonjour name, host address, port and TXT
 * record decoded into a string map.
 */
data class ResolvedService(
    val serviceName: String,
    val serviceType: String,
    val host: String,
    val port: Int,
    val txt: Map<String, String>,
) {
    companion object {
        fun from(info: NsdServiceInfo): ResolvedService {
            val host = info.host?.hostAddress ?: info.host?.hostName ?: ""
            val txt = mutableMapOf<String, String>()
            info.attributes?.forEach { (key, value) ->
                txt[key] = value?.let { String(it, Charsets.UTF_8) } ?: ""
            }
            return ResolvedService(
                serviceName = info.serviceName,
                serviceType = info.serviceType,
                host = host,
                port = info.port,
                txt = txt,
            )
        }
    }
}