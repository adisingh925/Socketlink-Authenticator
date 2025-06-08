package com.socketlink.android.authenticator

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.gson.Gson
import java.io.InputStream
import java.io.OutputStream

object OtpListSerializer : Serializer<List<OtpEntry>> {
    private val gson = Gson()

    override val defaultValue: List<OtpEntry> = emptyList()

    override suspend fun readFrom(input: InputStream): List<OtpEntry> {
        return try {
            val json = input.bufferedReader().readText()
            gson.fromJson(json, Array<OtpEntry>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            throw CorruptionException("Cannot read OTP list", e)
        }
    }

    override suspend fun writeTo(t: List<OtpEntry>, output: OutputStream) {
        output.bufferedWriter().use { writer ->
            writer.write(gson.toJson(t))
        }
    }
}
