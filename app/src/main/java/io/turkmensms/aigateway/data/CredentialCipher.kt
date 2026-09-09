package io.turkmensms.aigateway.data

import android.content.Context
import android.security.KeyPairGeneratorSpec
import android.util.Base64
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

class CredentialCipher(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        val aes = loadOrCreateAesKey()
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aes, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(payload: String): String {
        if (payload.isEmpty()) return ""
        return try {
            val data = Base64.decode(payload, Base64.NO_WRAP)
            if (data.size < 13) return ""
            val iv = data.copyOfRange(0, 12)
            val cipherBytes = data.copyOfRange(12, data.size)
            val aes = loadOrCreateAesKey()
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, aes, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    @Synchronized
    private fun loadOrCreateAesKey(): SecretKey {
        val wrapped = prefs.getString(WRAPPED_AES, null)
        if (wrapped != null) {
            val raw = unwrapAesKey(Base64.decode(wrapped, Base64.NO_WRAP))
            if (raw != null) return SecretKeySpec(raw, "AES")
        }
        val raw = ByteArray(32)
        SecureRandom().nextBytes(raw)
        val wrappedBytes = wrapAesKey(raw)
        prefs.edit().putString(WRAPPED_AES, Base64.encodeToString(wrappedBytes, Base64.NO_WRAP)).apply()
        return SecretKeySpec(raw, "AES")
    }

    private fun rsaKey(): java.security.KeyStore.PrivateKeyEntry {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        if (!keyStore.containsAlias(RSA_ALIAS)) {
            val start = Calendar.getInstance()
            val end = Calendar.getInstance()
            end.add(Calendar.YEAR, 30)
            val spec = KeyPairGeneratorSpec.Builder(appContext)
                .setAlias(RSA_ALIAS)
                .setSubject(X500Principal("CN=$RSA_ALIAS"))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.time)
                .setEndDate(end.time)
                .setKeySize(2048)
                .build()
            val generator = KeyPairGenerator.getInstance("RSA", ANDROID_KEYSTORE)
            generator.initialize(spec)
            generator.generateKeyPair()
        }
        return keyStore.getEntry(RSA_ALIAS, null) as KeyStore.PrivateKeyEntry
    }

    private fun wrapAesKey(raw: ByteArray): ByteArray {
        val entry = rsaKey()
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, entry.certificate.publicKey)
        return cipher.doFinal(raw)
    }

    private fun unwrapAesKey(wrapped: ByteArray): ByteArray? {
        return try {
            val entry = rsaKey()
            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, entry.privateKey)
            cipher.doFinal(wrapped)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS = "bridge_crypto"
        private const val WRAPPED_AES = "wrapped_aes"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val RSA_ALIAS = "bridge_rsa_wrap"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    }
}
