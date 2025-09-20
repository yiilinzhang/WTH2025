package com.example.myapplication

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Mock data manager to simulate Firebase without actual connection
 * Use this for testing when Firebase is not set up
 */
object MockDataManager {
    private const val TAG = "MockDataManager"

    // Store mock sessions in memory
    private val sessions = mutableMapOf<String, MutableMap<String, Any>>()
    private val handler = Handler(Looper.getMainLooper())

    // Use mock mode flag - set to true to bypass Firebase
    var USE_MOCK_DATA = true

    init {
        // Pre-populate with test sessions
        createMockSessions()
    }

    private fun createMockSessions() {
        // Test Session 1
        sessions["SESSION_TEST99"] = mutableMapOf(
            "sessionId" to "SESSION_TEST99",
            "hostId" to "mock_host_1",
            "createdAt" to System.currentTimeMillis(),
            "isActive" to true,
            "name" to "Test Walking Session",
            "participants" to mutableMapOf(
                "mock_host_1" to mapOf(
                    "userId" to "mock_host_1",
                    "userName" to "John (Host)",
                    "isHost" to true,
                    "joinedAt" to System.currentTimeMillis()
                ),
                "mock_user_2" to mapOf(
                    "userId" to "mock_user_2",
                    "userName" to "Sarah",
                    "isHost" to false,
                    "joinedAt" to System.currentTimeMillis()
                ),
                "mock_user_3" to mapOf(
                    "userId" to "mock_user_3",
                    "userName" to "Mike",
                    "isHost" to false,
                    "joinedAt" to System.currentTimeMillis()
                )
            )
        )

        // Coffee Walk Session
        sessions["COFFEE_WALK_01"] = mutableMapOf(
            "sessionId" to "COFFEE_WALK_01",
            "hostId" to "coffee_host",
            "createdAt" to System.currentTimeMillis(),
            "isActive" to true,
            "name" to "Coffee Trail Walk",
            "participants" to mutableMapOf(
                "coffee_host" to mapOf(
                    "userId" to "coffee_host",
                    "userName" to "Emma (Host)",
                    "isHost" to true,
                    "joinedAt" to System.currentTimeMillis()
                )
            )
        )

        Log.d(TAG, "Created ${sessions.size} mock sessions")
    }

    fun getSession(sessionId: String, callback: (Map<String, Any>?) -> Unit) {
        Log.d(TAG, "Getting mock session: $sessionId")

        // Simulate network delay
        handler.postDelayed({
            val session = sessions[sessionId]
            Log.d(TAG, "Mock session found: ${session != null}")
            callback(session)
        }, 500)
    }

    fun joinSession(sessionId: String, userId: String, userName: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Mock joining session: $sessionId as $userName")

        handler.postDelayed({
            val session = sessions[sessionId]
            if (session != null) {
                // Add participant to session
                val participants = session["participants"] as MutableMap<String, Any>
                participants[userId] = mapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "isHost" to false,
                    "joinedAt" to System.currentTimeMillis()
                )

                Log.d(TAG, "Mock user joined successfully")
                callback(true)
            } else {
                // Create new session if it doesn't exist
                sessions[sessionId] = mutableMapOf(
                    "sessionId" to sessionId,
                    "hostId" to userId,
                    "createdAt" to System.currentTimeMillis(),
                    "isActive" to true,
                    "name" to "New Session",
                    "participants" to mutableMapOf(
                        userId to mapOf(
                            "userId" to userId,
                            "userName" to userName,
                            "isHost" to true,
                            "joinedAt" to System.currentTimeMillis()
                        )
                    )
                )

                Log.d(TAG, "Mock session created and joined")
                callback(true)
            }
        }, 500)
    }

    fun updateLocation(sessionId: String, userId: String, lat: Double, lon: Double) {
        val session = sessions[sessionId] ?: return

        if (session["locations"] == null) {
            session["locations"] = mutableMapOf<String, Any>()
        }

        val locations = session["locations"] as MutableMap<String, Any>
        locations[userId] = mapOf(
            "latitude" to lat,
            "longitude" to lon,
            "timestamp" to System.currentTimeMillis()
        )
    }

    fun getParticipants(sessionId: String): List<Map<String, Any>> {
        val session = sessions[sessionId] ?: return emptyList()
        val participants = session["participants"] as Map<String, Any>

        return participants.values.map { it as Map<String, Any> }
    }

    fun startSession(sessionId: String, callback: (Boolean) -> Unit) {
        handler.postDelayed({
            val session = sessions[sessionId]
            if (session != null) {
                session["status"] = "active"
                session["startTime"] = System.currentTimeMillis()
                callback(true)
            } else {
                callback(false)
            }
        }, 300)
    }

    fun endSession(sessionId: String, stats: Map<String, Any>, callback: (Boolean) -> Unit) {
        handler.postDelayed({
            val session = sessions[sessionId]
            if (session != null) {
                session["status"] = "completed"
                session["endTime"] = System.currentTimeMillis()
                session["results"] = stats
                callback(true)
            } else {
                callback(false)
            }
        }, 300)
    }
}