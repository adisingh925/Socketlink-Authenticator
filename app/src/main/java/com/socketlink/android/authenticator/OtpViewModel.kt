package com.socketlink.android.authenticator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel to manage OTP entries and their progress for TOTP codes.
 *
 * @param application Application context to access storage.
 */
class OtpViewModel(application: Application) : AndroidViewModel(application) {

    private val _otpEntries = MutableStateFlow<List<OtpEntry>>(emptyList())
    val otpEntries: StateFlow<List<OtpEntry>> = _otpEntries

    private val _progressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progressMap: StateFlow<Map<String, Float>> = _progressMap

    init {
        // Initialize DataStore securely
        OtpStorage.initialize(application.applicationContext)

        // Load data and start ticker
        viewModelScope.launch {
            loadOtpEntries()
            updateOtpCodes()
            startTicker()
        }
    }

    /**
     * Loads OTP secrets from storage and updates the _otpEntries StateFlow.
     */
    private suspend fun loadOtpEntries() {
        val stored = OtpStorage.loadOtpList()
        _otpEntries.value = stored.map {
            it.copy(code = "") // empty code for now
        }
    }

    /**
     * Generates OTP codes for all entries.
     */
    private fun updateOtpCodes() {
        _otpEntries.value = _otpEntries.value.map { otp ->
            otp.copy(code = OtpUtils.generateOtp(otp.secret, otp.digits, otp.algorithm, otp.period))
        }
    }

    /**
     * Periodically updates OTP codes and progress.
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
     * Adds a new OTP entry and saves it securely.
     */
    fun addSecret(secret: OtpEntry) {
        val newEntry = secret.copy(
            code = OtpUtils.generateOtp(
                secret.secret,
                secret.digits,
                secret.algorithm,
                secret.period
            )
        )
        _otpEntries.value = _otpEntries.value + newEntry

        viewModelScope.launch(Dispatchers.IO) {
            val all = OtpStorage.loadOtpList() + newEntry
            OtpStorage.saveOtpList(all)
        }
    }


    /**
     * Deletes the given OTP and updates storage.
     */
    fun deleteSecret(otpToDelete: OtpEntry) {
        _otpEntries.value = _otpEntries.value.filterNot { it.id == otpToDelete.id }

        viewModelScope.launch {
            val all = OtpStorage.loadOtpList().filterNot { it.id == otpToDelete.id }
            OtpStorage.saveOtpList(all)
        }
    }
}




