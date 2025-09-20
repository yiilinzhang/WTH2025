package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Mock version of SessionDetailsActivity that works without Firebase
 */
class SessionDetailsActivityMock : AppCompatActivity() {

    private lateinit var sessionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.session_details)

        sessionId = intent.getStringExtra("SESSION_ID") ?: ""
        if (sessionId.isEmpty()) {
            finish()
            return
        }

        setupViews()
        loadMockSessionData()
        setupButtons()
    }

    private fun setupViews() {
        findViewById<TextView>(R.id.sessionCode).text = sessionId
    }

    private fun loadMockSessionData() {
        // Get mock data
        MockDataManager.getSession(sessionId) { sessionData ->
            if (sessionData != null) {
                runOnUiThread {
                    updateParticipantsUI(sessionData)
                }
            }
        }
    }

    private fun updateParticipantsUI(sessionData: Map<String, Any>) {
        val participants = sessionData["participants"] as? Map<String, Any> ?: return

        val participantCount = participants.size
        findViewById<TextView>(R.id.participantCount).text =
            "$participantCount people in session"

        // Update participant list (showing first 3)
        val participantViews = listOf(
            findViewById<TextView>(R.id.participant1),
            findViewById<TextView>(R.id.participant2),
            findViewById<TextView>(R.id.participant3)
        )

        val participantsList = participants.values.toList()
        participantsList.take(3).forEachIndexed { index, participant ->
            val participantMap = participant as Map<String, Any>
            val userName = participantMap["userName"] as? String ?: "Unknown"
            val isHost = participantMap["isHost"] as? Boolean ?: false
            participantViews[index].text = "👤 $userName${if (isHost) " (Host)" else ""}"
        }

        // Hide unused participant views
        for (i in participantCount until participantViews.size) {
            participantViews[i].visibility = TextView.GONE
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.inviteButton).setOnClickListener {
            shareSessionCode()
        }

        findViewById<Button>(R.id.startWalkButton).setOnClickListener {
            startWalkingSession()
        }
    }

    private fun shareSessionCode() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT,
            "Join my walking session! Session code: $sessionId")
        startActivity(Intent.createChooser(shareIntent, "Share session code"))
    }

    private fun startWalkingSession() {
        MockDataManager.startSession(sessionId) { success ->
            if (success) {
                navigateToMap()
            }
        }
    }

    private fun navigateToMap() {
        val intent = Intent(this, SessionMapActivityOSM::class.java)
        intent.putExtra("SESSION_ID", sessionId)
        startActivity(intent)
    }
}