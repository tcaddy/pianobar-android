package com.aistudio.pianobar.kxuvzq.pandora

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets

/**
 * Byte-for-byte replica of libpiano's crypt.c (pianobar 2024.12.21):
 * - Blowfish ECB with NoPadding
 * - Zero padding (0x00) up to 8-byte boundary (PianoEncryptString: calloc + memcpy)
 * - Raw hex decoding and time offset calculation
 */
object PandoraBlowfishCrypto {

    const val PARTNER_USERNAME = "android"
    const val PARTNER_PASSWORD = "AC7IBG09A3DTSYM4R41UJWL07VLN8JI7"
    const val DEVICE_MODEL = "android-generic"
    const val PROTOCOL_VERSION = "5"

    private const val ENCRYPT_KEY = "6#26FRL\$ZWD"
    private const val DECRYPT_KEY = "R=U!LH\$O2B#"
    private const val ALGORITHM = "Blowfish"
    private const val CIPHER_TRANSFORMATION = "Blowfish/ECB/NoPadding"

    /**
     * Mirrors PianoEncryptString in crypt.c:
     * size_t paddedInputLen = (inputLen % 8 == 0) ? inputLen : inputLen + (8-inputLen%8);
     * paddedInput = calloc (paddedInputLen+1, sizeof (*paddedInput));
     * memcpy (paddedInput, s, inputLen);
     * gcry_cipher_encrypt (h, paddedInput, paddedInputLen, NULL, 0);
     */
    @Suppress("GetInstance")
    fun encrypt(plainJson: String): String {
        val inputBytes = plainJson.toByteArray(StandardCharsets.UTF_8)
        val inputLen = inputBytes.size
        val paddedLen = if (inputLen % 8 == 0) inputLen else inputLen + (8 - (inputLen % 8))
        val paddedInput = ByteArray(paddedLen) // Java initializes ByteArrays to 0x00 (calloc behavior)
        System.arraycopy(inputBytes, 0, paddedInput, 0, inputLen)

        // Pandora partner protocol requires Blowfish/ECB/NoPadding; do not "upgrade".
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val secretKey = SecretKeySpec(ENCRYPT_KEY.toByteArray(StandardCharsets.UTF_8), ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(paddedInput)
        return bytesToHex(encryptedBytes)
    }

    /**
     * Mirrors PianoDecryptString in crypt.c
     */
    @Suppress("GetInstance")
    fun decrypt(cipherHex: String): String {
        val rawBytes = hexToBytes(cipherHex)
        // Pandora partner protocol requires Blowfish/ECB/NoPadding; do not "upgrade".
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val secretKey = SecretKeySpec(DECRYPT_KEY.toByteArray(StandardCharsets.UTF_8), ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decryptedBytes = cipher.doFinal(rawBytes)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    /**
     * Mirrors response.c in libpiano:
     * Drops the first 4 bytes of garbage, extracts only the digits of the timestamp.
     */
    @Suppress("GetInstance")
    fun decryptSyncTime(syncTimeHex: String): Long {
        val rawBytes = hexToBytes(syncTimeHex)
        // Pandora partner protocol requires Blowfish/ECB/NoPadding; do not "upgrade".
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val secretKey = SecretKeySpec(DECRYPT_KEY.toByteArray(StandardCharsets.UTF_8), ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decryptedBytes = cipher.doFinal(rawBytes)

        val digits = StringBuilder()
        for (i in 4 until decryptedBytes.size) {
            val c = decryptedBytes[i].toInt().toChar()
            if (c.isDigit()) {
                digits.append(c)
            } else if (digits.isNotEmpty()) {
                break
            }
        }
        return digits.toString().toLongOrNull() ?: (System.currentTimeMillis() / 1000L)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
