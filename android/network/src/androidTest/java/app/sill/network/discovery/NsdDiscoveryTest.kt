package app.sill.network.discovery

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class NsdDiscoveryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var advertiser: NsdAdvertiser? = null
    private var browser: NsdBrowser? = null
    private val serviceName = "SillTest-${UUID.randomUUID()}"

    @Before
    fun setUp() {
        Log.d("NsdDiscoveryTest", "setUp start")
        advertiser = NsdAdvertiser(context, serviceName, 45678, mapOf("r" to "test"))
        advertiser!!.start()
        Thread.sleep(1000)
        Log.d("NsdDiscoveryTest", "setUp done")
    }

    @After
    fun tearDown() {
        try { browser?.stop() } catch (_: Exception) {}
        try { advertiser?.stop() } catch (_: Exception) {}
    }

    @Test
    fun discoversRegisteredService() {
        val latch = CountDownLatch(1)
        var found: ResolvedService? = null
        val b = NsdBrowser(context)
        browser = b
        b.start(object : NsdBrowser.Listener {
            override fun onServiceFound(service: ResolvedService) {
                Log.d("NsdDiscoveryTest", "found: ${service.serviceName} ${service.host}:${service.port} ${service.txt}")
                if (service.serviceName.startsWith("SillTest-")) {
                    found = service
                    latch.countDown()
                }
            }
            override fun onServiceLost(serviceName: String) {
                Log.d("NsdDiscoveryTest", "lost: $serviceName")
            }
            override fun onError(message: String) {
                Log.e("NsdDiscoveryTest", "err: $message")
            }
        })
        val ok = latch.await(30, TimeUnit.SECONDS)
        assertTrue("service not discovered within timeout (found=$found)", ok)
        assertTrue("wrong port", found?.port == 45678)
        assertTrue("txt missing", found?.txt?.get("r") == "test")
    }
}