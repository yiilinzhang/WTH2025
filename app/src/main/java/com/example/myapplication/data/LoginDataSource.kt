package com.example.myapplication.data

import com.google.firebase.database.FirebaseDatabase

class LoginDataSource {

    private val database = FirebaseDatabase.getInstance().reference

    fun login(username: String, password: String, callback: (Result<LoggedInUserView>) -> Unit) {
        database.child("users").child(username).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val storedPassword = snapshot.child("password").getValue(String::class.java)
                    if (storedPassword == password) {
                        callback(Result.Success(LoggedInUserView(displayName = username)))
                    } else {
                        callback(Result.Error(Exception("Incorrect password")))
                    }
                } else {
                    callback(Result.Error(Exception("User not found")))
                }
            }
            .addOnFailureListener { callback(Result.Error(it)) }
    }

    fun signup(username: String, password: String, callback: (Result<LoggedInUserView>) -> Unit) {
        database.child("users").child(username).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    callback(Result.Error(Exception("User already exists")))
                } else {
                    val userMap = mapOf("password" to password)
                    database.child("users").child(username).setValue(userMap)
                        .addOnSuccessListener {
                            callback(Result.Success(LoggedInUserView(displayName = username)))
                        }
                        .addOnFailureListener { callback(Result.Error(it)) }
                }
            }
            .addOnFailureListener { callback(Result.Error(it)) }
    }
}
