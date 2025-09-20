package com.example.myapplication.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentLeaderboardBinding
import com.example.myapplication.databinding.ItemCheckinBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.myapplication.models.KopiUser
import com.example.myapplication.models.WalkSession
import java.text.SimpleDateFormat
import java.util.*

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var recentSessionsListener: ValueEventListener? = null
    private var userPointsListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // Initialize Firebase
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            database = FirebaseDatabase.getInstance("https://wth2025-default-rtdb.firebaseio.com/").reference

            // Navigation buttons
            binding.btnBack.setOnClickListener {
                findNavController().popBackStack()
            }
            binding.btnHome.setOnClickListener {
                findNavController().navigate(R.id.nav_dashboard)
            }

            // RecyclerView must have a LayoutManager + Adapter
            binding.rvCheckins.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

            val adapter = SimpleTestAdapter()
            binding.rvCheckins.adapter = adapter

            // Load real-time data from Firebase only if user is authenticated
            if (auth.currentUser != null) {
                loadUserPoints()
                loadRecentSessions(adapter)
            } else {
                // Show default data if not authenticated
                showDefaultData(adapter)
            }
        } catch (e: Exception) {
            // Handle initialization errors
            e.printStackTrace()
            showDefaultData(SimpleTestAdapter().also { binding.rvCheckins.adapter = it })
        }
    }

    private fun loadUserPoints() {
        val userId = auth.currentUser?.uid ?: return

        // Load from Firestore instead of Realtime Database
        firestore.collection("kopi")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("LeaderboardFragment", "Error loading user data", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val points = snapshot.getDouble("points") ?: 0.0
                    val walkHistory = snapshot.get("walkHistory") as? List<Map<String, Any>> ?: emptyList()

                    // Update UI with real points
                    binding.tvPoints.text = "Points: ${points.toInt()}"

                    // Calculate weekly streak from walk history
                    val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                    val sessionsByDay = mutableSetOf<String>()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    for (sessionMap in walkHistory) {
                        val startTime = (sessionMap["startTime"] as? Long) ?: 0
                        if (startTime > oneWeekAgo) {
                            sessionsByDay.add(dateFormat.format(Date(startTime)))
                        }
                    }

                    val daysWithActivity = sessionsByDay.size
                    binding.tvStreak.text = "Weekly streak: $daysWithActivity day(s)"
                    binding.progStreak.max = 7
                    binding.progStreak.progress = daysWithActivity.coerceIn(0, 7)
                } else {
                    // No data yet
                    binding.tvPoints.text = "Points: 0"
                    binding.tvStreak.text = "Weekly streak: 0 day(s)"
                    binding.progStreak.progress = 0
                }
            }
    }

    private fun loadRecentSessions(adapter: SimpleTestAdapter) {
        val userId = auth.currentUser?.uid ?: return

        // Load walk history from Firestore
        firestore.collection("kopi")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("LeaderboardFragment", "Error loading sessions", error)
                    adapter.submit(
                        listOf(CheckinRow("Unable to load sessions", "Check your connection", 0))
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val walkHistory = snapshot.get("walkHistory") as? List<Map<String, Any>> ?: emptyList()
                    val allEvents = mutableListOf<CheckinRow>()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                    // Add walking sessions
                    for (sessionMap in walkHistory) {
                        val location = (sessionMap["locationName"] as? String) ?: "Walking Session"
                        val startTime = (sessionMap["startTime"] as? Long) ?: 0L
                        val pointsEarned = ((sessionMap["pointsEarned"] as? Double) ?: 0.0).toInt()
                        val distance = ((sessionMap["distance"] as? Double) ?: 0.0)

                        if (startTime > 0) {
                            val timeStr = dateFormat.format(Date(startTime))
                            val details = if (distance > 0) String.format("%.1f km walked", distance / 1000) else ""
                            allEvents.add(CheckinRow(location, timeStr, pointsEarned, details, false))
                        }
                    }

                    // Load and add redemption history
                    loadRedemptionHistory(userId, allEvents, adapter, dateFormat)
                } else {
                    // No data
                    adapter.submit(
                        listOf(CheckinRow("No recent activity", "Start walking or redeem rewards to see history!", 0))
                    )
                }
            }
    }

    private fun loadRedemptionHistory(userId: String, allEvents: MutableList<CheckinRow>, adapter: SimpleTestAdapter, dateFormat: SimpleDateFormat) {
        firestore.collection("kopi")
            .document(userId)
            .collection("coupons")
            .orderBy("redeemedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                // Add redemption events
                for (document in documents) {
                    val drinkName = document.getString("drinkName") ?: "Kopi"
                    val pointsUsed = document.getLong("pointsUsed")?.toInt() ?: 0
                    val redeemedAt = document.getLong("redeemedAt") ?: 0L

                    if (redeemedAt > 0) {
                        val timeStr = dateFormat.format(Date(redeemedAt))
                        allEvents.add(CheckinRow("Redeemed $drinkName", timeStr, pointsUsed, "Coupon redeemed", true))
                    }
                }

                // Sort all events by timestamp (most recent first) and take top 15
                val sortedEvents = allEvents.sortedByDescending { event ->
                    try {
                        dateFormat.parse(event.time)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }.take(15)

                // If no events, show placeholder
                if (sortedEvents.isEmpty()) {
                    adapter.submit(listOf(CheckinRow("No recent activity", "Start walking or redeem rewards to see history!", 0)))
                } else {
                    adapter.submit(sortedEvents)
                }
            }
            .addOnFailureListener {
                // If redemption loading fails, still show walking sessions
                val walkingSessions = allEvents.filter { !it.isRedemption }.take(10)
                if (walkingSessions.isEmpty()) {
                    adapter.submit(listOf(CheckinRow("No recent activity", "Start walking to see history!", 0)))
                } else {
                    adapter.submit(walkingSessions)
                }
            }
    }

    private fun showDefaultData(adapter: SimpleTestAdapter) {
        // Show default/demo data when not connected to Firebase
        binding.tvPoints.text = "Points: 0"
        binding.tvStreak.text = "Weekly streak: 0 day(s)"
        binding.progStreak.max = 7
        binding.progStreak.progress = 0

        adapter.submit(
            listOf(
                CheckinRow("Start walking to earn points!", "Join a session", 0)
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Firestore listeners are automatically cleaned up when using addSnapshotListener
        // No need to manually remove them
        _binding = null
    }
}

data class CheckinRow(val spot: String, val time: String, val pts: Int, val details: String = "", val isRedemption: Boolean = false)

class SimpleTestAdapter : RecyclerView.Adapter<SimpleTestAdapter.VH>() {
    private val items = mutableListOf<CheckinRow>()

    fun submit(data: List<CheckinRow>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemCheckinBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemCheckinBinding.inflate(inf, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        holder.b.tvSpot.text = row.spot
        holder.b.tvTime.text = row.time

        // Handle details TextView
        val detailsView = holder.b.root.findViewById<TextView?>(R.id.tvDetails)
        if (row.details.isNotEmpty()) {
            detailsView?.text = row.details
            detailsView?.visibility = View.VISIBLE
        } else {
            detailsView?.visibility = View.GONE
        }

        // Handle points display
        val pointsView = holder.b.root.findViewById<TextView?>(R.id.tvPointsRow)
        if (row.pts != 0) {
            pointsView?.text = if (row.isRedemption) "-${row.pts}" else "+${row.pts}"
            pointsView?.setTextColor(if (row.isRedemption) 0xFFB74423.toInt() else 0xFFD4A574.toInt())
        } else {
            pointsView?.text = ""
        }
    }
}
