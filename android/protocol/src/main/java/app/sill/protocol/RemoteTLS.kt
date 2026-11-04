package app.sill.protocol

/**
 * Port of constants from `Sources/StreamProtocol/RemoteTLS.swift`.
 */
object RemoteTLS {
    const val sessionALPN = "sill/1"
    const val pairingALPN = "sill-pair/1"
    val serverALPNs = listOf(sessionALPN, pairingALPN)
    const val serverName = "sill"
    const val dialTimeout = 10
}