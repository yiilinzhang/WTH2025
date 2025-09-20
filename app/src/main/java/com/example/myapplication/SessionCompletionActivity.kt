package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import android.util.TypedValue
import android.graphics.drawable.GradientDrawable

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
    private lateinit var database: DatabaseReference

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

        // Get data from intent
        sessionId = intent.getStringExtra("sessionId") ?: ""
        userId = intent.getStringExtra("userId") ?: ""
        distance = intent.getDoubleExtra("distance", 0.0)
        duration = intent.getLongExtra("duration", 0L)

        // Calculate points (1 point per 100 meters)
        points = (distance / 100).toInt()

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference

        // Display stats
        displayStats()

        // Load participants
        loadParticipants()

        // Setup button listeners
        setupButtons()
    }

    private fun displayStats() {
        tvPointsEarned.text = "You've earned $points points!"
        tvDistanceValue.text = String.format("%.2f km", distance / 1000)

        val minutes = duration / 60000
        val seconds = (duration % 60000) / 1000
        tvDurationValue.text = String.format("%02d:%02d", minutes, seconds)

        // Calculate estimated steps (average stride length ~0.76m, so ~1.3 steps per meter)
        // More realistic: 2000 steps per km, or 2 steps per meter
        val steps = (distance * 1.3).toInt()
        tvStepsValue.text = steps.toString()
    }

    private fun loadParticipants() {
        if (sessionId.isEmpty()) {
            // Mock mode - show sample participants
            addParticipantRow("John Walker", "john123", true)
            addParticipantRow("Sarah Runner", "sarah456", false)
            addParticipantRow("Mike Jogger", "mike789", false)
            return
        }

        // Firebase mode - load real participants
        database.child("sessions").child(sessionId).child("participants")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    friendsListContainer.removeAllViews()

                    for (participantSnapshot in snapshot.children) {
                        val participantId = participantSnapshot.key ?: continue
                        if (participantId != userId) {
                            val name = participantSnapshot.child("name").getValue(String::class.java)
                                ?: "Unknown User"
                            val isAlreadyFriend = participantSnapshot.child("friends")
                                .hasChild(userId)

                            addParticipantRow(name, participantId, isAlreadyFriend)
                        }
                    }

                    if (friendsListContainer.childCount == 0) {
                        val noParticipantsText = TextView(this@SessionCompletionActivity).apply {
                            text = "No other companions on this walk"
                            gravity = Gravity.CENTER
                            setPadding(24, 48, 24, 48)
                            textSize = 20f
                            setTextColor(Color.parseColor("#7A8B9A"))
                            setBackgroundColor(Color.WHITE)
                        }
                        friendsListContainer.addView(noParticipantsText)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SessionCompletionActivity,
                        "Failed to load participants", Toast.LENGTH_SHORT).show()
                }
            })
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
                setBackgroundColor(Color.parseColor("#E8F5F2"))
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
            background = createCircleDrawable(Color.parseColor("#67AB9F"))
        }

        val nameText = TextView(this).apply {
            text = name
            textSize = 22f
            setTextColor(Color.parseColor("#2C3E50"))
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
                setBackgroundColor(Color.parseColor("#D0E8E0"))
                setTextColor(Color.parseColor("#67AB9F"))
            } else {
                text = "+ Add"
                setBackgroundColor(Color.parseColor("#67AB9F"))
                setTextColor(Color.WHITE)

                setOnClickListener {
                    addFriend(participantId, name)
                    text = "✓ Added"
                    isEnabled = false
                    setBackgroundColor(Color.parseColor("#67AB9F"))
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
                setBackgroundColor(Color.parseColor("#E8F5F2"))
            }
            friendsListContainer.addView(divider)
        }
    }

    private fun addFriend(friendId: String, friendName: String) {
        addedFriends.add(friendId)

        if (sessionId.isNotEmpty()) {
            // Add to Firebase
            val updates = hashMapOf<String, Any>(
                "users/$userId/friends/$friendId" to mapOf(
                    "name" to friendName,
                    "addedAt" to ServerValue.TIMESTAMP
                ),
                "users/$friendId/friends/$userId" to mapOf(
                    "name" to "You",
                    "addedAt" to ServerValue.TIMESTAMP
                )
            )

            database.updateChildren(updates)
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
            // Go back to main activity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun createCircleDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }
}