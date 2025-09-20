package com.example.myapplication.ui.walking

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.myapplication.SessionStartActivity
import com.example.myapplication.databinding.FragmentWalkingMainBinding

class WalkingMainFragment : Fragment() {

    private var _binding: FragmentWalkingMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalkingMainBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Setup join walking session button
        val joinButton: Button = binding.btnJoinWalkingSession
        joinButton.setOnClickListener {
            val intent = Intent(requireContext(), SessionStartActivity::class.java)
            startActivity(intent)
        }

        // Setup friends button if present
        binding.btnFriends?.setOnClickListener {
            // Navigate to friends through the nav drawer
            requireActivity().onBackPressed()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}