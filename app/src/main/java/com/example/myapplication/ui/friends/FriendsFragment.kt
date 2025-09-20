package com.example.myapplication.ui.friends

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentFriendsBinding
import com.example.myapplication.models.Friend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class FriendsFragment : Fragment() {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var friendsListener: ListenerRegistration? = null
    private lateinit var friendsAdapter: FriendsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Navigation buttons
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.nav_dashboard)
        }

        // Setup RecyclerView
        friendsAdapter = FriendsAdapter { friend ->
            removeFriend(friend)
        }
        binding.rvFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFriends.adapter = friendsAdapter

        // Add friend button
        binding.btnAddFriend.setOnClickListener {
            addFriend()
        }

        // Load friends
        loadFriends()
    }

    private fun addFriend() {
        val email = binding.etFriendEmail.text.toString().trim()
        if (email.isEmpty()) {
            Toast.makeText(context, "Please enter an email address", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(context, "You must be logged in to add friends", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if user exists and get their info
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "User not found with that email", Toast.LENGTH_SHORT).show()
                } else {
                    val userDoc = documents.first()
                    val friendId = userDoc.id
                    val friendName = userDoc.getString("name") ?: email.substringBefore("@")

                    if (friendId == currentUserId) {
                        Toast.makeText(context, "You can't add yourself as a friend!", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Add friend to current user's friends list
                    val friendData = Friend(
                        friendId = friendId,
                        friendName = friendName,
                        friendEmail = email,
                        addedAt = System.currentTimeMillis(),
                        status = "ACCEPTED"
                    )

                    db.collection("kopi")
                        .document(currentUserId)
                        .collection("friends")
                        .document(friendId)
                        .set(friendData)
                        .addOnSuccessListener {
                            // Add current user to friend's friends list (mutual)
                            val currentUserEmail = auth.currentUser?.email ?: ""
                            val currentUserName = auth.currentUser?.displayName ?: currentUserEmail.substringBefore("@")

                            val mutualFriendData = Friend(
                                friendId = currentUserId,
                                friendName = currentUserName,
                                friendEmail = currentUserEmail,
                                addedAt = System.currentTimeMillis(),
                                status = "ACCEPTED"
                            )

                            db.collection("kopi")
                                .document(friendId)
                                .collection("friends")
                                .document(currentUserId)
                                .set(mutualFriendData)

                            binding.etFriendEmail.text?.clear()
                            Toast.makeText(context, "$friendName added as Kopi Kaki! ☕", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to add friend: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error searching for user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadFriends() {
        val userId = auth.currentUser?.uid ?: return

        friendsListener = db.collection("kopi")
            .document(userId)
            .collection("friends")
            .whereEqualTo("status", "ACCEPTED")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading friends", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val friends = mutableListOf<Friend>()
                snapshot?.documents?.forEach { document ->
                    try {
                        val friend = document.toObject(Friend::class.java)
                        if (friend != null) {
                            friends.add(friend)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FriendsFragment", "Error parsing friend", e)
                    }
                }

                friendsAdapter.updateFriends(friends)
                updateEmptyState(friends.isEmpty())
            }
    }

    private fun removeFriend(friend: Friend) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("kopi")
            .document(userId)
            .collection("friends")
            .document(friend.friendId)
            .delete()
            .addOnSuccessListener {
                // Also remove from friend's list
                db.collection("kopi")
                    .document(friend.friendId)
                    .collection("friends")
                    .document(userId)
                    .delete()

                Toast.makeText(context, "${friend.friendName} removed from Kopi Kakis", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to remove friend: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.rvFriends.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.VISIBLE
        } else {
            binding.rvFriends.visibility = View.VISIBLE
            binding.emptyStateContainer.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        friendsListener?.remove()
        _binding = null
    }
}

class FriendsAdapter(private val onRemoveFriend: (Friend) -> Unit) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {
    private val friends = mutableListOf<Friend>()

    fun updateFriends(newFriends: List<Friend>) {
        friends.clear()
        friends.addAll(newFriends)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val cardView = CardView(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(Color.WHITE)
        }
        return FriendViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(friends[position], onRemoveFriend)
    }

    override fun getItemCount() = friends.size

    class FriendViewHolder(private val cardView: CardView) : RecyclerView.ViewHolder(cardView) {
        fun bind(friend: Friend, onRemove: (Friend) -> Unit) {
            val context = cardView.context
            cardView.removeAllViews()

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(20, 20, 20, 20)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Profile icon
            val profileIcon = TextView(context).apply {
                text = friend.friendName.take(1).uppercase()
                textSize = 20f
                setTextColor(Color.WHITE)
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(0, 0, 16, 0)
                }
                background = createCircleDrawable(Color.parseColor("#8B5A3C"))
            }

            // Friend info
            val infoContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val nameText = TextView(context).apply {
                text = friend.friendName
                textSize = 18f
                setTextColor(Color.parseColor("#6B4423"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val emailText = TextView(context).apply {
                text = friend.friendEmail
                textSize = 14f
                setTextColor(Color.parseColor("#8B5A3C"))
            }

            val dateText = TextView(context).apply {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                text = "Added: ${dateFormat.format(Date(friend.addedAt))}"
                textSize = 12f
                setTextColor(Color.parseColor("#999999"))
            }

            infoContainer.addView(nameText)
            infoContainer.addView(emailText)
            infoContainer.addView(dateText)

            // Remove button
            val removeButton = TextView(context).apply {
                text = "✕"
                textSize = 18f
                setTextColor(Color.parseColor("#CC6666"))
                setPadding(16, 16, 16, 16)
                background = createCircleDrawable(Color.parseColor("#FFEEEE"))
                setOnClickListener {
                    onRemove(friend)
                }
            }

            container.addView(profileIcon)
            container.addView(infoContainer)
            container.addView(removeButton)
            cardView.addView(container)
        }

        private fun createCircleDrawable(color: Int): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
        }
    }
}