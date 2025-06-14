package com.socketlink.android.authenticator

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class OtpEntry(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

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



