package com.example.myapplication.util

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object TestAccountInitializer {

    private const val TEST_EMAIL = "test@test.com"
    private const val TEST_PASSWORD = "testtest"

    /**
     * Creates a test account if it doesn't exist.
     * This should only be used in development/testing environments.
     */
    suspend fun ensureTestAccountExists() {
        val auth = FirebaseAuth.getInstance()

        try {
            // First, try to sign in with the test account
            auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).await()
            // If successful, the account exists - sign out to let user login normally
            auth.signOut()
        } catch (e: Exception) {
            // Account doesn't exist, try to create it
            try {
                auth.createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).await()
                auth.signOut()
            } catch (createError: Exception) {
                // Account creation failed - might already exist or other error
                // Silently ignore as this is just for testing convenience
            }
        }
    }
}