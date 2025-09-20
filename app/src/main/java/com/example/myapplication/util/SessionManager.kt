package com.example.myapplication.util

import com.example.myapplication.models.Location
import com.example.myapplication.models.LocationSession
import com.example.myapplication.models.WalkSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.*

object SessionManager {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Generate session ID from current time
     * Format: YYYYMMDDHHMM (e.g., "202509201430" for Sept 20, 2025 2:30 PM)
     */
    private fun generateSessionId(timestamp: Long = System.currentTimeMillis()): String {
        val dateFormat = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Find the closest active session for a user to join
     */
    private fun findClosestSession(locationId: String, userJoinTime: Long, callback: (String?) -> Unit) {
        db.collection("location")
            .document(locationId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    callback(null)
                    return@addOnSuccessListener
                }

                val sessions = document.get("sessions") as? Map<String, Map<String, Any>> ?: emptyMap()
                var closestSessionId: String? = null
                var closestTimeDiff = Long.MAX_VALUE

                for ((sessionId, sessionData) in sessions) {
                    val startTime = (sessionData["startTime"] as? Long) ?: 0L
                    val status = sessionData["status"] as? String ?: "ACTIVE"

                    // Only consider active sessions where user join time is after start time
                    if (status == "ACTIVE" && userJoinTime >= startTime) {
                        val timeDiff = userJoinTime - startTime
                        if (timeDiff < closestTimeDiff) {
                            closestTimeDiff = timeDiff
                            closestSessionId = sessionId
                        }
                    }
                }

                callback(closestSessionId)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    /**
     * Join a walking session at a location by scanning QR code
     */
    fun joinSession(locationId: String, locationName: String, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false, "User not authenticated")
            return
        }

        val joinTime = System.currentTimeMillis()

        // Find closest active session
        findClosestSession(locationId, joinTime) { closestSessionId ->
            if (closestSessionId != null) {
                // Join existing session
                joinExistingSession(locationId, closestSessionId, userId, callback)
            } else {
                // Create new session
                createNewSession(locationId, locationName, userId, joinTime, callback)
            }
        }
    }

    /**
     * Join an existing session
     */
    private fun joinExistingSession(locationId: String, sessionId: String, userId: String, callback: (Boolean, String?) -> Unit) {
        db.collection("location")
            .document(locationId)
            .update("sessions.$sessionId.participants", FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                callback(true, sessionId)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback(false, e.message)
            }
    }

    /**
     * Create a new session at a location
     */
    private fun createNewSession(locationId: String, locationName: String, userId: String, startTime: Long, callback: (Boolean, String?) -> Unit) {
        val sessionId = generateSessionId(startTime)

        val sessionData = hashMapOf(
            "sessionId" to sessionId,
            "startTime" to startTime,
            "participants" to listOf(userId),
            "status" to "ACTIVE"
        )

        val locationData = hashMapOf(
            "locationId" to locationId,
            "locationName" to locationName,
            "sessions" to mapOf(sessionId to sessionData)
        )

        // Try to update existing location document or create new one
        db.collection("location")
            .document(locationId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Update existing location with new session
                    db.collection("location")
                        .document(locationId)
                        .update("sessions.$sessionId", sessionData)
                        .addOnSuccessListener {
                            callback(true, sessionId)
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            callback(false, e.message)
                        }
                } else {
                    // Create new location document
                    db.collection("location")
                        .document(locationId)
                        .set(locationData)
                        .addOnSuccessListener {
                            callback(true, sessionId)
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            callback(false, e.message)
                        }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback(false, e.message)
            }
    }

    /**
     * End a walking session and update user's walk history
     */
    fun endSession(sessionId: String, locationName: String, distance: Double, duration: Long, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false)
            return
        }

        // Extract start time from session ID or calculate from duration
        val startTime = try {
            sessionId.split("_")[1].toLong()
        } catch (e: Exception) {
            System.currentTimeMillis() - duration
        }

        // Calculate base points (10 points per km)
        val basePoints = distance * 10.0

        // Get group size to calculate bonus points
        getGroupSizeForSession(sessionId, locationName) { groupSize ->
            // Calculate group bonus: 10% per additional person (beyond the user)
            val additionalMembers = maxOf(0, groupSize - 1)
            val groupBonus = basePoints * (additionalMembers * 0.1)
            val pointsEarned = basePoints + groupBonus

            // Create walk session data
            val walkSession = hashMapOf(
                "sessionId" to sessionId,
                "locationName" to locationName,
                "startTime" to startTime,
                "pointsEarned" to pointsEarned,
                "distance" to distance,
                "duration" to duration,
                "groupSize" to groupSize,
                "groupBonus" to groupBonus
            )

            // Update user's walk history and points
            db.collection("kopi")
                .document(userId)
                .update(
                    mapOf(
                        "walkHistory" to FieldValue.arrayUnion(walkSession),
                        "points" to FieldValue.increment(pointsEarned)
                    )
                )
                .addOnSuccessListener {
                    callback(true)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    callback(false)
                }
        }
    }

    /**
     * Get the group size for a walking session to calculate bonus points
     */
    private fun getGroupSizeForSession(sessionId: String, locationName: String, callback: (Int) -> Unit) {
        // First try to get from location-based sessions
        val locationId = locationName.lowercase().replace(" ", "_")
        db.collection("location")
            .document(locationId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val sessions = document.get("sessions") as? Map<String, Map<String, Any>>
                    val sessionData = sessions?.get(sessionId)
                    if (sessionData != null) {
                        val participants = sessionData["participants"] as? List<String> ?: emptyList()
                        callback(participants.size)
                        return@addOnSuccessListener
                    }
                }

                // If not found in location collection, try realtime database
                val database = FirebaseDatabase.getInstance("https://wth2025-default-rtdb.firebaseio.com/")
                database.reference.child("sessions").child(sessionId).child("participants")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val participantCount = snapshot.children.count()
                        callback(maxOf(1, participantCount)) // At least 1 (the user)
                    }
                    .addOnFailureListener {
                        // Default to 1 if can't determine group size
                        callback(1)
                    }
            }
            .addOnFailureListener {
                // Default to 1 if can't determine group size
                callback(1)
            }
    }

    /**
     * Get active sessions for a location
     */
    fun getActiveSessions(locationId: String, callback: (List<LocationSession>) -> Unit) {
        db.collection("location")
            .document(locationId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val sessions = document.get("sessions") as? Map<String, Map<String, Any>> ?: emptyMap()
                    val activeSessions = mutableListOf<LocationSession>()

                    for ((sessionId, sessionData) in sessions) {
                        val status = sessionData["status"] as? String ?: "ACTIVE"
                        if (status == "ACTIVE") {
                            val session = LocationSession(
                                sessionId = sessionId,
                                startTime = (sessionData["startTime"] as? Long) ?: 0L,
                                participants = (sessionData["participants"] as? List<String>) ?: emptyList(),
                                status = status
                            )
                            activeSessions.add(session)
                        }
                    }
                    callback(activeSessions)
                } else {
                    callback(emptyList())
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback(emptyList())
            }
    }


    /**
     * Get all available locations
     */
    fun getAllLocations(callback: (List<Location>) -> Unit) {
        db.collection("location")
            .get()
            .addOnSuccessListener { documents ->
                val locations = mutableListOf<Location>()
                for (document in documents) {
                    val locationId = document.id
                    val locationName = document.getString("locationName") ?: ""
                    val sessionsData = document.get("sessions") as? Map<String, Map<String, Any>> ?: emptyMap()

                    val sessions = mutableMapOf<String, LocationSession>()
                    for ((sessionId, sessionData) in sessionsData) {
                        val session = LocationSession(
                            sessionId = sessionId,
                            startTime = (sessionData["startTime"] as? Long) ?: 0L,
                            participants = (sessionData["participants"] as? List<String>) ?: emptyList(),
                            status = sessionData["status"] as? String ?: "ACTIVE"
                        )
                        sessions[sessionId] = session
                    }

                    val location = Location(
                        locationId = locationId,
                        locationName = locationName,
                        sessions = sessions
                    )
                    locations.add(location)
                }
                callback(locations)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback(emptyList())
            }
    }

    /**
     * Get user's walk history
     */
    fun getWalkHistory(callback: (List<WalkSession>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(emptyList())
            return
        }

        db.collection("kopi")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val walkHistory = document.get("walkHistory") as? List<Map<String, Any>> ?: emptyList()
                    val sessions = walkHistory.map { sessionMap ->
                        WalkSession(
                            sessionId = sessionMap["sessionId"] as? String ?: "",
                            locationName = sessionMap["locationName"] as? String ?: "",
                            startTime = (sessionMap["startTime"] as? Long) ?: 0L,
                            pointsEarned = (sessionMap["pointsEarned"] as? Double) ?: 0.0,
                            distance = (sessionMap["distance"] as? Double) ?: 0.0,
                            duration = (sessionMap["duration"] as? Long) ?: 0L
                        )
                    }
                    callback(sessions)
                } else {
                    callback(emptyList())
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback(emptyList())
            }
    }
}