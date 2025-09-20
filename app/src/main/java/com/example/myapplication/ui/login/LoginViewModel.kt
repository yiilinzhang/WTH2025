package com.example.myapplication.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.LoggedInUserView
import com.example.myapplication.data.LoginRepository
import com.example.myapplication.data.Result

class LoginViewModel(private val repository: LoginRepository) : ViewModel() {

    private val _loginFormState = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginFormState

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun loginDataChanged(username: String, password: String) {
        val usernameValid = username.isNotBlank()
        val passwordValid = password.length > 5
        _loginFormState.value = LoginFormState(
            usernameError = if (!usernameValid) "Invalid username" else null,
            passwordError = if (!passwordValid) "Password too short" else null,
            isDataValid = usernameValid && passwordValid
        )
    }

    fun login(username: String, password: String) {
        repository.login(username, password) { result ->
            when (result) {
                is Result.Success -> _loginResult.postValue(LoginResult(success = result.data))
                is Result.Error -> _loginResult.postValue(LoginResult(error = result.exception.message))
            }
        }
    }

    fun signup(username: String, password: String) {
        repository.signup(username, password) { result ->
            when (result) {
                is Result.Success -> _loginResult.postValue(LoginResult(success = result.data))
                is Result.Error -> _loginResult.postValue(LoginResult(error = result.exception.message))
            }
        }
    }
}
