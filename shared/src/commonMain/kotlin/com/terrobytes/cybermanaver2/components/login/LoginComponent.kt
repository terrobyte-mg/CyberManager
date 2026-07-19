package com.terrobytes.cybermanaver2.components.login

import com.arkivanov.decompose.value.Value
import com.terrobytes.cybermanaver2.models.RouterUiState

interface LoginComponent {
    val routeurState : Value<RouterUiState>
    fun onBackClicked()
    fun submitLogin(username: String, password: String, test: Boolean = false)
}