package com.example.myapplication.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.SessionStartActivity
import com.example.myapplication.ScheduledWalksActivity
import com.example.myapplication.FriendsListActivity
import com.example.myapplication.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Setup join walking session button
        val joinButton: Button = binding.btnJoinWalkingSession
        joinButton.setOnClickListener {
            val intent = Intent(requireContext(), SessionStartActivity::class.java)
            startActivity(intent)
        }

        // Setup friends button
        binding.btnFriends.setOnClickListener {
            val intent = Intent(requireContext(), FriendsListActivity::class.java)
            startActivity(intent)
        }

        // Setup leaderboard button
        binding.btnLeaderboard.setOnClickListener {
            findNavController().navigate(R.id.nav_leaderboard)
        }

        // Setup schedule button - now goes directly to My Walks tab
        binding.btnSchedule.setOnClickListener {
            val intent = Intent(requireContext(), ScheduledWalksActivity::class.java)
            startActivity(intent)
        }

        // Setup rewards button
        binding.btnRewards.setOnClickListener {
            findNavController().navigate(R.id.nav_rewards)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}