@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.terrobytes.cybermanaver2.storage

import android.content.Context

actual class CredentialsStore actual constructor() {

    private val prefs by lazy {
        AppContextProvider.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun saveCredentials(username: String, password: String) {
        prefs.edit()
            .putString(KEY_USER, username)
            .putString(KEY_PASS, password)
            .apply()
    }

    actual fun getCredentials(): Pair<String, String>? {
        val user = prefs.getString(KEY_USER, null) ?: return null
        val pass = prefs.getString(KEY_PASS, null) ?: return null
        return user to pass
    }

    actual fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "cybermanager_auth"
        const val KEY_USER = "username"
        const val KEY_PASS = "password"
    }
}