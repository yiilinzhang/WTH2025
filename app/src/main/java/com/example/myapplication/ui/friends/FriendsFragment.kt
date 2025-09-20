package com.example.myapplication.ui.friends

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.FriendsListActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentFriendsBinding

class FriendsFragment : Fragment() {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

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

        // Navigate directly to FriendsListActivity
        val intent = Intent(requireContext(), FriendsListActivity::class.java)
        startActivity(intent)

        // Navigation buttons
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.nav_dashboard)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}