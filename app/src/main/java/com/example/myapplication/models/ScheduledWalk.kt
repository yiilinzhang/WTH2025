package com.example.myapplication.models

data class ScheduledWalk(
    val walkId: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val locationName: String = "",
    val scheduledTime: Long = 0L,
    val maxParticipants: Int = 8,
    val currentParticipants: List<String> = emptyList(),
    val description: String = "",
    val isRecurring: Boolean = false,
    val recurringDays: List<String> = emptyList(), // ["MONDAY", "WEDNESDAY", "FRIDAY"]
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED", "CANCELLED"
    val createdAt: Long = 0L
)

data class WalkParticipant(
    val userId: String = "",
    val userName: String = "",
    val joinedAt: Long = 0L,
    val isCreator: Boolean = false
)