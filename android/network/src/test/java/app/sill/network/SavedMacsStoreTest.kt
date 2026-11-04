package app.sill.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedMacsStoreTest {
    @Test
    fun roundTrip() {
        val store = SavedMacsStore(InMemorySavedMacsListStorage(), InMemorySavedMacsKeyStorage())
        val mac = SavedMac(macID = "ABC", name = "Mac mini", lastRoute = "Tailscale", group = "g")
        store.save(listOf(mac))
        val loaded = store.load()
        assertEquals(1, loaded.size)
        assertEquals(mac, loaded[0])
        assertEquals("Tailscale", loaded[0].lastRoute)
        assertEquals("g", loaded[0].group)
    }

    @Test
    fun savedMacsObjectRoundTrip() {
        val mac = SavedMac(macID = "ABC", name = "Mac mini")
        val text = SavedMacs.encode(listOf(mac))
        val loaded = SavedMacs.decode(text)
        assertEquals(listOf(mac), loaded)
    }

    @Test
    fun keysRoundTrip() {
        val storage = InMemorySavedMacsKeyStorage()
        val store = SavedMacsStore(InMemorySavedMacsListStorage(), storage)
        store.saveKey("ABC", "secret-key")
        assertEquals("secret-key", store.loadKey("ABC"))
        assertTrue(store.hasKey("ABC"))
        store.deleteKey("ABC")
        assertEquals(null, store.loadKey("ABC"))
    }

    @Test
    fun emptyRoundTrip() {
        val store = SavedMacsStore(InMemorySavedMacsListStorage(), InMemorySavedMacsKeyStorage())
        assertEquals(emptyList<SavedMac>(), store.load())
        store.save(emptyList())
        assertEquals(emptyList<SavedMac>(), store.load())
    }
}
