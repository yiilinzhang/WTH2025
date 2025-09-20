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
    pathCoordinates: []
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

// Auth state observer
if (auth) {
    auth.onAuthStateChanged((user) => {
        hideLoading();
        if (user) {
            currentUser = user;
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
        startWalkingSession();
    } else {
        showMessage('Invalid session code');
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

        walkingSession.watchId = navigator.geolocation.watchPosition(
            (position) => {
                if (lastPosition) {
                    // Calculate distance between positions
                    const distance = calculateDistance(
                        lastPosition.coords.latitude,
                        lastPosition.coords.longitude,
                        position.coords.latitude,
                        position.coords.longitude
                    );

                    walkingSession.distance += distance;
                    walkingSession.points = Math.floor(walkingSession.distance * 4); // 4 points per km

                    // Update UI
                    document.getElementById('distanceValue').textContent = walkingSession.distance.toFixed(1);
                    document.getElementById('pointsValue').textContent = walkingSession.points;
                }

                // Update map with current position
                updateMapLocation(position.coords.latitude, position.coords.longitude);

                lastPosition = position;
            },
            (error) => {
                console.error('Location error:', error);
            },
            {
                enableHighAccuracy: true,
                maximumAge: 10000,
                timeout: 5000
            }
        );
    }
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
        navigator.geolocation.clearWatch(walkingSession.watchId);
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
        // Stop tracking
        walkingSession.isActive = false;
        clearInterval(walkingSession.timer);
        navigator.geolocation.clearWatch(walkingSession.watchId);

        // Clean up map
        if (walkingSession.map) {
            walkingSession.map.remove();
        }

        // Save session to Firebase
        await saveWalkingSession();

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
            pathCoordinates: []
        };

        showMessage(`Session completed! You earned ${walkingSession.points} points!`);
        showDashboard();
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
        duration: duration
    };

    try {
        // Add to user's walk history and update points
        await db.collection('kopi').doc(currentUser.uid).update({
            walkHistory: firebase.firestore.FieldValue.arrayUnion(sessionData),
            points: firebase.firestore.FieldValue.increment(walkingSession.points)
        });
    } catch (error) {
        console.error('Error saving session:', error);
    }
}

// Navigation Functions
function showFriends() {
    showScreen('friendsScreen');
    loadFriends();
}

function showHistory() {
    showScreen('historyScreen');
    loadHistory();
}

function showRewards() {
    showScreen('rewardsScreen');
    loadCoupons();
}

// Friends Functions
async function addFriend() {
    const email = document.getElementById('friendEmail').value.trim();
    if (!email) {
        showMessage('Please enter an email address');
        return;
    }

    if (!currentUser) return;

    try {
        // Find user by email
        const userQuery = await db.collection('users').where('email', '==', email).get();

        if (userQuery.empty) {
            showMessage('User not found with that email');
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

            document.getElementById('totalPoints').textContent = Math.floor(data.points || 0);

            if (walkHistory.length === 0) {
                historyList.innerHTML = '<p style="text-align: center; color: #8B5A3C; margin: 20px;">No walking sessions yet. Start your first walk!</p>';
                return;
            }

            // Sort by start time (newest first)
            walkHistory.sort((a, b) => b.startTime - a.startTime);

            walkHistory.forEach(session => {
                const historyCard = document.createElement('div');
                historyCard.className = 'history-card';

                const date = new Date(session.startTime).toLocaleDateString();
                const duration = formatDuration(session.duration || 0);

                historyCard.innerHTML = `
                    <div class="history-location">${session.locationName}</div>
                    <div class="history-details">
                        <span>${date}</span>
                        <span>${session.distance?.toFixed(1) || '0.0'} km</span>
                        <span>${duration}</span>
                        <span>${session.pointsEarned || 0} pts</span>
                    </div>
                `;

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
        const snapshot = await db.collection('kopi').doc(currentUser.uid)
            .collection('coupons').orderBy('redeemedAt', 'desc').get();

        const couponsList = document.getElementById('couponsList');
        couponsList.innerHTML = '';

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