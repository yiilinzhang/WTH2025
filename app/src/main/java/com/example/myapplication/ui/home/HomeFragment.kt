package com.example.myapplication.ui.home

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
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.databinding.ItemCheckinBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView must have a LayoutManager + Adapter
        binding.rvCheckins.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val adapter = SimpleTestAdapter()
        binding.rvCheckins.adapter = adapter

        // TEMP: feed dummy rows to confirm it renders
        adapter.submit(
            listOf(
                CheckinRow("Void Deck 123", "2025-09-20 10:12", 5),
                CheckinRow("Park Connector 5", "2025-09-20 09:30", 5),
                CheckinRow("Active Aging Hub", "2025-09-19 18:05", 5)
            )
        )

        // Update points + streak UI (demo logic)
        val totalPoints = 15
        binding.tvPoints.text = "Points: $totalPoints"
        val streak = 2
        binding.tvStreak.text = "Weekly streak: $streak day(s)"
        binding.progStreak.max = 7
        binding.progStreak.progress = streak.coerceIn(0, 7)

        // Quick actions
        binding.btnGoScan.setOnClickListener {
            findNavController().navigate(R.id.nav_gallery)
        }
        binding.btnGoRedeem.setOnClickListener {
            findNavController().navigate(R.id.nav_slideshow)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class CheckinRow(val spot: String, val time: String, val pts: Int)

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
        // If you added a points pill TextView with id tvPointsRow:
        val pointsView = holder.b.root.findViewById<TextView?>(R.id.tvPointsRow)
        pointsView?.text = "+${row.pts}"
    }
}
