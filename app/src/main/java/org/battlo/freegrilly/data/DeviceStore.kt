package org.battlo.freegrilly.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.deviceDataStore: DataStore<Preferences> by preferencesDataStore("device_prefs")

@Singleton
class DeviceStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.deviceDataStore

    companion object {
        val KEY_SELECTED_UUID = stringPreferencesKey("selected_device_uuid")
        val KEY_SELECTED_IP = stringPreferencesKey("selected_device_ip")
        val KEY_KNOWN_DEVICES = stringPreferencesKey("known_devices_json")
        val KEY_UNIT = stringPreferencesKey("temperature_unit")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_DEMO_MODE = booleanPreferencesKey("is_demo_mode")
    }

    val selectedDeviceIp: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_SELECTED_IP] }

    val selectedDeviceUuid: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_SELECTED_UUID] }

    val temperatureUnit: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_UNIT] ?: "celcius" }

    val appLanguage: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LANGUAGE] ?: "system" }

    val isDemoMode: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DEMO_MODE] ?: false }

    val knownDevices: Flow<List<KnownDevice>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val raw = prefs[KEY_KNOWN_DEVICES] ?: return@map emptyList()
            runCatching { Json.decodeFromString<List<KnownDevice>>(raw) }.getOrDefault(emptyList())
        }

    suspend fun setSelectedDevice(device: KnownDevice) {
        dataStore.edit {
            it[KEY_SELECTED_UUID] = device.uuid
            it[KEY_SELECTED_IP] = device.ip
        }
    }

    suspend fun saveKnownDevice(device: KnownDevice) {
        dataStore.edit { prefs ->
            val existing = prefs[KEY_KNOWN_DEVICES]?.let {
                runCatching { Json.decodeFromString<List<KnownDevice>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            val updated = existing.filter { it.uuid != device.uuid } + device
            prefs[KEY_KNOWN_DEVICES] = Json.encodeToString(updated)
        }
    }

    suspend fun removeDevice(uuid: String) {
        dataStore.edit { prefs ->
            val existing = prefs[KEY_KNOWN_DEVICES]?.let {
                runCatching { Json.decodeFromString<List<KnownDevice>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            prefs[KEY_KNOWN_DEVICES] = Json.encodeToString(existing.filter { it.uuid != uuid })
        }
    }

    suspend fun clearSelectedDevice() = dataStore.edit {
        it.remove(KEY_SELECTED_UUID)
        it.remove(KEY_SELECTED_IP)
    }

    suspend fun setTemperatureUnit(unit: String) = dataStore.edit { it[KEY_UNIT] = unit }
    suspend fun setAppLanguage(lang: String) = dataStore.edit { it[KEY_LANGUAGE] = lang }
    suspend fun setDemoMode(enabled: Boolean) = dataStore.edit { it[KEY_DEMO_MODE] = enabled }
}
