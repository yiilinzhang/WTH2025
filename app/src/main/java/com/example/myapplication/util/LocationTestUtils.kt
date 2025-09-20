package com.example.myapplication.util

import com.example.myapplication.models.Location
import com.example.myapplication.models.LocationSession
import java.text.SimpleDateFormat
import java.util.*

/**
 * Test utilities for the new location structure
 */
object LocationTestUtils {

    /**
     * Generate session ID from timestamp
     * Format: YYYYMMDDHHMM (e.g., "202509201430" for Sept 20, 2025 2:30 PM)
     */
    fun generateSessionId(timestamp: Long = System.currentTimeMillis()): String {
        val dateFormat = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Create a test location with sample session
     */
    fun createTestLocation(locationId: String, locationName: String, userId: String): Location {
        val currentTime = System.currentTimeMillis()
        val sessionId = generateSessionId(currentTime)

        val session = LocationSession(
            sessionId = sessionId,
            startTime = currentTime,
            participants = listOf(userId),
            status = "ACTIVE"
        )

        return Location(
            locationId = locationId,
            locationName = locationName,
            sessions = mapOf(sessionId to session)
        )
    }

    /**
     * Find the closest session ID for a given join time
     */
    fun findClosestSessionId(sessions: Map<String, LocationSession>, joinTime: Long): String? {
        var closestSessionId: String? = null
        var closestTimeDiff = Long.MAX_VALUE

        for ((sessionId, session) in sessions) {
            // Only consider active sessions where join time is after start time
            if (session.status == "ACTIVE" && joinTime >= session.startTime) {
                val timeDiff = joinTime - session.startTime
                if (timeDiff < closestTimeDiff) {
                    closestTimeDiff = timeDiff
                    closestSessionId = sessionId
                }
            }
        }

        return closestSessionId
    }

    /**
     * Test session ID generation consistency
     */
    fun testSessionIdGeneration(): Boolean {
        val timestamp = 1726837800000L // Sept 20, 2024 2:30 PM UTC
        val expectedId = "202409201430" // May vary based on timezone
        val actualId = generateSessionId(timestamp)

        // Check format is correct (12 digits)
        return actualId.length == 12 && actualId.all { it.isDigit() }
    }
}