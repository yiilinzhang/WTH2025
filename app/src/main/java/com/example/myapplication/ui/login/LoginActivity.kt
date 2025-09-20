package com.example.myapplication.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.LoginDataSource
import com.example.myapplication.data.LoginRepository

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameEditText = findViewById<EditText>(R.id.username)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.login)
        val signupButton = findViewById<Button>(R.id.signup)
        val loadingProgressBar = findViewById<ProgressBar>(R.id.loading)

        val repository = LoginRepository(LoginDataSource())
        val factory = LoginViewModelFactory(repository)
        loginViewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        loginViewModel.loginFormState.observe(this) {
            loginButton.isEnabled = it.isDataValid
        }

        loginViewModel.loginResult.observe(this) { result ->
            loadingProgressBar.visibility = ProgressBar.GONE
            if (result.success != null) {
                Toast.makeText(this, "Welcome ${result.success.displayName}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else if (result.error != null) {
                Toast.makeText(this, result.error, Toast.LENGTH_SHORT).show()
            }
        }

        val afterTextChangedListener = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                loginViewModel.loginDataChanged(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        usernameEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.addTextChangedListener(afterTextChangedListener)

        loginButton.setOnClickListener {
            loadingProgressBar.visibility = ProgressBar.VISIBLE
            loginViewModel.login(usernameEditText.text.toString(), passwordEditText.text.toString())
        }

        signupButton.setOnClickListener {
            loadingProgressBar.visibility = ProgressBar.VISIBLE
            loginViewModel.signup(usernameEditText.text.toString(), passwordEditText.text.toString())
        }
    }
}
