package com.example.myapplication.ui.friends

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.FriendsListActivity

class FriendsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Navigate directly to FriendsListActivity
        val intent = Intent(requireContext(), FriendsListActivity::class.java)
        startActivity(intent)

        // Pop back to prevent blank fragment
        requireActivity().onBackPressed()

        return null
    }
}