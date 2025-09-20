package com.example.myapplication.models

data class Session(
    val sessionId: String = "",
    val hostId: String = "",
    val createdAt: Long = 0L,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val isActive: Boolean = true,
    val status: String = "waiting", // waiting, active, completed
    val participants: Map<String, Participant> = emptyMap(),
    val locations: Map<String, UserLocation> = emptyMap(),
    val results: SessionResults? = null
)

data class Participant(
    val userId: String = "",
    val userName: String = "",
    val isHost: Boolean = false,
    val joinedAt: Long = 0L,
    val profilePicture: String? = null
)

data class UserLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L,
    val accuracy: Float = 0f,
    val speed: Float = 0f
)

data class SessionResults(
    val endTime: Long = 0L,
    val totalDistance: Double = 0.0,
    val totalSteps: Int = 0,
    val totalPoints: Int = 0,
    val duration: Long = 0L,
    val averageSpeed: Double = 0.0,
    val caloriesBurned: Int = 0
)

data class UserProfile(
    val userId: String = "",
    val userName: String = "",
    val email: String = "",
    val totalPoints: Int = 0,
    val totalDistance: Double = 0.0,
    val totalSessions: Int = 0,
    val joinDate: Long = 0L,
    val friends: List<String> = emptyList(),
    val achievements: List<Achievement> = emptyList()
)

data class Achievement(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val unlockedAt: Long = 0L,
    val type: String = "", // distance, points, sessions, etc.
    val requirement: Int = 0
)

// Helper class for managing session state
class SessionManager {
    companion object {
        fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadius = 6371000.0 // meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            return earthRadius * c
        }

        fun estimateSteps(distanceInMeters: Double): Int {
            // Average step length is about 0.762 meters
            return (distanceInMeters / 0.762).toInt()
        }

        fun calculatePoints(distanceInMeters: Double, durationInSeconds: Long): Int {
            // Base points for distance (1 point per 10 meters)
            val distancePoints = (distanceInMeters / 10).toInt()

            // Bonus points for duration (1 point per minute)
            val durationPoints = (durationInSeconds / 60).toInt()

            // Speed bonus (extra points for maintaining good pace)
            val averageSpeed = distanceInMeters / durationInSeconds // m/s
            val speedBonus = if (averageSpeed > 1.4) 50 else 0 // Walking pace > 5 km/h

            return distancePoints + durationPoints + speedBonus
        }

        fun estimateCalories(distanceInMeters: Double, weightInKg: Double = 70.0): Int {
            // Rough estimate: 0.05 calories per meter per kg
            return (0.05 * distanceInMeters * weightInKg).toInt()
        }
    }
}