package com.example.myapplication

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.graphics.drawable.GradientDrawable

class FriendsListActivity : AppCompatActivity() {

    private lateinit var friendsListContainer: LinearLayout
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var btnAddFriend: Button
    private lateinit var backButton: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val friendsList = mutableListOf<Friend>()

    data class Friend(
        val id: String = "",
        val name: String = "",
        val addedAt: Long = 0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends_list)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize views
        friendsListContainer = findViewById(R.id.friendsListContainer)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        btnAddFriend = findViewById(R.id.btnAddFriend)
        backButton = findViewById(R.id.backButton)

        // Setup button listeners
        backButton.setOnClickListener {
            finish()
        }

        btnAddFriend.setOnClickListener {
            showAddFriendDialog()
        }

        // Load friends
        loadFriends()
    }

    private fun loadFriends() {
        // Check if using mock data
        if (auth.currentUser == null) {
            // Mock friends for testing
            displayMockFriends()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        database.child("users").child(userId).child("friends")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    friendsList.clear()
                    friendsListContainer.removeAllViews()

                    for (friendSnapshot in snapshot.children) {
                        val friend = friendSnapshot.getValue(Friend::class.java)
                        friend?.let {
                            friendsList.add(it)
                            addFriendRow(it)
                        }
                    }

                    // Show empty state if no friends
                    if (friendsList.isEmpty()) {
                        emptyStateContainer.visibility = View.VISIBLE
                    } else {
                        emptyStateContainer.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@FriendsListActivity,
                        "Failed to load friends", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun displayMockFriends() {
        // Sample friends for demo
        val mockFriends = listOf(
            Friend("1", "Uncle Tan", System.currentTimeMillis()),
            Friend("2", "Auntie Mary", System.currentTimeMillis()),
            Friend("3", "Mr. Lim", System.currentTimeMillis())
        )

        friendsListContainer.removeAllViews()
        mockFriends.forEach { friend ->
            addFriendRow(friend)
        }
        emptyStateContainer.visibility = View.GONE
    }

    private fun addFriendRow(friend: Friend) {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
            elevation = 4f
        }

        // Profile icon
        val profileIcon = TextView(this).apply {
            text = friend.name.firstOrNull()?.toString()?.uppercase() ?: "?"
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

        // Friend details container
        val detailsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // Friend name
        val nameText = TextView(this).apply {
            text = friend.name
            textSize = 20f
            setTextColor(Color.parseColor("#3E2723"))
        }

        // Status text
        val statusText = TextView(this).apply {
            text = "Ready for kopi walk!"
            textSize = 14f
            setTextColor(Color.parseColor("#8B6B47"))
        }

        detailsContainer.addView(nameText)
        detailsContainer.addView(statusText)

        // Remove friend button
        val removeButton = Button(this).apply {
            text = "Remove"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            setBackgroundColor(Color.parseColor("#D4A574"))
            setTextColor(Color.WHITE)
            setPadding(24, 8, 24, 8)

            setOnClickListener {
                showRemoveFriendDialog(friend)
            }
        }

        rowLayout.addView(profileIcon)
        rowLayout.addView(detailsContainer)
        rowLayout.addView(removeButton)

        friendsListContainer.addView(rowLayout)
    }

    private fun showAddFriendDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val titleText = TextView(this).apply {
            text = "Add Friend by Code"
            textSize = 18f
            setTextColor(Color.parseColor("#6B4423"))
            setPadding(0, 0, 0, 16)
        }

        val friendCodeInput = EditText(this).apply {
            hint = "Enter friend code"
            textSize = 16f
        }

        dialogView.addView(titleText)
        dialogView.addView(friendCodeInput)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val friendCode = friendCodeInput.text.toString()
                if (friendCode.isNotEmpty()) {
                    addFriendByCode(friendCode)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun addFriendByCode(code: String) {
        // In mock mode, just add a sample friend
        if (auth.currentUser == null) {
            val newFriend = Friend(code, "Friend #$code", System.currentTimeMillis())
            addFriendRow(newFriend)
            emptyStateContainer.visibility = View.GONE
            Toast.makeText(this, "Friend added!", Toast.LENGTH_SHORT).show()
            return
        }

        // In real mode, would look up user by code and add to Firebase
        Toast.makeText(this, "Friend code: $code", Toast.LENGTH_SHORT).show()
    }

    private fun showRemoveFriendDialog(friend: Friend) {
        AlertDialog.Builder(this)
            .setTitle("Remove Friend")
            .setMessage("Remove ${friend.name} from your kopi kakis?")
            .setPositiveButton("Remove") { dialog, _ ->
                removeFriend(friend)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun removeFriend(friend: Friend) {
        if (auth.currentUser == null) {
            // Mock mode - just refresh the list
            loadFriends()
            Toast.makeText(this, "${friend.name} removed", Toast.LENGTH_SHORT).show()
            return
        }

        // Real mode - remove from Firebase
        val userId = auth.currentUser?.uid ?: return
        database.child("users").child(userId).child("friends").child(friend.id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "${friend.name} removed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createCircleDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }
}