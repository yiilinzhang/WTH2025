package com.example.myapplication.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemScheduledWalkBinding
import com.example.myapplication.models.ScheduledWalk
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ScheduledWalkAdapter(
    private val context: Context,
    private val onActionClick: (ScheduledWalk, String) -> Unit
) : ListAdapter<ScheduledWalk, ScheduledWalkAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(val binding: ItemScheduledWalkBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScheduledWalkBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val walk = getItem(position)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        with(holder.binding) {
            // Location with emoji
            val locationEmoji = when {
                walk.locationName.contains("East Coast") -> "🌊"
                walk.locationName.contains("Botanic") -> "🌺"
                walk.locationName.contains("Bishan") -> "🌳"
                else -> "📍"
            }
            tvLocation.text = "$locationEmoji ${walk.locationName}"

            // Date and Time - Simplified
            val dateTime = formatSimpleDateTime(walk.scheduledTime)
            tvDateTime.text = dateTime

            // Participants - Simple count
            val participantCount = walk.currentParticipants.size
            tvParticipants.text = "👥 $participantCount walking kakis joined so far"

            // Action Button - Simplified
            setupSimpleActionButton(walk, currentUserId, btnAction)
        }
    }

    private fun setupSimpleActionButton(
        walk: ScheduledWalk,
        currentUserId: String?,
        actionButton: android.widget.Button
    ) {
        val isCreator = walk.creatorId == currentUserId
        val isParticipant = walk.currentParticipants.contains(currentUserId)
        val isFull = walk.currentParticipants.size >= walk.maxParticipants
        val isPastWalk = walk.scheduledTime < System.currentTimeMillis()

        when {
            isPastWalk -> {
                actionButton.text = "⏰ Walk Finished"
                actionButton.isEnabled = false
                actionButton.setBackgroundColor(context.getColor(android.R.color.darker_gray))
            }
            isCreator -> {
                actionButton.text = "👑 Your Walk!"
                actionButton.isEnabled = true
                actionButton.setBackgroundColor(context.getColor(android.R.color.holo_green_dark))
                actionButton.setOnClickListener {
                    onActionClick(walk, "manage")
                }
            }
            isParticipant -> {
                actionButton.text = "❌ Leave Walk"
                actionButton.isEnabled = true
                actionButton.setBackgroundColor(context.getColor(android.R.color.holo_red_light))
                actionButton.setOnClickListener {
                    onActionClick(walk, "cancel")
                }
            }
            isFull -> {
                actionButton.text = "😔 Walk Full"
                actionButton.isEnabled = false
                actionButton.setBackgroundColor(context.getColor(android.R.color.darker_gray))
            }
            else -> {
                actionButton.text = "🚶‍♀️ Join This Walk!"
                actionButton.isEnabled = true
                actionButton.setBackgroundColor(context.getColor(android.R.color.holo_green_dark))
                actionButton.setOnClickListener {
                    onActionClick(walk, "join")
                }
            }
        }
    }

    private fun formatSimpleDateTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeString = timeFormat.format(calendar.time)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val timeEmoji = when {
            hour in 5..11 -> "🌅"
            hour in 17..21 -> "🌆"
            else -> "⏰"
        }

        val timeOfDay = when {
            hour in 5..11 -> "Morning"
            hour in 17..21 -> "Evening"
            else -> "Night"
        }

        return when {
            isSameDay(timestamp, now) -> "$timeEmoji Today $timeOfDay, $timeString"
            isSameDay(timestamp, now + TimeUnit.DAYS.toMillis(1)) -> "$timeEmoji Tomorrow $timeOfDay, $timeString"
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                "$timeEmoji ${dateFormat.format(calendar.time)} $timeOfDay, $timeString"
            }
        }
    }

    private fun formatDateTime(timestamp: Long): Pair<String, String> {
        val now = System.currentTimeMillis()
        val diff = timestamp - now

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeString = timeFormat.format(calendar.time)

        val dateString = when {
            isSameDay(timestamp, now) -> "Today, $timeString"
            isSameDay(timestamp, now + TimeUnit.DAYS.toMillis(1)) -> "Tomorrow, $timeString"
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                "${dayFormat.format(calendar.time)}, $timeString"
            }
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                "${dateFormat.format(calendar.time)}, $timeString"
            }
        }

        val timeFromNow = when {
            diff < 0 -> "started"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "in ${minutes}min"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "in ${hours}h"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "in ${days}d"
            }
            else -> {
                val weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7
                "in ${weeks}w"
            }
        }

        return Pair(dateString, timeFromNow)
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getTimeOfDay(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return when {
            hour in 5..11 -> "Morning"
            hour in 12..16 -> "Afternoon"
            hour in 17..21 -> "Evening"
            else -> "Night"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ScheduledWalk>() {
        override fun areItemsTheSame(oldItem: ScheduledWalk, newItem: ScheduledWalk): Boolean {
            return oldItem.walkId == newItem.walkId
        }

        override fun areContentsTheSame(oldItem: ScheduledWalk, newItem: ScheduledWalk): Boolean {
            return oldItem == newItem
        }
    }
}