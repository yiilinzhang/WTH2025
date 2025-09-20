package com.example.myapplication.ui.login

import com.example.myapplication.data.LoggedInUserView

data class LoginResult(
    val success: LoggedInUserView? = null,
    val error: String? = null
)
