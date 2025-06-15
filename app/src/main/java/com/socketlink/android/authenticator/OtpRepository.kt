package com.socketlink.android.authenticator

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OtpRepository(application: Application) {

    private val otpDao: OtpDao = AppDatabase.getDatabase(application).otpDao()

    suspend fun getAllOTPs(email : String): List<OtpEntry> {
        return withContext(Dispatchers.IO) {
            otpDao.getOTPsByEmail(email)
        }
    }

    suspend fun insertOtp(otpList: List<OtpEntry>) {
        withContext(Dispatchers.IO) {
            otpDao.insertOtp(otpList)
        }
    }

    suspend fun updateOtp(otp: OtpEntry) {
        withContext(Dispatchers.IO) {
            otpDao.updateOtp(otp)
        }
    }

    suspend fun deleteOtp(otp: OtpEntry) {
        withContext(Dispatchers.IO) {
            otpDao.deleteOtp(otp)
        }
    }
}