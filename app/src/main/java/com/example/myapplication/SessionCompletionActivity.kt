package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.util.TypedValue
import android.graphics.drawable.GradientDrawable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.example.myapplication.models.WalkSession
import com.example.myapplication.util.DatabaseInitializer

class SessionCompletionActivity : AppCompatActivity() {

    private lateinit var tvPointsEarned: TextView
    private lateinit var tvDistanceValue: TextView
    private lateinit var tvDurationValue: TextView
    private lateinit var tvStepsValue: TextView
    private lateinit var friendsListContainer: LinearLayout
    private lateinit var btnBackToHome: Button

    private var sessionId: String = ""
    private var userId: String = ""
    private var distance: Double = 0.0
    private var duration: Long = 0L
    private var points: Int = 0
    private var groupBonusInfo: Int = 0
    private lateinit var db: FirebaseFirestore

    private val addedFriends = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_completion)

        // Initialize views
        tvPointsEarned = findViewById(R.id.tvPointsEarned)
        tvDistanceValue = findViewById(R.id.tvDistanceValue)
        tvDurationValue = findViewById(R.id.tvDurationValue)
        tvStepsValue = findViewById(R.id.tvStepsValue)
        friendsListContainer = findViewById(R.id.friendsListContainer)
        btnBackToHome = findViewById(R.id.btnBackToHome)

        // Add back button functionality
        findViewById<Button>(R.id.backButton)?.setOnClickListener {
            onBackPressed()
        }

        // Get data from intent
        sessionId = intent.getStringExtra("sessionId") ?: ""
        userId = intent.getStringExtra("userId") ?: ""
        distance = intent.getDoubleExtra("distance", 0.0)
        duration = intent.getLongExtra("duration", 0L)

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance()

        // Load participants first to calculate points with group bonus
        loadParticipants()

        // Setup button listeners
        setupButtons()
    }

    private fun displayStats() {
        val pointsText = if (groupBonusInfo > 0) {
            "You've earned $points points! (+${groupBonusInfo * 10}% group bonus)"
        } else {
            "You've earned $points points!"
        }
        tvPointsEarned.text = pointsText
        tvDistanceValue.text = String.format("%.2f km", distance / 1000)

        val minutes = duration / 60000
        val seconds = (duration % 60000) / 1000
        tvDurationValue.text = String.format("%02d:%02d", minutes, seconds)

        // Calculate estimated steps (average stride length ~0.76m, so ~1.3 steps per meter)
        // More realistic: 2000 steps per km, or 2 steps per meter
        val steps = (distance * 1.3).toInt()
        tvStepsValue.text = steps.toString()

        // Update Firebase with session completion data
        updateFirebaseData()
    }

    private fun loadParticipants() {
        if (sessionId.isEmpty()) {
            // Mock mode - show sample participants and calculate points with group bonus
            val groupSize = 3 // Mock group of 3 people
            calculatePointsWithGroupBonus(groupSize)
            addParticipantRow("John Walker", "john123", true)
            addParticipantRow("Sarah Runner", "sarah456", false)
            addParticipantRow("Mike Jogger", "mike789", false)
            return
        }

        // Firestore mode - load real participants from location collection
        db.collection("location")
            .whereEqualTo("locationName", intent.getStringExtra("locationName") ?: "")
            .get()
            .addOnSuccessListener { documents ->
                friendsListContainer.removeAllViews()
                var hasParticipants = false
                var totalParticipants = 1 // Include the current user

                for (document in documents) {
                    val sessionMap = document.get("sessionID") as? Map<String, String> ?: continue
                    for ((key, participantId) in sessionMap) {
                        if (participantId != userId) {
                            // For now, use participant ID as name (you could fetch user details)
                            val name = "Walker ${participantId.take(4)}"
                            addParticipantRow(name, participantId, false)
                            hasParticipants = true
                            totalParticipants++
                        }
                    }
                }

                // Calculate points with group bonus based on actual participant count
                calculatePointsWithGroupBonus(totalParticipants)

                if (!hasParticipants) {
                    val noParticipantsText = TextView(this@SessionCompletionActivity).apply {
                        text = "No other companions on this walk"
                        gravity = Gravity.CENTER
                        setPadding(24, 48, 24, 48)
                        textSize = 20f
                        setTextColor(Color.parseColor("#9B7B5B"))
                        setBackgroundColor(Color.WHITE)
                    }
                    friendsListContainer.addView(noParticipantsText)
                }
            }
            .addOnFailureListener {
                // Default to solo walk if can't load participants
                calculatePointsWithGroupBonus(1)
                Toast.makeText(this@SessionCompletionActivity,
                    "Failed to load participants", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculatePointsWithGroupBonus(groupSize: Int) {
        // Calculate base points (10 points per km, same as SessionManager)
        val basePoints = (distance * 10.0 / 1000.0) // distance is in meters, convert to km

        // Calculate group bonus: 10% per additional person (beyond the user)
        val additionalMembers = maxOf(0, groupSize - 1)
        val groupBonus = basePoints * (additionalMembers * 0.1)
        val totalPoints = basePoints + groupBonus

        points = totalPoints.toInt()
        groupBonusInfo = additionalMembers // Store for display

        // Display stats after calculating points
        displayStats()

        // Log for debugging
        android.util.Log.d("SessionCompletion", "Group size: $groupSize, Base points: $basePoints, Group bonus: $groupBonus, Total points: $points")
    }

    private fun addParticipantRow(name: String, participantId: String, isAlreadyFriend: Boolean) {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(24, 20, 24, 20)
            setBackgroundColor(Color.WHITE)

            // Add divider line effect
            val divider = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                setBackgroundColor(Color.parseColor("#D4A574"))
            }
        }

        // Profile icon circle
        val profileIcon = TextView(this).apply {
            text = name.firstOrNull()?.toString()?.uppercase() ?: "U"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                100,
                100
            ).apply {
                setMargins(0, 0, 16, 0)
            }
            background = createCircleDrawable(Color.parseColor("#8B5A3C"))
        }

        val nameText = TextView(this).apply {
            text = name
            textSize = 22f
            setTextColor(Color.parseColor("#3E2723"))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        }

        val addButton = Button(this).apply {
            val buttonHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                56f,
                resources.displayMetrics
            ).toInt()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                buttonHeight
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            textSize = 18f
            setPadding(32, 0, 32, 0)

            if (isAlreadyFriend || addedFriends.contains(participantId)) {
                text = "✓ Friends"
                isEnabled = false
                setBackgroundColor(Color.parseColor("#E8D4C1"))
                setTextColor(Color.parseColor("#6B4423"))
            } else {
                text = "+ Add"
                setBackgroundColor(Color.parseColor("#8B5A3C"))
                setTextColor(Color.WHITE)

                setOnClickListener {
                    addFriend(participantId, name)
                    text = "✓ Added"
                    isEnabled = false
                    setBackgroundColor(Color.parseColor("#8B5A3C"))
                    setTextColor(Color.WHITE)
                }
            }
        }

        rowLayout.addView(profileIcon)
        rowLayout.addView(nameText)
        rowLayout.addView(addButton)
        friendsListContainer.addView(rowLayout)

        // Add divider line after each row (except last)
        if (friendsListContainer.childCount < 3) {  // Adjust based on expected participants
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                setBackgroundColor(Color.parseColor("#D4A574"))
            }
            friendsListContainer.addView(divider)
        }
    }

    private fun addFriend(friendId: String, friendName: String) {
        addedFriends.add(friendId)

        if (sessionId.isNotEmpty()) {
            // Add friend relationship in Firestore (you could create a friends collection)
            val friendData = hashMapOf(
                "friendId" to friendId,
                "friendName" to friendName,
                "addedAt" to System.currentTimeMillis()
            )

            // Store friend relationship (optional - you can implement this later)
            db.collection("kopi")
                .document(userId)
                .collection("friends")
                .document(friendId)
                .set(friendData)
                .addOnSuccessListener {
                    Toast.makeText(this, "$friendName added as friend!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add friend", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Mock mode
            Toast.makeText(this, "$friendName added as friend!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupButtons() {
        btnBackToHome.setOnClickListener {
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        // Clear the entire activity stack and go to MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // Prevent going back to the session map
        navigateToHome()
    }

    private fun createCircleDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun updateFirebaseData() {
        // Check if we have a valid user ID
        if (userId.isEmpty()) {
            // Try to get current user ID from Firebase Auth
            userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }

        if (userId.isEmpty()) {
            // Cannot save without a user ID
            android.util.Log.e("SessionCompletion", "No user ID available, cannot save to Firestore")
            return
        }

        // Generate session ID if needed
        if (sessionId.isEmpty()) {
            sessionId = "walk_${System.currentTimeMillis()}"
        }

        android.util.Log.d("SessionCompletion", "Saving session: $sessionId for user: $userId with $points points")

        try {
            val currentTime = System.currentTimeMillis()
            // Get actual location name from intent, or use a meaningful default
            val locationName = intent.getStringExtra("locationName")
                ?: intent.getStringExtra("location")
                ?: "Walking Session"

            // Create WalkSession object
            val walkSession = WalkSession(
                sessionId = sessionId,
                locationName = locationName,
                startTime = currentTime - duration,  // Start time is current time minus duration
                pointsEarned = points.toDouble(),
                distance = distance,
                duration = duration
            )

            // Use DatabaseInitializer to add the walk session to Firestore
            DatabaseInitializer.addWalkSession(walkSession) { success ->
                if (success) {
                    android.util.Log.d("SessionCompletion", "Successfully saved session to Firestore!")
                    runOnUiThread {
                        Toast.makeText(this, "Session saved! You earned $points points!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.util.Log.e("SessionCompletion", "Failed to save session to Firestore")
                    runOnUiThread {
                        Toast.makeText(this, "Failed to save session data", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Migration complete - Realtime Database code removed
            // Only using Firestore now

            // Session data saved to Firestore via DatabaseInitializer.addWalkSession above
            // Legacy Realtime Database code removed - fully migrated to Firestore
        } catch (e: Exception) {
            Toast.makeText(this, "Error updating Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}