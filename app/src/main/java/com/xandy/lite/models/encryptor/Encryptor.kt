package com.xandy.lite.models.encryptor

import android.security.keystore.KeyProperties
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object Encryptor {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LEN = 12        // 96‑bit IV for GCM
    private const val TAG_LEN = 128      // authentication tag length in bits

    // lazy decode the combined Base64 into raw key bytes
    private val KEY_BYTES: ByteArray by lazy {
        val raw = "${PW.one.get()}${PW.two.get()}${PW.three.get()}${PW.four.get()}${PW.five.get()}"
        val padded = raw + "=".repeat((4 - raw.length % 4) % 4)
        val test = Base64.decode(padded)
        test
    }

    private val SECRET_KEY: SecretKey by lazy {
        SecretKeySpec(KEY_BYTES, KeyProperties.KEY_ALGORITHM_AES)

    }

    fun encryptString(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY)
        val iv = cipher.iv                     // 12‑byte random IV auto‑generated
        val cipherText = cipher.doFinal(plain.toByteArray())
        // store iv + ciphertext together:  IV || C
        val combined = ByteArray(iv.size + cipherText.size).apply {
            System.arraycopy(iv, 0, this, 0, iv.size)
            System.arraycopy(cipherText, 0, this, iv.size, cipherText.size)
        }
        return Base64.encode(combined)
    }

    fun decryptString(encoded: String): String {
        val combined = Base64.decode(encoded)
        val iv = combined.copyOfRange(0, IV_LEN)
        val cipherText = combined.copyOfRange(IV_LEN, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LEN, iv)
        cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, spec)
        val plain = cipher.doFinal(cipherText)
        return String(plain)
    }
}