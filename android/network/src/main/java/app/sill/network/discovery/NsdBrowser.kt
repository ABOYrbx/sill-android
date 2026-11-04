package app.sill.network.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/**
 * Wraps [NsdManager.discoverServices] for the `_sill._tcp.` service type, resolving each found
 * service to host/port/TXT and reporting it through [listener].
 *
 * Usage:
 *   val browser = NsdBrowser(context)
 *   browser.start(object : NsdBrowser.Listener { ... })
 *   ...
 *   browser.stop()
 */
class NsdBrowser(private val context: Context) {

    interface Listener {
        fun onServiceFound(service: ResolvedService)
        fun onServiceLost(serviceName: String)
        fun onError(message: String)
    }

    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var listener: Listener? = null
    @Volatile
    private var running = false

    fun start(listener: Listener) {
        if (running) return
        this.listener = listener
        val dl = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "discovery started: $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "service found: ${serviceInfo.serviceName}")
                resolve(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "service lost: ${serviceInfo.serviceName}")
                listener.onServiceLost(serviceInfo.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                val msg = "start discovery failed: $serviceType ($errorCode)"
                Log.e(TAG, msg)
                listener.onError(msg)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                val msg = "stop discovery failed: $serviceType ($errorCode)"
                Log.e(TAG, msg)
                listener.onError(msg)
            }
        }
        discoveryListener = dl
        running = true
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, dl)
    }

    private fun resolve(serviceInfo: NsdServiceInfo) {
        val rl = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                val msg = "resolve failed: ${serviceInfo.serviceName} ($errorCode)"
                Log.e(TAG, msg)
                listener?.onError(msg)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "resolved: ${serviceInfo.serviceName} ${serviceInfo.host}:${serviceInfo.port}")
                listener?.onServiceFound(ResolvedService.from(serviceInfo))
            }
        }
        try {
            nsdManager.resolveService(serviceInfo, rl)
        } catch (e: Exception) {
            listener?.onError("resolve exception: ${e.message}")
        }
    }

    fun stop() {
        if (!running) return
        running = false
        discoveryListener?.let { dl ->
            try {
                nsdManager.stopServiceDiscovery(dl)
            } catch (e: Exception) {
                Log.e(TAG, "stop discovery exception: ${e.message}")
            }
        }
        discoveryListener = null
        listener = null
    }

    companion object {
        const val SERVICE_TYPE = "_sill._tcp."
        private const val TAG = "NsdBrowser"
    }
}