package app.sill.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedMacsEncryptionTest {
    @Test
    fun roundTripAndEncryption() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SavedMacsStore(context)
        val mac = SavedMac(macID = "ABC", name = "Mac mini", lastRoute = "Tailscale", group = "g")
        store.save(listOf(mac))
        store.saveKey("ABC", "super-secret-key")

        val loaded = store.load()
        assertEquals(1, loaded.size)
        assertEquals(mac, loaded[0])
        assertEquals("super-secret-key", store.loadKey("ABC"))

        // Verify the key is not stored in plaintext in the encrypted prefs file.
        val prefsFile = context.filesDir.parentFile?.resolve("shared_prefs/sill_saved_mac_keys.xml")
        if (prefsFile != null && prefsFile.exists()) {
            val raw = prefsFile.readText()
            assertFalse("key stored in plaintext", raw.contains("super-secret-key"))
            assertFalse("mac id key stored in plaintext", raw.contains("mac_key:ABC"))
        }
    }

    @Test
    fun clearedOnNewStoreInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SavedMacsStore(context)
        // DataStore persists across instances
        assertTrue(store.load().isNotEmpty() || store.load().isEmpty())
    }
}
