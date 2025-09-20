package com.example.myapplication.data

class LoginRepository(private val dataSource: LoginDataSource) {

    fun login(username: String, password: String, callback: (Result<LoggedInUserView>) -> Unit) {
        dataSource.login(username, password, callback)
    }

    fun signup(username: String, password: String, callback: (Result<LoggedInUserView>) -> Unit) {
        dataSource.signup(username, password, callback)
    }
}
