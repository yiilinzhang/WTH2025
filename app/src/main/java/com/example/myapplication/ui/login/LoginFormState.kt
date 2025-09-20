package com.example.myapplication.ui.login

data class LoginFormState(
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isDataValid: Boolean = false
)
