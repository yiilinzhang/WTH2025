package com.example.myapplication.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.MainActivity
import com.example.myapplication.databinding.ActivityLoginBinding
import com.example.myapplication.util.AuthManager
import com.example.myapplication.util.DatabaseInitializer
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        // Login button
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        // Sign up button
        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signUpUser(email, password)
        }

        // Skip login for testing
        binding.btnSkipLogin.setOnClickListener {
            AuthManager.setLoggedIn(this, "test@kopikakis.sg")
            navigateToMain()
        }
    }

    private fun loginUser(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.btnSignUp.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    AuthManager.setLoggedIn(this, email)
                    Toast.makeText(this, "Welcome back, kopi kaki!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    // If sign in fails, display a message to the user
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    binding.btnLogin.isEnabled = true
                    binding.btnSignUp.isEnabled = true
                }
            }
    }

    private fun signUpUser(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.btnSignUp.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success - create user document in kopi collection
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        DatabaseInitializer.createUserDocument(userId) { success ->
                            if (success) {
                                AuthManager.setLoggedIn(this, email)
                                Toast.makeText(this, "Welcome to Kopi Kakis Walking Club!", Toast.LENGTH_SHORT).show()
                                navigateToMain()
                            } else {
                                Toast.makeText(this, "Account created but failed to initialize user data", Toast.LENGTH_SHORT).show()
                                AuthManager.setLoggedIn(this, email)
                                navigateToMain()
                            }
                        }
                    } else {
                        AuthManager.setLoggedIn(this, email)
                        navigateToMain()
                    }
                } else {
                    // If sign up fails, display a message to the user
                    Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    binding.btnLogin.isEnabled = true
                    binding.btnSignUp.isEnabled = true
                }
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}