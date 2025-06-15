package com.socketlink.android.authenticator

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "otp_entries")
data class OtpEntry(
    @PrimaryKey
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("email")
    val email: String = "",

    @SerializedName("codeName")
    val codeName: String = "",

    @SerializedName("secret")
    val secret: String = "",

    @SerializedName("code")
    val code: String = "",

    @SerializedName("digits")
    val digits: Int = 6,

    @SerializedName("period")
    val period: Int = 30,

    @SerializedName("algorithm")
    val algorithm: String = "SHA1",

    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)



