package com.example.myapplication.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.LoggedInUserView
import com.example.myapplication.data.LoginRepository
import com.example.myapplication.data.Result
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: LoginRepository) : ViewModel() {

    private val _loginFormState = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginFormState

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun loginDataChanged(email: String, password: String) {
        val emailValid = isEmailValid(email)
        val passwordValid = password.length >= 6
        _loginFormState.value = LoginFormState(
            usernameError = if (!emailValid) "Invalid email" else null,
            passwordError = if (!passwordValid) "Password must be at least 6 characters" else null,
            isDataValid = emailValid && passwordValid
        )
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val result = repository.login(email, password)
            when (result) {
                is Result.Success -> _loginResult.value = LoginResult(success = result.data)
                is Result.Error -> _loginResult.value = LoginResult(error = result.exception.message)
            }
        }
    }

    fun signup(email: String, password: String) {
        viewModelScope.launch {
            val result = repository.signup(email, password)
            when (result) {
                is Result.Success -> _loginResult.value = LoginResult(success = result.data)
                is Result.Error -> _loginResult.value = LoginResult(error = result.exception.message)
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}