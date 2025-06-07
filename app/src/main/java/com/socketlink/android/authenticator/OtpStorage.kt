package com.socketlink.android.authenticator

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

object OtpStorage {
    private const val PREF_NAME = "otp_prefs"
    private const val KEY_DATA = "otp_list"

    fun saveOtpList(context: Context, otpList: List<OtpEntry>) {
        val gson = Gson()
        val json = gson.toJson(otpList)
        val prefs = encryptedPrefs(context)
        prefs.edit { putString(KEY_DATA, json) }
    }

    fun loadOtpList(context: Context): List<OtpEntry> {
        val prefs = encryptedPrefs(context)
        val json = prefs.getString(KEY_DATA, null) ?: return emptyList()
        val type = object : TypeToken<List<OtpEntry>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun encryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
