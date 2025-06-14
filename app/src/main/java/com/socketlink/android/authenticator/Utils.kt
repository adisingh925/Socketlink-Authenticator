package com.socketlink.android.authenticator

import android.content.Context
import java.util.UUID
import androidx.core.content.edit
import java.security.MessageDigest

object Utils {
    private const val PREF_NAME = "prefs"
    private const val KEY_USER_ID = "unique_user_id"
    private const val APP_LOCK_KEY = "app_lock_enabled"
    private const val UNLOCK_OPTION_KEY = "unlock_option"
    private const val CAMERA_PERMISSION_REQUESTED_KEY = "camera_permission_requested"
    private const val MULTI_DEVICE_SYNC_KEY = "multi_device_sync_enabled"
    private const val CLOUD_SYNC_ENABLED_KEY = "cloud_sync_enabled"
    private const val SYNCING_KEY = "syncing_key"
    private const val DEFAULT_UNLOCK_OPTION = 0

    fun getOrCreateUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val existingId = prefs.getString(KEY_USER_ID, null)
        if (existingId != null) {
            return existingId
        }

        val newId = UUID.randomUUID().toString()
        prefs.edit { putString(KEY_USER_ID, newId) }
        return newId
    }

    fun sha256(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /** Get whether app lock is enabled */
    fun isAppLockEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(APP_LOCK_KEY, false)
    }

    /** Set whether app lock is enabled */
    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(APP_LOCK_KEY, enabled)
        }
    }

    /** Get selected unlock option */
    fun getUnlockOption(context: Context): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(UNLOCK_OPTION_KEY, DEFAULT_UNLOCK_OPTION)
    }

    /** Set unlock option */
    fun setUnlockOption(context: Context, option: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putInt(UNLOCK_OPTION_KEY, option)
        }
    }

    /** Get whether camera permission was requested before */
    fun isCameraPermissionRequested(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(CAMERA_PERMISSION_REQUESTED_KEY, false)
    }

    /** Set camera permission requested flag */
    fun setCameraPermissionRequested(context: Context, requested: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(CAMERA_PERMISSION_REQUESTED_KEY, requested)
        }
    }

    /** Get whether multi-device sync is enabled */
    fun isMultiDeviceSyncEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(MULTI_DEVICE_SYNC_KEY, true)
    }

    /** Set whether multi-device sync is enabled */
    fun setMultiDeviceSyncEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(MULTI_DEVICE_SYNC_KEY, enabled)
        }
    }

    fun isCloudSyncEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(CLOUD_SYNC_ENABLED_KEY, true)
    }

    fun setCloudSyncEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(CLOUD_SYNC_ENABLED_KEY, enabled)
        }
    }

    fun getSyncingKey(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(SYNCING_KEY, "") ?: ""
    }

    fun setSyncingKey(context: Context, key: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(SYNCING_KEY, key)
        }
    }
}
