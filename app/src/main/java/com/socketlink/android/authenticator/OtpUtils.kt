package com.socketlink.android.authenticator

import android.util.Log
import androidx.core.net.toUri
import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object OtpUtils {

    fun generateOtp(secret: String, digits: Int = 6, algorithm: String = "SHA1", period : Int): String {
        return try {
            val secretBytes = Base32().decode(secret)// Base32 decoding (standard in OTP secrets)

            val time = System.currentTimeMillis() / 1000 / period
            val data = ByteBuffer.allocate(8).putLong(time).array()

            val algo = when (algorithm.uppercase()) {
                "SHA256" -> "HmacSHA256"
                "SHA512" -> "HmacSHA512"
                else -> "HmacSHA1"
            }

            val mac = Mac.getInstance(algo)
            mac.init(SecretKeySpec(secretBytes, algo))
            val hash = mac.doFinal(data)

            val offset = hash.last().toInt() and 0x0F
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val otp = (binary % 10.0.pow(digits).toInt()).toString().padStart(digits, '0')
            otp
        } catch (e: Exception) {
            "Invalid Secret"
        }
    }

    /**
     * Parses an OTP Auth URI and returns a populated [OtpEntry] object.
     *
     * This supports TOTP URIs following the format:
     * otpauth://totp/Issuer:AccountName?secret=SECRET&issuer=Issuer&digits=6&period=30&algorithm=SHA1
     *
     * @param uriString The raw URI string from a scanned QR code.
     * @return A valid [OtpEntry] object if the URI is correctly formatted, or null otherwise.
     */
    fun parseOtpAuthUri(uriString: String): OtpEntry? {
        return try {
            val uri = uriString.toUri()

            /** Check for the correct scheme: otpauth:// */
            if (uri.scheme != "otpauth") return null

            /** Ensure the URI is for TOTP (Time-based One-Time Passwords) */
            val type = uri.host ?: return null
            if (type != "totp") return null

            /** Extract the label path: usually "issuer:accountName" or just "accountName" */
            val path = uri.path ?: return null
            val label = path.trimStart('/')

            /** Split the label into issuer and accountName if possible */
            val (issuerFromLabel, accountName) = if (label.contains(":")) {
                val parts = label.split(":", limit = 2)
                parts[0] to parts[1]
            } else {
                "" to label
            }

            /** Extract query parameters */
            val secret = uri.getQueryParameter("secret") ?: return null
            val issuer = uri.getQueryParameter("issuer") ?: issuerFromLabel
            val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
            val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30
            val algorithm = uri.getQueryParameter("algorithm") ?: "SHA1"

            /** Create a single display name */
            val codeName = when {
                issuer.isNotBlank() && accountName.isNotBlank() && issuer != accountName -> "$issuer · $accountName"
                issuer.isNotBlank() -> issuer
                else -> accountName
            }

            Log.d("codeName", codeName)

            /** Return the parsed OtpEntry object */
            OtpEntry(
                codeName = codeName,
                secret = secret,
                code = "", // Will be generated separately
                digits = digits,
                period = period,
                algorithm = algorithm
            )
        } catch (e: Exception) {
            /** Return null on failure (invalid format, parsing error, etc.) */
            null
        }
    }
}