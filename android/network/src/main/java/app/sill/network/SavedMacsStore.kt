package app.sill.network

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

interface SavedMacsListStorage {
    fun read(): String?
    fun write(value: String?)
}

interface SavedMacsKeyStorage {
    fun put(key: String, value: String)
    fun get(key: String): String?
    fun remove(key: String)
    fun contains(key: String): Boolean
    fun keys(): Set<String>
}

class InMemorySavedMacsListStorage : SavedMacsListStorage {
    private var value: String? = null
    override fun read(): String? = value
    override fun write(value: String?) { this.value = value }
}

class InMemorySavedMacsKeyStorage : SavedMacsKeyStorage {
    private val map = mutableMapOf<String, String>()
    override fun put(key: String, value: String) { map[key] = value }
    override fun get(key: String): String? = map[key]
    override fun remove(key: String) { map.remove(key) }
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun keys(): Set<String> = map.keys.toSet()
}

class DataStoreSavedMacsListStorage(context: Context) : SavedMacsListStorage {
    private val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { File(context.filesDir, "sill_saved_macs.preferences_pb") })

    override fun read(): String? = runBlocking {
        dataStore.data.first()[KEY]
    }

    override fun write(value: String?) = runBlocking {
        dataStore.edit { prefs ->
            if (value == null) prefs.remove(KEY) else prefs[KEY] = value
        }
        Unit
    }

    companion object {
        private val KEY = stringPreferencesKey("saved_macs_json")
    }
}

class EncryptedSavedMacsKeyStorage(context: Context) : SavedMacsKeyStorage {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            "sill_saved_mac_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun put(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun remove(key: String) { prefs.edit().remove(key).apply() }
    override fun contains(key: String): Boolean = prefs.contains(key)
    override fun keys(): Set<String> = prefs.all.keys
}

class SavedMacsStore(
    private val listStorage: SavedMacsListStorage,
    private val keyStorage: SavedMacsKeyStorage,
) {
    constructor(context: Context) : this(
        DataStoreSavedMacsListStorage(context),
        EncryptedSavedMacsKeyStorage(context),
    )

    // List persistence
    fun load(): List<SavedMac> = SavedMac.decodeList(listStorage.read())
    fun save(macs: List<SavedMac>) { listStorage.write(SavedMac.encodeList(macs)) }

    fun loadMacs(): List<SavedMac> = load()
    fun saveMacs(macs: List<SavedMac>) = save(macs)
    fun getMacs(): List<SavedMac> = load()
    fun setMacs(macs: List<SavedMac>) = save(macs)

    fun clear() { listStorage.write(null) }

    // Key persistence (secrets in EncryptedSharedPreferences)
    fun saveKey(macID: String, key: String) { keyStorage.put(keyName(macID), key) }
    fun loadKey(macID: String): String? = keyStorage.get(keyName(macID))
    fun deleteKey(macID: String) { keyStorage.remove(keyName(macID)) }
    fun hasKey(macID: String): Boolean = keyStorage.contains(keyName(macID))

    fun putKey(macID: String, key: String) = saveKey(macID, key)
    fun getKey(macID: String): String? = loadKey(macID)
    fun removeKey(macID: String) = deleteKey(macID)

    fun saveKeys(keys: Map<String, String>) { keys.forEach { saveKey(it.key, it.value) } }
    fun loadKeys(): Map<String, String> =
        keyStorage.keys().associateWith { keyStorage.get(it) ?: "" }

    private fun keyName(macID: String): String = "mac_key:$macID"
}
