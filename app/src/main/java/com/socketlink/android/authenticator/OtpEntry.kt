package com.socketlink.android.authenticator

import java.util.UUID

data class OtpEntry(
    val id: String = UUID.randomUUID().toString(),
    val codeName: String,
    val secret: String,
    val code: String,
    val digits : Int = 6,
    val period : Int = 30,
    val algorithm : String = "SHA1",
)



