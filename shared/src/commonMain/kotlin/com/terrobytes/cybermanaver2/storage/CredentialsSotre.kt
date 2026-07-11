package com.terrobytes.cybermanaver2.storage

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class CredentialsStore() {
    fun saveCredentials(username: String, password: String)
    fun getCredentials(): Pair<String, String>?
    fun clearCredentials()
}