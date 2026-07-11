package com.terrobytes.cybermanaver2.storage

import android.content.Context

object AppContextProvider {
    lateinit var context: Context
        private set

    fun init(context: Context) {
        this.context = context.applicationContext
    }
}