package com.terrobytes.cybermanaver2.storage

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encryption at rest for the JVM/desktop storage backends
 * (CredentialsStore, BackupStore).
 *
 * There's no OS keystore to lean on here like Android's EncryptedSharedPreferences,
 * so the key lives in a file next to the data it protects, with permissions
 * restricted to the current user where the filesystem supports it. This
 * defends against someone browsing/copying the data file, or another OS user
 * account on the same machine reading it - it does NOT defend against an
 * attacker who can already run code as the same user (they could read the
 * key file too). True "safe even from yourself" secrecy would need a master
 * password from the user, which this app doesn't ask for.
 */
internal object JvmSecretBox {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    private val configDir = File(System.getProperty("user.home"), ".cybermanager")
    private val keyFile = File(configDir, "storage.key")

    private val secretKey: SecretKey by lazy { loadOrCreateKey() }

    fun encrypt(plain: ByteArray): ByteArray {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return iv + cipher.doFinal(plain)
    }

    fun decrypt(encrypted: ByteArray): ByteArray {
        val iv = encrypted.copyOfRange(0, IV_BYTES)
        val ciphertext = encrypted.copyOfRange(IV_BYTES, encrypted.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun encryptToBase64(plain: String): String =
        Base64.getEncoder().encodeToString(encrypt(plain.toByteArray(Charsets.UTF_8)))

    fun decryptFromBase64(value: String): String =
        String(decrypt(Base64.getDecoder().decode(value)), Charsets.UTF_8)

    private fun loadOrCreateKey(): SecretKey {
        configDir.mkdirs()

        if (keyFile.exists()) {
            return SecretKeySpec(keyFile.readBytes(), "AES")
        }

        val generator = KeyGenerator.getInstance("AES")
        generator.init(KEY_SIZE_BITS, SecureRandom())
        val key = generator.generateKey()

        keyFile.writeBytes(key.encoded)
        restrictToOwner(keyFile)

        return key
    }

    private fun restrictToOwner(file: File) {
        try {
            Files.setPosixFilePermissions(
                file.toPath(),
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
            )
        } catch (_: UnsupportedOperationException) {
            // Windows: no POSIX permission API - best-effort fallback.
            file.setReadable(false, false)
            file.setReadable(true, true)
            file.setWritable(false, false)
            file.setWritable(true, true)
        }
    }
}