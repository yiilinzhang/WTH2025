package com.example.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Color

class SessionDetailsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var sessionId: String
    private lateinit var sessionRef: DatabaseReference
    private var participantsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.session_details)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://wth2025-default-rtdb.firebaseio.com/")

        sessionId = intent.getStringExtra("SESSION_ID") ?: ""
        if (sessionId.isEmpty()) {
            finish()
            return
        }

        sessionRef = database.reference.child("sessions").child(sessionId)

        setupViews()
        loadSessionData()
        setupButtons()
    }

    private fun setupViews() {
        findViewById<TextView>(R.id.sessionCode).text = sessionId
    }

    private fun loadSessionData() {
        participantsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val participants = snapshot.child("participants").children
                updateParticipantsUI(participants)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SessionDetailsActivity,
                    "Error loading session data", Toast.LENGTH_SHORT).show()
            }
        }

        sessionRef.addValueEventListener(participantsListener!!)
    }

    private fun updateParticipantsUI(participants: Iterable<DataSnapshot>) {
        val participantCount = participants.count()
        findViewById<TextView>(R.id.participantCount).text =
            "$participantCount people in session"

        // Update participant list (simplified - showing first 3)
        val participantViews = listOf(
            findViewById<TextView>(R.id.participant1),
            findViewById<TextView>(R.id.participant2),
            findViewById<TextView>(R.id.participant3)
        )

        participants.take(3).forEachIndexed { index, participant ->
            val userName = participant.child("userName").getValue(String::class.java) ?: "Unknown"
            val isHost = participant.child("isHost").getValue(Boolean::class.java) ?: false
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
        // Update session status in Firebase
        sessionRef.child("status").setValue("active")
        sessionRef.child("startTime").setValue(System.currentTimeMillis())
            .addOnSuccessListener {
                navigateToMap()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to start session", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToMap() {
        val intent = Intent(this, SessionMapActivityOSM::class.java)
        intent.putExtra("SESSION_ID", sessionId)
        startActivity(intent)
    }

    private fun generateQRCode(text: String): Bitmap? {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        participantsListener?.let {
            sessionRef.removeEventListener(it)
        }
    }
}