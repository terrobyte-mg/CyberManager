package com.terrobytes.cybermanaver2.storage

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class BackupStore() {
    fun saveBackup(backup: RouterBackup)
    fun getBackup(): RouterBackup?
    fun clearBackup()
}