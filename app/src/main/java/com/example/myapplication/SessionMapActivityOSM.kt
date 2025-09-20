package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class SessionMapActivityOSM : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var sessionId: String
    private lateinit var sessionRef: DatabaseReference
    private lateinit var myLocationOverlay: MyLocationNewOverlay

    // UI Elements
    private lateinit var distanceText: TextView
    private lateinit var stepsText: TextView
    private lateinit var pointsText: TextView
    private lateinit var timerText: TextView
    private lateinit var pauseButton: Button
    private lateinit var endButton: Button

    // Tracking variables
    private var totalDistance = 0.0
    private var totalSteps = 0
    private var totalPoints = 0
    private var startTime = 0L
    private var isPaused = false
    private var lastLocation: Location? = null
    private val friendMarkers = mutableMapOf<String, Marker>()

    // Timer handler
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateTimer()
            if (!isPaused) {
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OpenStreetMap
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.session_map)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://wth2025-default-rtdb.firebaseio.com/")

        sessionId = intent.getStringExtra("SESSION_ID") ?: ""
        if (sessionId.isEmpty()) {
            finish()
            return
        }

        sessionRef = database.reference.child("sessions").child(sessionId)

        initializeViews()
        setupLocationClient()
        setupMap()
        setupButtons()
        startSession()
    }

    private fun initializeViews() {
        distanceText = findViewById(R.id.distanceValue)
        stepsText = findViewById(R.id.stepsValue)
        pointsText = findViewById(R.id.pointsValue)
        timerText = findViewById(R.id.sessionTimer)
        pauseButton = findViewById(R.id.pauseButton)
        endButton = findViewById(R.id.endSessionButton)
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateLocation(location)
                }
            }
        }
    }

    private fun setupMap() {
        map = MapView(this)
        map.setTileSource(TileSourceFactory.MAPNIK) // OpenStreetMap tiles
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        // Replace the FrameLayout with the MapView
        val mapContainer = findViewById<android.widget.FrameLayout>(R.id.mapContainer)
        mapContainer.removeAllViews()
        mapContainer.addView(map)

        // Set initial position and zoom
        map.controller.setZoom(15.0)

        // Add location overlay
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        map.overlays.add(myLocationOverlay)

        // Initialize UI to show zeros
        updateUI()

        // Check permissions and start tracking
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            // Get last known location immediately
            getInitialLocation()
            startLocationUpdates()

            // Only listen to friends if using Firebase
            if (!MockDataManager.USE_MOCK_DATA) {
                listenToFriendLocations()
            }
        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
        }
    }

    private fun getInitialLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // Set initial map position to current location
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    map.controller.setCenter(geoPoint)

                    // Store as last location but don't add distance (this is starting point)
                    lastLocation = location

                    android.util.Log.d("SessionMap", "Initial location: ${location.latitude}, ${location.longitude}")
                }
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(5000)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun updateLocation(location: Location) {
        android.util.Log.d("SessionMap", "Location update: ${location.latitude}, ${location.longitude}")

        // Update map position
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        if (lastLocation == null) {
            // First location update
            map.controller.setCenter(geoPoint)
            lastLocation = location
            android.util.Log.d("SessionMap", "First location set, distance starts at 0")
            return // Don't calculate distance for first location
        }

        // Calculate distance from last location
        val distance = lastLocation!!.distanceTo(location)
        android.util.Log.d("SessionMap", "Distance moved: $distance meters")

        if (distance > 1 && !isPaused) { // Count if moved more than 1 meter
            totalDistance += distance
            totalSteps += (distance * 1.3).toInt() // Rough step calculation
            totalPoints += (distance / 10).toInt() // 1 point per 10 meters

            android.util.Log.d("SessionMap", "Total distance: $totalDistance meters")
            updateUI()
        }

        lastLocation = location

        // Update location in Firebase/Mock only if moved significantly
        if (distance > 1) {
            if (!MockDataManager.USE_MOCK_DATA) {
                val userId = auth.currentUser?.uid ?: return
                val locationData = mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "timestamp" to System.currentTimeMillis()
                )
                sessionRef.child("locations").child(userId).setValue(locationData)
            }
        }
    }

    private fun listenToFriendLocations() {
        sessionRef.child("locations").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                updateFriendMarker(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                updateFriendMarker(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val userId = snapshot.key ?: return
                friendMarkers[userId]?.let {
                    map.overlays.remove(it)
                    map.invalidate()
                }
                friendMarkers.remove(userId)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateFriendMarker(snapshot: DataSnapshot) {
        val userId = snapshot.key ?: return
        if (userId == auth.currentUser?.uid) return // Don't show marker for self

        val lat = snapshot.child("latitude").getValue(Double::class.java) ?: return
        val lng = snapshot.child("longitude").getValue(Double::class.java) ?: return
        val geoPoint = GeoPoint(lat, lng)

        // Get user name from participants
        sessionRef.child("participants").child(userId).child("userName")
            .get().addOnSuccessListener { nameSnapshot ->
                val userName = nameSnapshot.getValue(String::class.java) ?: "Friend"

                runOnUiThread {
                    // Remove old marker if exists
                    friendMarkers[userId]?.let {
                        map.overlays.remove(it)
                    }

                    // Add new marker
                    val marker = Marker(map)
                    marker.position = geoPoint
                    marker.title = userName
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    // Set custom icon (you can customize this)
                    marker.icon = resources.getDrawable(android.R.drawable.ic_menu_myplaces, theme)

                    map.overlays.add(marker)
                    friendMarkers[userId] = marker
                    map.invalidate()
                }
            }
    }

    private fun setupButtons() {
        pauseButton.setOnClickListener {
            if (isPaused) {
                resumeSession()
            } else {
                pauseSession()
            }
        }

        endButton.setOnClickListener {
            showEndSessionDialog()
        }
    }

    private fun startSession() {
        startTime = System.currentTimeMillis()
        timerHandler.post(timerRunnable)
    }

    private fun pauseSession() {
        isPaused = true
        pauseButton.text = "Resume"
        fusedLocationClient.removeLocationUpdates(locationCallback)
        myLocationOverlay.disableFollowLocation()
    }

    private fun resumeSession() {
        isPaused = false
        pauseButton.text = "Pause"
        startLocationUpdates()
        myLocationOverlay.enableFollowLocation()
        timerHandler.post(timerRunnable)
    }

    private fun updateUI() {
        runOnUiThread {
            distanceText.text = String.format("%.2f km", totalDistance / 1000)
            stepsText.text = String.format("%,d", totalSteps)
            pointsText.text = "$totalPoints ☕"
        }
    }

    private fun updateTimer() {
        val elapsed = System.currentTimeMillis() - startTime
        val seconds = (elapsed / 1000) % 60
        val minutes = (elapsed / 1000 / 60) % 60
        val hours = elapsed / 1000 / 60 / 60

        timerText.text = String.format("Session Time: %02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun showEndSessionDialog() {
        AlertDialog.Builder(this)
            .setTitle("End Session")
            .setMessage("Are you sure you want to end this walking session?")
            .setPositiveButton("Yes") { _, _ ->
                endSession()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun endSession() {
        // Save session data to Firebase
        val sessionData = mapOf(
            "endTime" to System.currentTimeMillis(),
            "totalDistance" to totalDistance,
            "totalSteps" to totalSteps,
            "totalPoints" to totalPoints,
            "duration" to (System.currentTimeMillis() - startTime)
        )

        sessionRef.child("results").setValue(sessionData)
            .addOnSuccessListener {
                Toast.makeText(this,
                    "Session ended! You earned $totalPoints points!", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}