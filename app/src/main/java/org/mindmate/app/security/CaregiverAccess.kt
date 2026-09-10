package org.mindmate.app.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class CaregiverAccess(context: Context, private val vault: LocalVault = LocalVault()) {
    private val preferences = context.getSharedPreferences("secure_mindmate", Context.MODE_PRIVATE)

    fun hasPin(): Boolean = preferences.contains(KEY_HASH) && preferences.contains(KEY_SALT)

    fun setPin(pin: CharArray): Boolean {
        if (pin.size !in 4..8 || pin.any { !it.isDigit() }) return false
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = derive(pin, salt)
        pin.fill('\u0000')
        preferences.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(vault.encrypt(hash), Base64.NO_WRAP))
            .apply()
        hash.fill(0)
        return true
    }

    fun verify(pin: CharArray): Boolean {
        val salt = preferences.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val encrypted = preferences.getString(KEY_HASH, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val candidate = derive(pin, salt)
        pin.fill('\u0000')
        val expected = runCatching { vault.decrypt(encrypted) }.getOrNull() ?: return false
        val matches = MessageDigest.isEqual(candidate, expected)
        candidate.fill(0)
        expected.fill(0)
        return matches
    }

    private fun derive(pin: CharArray, salt: ByteArray): ByteArray =
        PBEKeySpec(pin, salt, 120_000, 256).let { spec ->
            try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        }

    private companion object {
        const val KEY_SALT = "caregiver_pin_salt"
        const val KEY_HASH = "caregiver_pin_hash"
    }
}
