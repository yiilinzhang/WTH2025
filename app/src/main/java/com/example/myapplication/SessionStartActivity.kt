package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.integration.android.IntentIntegrator
import java.util.UUID
import android.util.Log

class SessionStartActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.session_start)

        auth = FirebaseAuth.getInstance()
        // Initialize Firebase Database with URL
        database = FirebaseDatabase.getInstance("https://wth2025-default-rtdb.firebaseio.com/")

        setupButtons()
        checkLocationPermissions()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.scanQrButton).setOnClickListener {
            startQRScanner()
        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startQRScanner() {
        // For testing in emulator - show dialog to enter code manually
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Join Session")
        builder.setMessage("Enter Session Code (or use test code: East Coast Park)")

        val input = android.widget.EditText(this)
        input.hint = "East Coast Park"
        builder.setView(input)

        builder.setPositiveButton("Join") { _, _ ->
            val sessionCode = input.text.toString().ifEmpty { "East Coast Park" }
            joinSession(sessionCode)
        }

        builder.setNegativeButton("Scan QR") { _, _ ->
            // Try actual QR scanner
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Scan Session QR Code")
            integrator.setCameraId(0)
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(false)
            integrator.initiateScan()
        }

        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                joinSession(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun joinSession(sessionId: String) {
        Log.d("SessionStart", "Attempting to join session: $sessionId")

        val userId = auth.currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"
        val userName = auth.currentUser?.displayName ?: "Walker${(1000..9999).random()}"

        Log.d("SessionStart", "User: $userName (ID: $userId)")

        // Check if using mock data mode
        if (MockDataManager.USE_MOCK_DATA) {
            Log.d("SessionStart", "Using MOCK DATA mode")

            MockDataManager.joinSession(sessionId, userId, userName) { success ->
                if (success) {
                    Log.d("SessionStart", "Successfully joined mock session")
                    Toast.makeText(this, "Joined session (Mock Mode)!", Toast.LENGTH_SHORT).show()
                    navigateToSessionDetails(sessionId)
                } else {
                    Log.e("SessionStart", "Failed to join mock session")
                    Toast.makeText(this, "Failed to join session", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        val sessionRef = database.reference.child("sessions").child(sessionId)

        // Check if session exists
        sessionRef.get().addOnSuccessListener { snapshot ->
            Log.d("SessionStart", "Firebase response received. Exists: ${snapshot.exists()}")

            if (snapshot.exists()) {
                // Session exists, join it
                Log.d("SessionStart", "Session found, adding participant")

                val participant = mapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "joinedAt" to System.currentTimeMillis()
                )

                sessionRef.child("participants").child(userId).setValue(participant)
                    .addOnSuccessListener {
                        Log.d("SessionStart", "Successfully joined session")
                        Toast.makeText(this, "Joined walking session!", Toast.LENGTH_SHORT).show()
                        navigateToSessionDetails(sessionId)
                    }
                    .addOnFailureListener { e ->
                        Log.e("SessionStart", "Failed to join session", e)
                        Toast.makeText(this, "Failed to join session: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                // Session doesn't exist - for testing, create it automatically
                Log.w("SessionStart", "Session not found, creating test session")

                // Create the test session
                val sessionData = mapOf(
                    "sessionId" to sessionId,
                    "hostId" to userId,
                    "createdAt" to System.currentTimeMillis(),
                    "isActive" to true,
                    "name" to "Test Session",
                    "participants" to mapOf(
                        userId to mapOf(
                            "userId" to userId,
                            "userName" to userName,
                            "isHost" to true,
                            "joinedAt" to System.currentTimeMillis()
                        )
                    )
                )

                sessionRef.setValue(sessionData)
                    .addOnSuccessListener {
                        Log.d("SessionStart", "Test session created successfully")
                        Toast.makeText(this, "Created test session: $sessionId", Toast.LENGTH_SHORT).show()
                        navigateToSessionDetails(sessionId)
                    }
                    .addOnFailureListener { e ->
                        Log.e("SessionStart", "Failed to create test session", e)
                        Toast.makeText(this, "Failed to create session: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("SessionStart", "Failed to connect to Firebase", e)
            Toast.makeText(this, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    private fun navigateToSessionDetails(sessionId: String) {
        // Use mock activity if in mock mode
        val activityClass = if (MockDataManager.USE_MOCK_DATA) {
            SessionDetailsActivityMock::class.java
        } else {
            SessionDetailsActivity::class.java
        }

        val intent = Intent(this, activityClass)
        intent.putExtra("SESSION_ID", sessionId)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}