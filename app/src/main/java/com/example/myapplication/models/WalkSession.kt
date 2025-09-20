package com.example.myapplication.models

data class WalkSession(
    val sessionId: String = "",
    val locationName: String = "",
    val startTime: Long = 0L,
    val pointsEarned: Double = 0.0,
    val distance: Double = 0.0,
    val duration: Long = 0L  // Duration in milliseconds
)