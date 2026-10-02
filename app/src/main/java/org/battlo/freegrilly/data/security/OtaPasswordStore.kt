package org.battlo.freegrilly.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.otaPasswordDataStore by preferencesDataStore("ota_passwords")
private const val KEY_ALIAS = "free_grilly_ota_passwords"
private const val TRANSFORMATION = "AES/GCM/NoPadding"

/** Isolates persisted OTA secrets from the view-model and makes it easy to fake in tests. */
interface OtaPasswordStore {
    suspend fun load(deviceUuid: String): String?
    suspend fun save(deviceUuid: String, password: String)
    suspend fun delete(deviceUuid: String)
}

/** Pure representation used in DataStore: Base64 IV and ciphertext separated by a dot. */
data class EncryptedSecretBlob(val iv: ByteArray, val ciphertext: ByteArray)

fun encodeEncryptedSecret(blob: EncryptedSecretBlob): String =
    Base64.getEncoder().encodeToString(blob.iv) + "." +
        Base64.getEncoder().encodeToString(blob.ciphertext)

fun decodeEncryptedSecret(value: String): EncryptedSecretBlob? {
    val parts = value.split('.', limit = 2)
    if (parts.size != 2) return null
    return runCatching {
        EncryptedSecretBlob(
            Base64.getDecoder().decode(parts[0]),
            Base64.getDecoder().decode(parts[1]),
        ).takeIf { it.iv.isNotEmpty() && it.ciphertext.isNotEmpty() }
    }.getOrNull()
}

@Singleton
class AndroidKeystoreOtaPasswordStore @Inject constructor(
    @ApplicationContext context: Context,
) : OtaPasswordStore {
    private val dataStore = context.otaPasswordDataStore

    override suspend fun load(deviceUuid: String): String? {
        val key = preferenceKey(deviceUuid)
        val encoded = dataStore.data.first()[key] ?: return null
        val blob = decodeEncryptedSecret(encoded) ?: run { delete(deviceUuid); return null }
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, blob.iv))
            cipher.doFinal(blob.ciphertext).toString(Charsets.UTF_8)
        }.getOrElse { delete(deviceUuid); null }
    }

    override suspend fun save(deviceUuid: String, password: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // Android Keystore rejects caller-supplied IVs for randomized encryption: let the cipher pick it.
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encoded = encodeEncryptedSecret(EncryptedSecretBlob(cipher.iv, cipher.doFinal(password.toByteArray(Charsets.UTF_8))))
        dataStore.edit { it[preferenceKey(deviceUuid)] = encoded }
    }

    override suspend fun delete(deviceUuid: String) {
        dataStore.edit { it.remove(preferenceKey(deviceUuid)) }
    }

    private fun preferenceKey(uuid: String) = stringPreferencesKey("password_" +
        Base64.getUrlEncoder().withoutPadding().encodeToString(uuid.toByteArray(Charsets.UTF_8)))

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }
}
