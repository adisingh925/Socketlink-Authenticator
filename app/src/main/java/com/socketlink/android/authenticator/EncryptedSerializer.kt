package com.socketlink.android.authenticator

import androidx.datastore.core.Serializer
import com.google.crypto.tink.Aead
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class EncryptedSerializer<T>(
    private val delegate: Serializer<T>,
    private val authenticatedEncryptionWithAssociatedData: Aead
) : Serializer<T> {

    override val defaultValue: T = delegate.defaultValue

    override suspend fun readFrom(input: InputStream): T {
        val encrypted = input.readBytes()
        val decrypted = authenticatedEncryptionWithAssociatedData.decrypt(encrypted, null)
        return delegate.readFrom(decrypted.inputStream())
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        val plain = ByteArrayOutputStream()
        delegate.writeTo(t, plain)
        val encrypted = authenticatedEncryptionWithAssociatedData.encrypt(plain.toByteArray(), null)
        output.write(encrypted)
    }
}