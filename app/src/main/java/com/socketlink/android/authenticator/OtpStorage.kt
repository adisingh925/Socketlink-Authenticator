package com.socketlink.android.authenticator

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import com.google.crypto.tink.Aead as AuthenticatedEncryptor

/**
 * Singleton object for securely storing OTP entries using encrypted DataStore.
 */
object OtpStorage {

    private const val DATASTORE_FILE_NAME = "otp_entries.pb"
    private const val KEY_SET_PREF_NAME = "otp_datastore_keyset"
    private const val KEY_SET_NAME = "otp_keyset"
    private const val MASTER_KEY_URI = "android-keystore://otp_master_key"

    private lateinit var dataStoreInstance: DataStore<List<OtpEntry>>

    /**
     * Initializes the encrypted DataStore singleton.
     * Must be called once before accessing save/load functions.
     *
     * @param context The Android context.
     */
    fun initialize(context: Context) {
        if (!this::dataStoreInstance.isInitialized) {
            val authenticatedEncryptionWithAssociatedData = getAuthenticatedEncryptor(context)

            dataStoreInstance = DataStoreFactory.create(
                serializer = EncryptedSerializer(
                    OtpListSerializer,
                    authenticatedEncryptionWithAssociatedData
                ),
                produceFile = { context.dataStoreFile(DATASTORE_FILE_NAME) },
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            )
        }
    }

    /**
     * Provides an AEAD primitive for encryption/decryption using Tink.
     */
    private fun getAuthenticatedEncryptor(context: Context): AuthenticatedEncryptor {
        AeadConfig.register()

        val keySetHandle = AndroidKeysetManager.Builder()
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withSharedPref(context, KEY_SET_NAME, KEY_SET_PREF_NAME)
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        return keySetHandle.getPrimitive(AuthenticatedEncryptor::class.java)
    }

    /**
     * Saves the given list of OTP entries securely.
     * This is a blocking call.
     *
     * @param otpList The list of OTP entries to save.
     */
    suspend fun saveOtpList(otpList: List<OtpEntry>) {
        check(::dataStoreInstance.isInitialized) { "OtpStorage is not initialized. Call initialize(context) first." }
        dataStoreInstance.updateData { otpList }
    }

    suspend fun loadOtpList(): List<OtpEntry> {
        check(::dataStoreInstance.isInitialized) { "OtpStorage is not initialized. Call initialize(context) first." }
        return dataStoreInstance.data.first()
    }
}
