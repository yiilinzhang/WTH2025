package com.example.myapplication

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapplication.databinding.ActivityScheduledWalksBinding
import com.example.myapplication.databinding.FragmentUpcomingWalksBinding
import com.example.myapplication.databinding.FragmentCreateWalkBinding
import com.example.myapplication.databinding.ItemScheduledWalkBinding
import com.example.myapplication.models.ScheduledWalk
import com.example.myapplication.util.DatabaseInitializer
import com.example.myapplication.adapters.ScheduledWalkAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ScheduledWalksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduledWalksBinding
    private lateinit var auth: FirebaseAuth
    private var myWalksFragment: MyWalksFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduledWalksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupViewPager()
        setupNavigation()
        setupClearButton()
    }

    private fun setupViewPager() {
        val adapter = ScheduledWalksPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "My Walks"
                1 -> "Create Walk"
                else -> ""
            }
        }.attach()

        // Note: Removed automatic refresh on tab change to prevent duplicate refreshes
    }

    private fun setupNavigation() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

    private fun setupClearButton() {
        binding.btnClearDatabase.setOnClickListener {
            // Show confirmation dialog
            android.app.AlertDialog.Builder(this)
                .setTitle("Clear Database")
                .setMessage("Are you sure you want to delete ALL scheduled walks? This action cannot be undone.")
                .setPositiveButton("Yes, Clear All") { _, _ ->
                    clearDatabase()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun clearDatabase() {
        binding.btnClearDatabase.isEnabled = false
        binding.btnClearDatabase.text = "Clearing..."

        DatabaseInitializer.clearAllScheduledWalks { success ->
            runOnUiThread {
                binding.btnClearDatabase.isEnabled = true
                binding.btnClearDatabase.text = "🗑️ Clear Database (Test)"

                if (success) {
                    Toast.makeText(this, "Database cleared successfully! 🧹", Toast.LENGTH_SHORT).show()

                    // Refresh the current tab to show empty state
                    myWalksFragment?.refreshWalks()
                } else {
                    Toast.makeText(this, "Failed to clear database", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun switchToMyWalksTab() {
        binding.viewPager.currentItem = 0
    }

    // Simple method to refresh just the MyWalks data
    fun refreshMyWalksData() {
        myWalksFragment?.takeIf { it.isAdded }?.refreshWalks()
    }


    fun refreshWalksAndSwitchToMyWalks() {
        // Switch to My Walks tab
        switchToMyWalksTab()

        // Schedule a single delayed refresh to ensure tab switch completes first
        binding.viewPager.postDelayed({
            myWalksFragment?.takeIf { it.isAdded }?.refreshWalks()
        }, 200)
    }

    private fun refreshCurrentFragment() {
        val currentItem = binding.viewPager.currentItem
        android.util.Log.d("ScheduledWalks", "refreshCurrentFragment: currentItem=$currentItem")

        if (currentItem == 0) {
            // Use stored reference first
            if (myWalksFragment?.isAdded == true) {
                android.util.Log.d("ScheduledWalks", "Using stored MyWalksFragment reference, calling refreshWalks()")
                myWalksFragment?.refreshWalks()
                return
            }

            // Fallback: try to find via fragment manager
            android.util.Log.d("ScheduledWalks", "Stored reference not available, searching fragments...")
            val possibleTags = listOf("f$currentItem", "f0", "android:switcher:${binding.viewPager.id}:$currentItem")
            var foundFragment: MyWalksFragment? = null

            for (tag in possibleTags) {
                val fragment = supportFragmentManager.findFragmentByTag(tag)
                android.util.Log.d("ScheduledWalks", "Trying tag: $tag, found: ${fragment?.javaClass?.simpleName}")
                if (fragment is MyWalksFragment) {
                    foundFragment = fragment
                    myWalksFragment = fragment // Update reference
                    break
                }
            }

            if (foundFragment != null) {
                android.util.Log.d("ScheduledWalks", "Found MyWalksFragment via tag search, calling refreshWalks()")
                foundFragment.refreshWalks()
            } else {
                android.util.Log.w("ScheduledWalks", "MyWalksFragment not found via tags!")
                // Last resort: iterate through all fragments
                supportFragmentManager.fragments.forEach { fragment ->
                    android.util.Log.d("ScheduledWalks", "Available fragment: ${fragment.javaClass.simpleName}, tag: ${fragment.tag}")
                    if (fragment is MyWalksFragment && fragment.isAdded) {
                        android.util.Log.d("ScheduledWalks", "Found MyWalksFragment via iteration, calling refreshWalks()")
                        myWalksFragment = fragment // Update reference
                        fragment.refreshWalks()
                    }
                }
            }
        }
    }

    private inner class ScheduledWalksPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    val fragment = MyWalksFragment()
                    myWalksFragment = fragment // Store reference
                    fragment
                }
                1 -> CreateWalkFragment()
                else -> MyWalksFragment()
            }
        }
    }
}


class MyWalksFragment : Fragment() {
    private var _binding: FragmentUpcomingWalksBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ScheduledWalkAdapter
    private lateinit var nearbyAdapter: ScheduledWalkAdapter
    private var isRefreshing = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUpcomingWalksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide filters for My Walks but show create button
        binding.btnShowAll.visibility = View.GONE
        binding.btnShowSoon.visibility = View.GONE

        setupRecyclerView()
        setupNearbySection()
        updateTitles()
        loadMyWalks()
        loadNearbyWalks()
    }


    override fun onResume() {
        super.onResume()
        // Refresh when tab becomes visible
        if (isAdded && !isHidden) {
            loadMyWalks()
            loadNearbyWalks()
        }
    }

    private fun setupRecyclerView() {
        adapter = ScheduledWalkAdapter(requireContext()) { walk, action ->
            when (action) {
                "cancel" -> cancelMyWalk(walk)
                "leave" -> leaveWalk(walk)
            }
        }
        binding.rvUpcomingWalks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUpcomingWalks.adapter = adapter
    }

    private fun setupNearbySection() {
        nearbyAdapter = ScheduledWalkAdapter(requireContext()) { walk, action ->
            when (action) {
                "join" -> joinNearbyWalk(walk)
                "cancel" -> cancelRsvp(walk)
            }
        }
        binding.rvWalksNearby.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWalksNearby.adapter = nearbyAdapter
        binding.llWalksNearby.visibility = View.VISIBLE
    }

    private fun updateTitles() {
        binding.tvWalksTitle.text = "My Walking Sessions"
    }

    private fun loadMyWalks() {
        android.util.Log.d("MyWalksFragment", "loadMyWalks() called")
        // Load both created walks and RSVP'd walks
        DatabaseInitializer.getMyScheduledWalks { createdWalks ->
            android.util.Log.d("MyWalksFragment", "getMyScheduledWalks returned ${createdWalks.size} walks")
            DatabaseInitializer.getMyRsvpWalks { rsvpWalks ->
                android.util.Log.d("MyWalksFragment", "getMyRsvpWalks returned ${rsvpWalks.size} walks")
                requireActivity().runOnUiThread {
                    val allMyWalks = (createdWalks + rsvpWalks).distinctBy { it.walkId }
                        .sortedBy { it.scheduledTime }
                    android.util.Log.d("MyWalksFragment", "Total combined walks: ${allMyWalks.size}")
                    adapter.submitList(allMyWalks)
                }
            }
        }
    }

    private fun cancelMyWalk(walk: ScheduledWalk) {
        // TODO: Add confirmation dialog
        Toast.makeText(requireContext(), "Walk cancellation feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun leaveWalk(walk: ScheduledWalk) {
        DatabaseInitializer.cancelRsvp(walk.walkId) { success, message ->
            requireActivity().runOnUiThread {
                if (success) {
                    Toast.makeText(requireContext(), "Left the walk", Toast.LENGTH_SHORT).show()
                    loadMyWalks()
                    loadNearbyWalks() // Refresh nearby walks as well
                } else {
                    Toast.makeText(requireContext(), message ?: "Failed to leave walk", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadNearbyWalks() {
        DatabaseInitializer.getUpcomingWalks { allWalks ->
            DatabaseInitializer.getMyScheduledWalks { myCreatedWalks ->
                DatabaseInitializer.getMyRsvpWalks { myRsvpWalks ->
                    requireActivity().runOnUiThread {
                        val myWalkIds = (myCreatedWalks + myRsvpWalks).map { it.walkId }.toSet()
                        val nearbyWalks = allWalks.filter { it.walkId !in myWalkIds }
                            .sortedBy { it.scheduledTime }
                            .take(3) // Show only top 3 nearby walks
                        nearbyAdapter.submitList(nearbyWalks)
                    }
                }
            }
        }
    }

    private fun joinNearbyWalk(walk: ScheduledWalk) {
        DatabaseInitializer.rsvpToWalk(walk.walkId) { success, message ->
            requireActivity().runOnUiThread {
                if (success) {
                    Toast.makeText(requireContext(), "Successfully joined the walk!", Toast.LENGTH_SHORT).show()
                    loadMyWalks()
                    loadNearbyWalks()
                } else {
                    Toast.makeText(requireContext(), message ?: "Failed to join walk", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cancelRsvp(walk: ScheduledWalk) {
        DatabaseInitializer.cancelRsvp(walk.walkId) { success, message ->
            requireActivity().runOnUiThread {
                if (success) {
                    Toast.makeText(requireContext(), "RSVP cancelled", Toast.LENGTH_SHORT).show()
                    loadMyWalks()
                    loadNearbyWalks()
                } else {
                    Toast.makeText(requireContext(), message ?: "Failed to cancel RSVP", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun refreshWalks() {
        // Prevent multiple rapid refreshes
        if (isRefreshing) {
            android.util.Log.d("MyWalksFragment", "refreshWalks() called but already refreshing, skipping")
            return
        }

        android.util.Log.d("MyWalksFragment", "refreshWalks() called")
        isRefreshing = true
        loadMyWalks()
        loadNearbyWalks()

        // Reset refresh flag after a delay
        binding.root.postDelayed({
            isRefreshing = false
        }, 1000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CreateWalkFragment : Fragment() {
    private var _binding: FragmentCreateWalkBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    private var selectedDate = Calendar.getInstance()
    private var selectedTime = Calendar.getInstance()
    private var selectedLocation = "East Coast Park"
    private var selectedDateTime = Calendar.getInstance()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateWalkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setupLocationButtons()
        setupTimeButtons()
        setupCreateButton()
    }

    private fun setupLocationButtons() {
        // Set default
        selectedLocation = "East Coast Park"
        binding.btnEastCoast.setBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))

        binding.btnEastCoast.setOnClickListener {
            selectLocation("East Coast Park", binding.btnEastCoast)
        }
        binding.btnBotanic.setOnClickListener {
            selectLocation("Botanic Gardens", binding.btnBotanic)
        }
        binding.btnBishan.setOnClickListener {
            selectLocation("Bishan Park", binding.btnBishan)
        }
    }

    private fun selectLocation(location: String, button: Button) {
        selectedLocation = location

        // Reset all buttons
        binding.btnEastCoast.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
        binding.btnBotanic.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
        binding.btnBishan.setBackgroundColor(resources.getColor(android.R.color.transparent, null))

        // Highlight selected button
        button.setBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
    }

    private fun setupTimeButtons() {
        // Set default to tomorrow morning
        selectedDateTime = Calendar.getInstance()
        selectedDateTime.add(Calendar.DAY_OF_MONTH, 1)
        selectedDateTime.set(Calendar.HOUR_OF_DAY, 7)
        selectedDateTime.set(Calendar.MINUTE, 0)

        binding.btnTomorrowMorning.setBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))

        binding.btnTomorrowMorning.setOnClickListener {
            selectTime("morning")
        }
        binding.btnTomorrowEvening.setOnClickListener {
            selectTime("evening")
        }
        binding.btnChooseDateTime.setOnClickListener {
            showDateTimePicker()
        }
    }

    private fun selectTime(timeOfDay: String) {
        selectedDateTime = Calendar.getInstance()
        selectedDateTime.add(Calendar.DAY_OF_MONTH, 1)

        when (timeOfDay) {
            "morning" -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, 7)
                selectedDateTime.set(Calendar.MINUTE, 0)

                // Reset buttons
                binding.btnTomorrowMorning.setBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
                binding.btnTomorrowEvening.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            }
            "evening" -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, 18)
                selectedDateTime.set(Calendar.MINUTE, 0)

                // Reset buttons
                binding.btnTomorrowMorning.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
                binding.btnTomorrowEvening.setBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
            }
        }
    }

    private fun showDateTimePicker() {
        val now = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        selectedDateTime.set(year, month, day, hour, minute)

                        // Reset time buttons since custom time was selected
                        binding.btnTomorrowMorning.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
                        binding.btnTomorrowEvening.setBackgroundColor(resources.getColor(android.R.color.transparent, null))

                        Toast.makeText(requireContext(), "Custom time selected!", Toast.LENGTH_SHORT).show()
                    },
                    7, 0, false
                ).show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }

    private fun setupCreateButton() {
        binding.btnCreateWalk.setOnClickListener {
            createWalk()
        }
    }

    private fun createWalk() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please log in to create a walk", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDateTime.timeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(requireContext(), "Please select a future date and time", Toast.LENGTH_SHORT).show()
            return
        }

        // Get user's display name or email
        val creatorName = currentUser.displayName ?: currentUser.email?.substringBefore('@') ?: "Walking Kaki"

        val scheduledWalk = ScheduledWalk(
            creatorId = currentUser.uid,
            creatorName = creatorName,
            locationName = selectedLocation,
            scheduledTime = selectedDateTime.timeInMillis,
            maxParticipants = 6, // Fixed at 6 for simplicity
            currentParticipants = listOf(currentUser.uid), // Creator auto-joins
            description = "", // No description for simplicity
            isRecurring = false, // No recurring for simplicity
            recurringDays = emptyList(),
            status = "ACTIVE",
            createdAt = System.currentTimeMillis()
        )

        binding.btnCreateWalk.isEnabled = false
        binding.btnCreateWalk.text = "Creating..."

        DatabaseInitializer.createScheduledWalk(scheduledWalk) { success, walkId ->
            requireActivity().runOnUiThread {
                binding.btnCreateWalk.isEnabled = true
                binding.btnCreateWalk.text = "🚶‍♀️ Create Walking Session 🚶‍♂️"

                if (success) {
                    Toast.makeText(requireContext(), "Walking session created! 🎉", Toast.LENGTH_LONG).show()
                    clearForm()

                    // Switch to "My Walks" tab and refresh to show the new walk
                    val activity = requireActivity() as ScheduledWalksActivity
                    activity.refreshWalksAndSwitchToMyWalks() // Single refresh call
                } else {
                    Toast.makeText(requireContext(), "Failed to create walk. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun clearForm() {
        // Reset to defaults
        selectedLocation = "East Coast Park"
        selectedDateTime = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
        }

        // Reset UI
        selectLocation("East Coast Park", binding.btnEastCoast)
        selectTime("morning")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}