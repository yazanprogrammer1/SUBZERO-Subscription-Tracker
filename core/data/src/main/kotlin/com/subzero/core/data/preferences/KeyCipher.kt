package com.subzero.core.data.preferences

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Protects the one credential SUBZERO stores. Null out means "could not", never "in the clear". */
interface KeyCipher {
    /** Returns the envelope to store, or null when this device cannot encrypt. */
    fun encrypt(plaintext: String): String?

    /** Returns the key, or null when the envelope is unreadable (restored backup, rotated key). */
    fun decrypt(stored: String): String?
}

/**
 * Encrypts the assistant API key with a key that lives in the Android Keystore, so the stored
 * envelope is useless on another device and unreadable from a backup. Everything else SUBZERO
 * stores is the user's own data; this one value is a credential, so it gets stronger treatment.
 *
 * Format: `base64(iv):base64(ciphertext)`. Anything that fails to decrypt — a restored backup, a
 * Keystore entry invalidated by a lock-screen change — reads back as null, which the caller treats
 * as "no key configured" rather than crashing.
 */
@Singleton
class AndroidKeyStoreCipher @Inject constructor() : KeyCipher {

    override fun encrypt(plaintext: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val encrypted = cipher.doFinal(plaintext.toByteArray())
        "${cipher.iv.encode()}$SEPARATOR${encrypted.encode()}"
    }.getOrNull()

    override fun decrypt(stored: String): String? = runCatching {
        val (iv, body) = stored.split(SEPARATOR).takeIf { it.size == 2 } ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv.decode()))
        }
        String(cipher.doFinal(body.decode()))
    }.getOrNull()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.decode(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "subzero.ai.key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = ":"
        const val TAG_BITS = 128
        const val KEY_BITS = 256
    }
}

/** Thrown when a key cannot be stored safely, so the UI can say so instead of silently losing it. */
class KeyStorageException(message: String) : IllegalStateException(message)
