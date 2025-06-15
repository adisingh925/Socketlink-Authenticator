package com.socketlink.android.authenticator

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OtpDao {

    /**
     * Inserts a new OTP entry into the database.
     * Replaces any existing entry with the same ID.
     *
     * @param otp The OTP entry to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtp(otpList: List<OtpEntry>)

    /**
     * Updates an existing OTP entry in the database.
     *
     * @param otp The OTP entry with updated values.
     */
    @Update
    suspend fun updateOtp(otp: OtpEntry)

    /**
     * Deletes the given OTP entry from the database.
     *
     * @param otp The OTP entry to delete.
     */
    @Delete
    suspend fun deleteOtp(otp: OtpEntry)

    /**
     * Observes OTP entries associated with the given email address.
     * Automatically emits updates when the data changes.
     *
     * @param email The email address to filter by.
     * @return A Flow of OTP entries matching the email.
     */
    @Query("SELECT * FROM otp_entries WHERE email = :email")
    suspend fun getOTPsByEmail(email: String): List<OtpEntry>
}