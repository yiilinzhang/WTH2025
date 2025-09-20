package com.example.myapplication.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object DatabaseSyncManager {

    private val TAG = "DatabaseSyncManager"
    private val database = FirebaseDatabase.getInstance("https://wth2025-default-rtdb.firebaseio.com/").reference
    private val auth = FirebaseAuth.getInstance()

    interface SyncCallback {
        fun onSyncComplete(success: Boolean, message: String)
        fun onProgressUpdate(progress: Int, message: String)
    }

    fun syncUserData(context: Context, callback: SyncCallback) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback.onSyncComplete(false, "User not authenticated")
            return
        }

        callback.onProgressUpdate(10, "Starting sync...")

        // Step 1: Sync user profile
        syncUserProfile(userId) { profileSuccess ->
            if (!profileSuccess) {
                callback.onSyncComplete(false, "Failed to sync user profile")
                return@syncUserProfile
            }

            callback.onProgressUpdate(30, "User profile synced")

            // Step 2: Sync completed sessions
            syncCompletedSessions(userId) { sessionsSuccess ->
                if (!sessionsSuccess) {
                    callback.onSyncComplete(false, "Failed to sync sessions")
                    return@syncCompletedSessions
                }

                callback.onProgressUpdate(60, "Sessions synced")

                // Step 3: Sync leaderboard entry
                syncLeaderboardEntry(userId) { leaderboardSuccess ->
                    if (!leaderboardSuccess) {
                        callback.onSyncComplete(false, "Failed to sync leaderboard")
                        return@syncLeaderboardEntry
                    }

                    callback.onProgressUpdate(90, "Leaderboard synced")

                    // Step 4: Clean up old data
                    cleanupOldData { cleanupSuccess ->
                        callback.onProgressUpdate(100, "Sync complete")
                        callback.onSyncComplete(true, "All data synced successfully")
                    }
                }
            }
        }
    }

    private fun syncUserProfile(userId: String, callback: (Boolean) -> Unit) {
        database.child("users").child(userId).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        // Create new user profile
                        val userData = mapOf(
                            "userId" to userId,
                            "userName" to (auth.currentUser?.displayName ?: "Walker"),
                            "email" to (auth.currentUser?.email ?: ""),
                            "totalPoints" to 0,
                            "sessionsCompleted" to 0,
                            "joinedAt" to System.currentTimeMillis()
                        )

                        database.child("users").child(userId).setValue(userData)
                            .addOnSuccessListener { callback(true) }
                            .addOnFailureListener { callback(false) }
                    } else {
                        // Update last sync time
                        database.child("users").child(userId).child("lastSyncTime")
                            .setValue(System.currentTimeMillis())
                            .addOnSuccessListener { callback(true) }
                            .addOnFailureListener { callback(false) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to sync user profile: ${error.message}")
                    callback(false)
                }
            }
        )
    }

    private fun syncCompletedSessions(userId: String, callback: (Boolean) -> Unit) {
        database.child("users").child(userId).child("completedSessions")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Calculate total points from sessions
                    var totalPoints = 0
                    var sessionCount = 0

                    for (sessionSnapshot in snapshot.children) {
                        val points = sessionSnapshot.child("points").getValue(Int::class.java) ?: 0
                        totalPoints += points
                        sessionCount++
                    }

                    // Update aggregated values
                    val updates = mapOf(
                        "users/$userId/totalPoints" to totalPoints,
                        "users/$userId/sessionsCompleted" to sessionCount
                    )

                    database.updateChildren(updates)
                        .addOnSuccessListener { callback(true) }
                        .addOnFailureListener { callback(false) }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to sync sessions: ${error.message}")
                    callback(false)
                }
            })
    }

    private fun syncLeaderboardEntry(userId: String, callback: (Boolean) -> Unit) {
        database.child("users").child(userId).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val totalPoints = snapshot.child("totalPoints").getValue(Int::class.java) ?: 0
                    val sessionsCompleted = snapshot.child("sessionsCompleted").getValue(Int::class.java) ?: 0
                    val userName = snapshot.child("userName").getValue(String::class.java) ?: "Walker"

                    val leaderboardEntry = mapOf(
                        "userId" to userId,
                        "userName" to userName,
                        "totalPoints" to totalPoints,
                        "sessionsCompleted" to sessionsCompleted,
                        "lastUpdated" to System.currentTimeMillis()
                    )

                    database.child("leaderboard").child(userId).setValue(leaderboardEntry)
                        .addOnSuccessListener { callback(true) }
                        .addOnFailureListener { callback(false) }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to sync leaderboard: ${error.message}")
                    callback(false)
                }
            }
        )
    }

    private fun cleanupOldData(callback: (Boolean) -> Unit) {
        // Remove sessions older than 30 days from recentSessions
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

        database.child("recentSessions")
            .orderByChild("completedAt")
            .endAt(thirtyDaysAgo.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updates = mutableMapOf<String, Any?>()

                    for (sessionSnapshot in snapshot.children) {
                        updates["recentSessions/${sessionSnapshot.key}"] = null
                    }

                    if (updates.isNotEmpty()) {
                        database.updateChildren(updates)
                            .addOnSuccessListener {
                                Log.d(TAG, "Cleaned up ${updates.size} old sessions")
                                callback(true)
                            }
                            .addOnFailureListener { callback(false) }
                    } else {
                        callback(true)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to cleanup old data: ${error.message}")
                    callback(true) // Don't fail sync due to cleanup failure
                }
            })
    }

    fun forceSyncNow(context: Context) {
        syncUserData(context, object : SyncCallback {
            override fun onSyncComplete(success: Boolean, message: String) {
                Log.d(TAG, "Force sync complete: $success - $message")
            }

            override fun onProgressUpdate(progress: Int, message: String) {
                Log.d(TAG, "Sync progress: $progress% - $message")
            }
        })
    }
}