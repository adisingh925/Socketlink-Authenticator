package com.socketlink.android.authenticator

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * ViewModel to manage OTP entries and their progress for TOTP codes.
 *
 * @param application Application context to access storage.
 */
class OtpViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Backing StateFlow holding the current list of OTP entries.
     */
    private val _otpEntries = MutableStateFlow<List<OtpEntry>>(emptyList())

    /**
     * Public immutable StateFlow exposing OTP entries.
     */
    val otpEntries: StateFlow<List<OtpEntry>> = _otpEntries

    /**
     * Backing StateFlow holding progress (0f..1f) for each OTP entry identified by
     * a unique key (accountName + issuer).
     */
    private val _progressMap = MutableStateFlow<Map<String, Float>>(emptyMap())

    /**
     * Public immutable StateFlow exposing progress map for OTP entries.
     */
    val progressMap: StateFlow<Map<String, Float>> = _progressMap

    /**
     * Initialization block to load OTP secrets, update OTP codes immediately,
     * and start the periodic ticker for progress and code updates.
     */
    init {
        // Load stored OTP secrets into entries
        loadOtpEntries()

        // Generate OTP codes immediately for initial display
        updateOtpCodes()

        // Start ticker to update progress and OTP codes on period boundary
        startTicker()
    }

    /**
     * Loads OTP secrets from storage and updates the _otpEntries StateFlow.
     * Sets initial codes to empty strings; codes will be generated separately.
     */
    private fun loadOtpEntries() {
        val stored = OtpStorage.loadOtpList(getApplication())
        _otpEntries.value = stored.map {
            OtpEntry(
                id = it.id,
                codeName = it.codeName,
                secret = it.secret,
                code = "", // Will be generated next
                digits = it.digits,
                algorithm = it.algorithm,
                period = it.period
            )
        }
    }

    /**
     * Generates OTP codes for all entries and updates the _otpEntries StateFlow.
     * Should be called on period boundary or during initialization.
     */
    private fun updateOtpCodes() {
        _otpEntries.value = _otpEntries.value.map { otp ->
            otp.copy(code = OtpUtils.generateOtp(otp.secret, otp.digits, otp.algorithm, otp.period))
        }
    }

    /**
     * Starts a coroutine ticker that updates OTP code progress every second,
     * and regenerates OTP codes exactly at each period boundary.
     */
    /**
     * Starts a coroutine ticker that updates OTP codes and progress values continuously.
     * It runs in a loop with a short delay to achieve smooth progress bar animations
     * and ensures OTP codes update exactly at the period boundary.
     */
    private fun startTicker() {
        viewModelScope.launch {
            val lastPeriodMap = mutableMapOf<String, Long>()

            while (true) {
                val now = System.currentTimeMillis()

                val updatedCodes = _otpEntries.value.map { otp ->
                    val periodMillis = otp.period * 1000L
                    val currentPeriod = now / periodMillis
                    val lastPeriod = lastPeriodMap[otp.id] ?: -1L

                    val newCode = if (currentPeriod != lastPeriod) {
                        OtpUtils.generateOtp(otp.secret, otp.digits, otp.algorithm, otp.period)
                    } else {
                        otp.code
                    }

                    lastPeriodMap[otp.id] = currentPeriod

                    otp.copy(code = newCode)
                }

                _otpEntries.value = updatedCodes

                val newProgressMap = updatedCodes.associate { otp ->
                    val periodMillis = otp.period * 1000L
                    val elapsedInPeriod = now % periodMillis
                    val progress = 1f - elapsedInPeriod.toFloat() / periodMillis
                    otp.id to progress.coerceIn(0f, 1f)
                }

                _progressMap.value = newProgressMap

                delay(10L)
            }
        }
    }

    /**
     * Adds a new OTP secret to the stored list, saves it,
     * reloads all OTP entries, and updates OTP codes immediately.
     *
     * @param secret The new OTP secret to add.
     */
    suspend fun addSecret(secret: OtpEntry) {
        // Save the updated list in the background
        withContext(Dispatchers.IO) {
            val all = OtpStorage.loadOtpList(getApplication()) + secret
            OtpStorage.saveOtpList(getApplication(), all)
        }

        // Update codes only if needed
        withContext(Dispatchers.Main) {
            _otpEntries.value = _otpEntries.value + secret
            updateOtpCodes()
        }
    }

    fun deleteSecret(otpToDelete: OtpEntry) {
        // Immediately update the in-memory list for fast UI response
        _otpEntries.value = _otpEntries.value.filterNot { it.id == otpToDelete.id }

        // Launch background coroutine to update persistent storage
        viewModelScope.launch(Dispatchers.IO) {
            val all = OtpStorage.loadOtpList(getApplication())
                .filterNot { it.id == otpToDelete.id }
            OtpStorage.saveOtpList(getApplication(), all)
        }
    }
}



