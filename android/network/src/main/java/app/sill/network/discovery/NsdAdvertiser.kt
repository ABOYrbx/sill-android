package app.sill.network.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/**
 * Registers a Sill service via NSD so other devices (or tests) can discover it.
 */
class NsdAdvertiser(
    private val context: Context,
    private val serviceName: String,
    private val port: Int,
    private val txt: Map<String, String> = emptyMap(),
) {
    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    @Volatile
    private var registered = false

    fun start() {
        if (registered) return
        val info = NsdServiceInfo()
        info.serviceName = serviceName
        info.serviceType = NsdBrowser.SERVICE_TYPE
        info.port = port
        txt.forEach { (k, v) ->
            info.setAttribute(k, v)
        }
        val rl = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "registered: ${serviceInfo.serviceName}")
                registered = true
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "registration failed: ${serviceInfo.serviceName} ($errorCode)")
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "unregistered: ${serviceInfo.serviceName}")
                registered = false
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "unregistration failed: ${serviceInfo.serviceName} ($errorCode)")
            }
        }
        registrationListener = rl
        nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, rl)
    }

    fun stop() {
        registrationListener?.let { rl ->
            try {
                nsdManager.unregisterService(rl)
            } catch (e: Exception) {
                Log.e(TAG, "unregister exception: ${e.message}")
            }
        }
        registrationListener = null
        registered = false
    }

    companion object {
        private const val TAG = "NsdAdvertiser"
    }
}