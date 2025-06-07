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
     * Time period (in seconds) for each OTP code validity.
     * Typically 30 seconds for TOTP.
     */
    private val period = 30

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
                algorithm = it.algorithm
            )
        }
    }

    /**
     * Generates OTP codes for all entries and updates the _otpEntries StateFlow.
     * Should be called on period boundary or during initialization.
     */
    private fun updateOtpCodes() {
        _otpEntries.value = _otpEntries.value.map { otp ->
            otp.copy(code = OtpUtils.generateOtp(otp.secret, otp.digits, otp.algorithm))
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
            /**
             * Holds the elapsed time in the last tick to detect period boundary wrap-around.
             * Initialized to a very large value so first tick always triggers an update.
             */
            var lastElapsedInPeriod = Long.MAX_VALUE

            while (true) {
                /** Current system time in milliseconds */
                val now = System.currentTimeMillis()

                /** OTP code validity period in milliseconds (e.g., 30 seconds) */
                val periodMillis = period * 1000L

                /**
                 * Calculate elapsed time within the current period
                 * This resets to 0 every time a new period starts
                 */
                val elapsedInPeriod = now % periodMillis

                /**
                 * Calculate the progress as fraction of remaining time in current period,
                 * ranges from 1.0 (just started) to 0.0 (just about to reset)
                 */
                val progress = 1f - elapsedInPeriod.toFloat() / periodMillis

                /**
                 * Detect a new period boundary by checking if elapsed time has wrapped around
                 * This happens when elapsedInPeriod < lastElapsedInPeriod, indicating reset
                 */
                if (elapsedInPeriod < lastElapsedInPeriod) {
                    /**
                     * New period started: regenerate OTP codes for all entries
                     * Updates _otpEntries StateFlow with new codes
                     */
                    val updatedCodes = _otpEntries.value.map { otp ->
                        otp.copy(code = OtpUtils.generateOtp(otp.secret, otp.digits, otp.algorithm))
                    }
                    _otpEntries.value = updatedCodes
                }

                /** Update last elapsed time to current for next iteration */
                lastElapsedInPeriod = elapsedInPeriod

                /**
                 * Create a new map of progress values for each OTP entry
                 * The key is a unique identifier combining accountName and issuer
                 */
                val newProgressMap = _otpEntries.value.associate { otp ->
                    otp.id to progress.coerceIn(0f, 1f)
                }
                /** Update the progress StateFlow */
                _progressMap.value = newProgressMap

                /**
                 * Suspend the coroutine for 10 milliseconds to
                 * allow smooth animation and frequent OTP boundary checks
                 */
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
    fun addSecret(secret: OtpEntry) {
        val all = OtpStorage.loadOtpList(getApplication()) + secret
        OtpStorage.saveOtpList(getApplication(), all)
        loadOtpEntries()
        updateOtpCodes()
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



