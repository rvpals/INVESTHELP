package com.investhelp.app.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordManager @Inject constructor(
    private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "invest_help_auth",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val isSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)

    fun setupPassword(password: String): ByteArray {
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val passphrase = deriveKey(password, salt)

        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PASSPHRASE_HASH, Base64.encodeToString(hashPassphrase(passphrase), Base64.NO_WRAP))
            .putBoolean(KEY_SETUP_COMPLETE, true)
            .apply()

        return passphrase
    }

    fun validateAndGetPassphrase(password: String): ByteArray? {
        val saltStr = prefs.getString(KEY_SALT, null) ?: return null
        val storedHash = prefs.getString(KEY_PASSPHRASE_HASH, null) ?: return null

        val salt = Base64.decode(saltStr, Base64.NO_WRAP)
        val passphrase = deriveKey(password, salt)
        val hash = hashPassphrase(passphrase)

        return if (Base64.encodeToString(hash, Base64.NO_WRAP) == storedHash) {
            passphrase
        } else {
            null
        }
    }

    fun getStoredPassphrase(): ByteArray? {
        val passphraseStr = prefs.getString(KEY_CACHED_PASSPHRASE, null) ?: return null
        return Base64.decode(passphraseStr, Base64.NO_WRAP)
    }

    fun cachePassphrase(passphrase: ByteArray) {
        prefs.edit()
            .putString(KEY_CACHED_PASSPHRASE, Base64.encodeToString(passphrase, Base64.NO_WRAP))
            .apply()
    }

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun hashPassphrase(passphrase: ByteArray): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(passphrase)
    }

    companion object {
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_SALT = "password_salt"
        private const val KEY_PASSPHRASE_HASH = "passphrase_hash"
        private const val KEY_CACHED_PASSPHRASE = "cached_passphrase"
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH = 256
    }
}
