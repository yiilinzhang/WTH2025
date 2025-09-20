package com.example.myapplication.data

class LoginRepository(private val dataSource: LoginDataSource) {

    suspend fun login(email: String, password: String): Result<LoggedInUserView> {
        return dataSource.login(email, password)
    }

    suspend fun signup(email: String, password: String): Result<LoggedInUserView> {
        return dataSource.signup(email, password)
    }

    fun logout() {
        dataSource.logout()
    }

    fun isLoggedIn(): Boolean {
        return dataSource.getCurrentUser() != null
    }
}