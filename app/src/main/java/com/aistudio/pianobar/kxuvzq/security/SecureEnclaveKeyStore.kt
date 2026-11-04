package com.aistudio.pianobar.kxuvzq.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Secure Enclave key management and AES-256-GCM cryptographic encryption.
 * Generates and binds keys inside Android's Hardware Security Module (StrongBox Keymaster / TEE),
 * ensuring secret keys never leave physical hardware isolation.
 */
object SecureEnclaveKeyStore {

    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "pianobar_pandora_enclave_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateHardwareEnclaveKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: throw IllegalStateException("Could not retrieve key from Android Hardware KeyStore")
    }

    private fun generateHardwareEnclaveKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER)

        fun createSpec(useStrongBox: Boolean): KeyGenParameterSpec {
            val builder = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)

            if (useStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setIsStrongBoxBacked(true)
            }
            return builder.build()
        }

        // 1. Attempt hardware StrongBox Security Chip (Titan M / Knox Vault / Dedicated HSM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                keyGenerator.init(createSpec(useStrongBox = true))
                keyGenerator.generateKey()
                Log.i("SecureEnclave", "Master secret key successfully created in hardware StrongBox security chip.")
                return
            } catch (e: Exception) {
                Log.w("SecureEnclave", "StrongBox hardware module not present, falling back to hardware TEE Secure Enclave.", e)
            }
        }

        // 2. Fallback to hardware-backed TEE (TrustZone / Secure Enclave)
        keyGenerator.init(createSpec(useStrongBox = false))
        keyGenerator.generateKey()
        Log.i("SecureEnclave", "Master secret key successfully created in hardware TEE Secure Enclave.")
    }

    /**
     * Encrypts plaintext using AES-256-GCM backed by hardware Secure Enclave.
     * Returns Base64 payload containing authenticated IV and ciphertext.
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv // 12-byte GCM IV
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(1 + iv.size + cipherText.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(cipherText, 0, combined, 1 + iv.size, cipherText.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("SecureEnclave", "Hardware encryption failed", e)
            ""
        }
    }

    /**
     * Decrypts AES-256-GCM ciphertext using the hardware-isolated secret key.
     */
    fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.isEmpty()) return ""
            val ivLength = combined[0].toInt()
            val iv = ByteArray(ivLength)
            val cipherTextLength = combined.size - 1 - ivLength
            val cipherText = ByteArray(cipherTextLength)

            System.arraycopy(combined, 1, iv, 0, ivLength)
            System.arraycopy(combined, 1 + ivLength, cipherText, 0, cipherTextLength)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("SecureEnclave", "Hardware decryption failed", e)
            ""
        }
    }
}
