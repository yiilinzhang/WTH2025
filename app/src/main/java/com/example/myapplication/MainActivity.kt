package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.ui.login.LoginActivity
import com.example.myapplication.util.AuthManager
import com.example.myapplication.util.DatabaseInitializer
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is logged in (Firebase or fallback)
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null && !AuthManager.isLoggedIn(this)) {
            // User is not logged in, redirect to login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide the default action bar
        supportActionBar?.hide()

        // Initialize database structure for the user (only if authenticated)
        if (auth.currentUser != null) {
            try {
                DatabaseInitializer.initializeDatabase { success ->
                    if (success) {
                        // Optionally seed test data on first run
                        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        val isFirstRun = prefs.getBoolean("first_run", true)

                        if (isFirstRun) {
                            DatabaseInitializer.seedTestData { seeded ->
                                if (seeded) {
                                    runOnUiThread {
                                        Toast.makeText(this, "Welcome! Sample walking data loaded", Toast.LENGTH_SHORT).show()
                                    }
                                    prefs.edit().putBoolean("first_run", false).apply()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Log error but don't crash the app
                e.printStackTrace()
            }
        }
    }
}
