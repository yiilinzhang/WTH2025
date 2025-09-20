package com.example.myapplication.models

import com.google.firebase.firestore.PropertyName

/**
 * Data model for location document in the "location" collection
 * Document ID is the unique location ID from QR code
 * Each session is identified by date-time string with array of participant UUIDs
 */
data class Location(
    @PropertyName("locationId")
    val locationId: String = "",

    @PropertyName("locationName")
    val locationName: String = "",

    @PropertyName("sessions")
    val sessions: Map<String, LocationSession> = emptyMap() // Maps sessionId to LocationSession
) {
    // No-argument constructor required for Firestore
    constructor() : this("", "", emptyMap())
}

/**
 * Data model for individual session within a location
 * SessionId format: YYYYMMDDHHMM (e.g., "202509201430" for Sept 20, 2025 2:30 PM)
 */
data class LocationSession(
    @PropertyName("sessionId")
    val sessionId: String = "",

    @PropertyName("startTime")
    val startTime: Long = 0L, // Timestamp when session started

    @PropertyName("participants")
    val participants: List<String> = emptyList(), // Array of user UUIDs

    @PropertyName("status")
    val status: String = "ACTIVE" // ACTIVE, COMPLETED, CANCELLED
) {
    constructor() : this("", 0L, emptyList(), "ACTIVE")
}