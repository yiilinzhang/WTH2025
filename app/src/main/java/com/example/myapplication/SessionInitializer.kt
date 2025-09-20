package com.example.myapplication

import com.google.firebase.database.FirebaseDatabase
import android.util.Log

class SessionInitializer {
    companion object {
        private const val TAG = "SessionInitializer"

        fun createTestSessions() {
            val database = FirebaseDatabase.getInstance("https://wth2025-default-rtdb.firebaseio.com/")

            // List of test sessions to create
            val testSessions = listOf(
                mapOf(
                    "sessionId" to "SESSION_TEST99",
                    "hostId" to "system",
                    "createdAt" to System.currentTimeMillis(),
                    "isActive" to true,
                    "name" to "Test Walking Session",
                    "description" to "Test session for development",
                    "participants" to mapOf(
                        "system" to mapOf(
                            "userId" to "system",
                            "userName" to "Test Host",
                            "isHost" to true,
                            "joinedAt" to System.currentTimeMillis()
                        )
                    )
                ),
                mapOf(
                    "sessionId" to "COFFEE_WALK_01",
                    "hostId" to "system",
                    "createdAt" to System.currentTimeMillis(),
                    "isActive" to true,
                    "name" to "Coffee Trail Walk",
                    "description" to "Walk between coffee shops",
                    "participants" to mapOf(
                        "system" to mapOf(
                            "userId" to "system",
                            "userName" to "Coffee Host",
                            "isHost" to true,
                            "joinedAt" to System.currentTimeMillis()
                        )
                    )
                ),
                mapOf(
                    "sessionId" to "PARK_MORNING_01",
                    "hostId" to "system",
                    "createdAt" to System.currentTimeMillis(),
                    "isActive" to true,
                    "name" to "Morning Park Walk",
                    "description" to "Central Park morning walks",
                    "participants" to mapOf(
                        "system" to mapOf(
                            "userId" to "system",
                            "userName" to "Park Host",
                            "isHost" to true,
                            "joinedAt" to System.currentTimeMillis()
                        )
                    )
                )
            )

            // Create each session if it doesn't exist
            testSessions.forEach { sessionData ->
                val sessionId = sessionData["sessionId"] as String
                val sessionRef = database.reference.child("sessions").child(sessionId)

                // Check if session exists before creating
                sessionRef.get().addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        sessionRef.setValue(sessionData)
                            .addOnSuccessListener {
                                Log.d(TAG, "Created test session: $sessionId")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to create session $sessionId", e)
                            }
                    } else {
                        Log.d(TAG, "Session already exists: $sessionId")
                    }
                }
            }
        }
    }
}