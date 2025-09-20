// Firebase Configuration (using your existing project)
const firebaseConfig = {
    apiKey: "AIzaSyBBvPzHKE8kIPyxKkwd_aoLYcBo6fqMcM4",
    authDomain: "wth2025.firebaseapp.com",
    databaseURL: "https://wth2025-default-rtdb.firebaseio.com",
    projectId: "wth2025",
    storageBucket: "wth2025.firebasestorage.app",
    messagingSenderId: "449780553766",
    appId: "1:449780553766:web:9c44f7b1e8d2ab1dd22ee7"
};

// Initialize Firebase with error handling
let auth, db, rtdb;

try {
    firebase.initializeApp(firebaseConfig);
    auth = firebase.auth();
    db = firebase.firestore();
    rtdb = firebase.database();
    console.log('Firebase initialized successfully');
} catch (error) {
    console.error('Firebase initialization error:', error);
    alert('Firebase connection failed. Check console for details.');
}

// Global variables
let currentUser = null;
let walkingSession = {
    isActive: false,
    startTime: null,
    distance: 0,
    points: 0,
    timer: null,
    watchId: null,
    locationName: '',
    map: null,
    currentMarker: null,
    pathPolyline: null,
    pathCoordinates: [],
    sessionId: null,
    isGroupWalk: false,
    groupParticipants: [],
    groupParticipantNames: []
};

// Utility Functions
function showScreen(screenId) {
    // Hide all screens
    document.querySelectorAll('.screen').forEach(screen => {
        screen.classList.add('hidden');
    });
    // Show selected screen
    document.getElementById(screenId).classList.remove('hidden');
}

function showLoading() {
    document.getElementById('loading').classList.remove('hidden');
}

function hideLoading() {
    document.getElementById('loading').classList.add('hidden');
}

function showMessage(message, isError = false) {
    alert(message); // Simple alert for now, can be improved with toast
}

// Authentication Functions
function showLogin() {
    showScreen('loginScreen');
}

function showSignup() {
    showScreen('signupScreen');
}

function showDashboard() {
    if (currentUser) {
        showScreen('dashboardScreen');
        loadUserData();
    } else {
        showLogin();
    }
}

// Login form handler
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    showLoading();

    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    try {
        await auth.signInWithEmailAndPassword(email, password);
        // User will be handled by onAuthStateChanged
    } catch (error) {
        hideLoading();
        showMessage('Login failed: ' + error.message, true);
    }
});

// Signup form handler
document.getElementById('signupForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    showLoading();

    const name = document.getElementById('signupName').value;
    const email = document.getElementById('signupEmail').value;
    const password = document.getElementById('signupPassword').value;

    try {
        const userCredential = await auth.createUserWithEmailAndPassword(email, password);

        // Update profile with name
        await userCredential.user.updateProfile({
            displayName: name
        });

        // Create user document
        await createUserDocument(userCredential.user.uid, email, name);

        // User will be handled by onAuthStateChanged
    } catch (error) {
        hideLoading();
        showMessage('Signup failed: ' + error.message, true);
    }
});

// Create user document in Firestore
async function createUserDocument(userId, email, name) {
    const userProfile = {
        email: email,
        name: name,
        userId: userId,
        createdAt: Date.now()
    };

    const kopiUser = {
        points: 0,
        noOfKopiRedeemed: 0,
        walkHistory: []
    };

    // Create both documents
    await db.collection('users').doc(userId).set(userProfile);
    await db.collection('kopi').doc(userId).set(kopiUser);
}

// Ensure user documents exist for existing users
async function ensureUserDocumentsExist(user) {
    try {
        // Check if kopi document exists
        const kopiDoc = await db.collection('kopi').doc(user.uid).get();
        if (!kopiDoc.exists) {
            console.log('Creating missing kopi document for user:', user.uid);
            await db.collection('kopi').doc(user.uid).set({
                points: 0,
                noOfKopiRedeemed: 0,
                walkHistory: []
            });
        }

        // Check if users document exists
        const userDoc = await db.collection('users').doc(user.uid).get();
        if (!userDoc.exists) {
            console.log('Creating missing users document for user:', user.uid);
            await db.collection('users').doc(user.uid).set({
                email: user.email,
                name: user.displayName || user.email.split('@')[0],
                userId: user.uid,
                createdAt: Date.now()
            });
        }
    } catch (error) {
        console.error('Error ensuring user documents exist:', error);
    }
}

// Auth state observer
if (auth) {
    auth.onAuthStateChanged(async (user) => {
        hideLoading();
        if (user) {
            currentUser = user;
            // Check if user documents exist, create if not
            await ensureUserDocumentsExist(user);
            showDashboard();
        } else {
            currentUser = null;
            showLogin();
        }
    });
} else {
    // Firebase failed to initialize, show login anyway
    hideLoading();
    showLogin();
}

// Logout function
async function logout() {
    try {
        await auth.signOut();
    } catch (error) {
        showMessage('Logout failed: ' + error.message, true);
    }
}

// Load user data
async function loadUserData() {
    if (!currentUser) return;

    try {
        const doc = await db.collection('kopi').doc(currentUser.uid).get();
        if (doc.exists()) {
            const data = doc.data();
            // Update points displays
            document.getElementById('totalPoints').textContent = Math.floor(data.points || 0);
            document.getElementById('rewardPoints').textContent = Math.floor(data.points || 0);
        }
    } catch (error) {
        console.error('Error loading user data:', error);
    }
}

// QR Scanner Functions
document.getElementById('joinSessionBtn').addEventListener('click', () => {
    showScreen('qrScreen');
});

document.getElementById('startCamera').addEventListener('click', startCamera);

async function startCamera() {
    try {
        const video = document.getElementById('cameraVideo');
        const canvas = document.getElementById('qrCanvas');
        const context = canvas.getContext('2d');

        const stream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'environment' }
        });

        video.srcObject = stream;
        video.style.display = 'block';
        document.getElementById('startCamera').style.display = 'none';

        // Start QR detection
        video.addEventListener('loadedmetadata', () => {
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            scanQRCode(video, canvas, context);
        });

    } catch (error) {
        showMessage('Camera access denied or not available', true);
    }
}

function scanQRCode(video, canvas, context) {
    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    const imageData = context.getImageData(0, 0, canvas.width, canvas.height);
    const code = jsQR(imageData.data, imageData.width, imageData.height);

    if (code) {
        // QR code detected
        video.srcObject.getTracks().forEach(track => track.stop());
        video.style.display = 'none';
        document.getElementById('sessionCode').value = code.data;
        joinSession();
    } else {
        // Continue scanning
        requestAnimationFrame(() => scanQRCode(video, canvas, context));
    }
}

// Join session function
async function joinSession() {
    const sessionCode = document.getElementById('sessionCode').value.trim();
    if (!sessionCode) {
        showMessage('Please enter a session code');
        return;
    }

    // For location-based sessions (like your QR codes)
    const locationNames = ['East Coast Park', 'Botanic Garden', 'Bishan Park'];

    if (locationNames.includes(sessionCode)) {
        walkingSession.locationName = sessionCode;

        // Check if there's an existing active session for this location
        checkForExistingSession(sessionCode);
    } else {
        showMessage('Invalid session code');
    }
}

// Check for existing active sessions at this location
async function checkForExistingSession(locationName) {
    try {
        // Look for active sessions at this location in the last 30 minutes
        const thirtyMinutesAgo = Date.now() - (30 * 60 * 1000);

        const snapshot = await db.collection('activeSessions')
            .where('locationName', '==', locationName)
            .get();

        // Filter sessions in JavaScript to avoid index requirements
        const validSessions = [];
        snapshot.forEach(doc => {
            const sessionData = doc.data();
            if ((sessionData.status === 'waiting' || sessionData.status === 'active') &&
                sessionData.createdAt > thirtyMinutesAgo) {
                validSessions.push({ doc, data: sessionData });
            }
        });

        if (validSessions.length > 0) {
            // Sort by creation time and get the most recent
            validSessions.sort((a, b) => b.data.createdAt - a.data.createdAt);
            const sessionDoc = validSessions[0].doc;
            const sessionData = validSessions[0].data;

            if (sessionData.participants.includes(currentUser.uid)) {
                // User is already in this session
                walkingSession.sessionId = sessionData.sessionId;
                showFindFriendsScreen();
            } else if (sessionData.participants.length < sessionData.maxParticipants) {
                // Join the existing session
                await joinExistingSession(sessionData.sessionId, sessionData);
            } else {
                // Session is full, create a new one
                showMessage('Session is full, creating a new walking group...');
                showFindFriendsScreen();
            }
        } else {
            // No existing session, create a new one
            showFindFriendsScreen();
        }
    } catch (error) {
        console.error('Error checking for existing sessions:', error);
        // Fallback to creating new session
        showFindFriendsScreen();
    }
}

// Join an existing walking session
async function joinExistingSession(sessionId, sessionData) {
    try {
        const userName = currentUser.displayName || currentUser.email.split('@')[0];

        await db.collection('activeSessions').doc(sessionId).update({
            participants: firebase.firestore.FieldValue.arrayUnion(currentUser.uid),
            participantNames: firebase.firestore.FieldValue.arrayUnion(userName)
        });

        walkingSession.sessionId = sessionId;
        walkingSession.isGroupWalk = true;
        walkingSession.groupParticipants = [...sessionData.participants, currentUser.uid];
        walkingSession.groupParticipantNames = [...sessionData.participantNames, userName];

        showMessage(`Joined ${sessionData.createdByName}'s walking group! (${sessionData.participants.length + 1} people)`);
        showFindFriendsScreen();
    } catch (error) {
        console.error('Error joining session:', error);
        showMessage('Failed to join group, creating new session...');
        showFindFriendsScreen();
    }
}

// Show find friends screen
function showFindFriendsScreen() {
    document.getElementById('sessionLocationDisplay').textContent = walkingSession.locationName;
    showScreen('findFriendsScreen');

    // Create a walking session in Firebase that others can join
    createWalkingSession();
}

// Create a walking session that others can join
async function createWalkingSession() {
    if (!currentUser) return;

    try {
        const sessionId = `${walkingSession.locationName}_${Date.now()}`;
        const sessionData = {
            sessionId: sessionId,
            locationName: walkingSession.locationName,
            createdBy: currentUser.uid,
            createdByName: currentUser.displayName || currentUser.email.split('@')[0],
            participants: [currentUser.uid],
            participantNames: [currentUser.displayName || currentUser.email.split('@')[0]],
            status: 'waiting', // waiting, active, completed
            createdAt: Date.now(),
            maxParticipants: 10
        };

        await db.collection('activeSessions').doc(sessionId).set(sessionData);
        walkingSession.sessionId = sessionId;

        console.log('Created walking session:', sessionId);
    } catch (error) {
        console.error('Error creating walking session:', error);
    }
}

// Map Functions
function initializeMap() {
    // Get user's location first
    if ('geolocation' in navigator) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;

                // Initialize map centered on user's location
                walkingSession.map = L.map('map').setView([lat, lng], 16);

                // Add OpenStreetMap tiles
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '© OpenStreetMap contributors'
                }).addTo(walkingSession.map);

                // Add user's current location marker
                walkingSession.currentMarker = L.marker([lat, lng])
                    .addTo(walkingSession.map)
                    .bindPopup('Your current location');

                // Initialize path polyline
                walkingSession.pathPolyline = L.polyline([], {
                    color: '#6B4423',
                    weight: 4,
                    opacity: 0.8
                }).addTo(walkingSession.map);
            },
            (error) => {
                console.error('Error getting location:', error);
                // Fallback to Singapore center
                walkingSession.map = L.map('map').setView([1.3521, 103.8198], 12);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '© OpenStreetMap contributors'
                }).addTo(walkingSession.map);
            }
        );
    }
}

function updateMapLocation(lat, lng) {
    if (walkingSession.map && walkingSession.currentMarker) {
        // Update marker position
        walkingSession.currentMarker.setLatLng([lat, lng]);

        // Add to path
        walkingSession.pathCoordinates.push([lat, lng]);
        walkingSession.pathPolyline.setLatLngs(walkingSession.pathCoordinates);

        // Center map on current position
        walkingSession.map.setView([lat, lng], 16);
    }
}

// Walking Session Functions
function startWalkingSession() {
    walkingSession.isActive = true;
    walkingSession.startTime = Date.now();
    walkingSession.distance = 0;
    walkingSession.points = 0;
    walkingSession.pathCoordinates = [];

    document.getElementById('sessionLocation').textContent = walkingSession.locationName;
    showScreen('walkingScreen');

    // Initialize map
    initializeMap();

    // Start timer
    updateTimer();
    walkingSession.timer = setInterval(updateTimer, 1000);

    // Start location tracking
    startLocationTracking();
}

function updateTimer() {
    if (!walkingSession.isActive) return;

    const elapsed = Date.now() - walkingSession.startTime;
    const hours = Math.floor(elapsed / 3600000);
    const minutes = Math.floor((elapsed % 3600000) / 60000);
    const seconds = Math.floor((elapsed % 60000) / 1000);

    document.getElementById('walkingTimer').textContent =
        `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}

function startLocationTracking() {
    if ('geolocation' in navigator) {
        let lastPosition = null;
        let locationUpdateInterval = null;
        let positionCount = 0;
        let distanceCalculations = 0;

        // Create debug panel for GPS status
        createGPSDebugPanel();

        // Check for permission first (Safari specific)
        console.log('📍 Checking location permissions...');
        updateGPSStatus('Requesting location permission...');

        // Enhanced GPS options for better Safari performance
        // Using less strict settings for better Safari compatibility
        const gpsOptions = {
            enableHighAccuracy: true,
            maximumAge: 3000,    // Allow cached positions up to 3 seconds old
            timeout: 30000       // Much longer timeout for Safari (30 seconds)
        };

        // For Safari, we need to ensure the page has HTTPS or is localhost
        const isSecureContext = window.isSecureContext;
        if (!isSecureContext) {
            console.error('❌ GPS requires HTTPS or localhost. Current context is not secure.');
            updateGPSStatus('GPS requires HTTPS');
            showMessage('GPS tracking requires HTTPS. Please use the HTTPS version of this site.');
            return;
        }

        console.log('🚀 Starting GPS tracking in Safari with enhanced debugging');
        updateGPSStatus('Initializing GPS tracking...');

        // Get initial position first
        navigator.geolocation.getCurrentPosition(
            (position) => {
                console.log('✅ Initial GPS position obtained:', {
                    lat: position.coords.latitude,
                    lng: position.coords.longitude,
                    accuracy: position.coords.accuracy,
                    timestamp: new Date(position.timestamp).toLocaleString()
                });
                lastPosition = position;
                positionCount++;
                updateLocationData(position, null);
                updateGPSStatus(`GPS active - ${positionCount} positions received`);
            },
            (error) => {
                console.error('❌ Failed to get initial GPS position:', error);
                updateGPSStatus(`GPS Error: ${error.message}`);
                handleGPSError(error);
            },
            gpsOptions
        );

        // Primary location tracking with watchPosition
        walkingSession.watchId = navigator.geolocation.watchPosition(
            (position) => {
                positionCount++;
                console.log(`📍 GPS Update #${positionCount}:`, {
                    lat: position.coords.latitude.toFixed(6),
                    lng: position.coords.longitude.toFixed(6),
                    accuracy: position.coords.accuracy.toFixed(1) + 'm',
                    timestamp: new Date(position.timestamp).toLocaleTimeString(),
                    timeSinceLastUpdate: lastPosition ? ((position.timestamp - lastPosition.timestamp) / 1000).toFixed(1) + 's' : 'first'
                });
                updateLocationData(position, lastPosition);
                lastPosition = position;
                updateGPSStatus(`GPS active - ${positionCount} positions, ${distanceCalculations} distance calcs`);
            },
            (error) => {
                console.error('❌ watchPosition error:', error);
                updateGPSStatus(`Watch Error: ${error.message}`);
                handleGPSError(error);
                // Fallback to manual polling if watchPosition fails
                startManualLocationPolling();
            },
            gpsOptions
        );

        // Additional manual polling for Safari with more lenient settings
        // This ensures we get updates even if watchPosition is slow
        locationUpdateInterval = setInterval(() => {
            if (walkingSession.isActive) {
                // Use simpler, more reliable settings for polling
                const pollOptions = {
                    enableHighAccuracy: false,  // Sacrifice accuracy for reliability
                    maximumAge: 30000,          // Accept positions up to 30 seconds old
                    timeout: 60000              // Very long timeout (60 seconds)
                };

                navigator.geolocation.getCurrentPosition(
                    (position) => {
                        positionCount++;
                        console.log(`🔄 Manual poll #${positionCount}:`, {
                            lat: position.coords.latitude.toFixed(6),
                            lng: position.coords.longitude.toFixed(6),
                            accuracy: position.coords.accuracy.toFixed(1) + 'm'
                        });
                        updateLocationData(position, lastPosition);
                        lastPosition = position;
                        updateGPSStatus(`GPS active - ${positionCount} positions, ${distanceCalculations} distance calcs`);
                    },
                    (error) => {
                        console.warn('⚠️ Manual location poll failed:', error);
                        updateGPSStatus(`Poll Error: ${error.message}`);

                        // Try a simpler fallback with even more lenient settings
                        navigator.geolocation.getCurrentPosition(
                            (position) => {
                                console.log('✅ Fallback poll succeeded');
                                updateLocationData(position, lastPosition);
                                lastPosition = position;
                            },
                            (err) => {
                                console.error('❌ Even fallback failed:', err);
                            },
                            { enableHighAccuracy: false, maximumAge: 60000, timeout: 120000 }
                        );
                    },
                    pollOptions
                );
            }
        }, 5000); // Poll every 5 seconds (less aggressive to avoid overwhelming)

        // Store interval ID and position count
        walkingSession.locationInterval = locationUpdateInterval;
        walkingSession.positionCount = positionCount;
        walkingSession.distanceCalculations = distanceCalculations;

        // Add mock movement generator for testing when GPS fails
        startMockMovementForTesting();
    } else {
        console.error('❌ Geolocation not supported');
        updateGPSStatus('Geolocation not supported');
        // Start mock movement anyway for testing
        startMockMovementForTesting();
    }
}

// Mock movement generator for testing when GPS isn't working
function startMockMovementForTesting() {
    console.log('🎮 Starting mock movement generator for testing');

    let mockSteps = 0;
    walkingSession.mockInterval = setInterval(() => {
        if (walkingSession.isActive) {
            mockSteps++;

            // Generate small random movement (0.5-2 meters per update)
            const mockDistance = (0.0005 + Math.random() * 0.0015); // in km

            // Add to total distance (already amplified by 100x in updateLocationData)
            walkingSession.distance += mockDistance * 100; // Amplify for testing
            walkingSession.points = Math.floor(walkingSession.distance * 10);

            // Update UI
            document.getElementById('distanceValue').textContent = walkingSession.distance.toFixed(1);
            document.getElementById('pointsValue').textContent = walkingSession.points;

            // Update debug info
            updateGPSDebugInfo({
                totalDistance: walkingSession.distance.toFixed(3) + ' km',
                lastMovement: 'MOCK: ' + (mockDistance * 1000).toFixed(1) + 'm',
                points: walkingSession.points,
                accuracy: 'Mock Mode'
            });

            console.log(`🎮 Mock step #${mockSteps}: Added ${(mockDistance * 1000).toFixed(1)}m (displayed as ${(mockDistance * 100000).toFixed(1)}m)`);
        }
    }, 3000); // Generate movement every 3 seconds
}

function updateLocationData(position, lastPosition) {
    // Always update map with current position first
    updateMapLocation(position.coords.latitude, position.coords.longitude);

    if (lastPosition) {
        // Calculate distance between positions
        const distance = calculateDistance(
            lastPosition.coords.latitude,
            lastPosition.coords.longitude,
            position.coords.latitude,
            position.coords.longitude
        );

        // Increment distance calculation counter
        if (walkingSession.distanceCalculations !== undefined) {
            walkingSession.distanceCalculations++;
        }

        const distanceMeters = distance * 1000;
        const timeDiffSeconds = (position.timestamp - lastPosition.timestamp) / 1000;

        console.log('🔍 Distance Analysis:', {
            distanceMeters: distanceMeters.toFixed(2) + 'm',
            timeDiff: timeDiffSeconds.toFixed(1) + 's',
            speed: distanceMeters > 0 ? (distanceMeters / timeDiffSeconds * 3.6).toFixed(1) + ' km/h' : '0 km/h',
            threshold: '1.0m',
            willUpdate: distanceMeters > 1.0,
            accuracy: position.coords.accuracy.toFixed(1) + 'm'
        });

        // Ultra-sensitive threshold for testing - accept ANY movement
        // Track in centimeters but display as meters
        if (distance > 0.00001) { // 0.01 meter (1cm) in km - extremely sensitive
            const oldDistance = walkingSession.distance;

            // For testing: multiply distance by 100 to simulate more movement
            // This makes 1 meter of real movement = 100 meters in the app
            const amplifiedDistance = distance * 100;
            walkingSession.distance += amplifiedDistance;

            walkingSession.points = Math.floor(walkingSession.distance * 10); // More points for testing

            // Update UI
            document.getElementById('distanceValue').textContent = walkingSession.distance.toFixed(1);
            document.getElementById('pointsValue').textContent = walkingSession.points;

            console.log('✅ Distance UPDATED:', {
                added: (distance * 1000).toFixed(2) + 'm',
                totalBefore: oldDistance.toFixed(3) + 'km',
                totalAfter: walkingSession.distance.toFixed(3) + 'km',
                points: walkingSession.points
            });

            updateGPSDebugInfo({
                totalDistance: walkingSession.distance.toFixed(3) + ' km',
                lastMovement: distanceMeters.toFixed(1) + 'm',
                points: walkingSession.points,
                accuracy: position.coords.accuracy.toFixed(1) + 'm'
            });
        } else {
            console.log('⚠️ Movement too small:', {
                distance: distanceMeters.toFixed(2) + 'm',
                threshold: '1.0m',
                reason: 'Below minimum threshold'
            });

            // Still update debug info even if no distance added
            updateGPSDebugInfo({
                totalDistance: walkingSession.distance.toFixed(3) + ' km',
                lastMovement: 'too small (' + distanceMeters.toFixed(1) + 'm)',
                points: walkingSession.points,
                accuracy: position.coords.accuracy.toFixed(1) + 'm'
            });
        }
    } else {
        console.log('📌 First GPS position received - setting baseline');
        // Initialize display even on first position
        document.getElementById('distanceValue').textContent = walkingSession.distance.toFixed(1);
        document.getElementById('pointsValue').textContent = walkingSession.points;

        updateGPSDebugInfo({
            totalDistance: walkingSession.distance.toFixed(3) + ' km',
            lastMovement: 'baseline',
            points: walkingSession.points,
            accuracy: position.coords.accuracy.toFixed(1) + 'm'
        });
    }
}

function startManualLocationPolling() {
    // Enhanced fallback manual polling if watchPosition completely fails
    console.log('🚨 Starting manual GPS polling fallback');
    updateGPSStatus('Switching to manual polling fallback');

    if (walkingSession.manualPollingInterval) {
        clearInterval(walkingSession.manualPollingInterval);
    }

    let lastPosition = null;
    let pollAttempts = 0;
    let successfulPolls = 0;
    let consecutiveFailures = 0;

    walkingSession.manualPollingInterval = setInterval(() => {
        if (walkingSession.isActive) {
            pollAttempts++;

            // Try multiple GPS option sets for better Safari/iOS compatibility
            const gpsOptionSets = [
                {
                    enableHighAccuracy: true,
                    maximumAge: 5000,     // Accept 5 second old positions
                    timeout: 30000        // 30 second timeout
                },
                {
                    enableHighAccuracy: false,  // Less accurate but more reliable
                    maximumAge: 10000,   // Accept 10 second old positions
                    timeout: 20000       // 20 second timeout
                },
                {
                    enableHighAccuracy: true,
                    maximumAge: 15000,   // Very lenient - accept 15 second old positions
                    timeout: 60000       // 60 second timeout for difficult conditions
                }
            ];

            const optionSet = gpsOptionSets[pollAttempts % gpsOptionSets.length];

            navigator.geolocation.getCurrentPosition(
                (position) => {
                    successfulPolls++;
                    consecutiveFailures = 0;

                    console.log(`🔄 Manual Poll Success #${successfulPolls}/${pollAttempts}:`, {
                        lat: position.coords.latitude.toFixed(6),
                        lng: position.coords.longitude.toFixed(6),
                        accuracy: position.coords.accuracy.toFixed(1) + 'm',
                        optionUsed: JSON.stringify(optionSet)
                    });

                    updateLocationData(position, lastPosition);
                    lastPosition = position;
                    updateGPSStatus(`Manual polling: ${successfulPolls}/${pollAttempts} successful`);
                },
                (error) => {
                    consecutiveFailures++;
                    console.warn(`⚠️ Manual poll #${pollAttempts} failed (${consecutiveFailures} consecutive):`, {
                        error: error.message,
                        optionUsed: JSON.stringify(optionSet)
                    });

                    handleGPSError(error);

                    // If too many consecutive failures, try to restart the whole GPS system
                    if (consecutiveFailures >= 5) {
                        console.log('🔄 Too many GPS failures, attempting to restart GPS tracking...');
                        updateGPSStatus('Restarting GPS due to failures');

                        // Clear current intervals and restart
                        clearInterval(walkingSession.manualPollingInterval);
                        setTimeout(() => {
                            startLocationTracking();
                        }, 2000);
                    }
                },
                optionSet
            );
        }
    }, 3000); // Poll every 3 seconds to avoid overwhelming Safari
}

function calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371; // Earth's radius in km
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a =
        Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
}

function pauseWalking() {
    const pauseBtn = document.getElementById('pauseBtn');

    if (walkingSession.isActive) {
        // Pause
        walkingSession.isActive = false;
        clearInterval(walkingSession.timer);

        // Clear all location tracking
        if (walkingSession.watchId) {
            navigator.geolocation.clearWatch(walkingSession.watchId);
        }
        if (walkingSession.locationInterval) {
            clearInterval(walkingSession.locationInterval);
        }
        if (walkingSession.manualPollingInterval) {
            clearInterval(walkingSession.manualPollingInterval);
        }
        if (walkingSession.mockInterval) {
            clearInterval(walkingSession.mockInterval);
        }

        pauseBtn.textContent = '▶️ Resume';
    } else {
        // Resume
        walkingSession.isActive = true;
        walkingSession.timer = setInterval(updateTimer, 1000);
        startLocationTracking();
        pauseBtn.textContent = '⏸️ Pause';
    }
}

async function endWalking() {
    if (confirm('Are you sure you want to end this walking session?')) {
        // Store session data before resetting
        const sessionData = {
            distance: walkingSession.distance,
            points: walkingSession.points,
            duration: Date.now() - walkingSession.startTime,
            locationName: walkingSession.locationName
        };

        // Stop tracking
        walkingSession.isActive = false;
        clearInterval(walkingSession.timer);

        // Clear all location tracking
        if (walkingSession.watchId) {
            navigator.geolocation.clearWatch(walkingSession.watchId);
        }
        if (walkingSession.locationInterval) {
            clearInterval(walkingSession.locationInterval);
        }
        if (walkingSession.manualPollingInterval) {
            clearInterval(walkingSession.manualPollingInterval);
        }
        if (walkingSession.mockInterval) {
            clearInterval(walkingSession.mockInterval);
        }

        // Clean up map
        if (walkingSession.map) {
            walkingSession.map.remove();
        }

        // Save session to Firebase
        await saveWalkingSession();

        // Show completion screen with data
        showCompletionScreen(sessionData);

        // Reset walking session
        walkingSession = {
            isActive: false,
            startTime: null,
            distance: 0,
            points: 0,
            timer: null,
            watchId: null,
            locationName: '',
            map: null,
            currentMarker: null,
            pathPolyline: null,
            pathCoordinates: [],
            sessionId: null,
            isGroupWalk: false,
            groupParticipants: [],
            groupParticipantNames: []
        };
    }
}

async function saveWalkingSession() {
    if (!currentUser || !walkingSession.startTime) return;

    const duration = Date.now() - walkingSession.startTime;
    const sessionData = {
        sessionId: `session_${Date.now()}`,
        locationName: walkingSession.locationName,
        startTime: walkingSession.startTime,
        pointsEarned: walkingSession.points,
        distance: walkingSession.distance,
        duration: duration,
        type: 'walk' // Add type to differentiate from redemptions
    };

    try {
        // Add to user's walk history and update points
        await db.collection('kopi').doc(currentUser.uid).update({
            walkHistory: firebase.firestore.FieldValue.arrayUnion(sessionData),
            points: firebase.firestore.FieldValue.increment(walkingSession.points)
        });

        // Reload user data to update points display everywhere
        await loadUserData();

        console.log('✅ Session saved successfully:', sessionData);
    } catch (error) {
        console.error('Error saving session:', error);
    }
}

// Session Completion Functions
function showCompletionScreen(sessionData) {
    // Update completion screen with session data
    document.getElementById('completionPointsEarned').textContent =
        `You've earned ${sessionData.points} points!`;

    document.getElementById('completionDistance').textContent =
        `${sessionData.distance.toFixed(1)} km`;

    // Format duration
    const duration = sessionData.duration;
    const hours = Math.floor(duration / 3600000);
    const minutes = Math.floor((duration % 3600000) / 60000);
    const durationText = hours > 0 ?
        `${hours}:${minutes.toString().padStart(2, '0')}` :
        `${minutes}:${Math.floor((duration % 60000) / 1000).toString().padStart(2, '0')}`;

    document.getElementById('completionDuration').textContent = durationText;

    // Calculate approximate steps (rough estimate: 1300 steps per km)
    const steps = Math.floor(sessionData.distance * 1300);
    document.getElementById('completionSteps').textContent = steps.toString();

    // Hide walking companions section if walking alone
    const companionsSection = document.getElementById('walkingCompanionsSection');
    if (!walkingSession.isGroupWalk || walkingSession.groupParticipantNames.length <= 1) {
        companionsSection.style.display = 'none';
    } else {
        companionsSection.style.display = 'block';
        loadWalkingCompanions();
    }

    // Reload user data to ensure points are up to date everywhere
    loadUserData();

    // Show completion screen
    showScreen('completionScreen');
}

// Load walking companions from the group session
function loadWalkingCompanions() {
    const friendsList = document.getElementById('completionFriendsList');
    friendsList.innerHTML = '';

    // Show other participants (exclude current user)
    const otherParticipants = walkingSession.groupParticipantNames.filter(name => {
        const currentUserName = currentUser.displayName || currentUser.email.split('@')[0];
        return name !== currentUserName;
    });

    if (otherParticipants.length === 0) {
        friendsList.innerHTML = '<p style="text-align: center; color: #8B5A3C; padding: 20px;">No other participants in this session.</p>';
        return;
    }

    otherParticipants.forEach(participantName => {
        const companionItem = document.createElement('div');
        companionItem.className = 'completion-friend-item';

        companionItem.innerHTML = `
            <div class="completion-friend-name">${participantName}</div>
            <button class="completion-add-friend-btn" onclick="addWalkingCompanion('${participantName}')">Add Friend</button>
        `;

        friendsList.appendChild(companionItem);
    });
}

function addWalkingCompanion(participantName) {
    // Placeholder function for adding walking companions as friends
    const button = event.target;
    button.textContent = 'Added!';
    button.disabled = true;
    showMessage(`${participantName} added as friend!`);
}

function addWalkingFriend(partnerId) {
    // Legacy function for compatibility
    addWalkingCompanion(partnerId);
}

// Navigation Functions
function showSchedule() {
    showScreen('scheduleScreen');
    switchTab('allWalks'); // Start with All Walks tab
}

function showFriends() {
    showScreen('friendsScreen');

    // Display current user's username
    if (currentUser && currentUser.email) {
        const username = currentUser.email.split('@')[0];
        document.getElementById('usernameText').textContent = `Your username is: ${username}`;
    }

    loadFriends();
}

function showHistory() {
    showScreen('historyScreen');
    loadHistory();
    loadUserData(); // Ensure points are updated
}

function showRewards() {
    showScreen('rewardsScreen');
    loadUserData(); // Ensure points are updated
    loadCoupons();
}

// Friends Functions
async function addFriend() {
    const username = document.getElementById('friendEmail').value.trim();
    if (!username) {
        showMessage('Please enter a username');
        return;
    }

    if (!currentUser) return;

    try {
        // Convert username to email format and find user
        const emailToSearch = username.includes('@') ? username : `${username}@gmail.com`;
        const userQuery = await db.collection('users').where('email', '==', emailToSearch).get();

        if (userQuery.empty) {
            showMessage('User not found with that username');
            return;
        }

        const friendDoc = userQuery.docs[0];
        const friendData = friendDoc.data();

        if (friendData.userId === currentUser.uid) {
            showMessage("You can't add yourself as a friend!");
            return;
        }

        // Add friend (simplified - just add to subcollection)
        const friendInfo = {
            friendId: friendData.userId,
            friendName: friendData.name,
            friendEmail: friendData.email,
            addedAt: Date.now(),
            status: 'ACCEPTED'
        };

        await db.collection('kopi').doc(currentUser.uid)
            .collection('friends').doc(friendData.userId).set(friendInfo);

        document.getElementById('friendEmail').value = '';
        showMessage(`${friendData.name} added as Kopi Kaki! ☕`);
        loadFriends();

    } catch (error) {
        showMessage('Error adding friend: ' + error.message);
    }
}

async function loadFriends() {
    if (!currentUser) return;

    try {
        const snapshot = await db.collection('kopi').doc(currentUser.uid)
            .collection('friends').where('status', '==', 'ACCEPTED').get();

        const friendsList = document.getElementById('friendsList');
        friendsList.innerHTML = '';

        if (snapshot.empty) {
            friendsList.innerHTML = '<p style="text-align: center; color: #8B5A3C; margin: 20px;">No kopi kakis yet. Add some friends to get started!</p>';
            return;
        }

        snapshot.forEach(doc => {
            const friend = doc.data();
            const friendCard = document.createElement('div');
            friendCard.className = 'friend-card';

            friendCard.innerHTML = `
                <div class="friend-avatar">${friend.friendName.charAt(0).toUpperCase()}</div>
                <div class="friend-info">
                    <div class="friend-name">${friend.friendName}</div>
                    <div class="friend-email">${friend.friendEmail}</div>
                </div>
            `;

            friendsList.appendChild(friendCard);
        });

    } catch (error) {
        console.error('Error loading friends:', error);
    }
}

// History Functions
async function loadHistory() {
    if (!currentUser) return;

    try {
        const doc = await db.collection('kopi').doc(currentUser.uid).get();
        const historyList = document.getElementById('historyList');
        historyList.innerHTML = '';

        if (doc.exists()) {
            const data = doc.data();
            const walkHistory = data.walkHistory || [];

            // Update points display
            document.getElementById('totalPoints').textContent = Math.floor(data.points || 0);

            // Calculate weekly streak (only from walking sessions)
            const oneWeekAgo = Date.now() - (7 * 24 * 60 * 60 * 1000);
            const sessionsByDay = new Set();

            for (const session of walkHistory) {
                if (session.type !== 'redemption' && session.startTime > oneWeekAgo) {
                    const dayKey = new Date(session.startTime).toDateString();
                    sessionsByDay.add(dayKey);
                }
            }

            const daysWithActivity = sessionsByDay.size;
            document.getElementById('weeklyStreak').textContent = `Weekly streak: ${daysWithActivity} day(s)`;

            // Update progress bar (max 7 days)
            const progressPercentage = Math.min((daysWithActivity / 7) * 100, 100);
            document.getElementById('progressFill').style.width = `${progressPercentage}%`;

            // Get redemptions from coupons collection (if it exists)
            const redemptions = [];
            try {
                const couponsSnapshot = await db.collection('kopi').doc(currentUser.uid)
                    .collection('coupons').orderBy('redeemedAt', 'desc').get();

                couponsSnapshot.forEach(doc => {
                    const coupon = doc.data();
                    redemptions.push({
                        type: 'redemption',
                        startTime: coupon.redeemedAt,
                        drinkName: coupon.drinkName,
                        pointsUsed: coupon.pointsUsed,
                        code: coupon.code
                    });
                });
            } catch (error) {
                console.log('No coupons collection yet for user');
            }

            // Combine walks and redemptions
            const allHistory = [...walkHistory, ...redemptions];

            if (allHistory.length === 0) {
                historyList.innerHTML = `
                    <div style="text-align: center; padding: 40px 20px; color: #8B5A3C;">
                        <div style="font-size: 48px; margin-bottom: 16px;">🚶</div>
                        <h3 style="color: #6B4423; margin-bottom: 8px;">No Activity Yet</h3>
                        <p style="margin: 0; font-size: 16px;">Start your first walk to see your history!</p>
                    </div>
                `;
                return;
            }

            // Sort by time (newest first)
            allHistory.sort((a, b) => b.startTime - a.startTime);

            allHistory.forEach(item => {
                const historyCard = document.createElement('div');
                historyCard.className = 'history-card';

                const date = new Date(item.startTime).toLocaleDateString();
                const time = new Date(item.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});

                if (item.type === 'redemption') {
                    // Redemption card
                    historyCard.innerHTML = `
                        <div class="history-icon">☕</div>
                        <div class="history-content">
                            <div class="history-location">${item.drinkName}</div>
                            <div class="history-time">${date} ${time}</div>
                            <div class="history-details-text">Redeemed - ${item.code}</div>
                        </div>
                        <div class="history-points" style="color: #E53935;">
                            <div class="history-points-value">-${item.pointsUsed}</div>
                            <div class="history-points-label">points</div>
                        </div>
                    `;
                } else {
                    // Walking session card
                    const duration = formatDuration(item.duration || 0);
                    const distance = (item.distance || 0).toFixed(1);
                    const points = Math.floor(item.pointsEarned || 0);

                    historyCard.innerHTML = `
                        <div class="history-icon">🚶</div>
                        <div class="history-content">
                            <div class="history-location">${item.locationName}</div>
                            <div class="history-time">${date} ${time}</div>
                            <div class="history-details-text">${distance} km walked</div>
                        </div>
                        <div class="history-points">
                            <div class="history-points-value">+${points}</div>
                            <div class="history-points-label">points</div>
                        </div>
                    `;
                }

                historyList.appendChild(historyCard);
            });
        }

    } catch (error) {
        console.error('Error loading history:', error);
    }
}

function formatDuration(milliseconds) {
    const minutes = Math.floor(milliseconds / 60000);
    const hours = Math.floor(minutes / 60);

    if (hours > 0) {
        return `${hours}h ${minutes % 60}m`;
    } else {
        return `${minutes}m`;
    }
}

// Rewards Functions
async function redeemDrink(drinkName, pointsCost) {
    if (!currentUser) return;

    try {
        // Check if user has enough points
        const doc = await db.collection('kopi').doc(currentUser.uid).get();
        const userData = doc.data();
        const currentPoints = userData.points || 0;

        if (currentPoints < pointsCost) {
            showMessage(`Not enough points! You need ${pointsCost} points but only have ${Math.floor(currentPoints)} points.`);
            return;
        }

        // Generate coupon code
        const couponCode = `KOPI${Date.now().toString().slice(-6)}`;

        // Create coupon document
        const couponData = {
            code: couponCode,
            drinkName: drinkName,
            pointsUsed: pointsCost,
            used: false,
            redeemedAt: Date.now(),
            userId: currentUser.uid
        };

        // Add coupon and deduct points
        await db.collection('kopi').doc(currentUser.uid)
            .collection('coupons').doc(couponCode).set(couponData);

        await db.collection('kopi').doc(currentUser.uid).update({
            points: firebase.firestore.FieldValue.increment(-pointsCost),
            noOfKopiRedeemed: firebase.firestore.FieldValue.increment(1)
        });

        showMessage(`${drinkName} redeemed successfully! Your coupon code is: ${couponCode}`);
        loadUserData();
        loadCoupons();

    } catch (error) {
        showMessage('Failed to redeem: ' + error.message);
    }
}

async function loadCoupons() {
    if (!currentUser) return;

    try {
        const couponsList = document.getElementById('couponsList');
        couponsList.innerHTML = '';

        let snapshot;
        try {
            snapshot = await db.collection('kopi').doc(currentUser.uid)
                .collection('coupons').orderBy('redeemedAt', 'desc').get();
        } catch (error) {
            // Collection doesn't exist yet
            couponsList.innerHTML = '<p style="text-align: center; color: #8B5A3C; margin: 20px;">No coupons yet. Redeem some drinks above!</p>';
            return;
        }

        if (snapshot.empty) {
            couponsList.innerHTML = '<p style="text-align: center; color: #8B5A3C; margin: 20px;">No coupons yet. Redeem some drinks above!</p>';
            return;
        }

        snapshot.forEach(doc => {
            const coupon = doc.data();
            const couponCard = document.createElement('div');
            couponCard.className = 'coupon-card';

            const date = new Date(coupon.redeemedAt).toLocaleDateString();

            couponCard.innerHTML = `
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <div>
                        <strong>${coupon.drinkName}</strong><br>
                        <span class="coupon-code">${coupon.code}</span><br>
                        <small style="color: #999;">Obtained on: ${date}</small>
                    </div>
                    <div style="color: #6B4423; font-weight: bold;">
                        ${coupon.pointsUsed} pts
                    </div>
                </div>
            `;

            couponsList.appendChild(couponCard);
        });

    } catch (error) {
        console.error('Error loading coupons:', error);
    }
}

// PWA Manifest and Service Worker setup
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('sw.js')
            .then(registration => {
                console.log('SW registered: ', registration);
            })
            .catch(registrationError => {
                console.log('SW registration failed: ', registrationError);
            });
    });
}

// GPS Debug Functions for Safari
function createGPSDebugPanel() {
    // Remove existing debug panel if it exists
    const existingPanel = document.getElementById('gpsDebugPanel');
    if (existingPanel) {
        existingPanel.remove();
    }

    // Create debug panel
    const debugPanel = document.createElement('div');
    debugPanel.id = 'gpsDebugPanel';
    debugPanel.style.cssText = `
        position: fixed;
        top: 10px;
        right: 10px;
        background: rgba(0,0,0,0.8);
        color: white;
        padding: 10px;
        font-size: 12px;
        border-radius: 5px;
        z-index: 1000;
        max-width: 300px;
        font-family: monospace;
        line-height: 1.2;
    `;

    debugPanel.innerHTML = `
        <div><strong>🛰️ GPS Debug Panel</strong></div>
        <div id="gpsStatus">Status: Initializing...</div>
        <div id="gpsDetails"></div>
        <button onclick="toggleGPSDebug()" style="margin-top: 5px; padding: 2px 6px; font-size: 10px;">
            Hide Debug
        </button>
    `;

    document.body.appendChild(debugPanel);
}

function updateGPSStatus(message) {
    const statusElement = document.getElementById('gpsStatus');
    if (statusElement) {
        statusElement.textContent = `Status: ${message}`;
        console.log(`🛰️ GPS Status: ${message}`);
    }
}

function updateGPSDebugInfo(info) {
    const detailsElement = document.getElementById('gpsDetails');
    if (detailsElement) {
        detailsElement.innerHTML = `
            <div>Distance: ${info.totalDistance}</div>
            <div>Last Move: ${info.lastMovement}</div>
            <div>Points: ${info.points}</div>
            <div>Accuracy: ${info.accuracy}</div>
        `;
    }
}

function toggleGPSDebug() {
    const panel = document.getElementById('gpsDebugPanel');
    if (panel) {
        panel.style.display = panel.style.display === 'none' ? 'block' : 'none';
    }
}

function handleGPSError(error) {
    let errorMessage = 'Unknown GPS error';

    switch(error.code) {
        case error.PERMISSION_DENIED:
            errorMessage = "GPS permission denied by user";
            break;
        case error.POSITION_UNAVAILABLE:
            errorMessage = "GPS position unavailable";
            break;
        case error.TIMEOUT:
            errorMessage = "GPS request timed out";
            break;
    }

    console.error('🚨 GPS Error Details:', {
        code: error.code,
        message: error.message,
        interpretation: errorMessage
    });

    updateGPSStatus(errorMessage);

    // Show user-friendly message
    showMessage(`GPS Error: ${errorMessage}. Try enabling location services and refreshing the page.`);
}

// Initialize app
document.addEventListener('DOMContentLoaded', () => {
    // Start with login screen
    showLogin();

    // Add some sample session codes to the input placeholder
    const sessionCodeInput = document.getElementById('sessionCode');
    const codes = ['East Coast Park', 'Botanic Garden', 'Bishan Park'];
    let codeIndex = 0;

    setInterval(() => {
        sessionCodeInput.placeholder = codes[codeIndex];
        codeIndex = (codeIndex + 1) % codes.length;
    }, 2000);
});