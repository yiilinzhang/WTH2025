package com.example.myapplication.util

import com.example.myapplication.models.KopiUser
import com.example.myapplication.models.WalkSession
import com.example.myapplication.models.ScheduledWalk
import com.example.myapplication.models.WalkParticipant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query

object DatabaseInitializer {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Initialize database structure for the current user
     */
    fun initializeDatabase(callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false)
            return
        }

        val userId = currentUser.uid

        // Check if user document exists in kopi collection
        db.collection("kopi")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    // Create new user document with initial values
                    val newUser = hashMapOf(
                        "points" to 0.0,
                        "noOfKopiRedeemed" to 0,
                        "walkHistory" to emptyList<WalkSession>()
                    )

                    db.collection("kopi")
                        .document(userId)
                        .set(newUser)
                        .addOnSuccessListener {
                            callback(true)
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            // If permission denied, try to proceed anyway
                            if (e.message?.contains("PERMISSION_DENIED") == true) {
                                android.util.Log.w("DatabaseInitializer", "Permission denied - Firestore rules need to be configured")
                            }
                            callback(false)
                        }
                } else {
                    // User document already exists
                    callback(true)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                // If permission denied, try to proceed anyway
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    android.util.Log.w("DatabaseInitializer", "Permission denied - Firestore rules need to be configured")
                }
                callback(false)
            }
    }

    /**
     * Seed test data for demonstration purposes
     */
    fun seedTestData(callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false)
            return
        }

        val userId = currentUser.uid
        val currentTime = System.currentTimeMillis()

        // Create sample walk history
        val sampleWalkHistory = listOf(
            hashMapOf(
                "sessionId" to "session_${System.currentTimeMillis()}_1",
                "locationName" to "East Coast Park",
                "startTime" to (currentTime - 86400000), // Yesterday
                "pointsEarned" to 10.0,
                "distance" to 2.5,
                "duration" to 2600000L // ~43 minutes
            ),
            hashMapOf(
                "sessionId" to "session_${System.currentTimeMillis()}_2",
                "locationName" to "Botanic Gardens",
                "startTime" to (currentTime - 172800000), // 2 days ago
                "pointsEarned" to 15.0,
                "distance" to 3.2,
                "duration" to 3600000L // 1 hour
            ),
            hashMapOf(
                "sessionId" to "session_${System.currentTimeMillis()}_3",
                "locationName" to "MacRitchie Reservoir",
                "startTime" to (currentTime - 259200000), // 3 days ago
                "pointsEarned" to 12.0,
                "distance" to 2.8,
                "duration" to 3000000L // 50 minutes
            )
        )

        // Update user document with sample data
        db.collection("kopi")
            .document(userId)
            .update(
                mapOf(
                    "points" to 37.0,
                    "walkHistory" to sampleWalkHistory,
                    "noOfKopiRedeemed" to 0
                )
            )
            .addOnSuccessListener {
                // Also create some sample location documents
                createSampleLocations(callback)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback(false)
            }
    }

    /**
     * Create sample location documents
     */
    private fun createSampleLocations(callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: ""
        val currentTime = System.currentTimeMillis()

        // Generate session ID for current time
        val dateFormat = java.text.SimpleDateFormat("yyyyMMddHHmm", java.util.Locale.getDefault())
        val sessionId = dateFormat.format(java.util.Date(currentTime))

        val locations = listOf(
            Triple("LOC001", "East Coast Park", sessionId),
            Triple("LOC002", "Botanic Gardens", sessionId),
            Triple("LOC003", "MacRitchie Reservoir", sessionId)
        )

        var successCount = 0
        var failureCount = 0

        locations.forEach { (locationId, locationName, sessionId) ->
            val sessionData = hashMapOf(
                "sessionId" to sessionId,
                "startTime" to currentTime,
                "participants" to listOf(userId),
                "status" to "ACTIVE"
            )

            val locationData = hashMapOf(
                "locationId" to locationId,
                "locationName" to locationName,
                "sessions" to mapOf(sessionId to sessionData)
            )

            db.collection("location")
                .document(locationId)
                .set(locationData)
                .addOnSuccessListener {
                    successCount++
                    if (successCount + failureCount == locations.size) {
                        callback(failureCount == 0)
                    }
                }
                .addOnFailureListener {
                    failureCount++
                    if (successCount + failureCount == locations.size) {
                        callback(failureCount == 0)
                    }
                }
        }
    }

    /**
     * Create a new user document when user signs up
     */
    fun createUserDocument(userId: String, callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        val email = currentUser?.email ?: ""
        val name = currentUser?.displayName ?: email.substringBefore("@")

        val newUser = hashMapOf(
            "points" to 0.0,
            "noOfKopiRedeemed" to 0,
            "walkHistory" to emptyList<WalkSession>()
        )

        // Create user profile for friend search
        val userProfile = hashMapOf(
            "email" to email,
            "name" to name,
            "userId" to userId,
            "createdAt" to System.currentTimeMillis()
        )

        // Create both documents
        val batch = db.batch()
        batch.set(db.collection("kopi").document(userId), newUser)
        batch.set(db.collection("users").document(userId), userProfile)

        batch.commit()
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback(false)
            }
    }

    /**
     * Add points to user account
     */
    fun addPoints(points: Double, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false)
            return
        }

        db.collection("kopi")
            .document(userId)
            .update("points", FieldValue.increment(points))
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback(false)
            }
    }

    /**
     * Record kopi redemption
     */
    fun redeemKopi(callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false)
            return
        }

        // First check if user has enough points (assuming 100 points per kopi)
        db.collection("kopi")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val currentPoints = document.getDouble("points") ?: 0.0
                if (currentPoints >= 100.0) {
                    // User has enough points, proceed with redemption
                    db.collection("kopi")
                        .document(userId)
                        .update(
                            mapOf(
                                "noOfKopiRedeemed" to FieldValue.increment(1),
                                "points" to FieldValue.increment(-100.0)
                            )
                        )
                        .addOnSuccessListener {
                            callback(true)
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                } else {
                    // Not enough points
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    /**
     * Add a completed walk session to user's history
     */
    fun addWalkSession(session: WalkSession, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false)
            return
        }

        // Convert WalkSession to HashMap for Firestore
        val sessionData = hashMapOf(
            "sessionId" to session.sessionId,
            "locationName" to session.locationName,
            "startTime" to session.startTime,
            "pointsEarned" to session.pointsEarned,
            "distance" to session.distance,
            "duration" to session.duration
        )

        // Add to walkHistory array and update points
        db.collection("kopi")
            .document(userId)
            .update(
                mapOf(
                    "walkHistory" to FieldValue.arrayUnion(sessionData),
                    "points" to FieldValue.increment(session.pointsEarned)
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

    /**
     * Create a new scheduled walk
     */
    fun createScheduledWalk(scheduledWalk: ScheduledWalk, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false, "User not authenticated")
            return
        }

        // Generate walkId if not provided
        val walkId = if (scheduledWalk.walkId.isEmpty()) {
            "walk_${System.currentTimeMillis()}"
        } else {
            scheduledWalk.walkId
        }

        // Convert ScheduledWalk to HashMap for Firestore
        val walkData = hashMapOf(
            "walkId" to walkId,
            "creatorId" to scheduledWalk.creatorId,
            "creatorName" to scheduledWalk.creatorName,
            "locationName" to scheduledWalk.locationName,
            "scheduledTime" to scheduledWalk.scheduledTime,
            "maxParticipants" to scheduledWalk.maxParticipants,
            "currentParticipants" to scheduledWalk.currentParticipants,
            "description" to scheduledWalk.description,
            "isRecurring" to scheduledWalk.isRecurring,
            "recurringDays" to scheduledWalk.recurringDays,
            "status" to scheduledWalk.status,
            "createdAt" to System.currentTimeMillis()
        )

        // Add to scheduledWalks collection
        db.collection("scheduledWalks")
            .document(walkId)
            .set(walkData)
            .addOnSuccessListener {
                callback(true, walkId)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback(false, e.message)
            }
    }

    /**
     * Get upcoming scheduled walks
     */
    fun getUpcomingWalks(callback: (List<ScheduledWalk>) -> Unit) {
        val currentTime = System.currentTimeMillis()

        // Simplified query: just get all active walks and filter/sort in code
        db.collection("scheduledWalks")
            .whereEqualTo("status", "ACTIVE")
            .get()
            .addOnSuccessListener { documents ->
                val walks = mutableListOf<ScheduledWalk>()
                for (document in documents) {
                    try {
                        val walk = ScheduledWalk(
                            walkId = document.getString("walkId") ?: "",
                            creatorId = document.getString("creatorId") ?: "",
                            creatorName = document.getString("creatorName") ?: "",
                            locationName = document.getString("locationName") ?: "",
                            scheduledTime = document.getLong("scheduledTime") ?: 0L,
                            maxParticipants = document.getLong("maxParticipants")?.toInt() ?: 8,
                            currentParticipants = document.get("currentParticipants") as? List<String> ?: emptyList(),
                            description = document.getString("description") ?: "",
                            isRecurring = document.getBoolean("isRecurring") ?: false,
                            recurringDays = document.get("recurringDays") as? List<String> ?: emptyList(),
                            status = document.getString("status") ?: "ACTIVE",
                            createdAt = document.getLong("createdAt") ?: 0L
                        )
                        walks.add(walk)
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseInitializer", "Error parsing scheduled walk", e)
                    }
                }

                // Filter and sort in code to avoid complex Firestore indexes
                val upcomingWalks = walks
                    .filter { it.scheduledTime > currentTime }
                    .sortedBy { it.scheduledTime }
                    .take(20)

                android.util.Log.d("DatabaseInitializer", "getUpcomingWalks: found ${walks.size} total, ${upcomingWalks.size} upcoming")
                callback(upcomingWalks)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DatabaseInitializer", "Error loading scheduled walks", e)
                callback(emptyList())
            }
    }

    /**
     * RSVP to a scheduled walk
     */
    fun rsvpToWalk(walkId: String, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false, "User not authenticated")
            return
        }

        // First check if walk exists and has space
        db.collection("scheduledWalks")
            .document(walkId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    callback(false, "Walk not found")
                    return@addOnSuccessListener
                }

                val currentParticipants = document.get("currentParticipants") as? List<String> ?: emptyList()
                val maxParticipants = document.getLong("maxParticipants")?.toInt() ?: 8

                if (currentParticipants.contains(userId)) {
                    callback(false, "Already RSVP'd to this walk")
                    return@addOnSuccessListener
                }

                if (currentParticipants.size >= maxParticipants) {
                    callback(false, "Walk is full")
                    return@addOnSuccessListener
                }

                // Add user to participants
                db.collection("scheduledWalks")
                    .document(walkId)
                    .update("currentParticipants", FieldValue.arrayUnion(userId))
                    .addOnSuccessListener {
                        callback(true, null)
                    }
                    .addOnFailureListener { e ->
                        callback(false, e.message)
                    }
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

    /**
     * Cancel RSVP to a scheduled walk
     */
    fun cancelRsvp(walkId: String, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false, "User not authenticated")
            return
        }

        db.collection("scheduledWalks")
            .document(walkId)
            .update("currentParticipants", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

    /**
     * Get walks created by current user
     */
    fun getMyScheduledWalks(callback: (List<ScheduledWalk>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(emptyList())
            return
        }

        // Simplified query: get by creatorId only, filter status in code
        db.collection("scheduledWalks")
            .whereEqualTo("creatorId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val walks = mutableListOf<ScheduledWalk>()
                for (document in documents) {
                    try {
                        val walk = ScheduledWalk(
                            walkId = document.getString("walkId") ?: "",
                            creatorId = document.getString("creatorId") ?: "",
                            creatorName = document.getString("creatorName") ?: "",
                            locationName = document.getString("locationName") ?: "",
                            scheduledTime = document.getLong("scheduledTime") ?: 0L,
                            maxParticipants = document.getLong("maxParticipants")?.toInt() ?: 8,
                            currentParticipants = document.get("currentParticipants") as? List<String> ?: emptyList(),
                            description = document.getString("description") ?: "",
                            isRecurring = document.getBoolean("isRecurring") ?: false,
                            recurringDays = document.get("recurringDays") as? List<String> ?: emptyList(),
                            status = document.getString("status") ?: "ACTIVE",
                            createdAt = document.getLong("createdAt") ?: 0L
                        )
                        walks.add(walk)
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseInitializer", "Error parsing my scheduled walk", e)
                    }
                }

                // Filter and sort in code to avoid complex Firestore indexes
                val activeWalks = walks
                    .filter { it.status == "ACTIVE" }
                    .sortedBy { it.scheduledTime }

                android.util.Log.d("DatabaseInitializer", "getMyScheduledWalks: found ${walks.size} total, ${activeWalks.size} active")
                callback(activeWalks)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    /**
     * Get walks user has RSVP'd to
     */
    fun getMyRsvpWalks(callback: (List<ScheduledWalk>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(emptyList())
            return
        }

        // Simplified query: get by participant only, filter status in code
        db.collection("scheduledWalks")
            .whereArrayContains("currentParticipants", userId)
            .get()
            .addOnSuccessListener { documents ->
                val walks = mutableListOf<ScheduledWalk>()
                for (document in documents) {
                    try {
                        val walk = ScheduledWalk(
                            walkId = document.getString("walkId") ?: "",
                            creatorId = document.getString("creatorId") ?: "",
                            creatorName = document.getString("creatorName") ?: "",
                            locationName = document.getString("locationName") ?: "",
                            scheduledTime = document.getLong("scheduledTime") ?: 0L,
                            maxParticipants = document.getLong("maxParticipants")?.toInt() ?: 8,
                            currentParticipants = document.get("currentParticipants") as? List<String> ?: emptyList(),
                            description = document.getString("description") ?: "",
                            isRecurring = document.getBoolean("isRecurring") ?: false,
                            recurringDays = document.get("recurringDays") as? List<String> ?: emptyList(),
                            status = document.getString("status") ?: "ACTIVE",
                            createdAt = document.getLong("createdAt") ?: 0L
                        )
                        walks.add(walk)
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseInitializer", "Error parsing RSVP walk", e)
                    }
                }

                // Filter and sort in code to avoid complex Firestore indexes
                val activeRsvpWalks = walks
                    .filter { it.status == "ACTIVE" }
                    .sortedBy { it.scheduledTime }

                android.util.Log.d("DatabaseInitializer", "getMyRsvpWalks: found ${walks.size} total, ${activeRsvpWalks.size} active")
                callback(activeRsvpWalks)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    /**
     * Clear all scheduled walks from database (for testing)
     */
    fun clearAllScheduledWalks(callback: (Boolean) -> Unit) {
        db.collection("scheduledWalks")
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (document in documents) {
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        android.util.Log.d("DatabaseInitializer", "All scheduled walks cleared successfully")
                        callback(true)
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("DatabaseInitializer", "Error clearing scheduled walks", e)
                        callback(false)
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DatabaseInitializer", "Error fetching scheduled walks to clear", e)
                callback(false)
            }
    }
}