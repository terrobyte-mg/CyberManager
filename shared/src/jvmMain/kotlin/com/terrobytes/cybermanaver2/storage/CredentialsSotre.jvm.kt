package com.terrobytes.cybermanaver2.storage

import java.io.File
import java.util.Properties

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class CredentialsStore actual constructor() {

    private val configDir = File(System.getProperty("user.home"), ".cybermanager")
    private val configFile = File(configDir, "credentials.properties")

    actual fun saveCredentials(username: String, password: String) {
        configDir.mkdirs()
        val props = Properties()
        props.setProperty(KEY_USER, username)
        props.setProperty(KEY_PASS, JvmSecretBox.encryptToBase64(password))
        configFile.outputStream().use { props.store(it, "CyberManager - identifiants admin") }
    }

    actual fun getCredentials(): Pair<String, String>? {
        if (!configFile.exists()) return null
        val props = Properties()
        configFile.inputStream().use { props.load(it) }
        val user = props.getProperty(KEY_USER) ?: return null
        val pass = props.getProperty(KEY_PASS)?.let { JvmSecretBox.decryptFromBase64(it) } ?: return null
        return user to pass
    }

    actual fun clearCredentials() {
        if (configFile.exists()) configFile.delete()
    }

    private companion object {
        const val KEY_USER = "username"
        const val KEY_PASS = "password"
    }
}