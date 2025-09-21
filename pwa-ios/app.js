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
let currentLanguage = localStorage.getItem('selectedLanguage') || 'en';
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

// Translation objects
const translations = {
    en: {
        // Login/Signup
        loginTitle: "Kopi Kakis Walking Club",
        tagline: "Jalan-jalan with your friends!",
        emailPlaceholder: "Email",
        passwordPlaceholder: "Password",
        loginButton: "Login",
        signupPrompt: "Don't have an account?",
        signupLink: "Sign up here",
        joinTitle: "Join Kopi Kakis",
        namePlaceholder: "Your Name",
        signupButton: "Sign Up",
        loginPrompt: "Already have an account?",
        loginLink: "Login here",

        // Dashboard
        dashboardTitle: "Kopi Kakis Walking Club",
        joinSessionButton: "☕ JOIN WALKING SESSION ☕",
        scheduleButton: "📅 WALKING SCHEDULE 📅",
        dashboardDescription: "Scan QR or enter code to lim kopi and walk!",
        friendsButton: "👥 MY KOPI KAKIS",
        historyButton: "📜 HISTORY",
        rewardsButton: "🎁 REWARDS",
        bottomText: "Your neighborhood kopitiam walking group",
        logoutButton: "Logout",

        // QR Scanner
        joinSessionTitle: "Join Session",
        scanButton: "SCAN QR CODE TO JOIN",
        qrInfoText: "Look for QR codes posted in<br>coffee shops, parks, and walking trails",
        manualEntryText: "Or enter session code manually:",
        sessionPlaceholder: "East Coast Park",
        joinButton: "Join Session",

        // Find Friends/Group
        readyToWalkTitle: "Ready to Walk?",
        participantsWaiting: "Waiting for others to join...",
        joinedParticipants: "Joined Participants:",
        startWalkingButton: "🚶 START WALKING 🚶",
        othersCanJoin: "Others can join by scanning the same QR code",

        // Walking Session
        walkingSessionTitle: "Walking Session",
        distanceLabel: "km",
        pointsLabel: "points",
        pauseButton: "⏸️ Pause",
        endSessionButton: "🏁 End Session",

        // Friends
        friendsTitle: "☕ My Kopi Kakis ☕",
        usernameText: "Your username is: ",
        usernamePlaceholder: "username",
        addFriendButton: "Add Friend",

        // History
        historyTitle: "☕ Walking History ☕",
        historySubtitle: "Track your walking journey!",
        refreshButton: "🔄 Refresh History",
        pointsText: "Points: ",
        weeklyStreakText: "Weekly streak: ",
        dayText: "day(s)",
        motivationalText: "🏆 Keep walking to earn more kopi points!",
        sessionsHeader: "Your Walking Sessions ☕",

        // Session Completion
        completionTitle: "🎉 Well Done!",
        pointsEarned: "You've earned {points} points!",
        distanceStatLabel: "Distance",
        durationStatLabel: "Duration",
        stepsStatLabel: "Steps",
        walkingCompanionsTitle: "Walking Companions",
        companionsSubtitle: "Add companions from this walk as friends",
        nameHeader: "Name",
        actionHeader: "Action",
        addFriendAction: "Add Friend",
        backToHomeButton: "Back to Home",

        // Rewards
        rewardsTitle: "🎁 Rewards 🎁",
        pointsAvailable: "Points Available",
        redeemDrinksTitle: "Redeem Your Drinks",
        kopiCouponsTitle: "Your Kopi Coupons",

        // Schedule
        scheduleTitle: "📅 Walking Schedule",
        allWalksTab: "All Walks",
        scheduleWalkTab: "Schedule Walk",
        myScheduledWalks: "My Scheduled Walks",
        nearbyWalks: "Nearby Walks",
        chooseLocation: "📍 Choose Location",
        dateLabel: "📅 Date",
        timeLabel: "⏰ Time",
        createWalkButton: "🚶 CREATE WALK 🚶",

        // Common
        backButton: "←"
    },

    zh: {
        // Login/Signup
        loginTitle: "咖啡朋友行山俱乐部",
        tagline: "和朋友一起 Jalan-jalan！",
        emailPlaceholder: "电子邮件",
        passwordPlaceholder: "密码",
        loginButton: "登录",
        signupPrompt: "还没有账户？",
        signupLink: "在这里注册",
        joinTitle: "加入咖啡朋友",
        namePlaceholder: "您的姓名",
        signupButton: "注册",
        loginPrompt: "已有账户？",
        loginLink: "在这里登录",

        // Dashboard
        dashboardTitle: "咖啡朋友行山俱乐部",
        joinSessionButton: "☕ 加入行山活动 ☕",
        scheduleButton: "📅 行山时间表 📅",
        dashboardDescription: "扫描二维码或输入代码来饮咖啡和行山！",
        friendsButton: "👥 我的咖啡朋友",
        historyButton: "📜 历史记录",
        rewardsButton: "🎁 奖励",
        bottomText: "您的邻里茶餐厅行山群组",
        logoutButton: "登出",

        // QR Scanner
        joinSessionTitle: "加入活动",
        scanButton: "扫描二维码加入",
        qrInfoText: "在咖啡店、公园和<br>行山径寻找二维码",
        manualEntryText: "或手动输入活动代码：",
        sessionPlaceholder: "东海岸公园",
        joinButton: "加入活动",

        // Find Friends/Group
        readyToWalkTitle: "准备行山？",
        participantsWaiting: "等待其他人加入...",
        joinedParticipants: "已加入参与者：",
        startWalkingButton: "🚶 开始行山 🚶",
        othersCanJoin: "其他人可以通过扫描相同的二维码加入",

        // Walking Session
        walkingSessionTitle: "行山活动",
        distanceLabel: "公里",
        pointsLabel: "积分",
        pauseButton: "⏸️ 暂停",
        endSessionButton: "🏁 结束活动",

        // Friends
        friendsTitle: "☕ 我的咖啡朋友 ☕",
        usernameText: "您的用户名是：",
        usernamePlaceholder: "用户名",
        addFriendButton: "添加朋友",

        // History
        historyTitle: "☕ 行山历史 ☕",
        historySubtitle: "追踪您的行山旅程！",
        refreshButton: "🔄 刷新历史",
        pointsText: "积分：",
        weeklyStreakText: "每周连胜：",
        dayText: "天",
        motivationalText: "🏆 继续行山赚取更多咖啡积分！",
        sessionsHeader: "您的行山活动 ☕",

        // Session Completion
        completionTitle: "🎉 做得好！",
        pointsEarned: "您获得了 {points} 积分！",
        distanceStatLabel: "距离",
        durationStatLabel: "时长",
        stepsStatLabel: "步数",
        walkingCompanionsTitle: "行山伙伴",
        companionsSubtitle: "将此次行山的伙伴添加为朋友",
        nameHeader: "姓名",
        actionHeader: "操作",
        addFriendAction: "添加朋友",
        backToHomeButton: "返回主页",

        // Rewards
        rewardsTitle: "🎁 奖励 🎁",
        pointsAvailable: "可用积分",
        redeemDrinksTitle: "兑换您的饮品",
        kopiCouponsTitle: "您的咖啡优惠券",

        // Schedule
        scheduleTitle: "📅 行山时间表",
        allWalksTab: "所有行山",
        scheduleWalkTab: "安排行山",
        myScheduledWalks: "我安排的行山",
        nearbyWalks: "附近的行山",
        chooseLocation: "📍 选择地点",
        dateLabel: "📅 日期",
        timeLabel: "⏰ 时间",
        createWalkButton: "🚶 创建行山 🚶",

        // Common
        backButton: "←"
    },

    ms: {
        // Login/Signup
        loginTitle: "Kelab Jalan Kaki Kopi Kawan",
        tagline: "Jalan-jalan dengan kawan-kawan!",
        emailPlaceholder: "E-mel",
        passwordPlaceholder: "Kata laluan",
        loginButton: "Log masuk",
        signupPrompt: "Belum ada akaun?",
        signupLink: "Daftar di sini",
        joinTitle: "Sertai Kopi Kawan",
        namePlaceholder: "Nama Anda",
        signupButton: "Daftar",
        loginPrompt: "Sudah ada akaun?",
        loginLink: "Log masuk di sini",

        // Dashboard
        dashboardTitle: "Kelab Jalan Kaki Kopi Kawan",
        joinSessionButton: "☕ SERTAI SESI JALAN KAKI ☕",
        scheduleButton: "📅 JADUAL JALAN KAKI 📅",
        dashboardDescription: "Imbas QR atau masukkan kod untuk minum kopi dan jalan!",
        friendsButton: "👥 KOPI KAWAN SAYA",
        historyButton: "📜 SEJARAH",
        rewardsButton: "🎁 GANJARAN",
        bottomText: "Kumpulan jalan kaki kopitiam kejiranan anda",
        logoutButton: "Log keluar",

        // QR Scanner
        joinSessionTitle: "Sertai Sesi",
        scanButton: "IMBAS KOD QR UNTUK SERTAI",
        qrInfoText: "Cari kod QR yang ditampal di<br>kedai kopi, taman, dan denai jalan kaki",
        manualEntryText: "Atau masukkan kod sesi secara manual:",
        sessionPlaceholder: "Taman Pantai Timur",
        joinButton: "Sertai Sesi",

        // Find Friends/Group
        readyToWalkTitle: "Bersedia untuk Jalan?",
        participantsWaiting: "Menunggu orang lain untuk sertai...",
        joinedParticipants: "Peserta yang Menyertai:",
        startWalkingButton: "🚶 MULA JALAN KAKI 🚶",
        othersCanJoin: "Orang lain boleh sertai dengan mengimbas kod QR yang sama",

        // Walking Session
        walkingSessionTitle: "Sesi Jalan Kaki",
        distanceLabel: "km",
        pointsLabel: "mata",
        pauseButton: "⏸️ Jeda",
        endSessionButton: "🏁 Tamat Sesi",

        // Friends
        friendsTitle: "☕ Kopi Kawan Saya ☕",
        usernameText: "Nama pengguna anda ialah: ",
        usernamePlaceholder: "nama pengguna",
        addFriendButton: "Tambah Kawan",

        // History
        historyTitle: "☕ Sejarah Jalan Kaki ☕",
        historySubtitle: "Jejak perjalanan jalan kaki anda!",
        refreshButton: "🔄 Segar Semula Sejarah",
        pointsText: "Mata: ",
        weeklyStreakText: "Rentetan mingguan: ",
        dayText: "hari",
        motivationalText: "🏆 Terus jalan untuk dapat lebih banyak mata kopi!",
        sessionsHeader: "Sesi Jalan Kaki Anda ☕",

        // Session Completion
        completionTitle: "🎉 Bagus Sekali!",
        pointsEarned: "Anda telah memperoleh {points} mata!",
        distanceStatLabel: "Jarak",
        durationStatLabel: "Tempoh",
        stepsStatLabel: "Langkah",
        walkingCompanionsTitle: "Teman Jalan Kaki",
        companionsSubtitle: "Tambahkan teman dari jalan kaki ini sebagai kawan",
        nameHeader: "Nama",
        actionHeader: "Tindakan",
        addFriendAction: "Tambah Kawan",
        backToHomeButton: "Kembali ke Laman Utama",

        // Rewards
        rewardsTitle: "🎁 Ganjaran 🎁",
        pointsAvailable: "Mata Tersedia",
        redeemDrinksTitle: "Tebus Minuman Anda",
        kopiCouponsTitle: "Kupon Kopi Anda",

        // Schedule
        scheduleTitle: "📅 Jadual Jalan Kaki",
        allWalksTab: "Semua Jalan Kaki",
        scheduleWalkTab: "Jadualkan Jalan Kaki",
        myScheduledWalks: "Jalan Kaki Terjadual Saya",
        nearbyWalks: "Jalan Kaki Berdekatan",
        chooseLocation: "📍 Pilih Lokasi",
        dateLabel: "📅 Tarikh",
        timeLabel: "⏰ Masa",
        createWalkButton: "🚶 CIPTA JALAN KAKI 🚶",

        // Common
        backButton: "←"
    }
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
    updateAllText();
}

function showSignup() {
    showScreen('signupScreen');
    updateAllText();
}

async function showDashboard() {
    if (currentUser) {
        showScreen('dashboardScreen');
        // Initialize language and update text
        initializeLanguage();
        // Ensure documents exist before loading data
        await ensureUserDocumentsExist(currentUser);
        await loadUserData();
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
            // Documents will be ensured in showDashboard
            await showDashboard();
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

// Load user data and refresh all UI elements
async function loadUserData() {
    if (!currentUser) return;

    try {
        const doc = await db.collection('kopi').doc(currentUser.uid).get();
        if (doc && doc.exists) {
            const data = doc.data();
            const points = Math.floor(data.points || 0);

            // Update ALL points displays in the UI - force refresh
            const totalPointsElement = document.getElementById('totalPoints');
            if (totalPointsElement) {
                totalPointsElement.textContent = points;
                // Force reflow to ensure update
                totalPointsElement.style.display = 'none';
                totalPointsElement.offsetHeight; // Trigger reflow
                totalPointsElement.style.display = '';
            }

            const rewardPointsElement = document.getElementById('rewardPoints');
            if (rewardPointsElement) {
                rewardPointsElement.textContent = points;
                // Force reflow to ensure update
                rewardPointsElement.style.display = 'none';
                rewardPointsElement.offsetHeight; // Trigger reflow
                rewardPointsElement.style.display = '';
            }

            console.log('✅ Points updated in UI:', points);

            // Trigger a custom event to notify other parts of the app
            window.dispatchEvent(new CustomEvent('pointsUpdated', { detail: { points } }));
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

        // Don't show message, just navigate to the screen which will show participants
        console.log(`Joined ${sessionData.createdByName}'s walking group! (${sessionData.participants.length + 1} people)`);
        showFindFriendsScreen();
    } catch (error) {
        console.error('Error joining session:', error);
        // Don't show message, just create new session
        showFindFriendsScreen();
    }
}

// Show find friends screen
function showFindFriendsScreen() {
    document.getElementById('sessionLocationDisplay').textContent = walkingSession.locationName;
    showScreen('findFriendsScreen');

    // Clear any previous participants display
    const participantsLoading = document.getElementById('participantsLoading');
    const participantsDisplay = document.getElementById('participantsDisplay');
    const participantsNames = document.getElementById('participantsNames');

    if (participantsLoading) participantsLoading.style.display = 'block';
    if (participantsDisplay) participantsDisplay.style.display = 'none';
    if (participantsNames) participantsNames.innerHTML = '';

    // If we already joined an existing session, display the participants immediately
    if (walkingSession.sessionId && walkingSession.groupParticipantNames && walkingSession.groupParticipantNames.length > 0) {
        displayParticipants({
            names: walkingSession.groupParticipantNames,
            ids: walkingSession.groupParticipants || []
        });
    } else {
        // Create a walking session in Firebase that others can join
        createWalkingSession();
    }

    // Start listening for other participants joining
    listenForParticipants();
}

// Create a walking session that others can join
async function createWalkingSession() {
    if (!currentUser) return;

    try {
        // First, clean up old stale sessions (older than 2 hours)
        const twoHoursAgo = Date.now() - (2 * 60 * 60 * 1000);
        const staleSessionsSnapshot = await db.collection('activeSessions')
            .where('createdAt', '<', twoHoursAgo)
            .get();

        // Delete stale sessions in batches
        const deletePromises = [];
        staleSessionsSnapshot.forEach(doc => {
            const sessionData = doc.data();
            // Only delete if it's not already marked as completed
            if (sessionData.status !== 'completed') {
                deletePromises.push(doc.ref.delete());
                console.log('Cleaning up stale session:', doc.id);
            }
        });

        if (deletePromises.length > 0) {
            await Promise.all(deletePromises);
            console.log(`Cleaned up ${deletePromises.length} stale sessions`);
        }

        // Now create the new session
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
    // Stop listening for participants once walking starts
    if (walkingSession.participantsListener) {
        walkingSession.participantsListener();
        walkingSession.participantsListener = null;
    }

    // Update session status to active
    if (walkingSession.sessionId) {
        db.collection('activeSessions').doc(walkingSession.sessionId).update({
            status: 'active'
        }).catch(err => console.error('Error updating session status:', err));
    }

    walkingSession.isActive = true;
    walkingSession.startTime = Date.now();
    walkingSession.distance = 0;
    walkingSession.points = 0;
    walkingSession.pathCoordinates = [];

    document.getElementById('sessionLocation').textContent = walkingSession.locationName;
    showScreen('walkingScreen');

    // Show initial group bonus if applicable
    if (walkingSession.isGroupWalk && walkingSession.groupParticipants.length > 1) {
        const bonusPercentage = (walkingSession.groupParticipants.length - 1) * 10;
        const bonusIndicator = document.getElementById('bonusIndicator');
        if (bonusIndicator) {
            bonusIndicator.textContent = `(+${bonusPercentage}%)`;
        }
    }

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

            // Calculate points with group bonus (10% per additional person)
            const basePoints = walkingSession.distance * 10;
            const groupMultiplier = walkingSession.isGroupWalk && walkingSession.groupParticipants.length > 1
                ? 1 + ((walkingSession.groupParticipants.length - 1) * 0.1)
                : 1;
            walkingSession.points = Math.floor(basePoints * groupMultiplier);

            // Update UI
            document.getElementById('distanceValue').textContent = walkingSession.distance.toFixed(1);
            document.getElementById('pointsValue').textContent = walkingSession.points;

            // Show group bonus indicator
            if (walkingSession.isGroupWalk && walkingSession.groupParticipants.length > 1) {
                const bonusPercentage = (walkingSession.groupParticipants.length - 1) * 10;
                document.getElementById('bonusIndicator').textContent = `(+${bonusPercentage}%)`;
            }

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

            // Calculate points with group bonus (10% per additional person)
            const basePoints = walkingSession.distance * 10;
            const groupMultiplier = walkingSession.isGroupWalk && walkingSession.groupParticipants.length > 1
                ? 1 + ((walkingSession.groupParticipants.length - 1) * 0.1)
                : 1;
            walkingSession.points = Math.floor(basePoints * groupMultiplier);

            // Update UI
            document.getElementById('distanceValue').textContent = walkingSession.distance.toFixed(1);
            document.getElementById('pointsValue').textContent = walkingSession.points;

            // Show group bonus indicator
            if (walkingSession.isGroupWalk && walkingSession.groupParticipants.length > 1) {
                const bonusPercentage = (walkingSession.groupParticipants.length - 1) * 10;
                document.getElementById('bonusIndicator').textContent = `(+${bonusPercentage}%)`;
            }

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

        // Add a small delay to ensure database writes complete
        await new Promise(resolve => setTimeout(resolve, 500));

        // Reload user data to ensure points are refreshed
        await loadUserData();

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
        // First ensure the document exists
        await ensureUserDocumentsExist(currentUser);

        // Get current document to check if it exists and get current data
        const docRef = db.collection('kopi').doc(currentUser.uid);
        const doc = await docRef.get();

        if (!doc.exists) {
            // Create document with initial data if it doesn't exist
            await docRef.set({
                points: walkingSession.points,
                noOfKopiRedeemed: 0,
                walkHistory: [sessionData]
            });
            console.log('✅ Created new document and saved session:', sessionData);
        } else {
            // Update existing document
            await docRef.update({
                walkHistory: firebase.firestore.FieldValue.arrayUnion(sessionData),
                points: firebase.firestore.FieldValue.increment(walkingSession.points)
            });
            console.log('✅ Updated existing document with session:', sessionData);
        }

        // Reload user data to update points display everywhere
        await loadUserData();

        console.log('✅ Session saved successfully with points:', walkingSession.points);

        // Remove the session from activeSessions collection since it's completed
        if (walkingSession.sessionId) {
            try {
                await db.collection('activeSessions').doc(walkingSession.sessionId).delete();
                console.log('✅ Removed completed session from activeSessions:', walkingSession.sessionId);
            } catch (deleteError) {
                console.error('❌ Error removing session from activeSessions:', deleteError);
                // Try to at least mark it as completed
                try {
                    await db.collection('activeSessions').doc(walkingSession.sessionId).update({
                        status: 'completed',
                        completedAt: Date.now()
                    });
                    console.log('✅ Marked session as completed in activeSessions');
                } catch (updateError) {
                    console.error('❌ Could not update session status:', updateError);
                }
            }
        }
    } catch (error) {
        console.error('❌ Error saving session:', error);
        console.error('Session data that failed to save:', sessionData);
        // Try a simpler approach as fallback
        try {
            await db.collection('kopi').doc(currentUser.uid).set({
                points: walkingSession.points,
                walkHistory: [sessionData],
                noOfKopiRedeemed: 0
            }, { merge: true });
            console.log('✅ Saved session using merge approach');
        } catch (fallbackError) {
            console.error('❌ Fallback save also failed:', fallbackError);
        }
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
    loadUserData().then(() => {
        // Also refresh history to show the new session
        if (currentUser) {
            loadHistory();
        }
    });

    // Show completion screen
    showScreen('completionScreen');
}

// Load walking companions from the group session
async function loadWalkingCompanions() {
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

    // Get current user's friends list
    let existingFriends = new Set();
    try {
        const friendsSnapshot = await db.collection('kopi').doc(currentUser.uid)
            .collection('friends').where('status', '==', 'ACCEPTED').get();

        friendsSnapshot.forEach(doc => {
            const friend = doc.data();
            existingFriends.add(friend.friendName);
        });
    } catch (error) {
        console.error('Error loading friends list:', error);
    }

    // Display each participant with appropriate button
    for (const participantName of otherParticipants) {
        const companionItem = document.createElement('div');
        companionItem.className = 'completion-friend-item';

        const isAlreadyFriend = existingFriends.has(participantName);

        if (isAlreadyFriend) {
            companionItem.innerHTML = `
                <div class="completion-friend-name">${participantName}</div>
                <button class="completion-add-friend-btn" disabled style="background: #4CAF50; cursor: default;">
                    ✓ Already Friend
                </button>
            `;
        } else {
            companionItem.innerHTML = `
                <div class="completion-friend-name">${participantName}</div>
                <button class="completion-add-friend-btn" onclick="addWalkingCompanion('${participantName}')">Add Friend</button>
            `;
        }

        friendsList.appendChild(companionItem);
    }
}

async function addWalkingCompanion(participantName) {
    if (!currentUser) return;

    const button = event.target;

    try {
        // Find the user by name
        const userQuery = await db.collection('users')
            .where('name', '==', participantName)
            .get();

        if (userQuery.empty) {
            console.log('User not found:', participantName);
            button.textContent = 'Not Found';
            button.style.background = '#E53935';
            return;
        }

        const friendDoc = userQuery.docs[0];
        const friendData = friendDoc.data();

        // Add friend bidirectionally
        const friendInfo = {
            friendId: friendData.userId,
            friendName: friendData.name,
            friendEmail: friendData.email,
            addedAt: Date.now(),
            status: 'ACCEPTED'
        };

        // Add friend to current user's list
        await db.collection('kopi').doc(currentUser.uid)
            .collection('friends').doc(friendData.userId).set(friendInfo);

        // Also add current user to friend's list
        const currentUserInfo = {
            friendId: currentUser.uid,
            friendName: currentUser.displayName || currentUser.email.split('@')[0],
            friendEmail: currentUser.email,
            addedAt: Date.now(),
            status: 'ACCEPTED'
        };

        await db.collection('kopi').doc(friendData.userId)
            .collection('friends').doc(currentUser.uid).set(currentUserInfo);

        button.textContent = 'Added!';
        button.disabled = true;
        button.style.background = '#4CAF50';

        showMessage(`${participantName} added as friend!`);
    } catch (error) {
        console.error('Error adding walking companion:', error);
        button.textContent = 'Error';
        button.disabled = true;
    }
}

function addWalkingFriend(partnerId) {
    // Legacy function for compatibility
    addWalkingCompanion(partnerId);
}

// Navigation Functions
function showSchedule() {
    showScreen('scheduleScreen');
    updateAllText();
    switchTab('allWalks'); // Start with All Walks tab
}

function showFriends() {
    showScreen('friendsScreen');

    // Update all text for current language
    updateAllText();

    // Display current user's username
    if (currentUser && currentUser.email) {
        const username = currentUser.email.split('@')[0];
        document.getElementById('usernameText').textContent = t('usernameText') + username;
    }

    loadFriends();
}

async function showHistory() {
    showScreen('historyScreen');
    updateAllText();
    if (currentUser) {
        await ensureUserDocumentsExist(currentUser);
        await loadHistory();
        await loadUserData(); // Ensure points are updated
    }
}

async function showRewards() {
    showScreen('rewardsScreen');
    updateAllText();
    if (currentUser) {
        await ensureUserDocumentsExist(currentUser);
        await loadUserData(); // Ensure points are updated
        await loadCoupons();
    }
}

// Friends Functions
async function addFriend() {
    const username = document.getElementById('friendEmail').value.trim().toLowerCase();
    if (!username) {
        showMessage('Please enter a username');
        return;
    }

    if (!currentUser) return;

    try {
        // First, try searching by name
        let userQuery = await db.collection('users')
            .where('name', '==', username)
            .get();

        // If not found by name, try searching by email
        if (userQuery.empty) {
            const emailToSearch = username.includes('@') ? username : `${username}@gmail.com`;
            userQuery = await db.collection('users')
                .where('email', '==', emailToSearch)
                .get();
        }

        // If still not found, try case-insensitive name search
        if (userQuery.empty) {
            console.log('Searching all users for:', username);
            const allUsersSnapshot = await db.collection('users').get();
            const matchingUsers = [];

            console.log('Total users in database:', allUsersSnapshot.size);

            allUsersSnapshot.forEach(doc => {
                const userData = doc.data();
                console.log('Checking user:', userData.name, userData.email);

                // Check if name matches (case-insensitive) or email starts with username
                if (userData.name && userData.name.toLowerCase() === username) {
                    console.log('Found match by name:', userData.name);
                    matchingUsers.push(doc);
                } else if (userData.email && userData.email.toLowerCase().startsWith(username)) {
                    console.log('Found match by email:', userData.email);
                    matchingUsers.push(doc);
                }
            });

            if (matchingUsers.length > 0) {
                userQuery = { empty: false, docs: matchingUsers };
            }
        }

        if (userQuery.empty) {
            showMessage('User not found. Try their full name or email');
            return;
        }

        const friendDoc = userQuery.docs[0];
        const friendData = friendDoc.data();

        if (friendData.userId === currentUser.uid) {
            showMessage("You can't add yourself as a friend!");
            return;
        }

        // Add friend bidirectionally - to both users' friend lists
        const friendInfo = {
            friendId: friendData.userId,
            friendName: friendData.name,
            friendEmail: friendData.email,
            addedAt: Date.now(),
            status: 'ACCEPTED'
        };

        // Add friend to current user's list
        await db.collection('kopi').doc(currentUser.uid)
            .collection('friends').doc(friendData.userId).set(friendInfo);

        // Also add current user to friend's list
        const currentUserInfo = {
            friendId: currentUser.uid,
            friendName: currentUser.displayName || currentUser.email.split('@')[0],
            friendEmail: currentUser.email,
            addedAt: Date.now(),
            status: 'ACCEPTED'
        };

        await db.collection('kopi').doc(friendData.userId)
            .collection('friends').doc(currentUser.uid).set(currentUserInfo);

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
                    <button onclick="openChat('${friend.friendId}', '${friend.friendName}')" class="chat-btn">💬 Chat</button>
                </div>
            `;

            friendsList.appendChild(friendCard);
        });

    } catch (error) {
        console.error('Error loading friends:', error);
    }
}

// History Functions
async function refreshHistory() {
    if (currentUser) {
        showMessage('Refreshing history...');
        await ensureUserDocumentsExist(currentUser);
        await loadHistory();
        await loadUserData();
        showMessage('History refreshed!');
    }
}

async function loadHistory() {
    if (!currentUser) return;

    try {
        // Ensure document exists first
        await ensureUserDocumentsExist(currentUser);

        const doc = await db.collection('kopi').doc(currentUser.uid).get();
        const historyList = document.getElementById('historyList');
        historyList.innerHTML = '';

        if (doc && doc.exists) {
            const data = doc.data();
            const walkHistory = data.walkHistory || [];

            console.log('📊 Loading history - Points:', data.points, 'Sessions:', walkHistory.length);

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
        await loadUserData();
        // loadCoupons() will be called automatically by the Firebase listener

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

// Display participants in the UI
async function displayParticipants(participantData) {
    const participantsLoading = document.getElementById('participantsLoading');
    const participantsDisplay = document.getElementById('participantsDisplay');
    const participantsNames = document.getElementById('participantsNames');

    // participantData should contain both names and IDs
    const participants = participantData.names || [];
    const participantIds = participantData.ids || [];

    // Only show participants if there's more than just the current user
    if (participants.length <= 1) {
        // Just the current user, keep showing "waiting for others"
        if (participantsLoading) participantsLoading.style.display = 'block';
        if (participantsDisplay) participantsDisplay.style.display = 'none';
        if (participantsNames) participantsNames.innerHTML = '';
        return;
    }

    if (participants.length > 1) { // More than just the current user
        // Hide loading message, show participants
        if (participantsLoading) participantsLoading.style.display = 'none';
        if (participantsDisplay) participantsDisplay.style.display = 'block';

        // First, get current user's friends list
        let userFriends = [];
        try {
            const friendsSnapshot = await db.collection('kopi').doc(currentUser.uid)
                .collection('friends').get();
            friendsSnapshot.forEach(doc => {
                userFriends.push(doc.id); // Friend user IDs
            });
        } catch (error) {
            console.error('Error loading friends list:', error);
        }

        // Clear and rebuild participants list with add friend buttons
        if (participantsNames) {
            participantsNames.innerHTML = '';
            const currentUserName = currentUser.displayName || currentUser.email.split('@')[0];

            for (let i = 0; i < participants.length; i++) {
                const name = participants[i];
                const userId = participantIds[i];

                const participantDiv = document.createElement('div');
                participantDiv.style.cssText = `
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    margin-bottom: 10px;
                    padding: 10px;
                    background: #F5E6D3;
                    border-radius: 10px;
                `;

                const badge = document.createElement('div');
                badge.style.cssText = `
                    background: #8B5A3C;
                    color: white;
                    padding: 8px 15px;
                    border-radius: 15px;
                    font-size: 14px;
                    flex: 1;
                `;

                // Check if this is the current user
                if (userId === currentUser.uid) {
                    badge.textContent = name + ' (You)';
                    participantDiv.appendChild(badge);
                } else {
                    badge.textContent = name;

                    // Check if already friends
                    const isAlreadyFriend = userFriends.includes(userId);

                    if (isAlreadyFriend) {
                        // Show "Already Friends" indicator
                        const friendIndicator = document.createElement('div');
                        friendIndicator.style.cssText = `
                            background: #4CAF50;
                            color: white;
                            padding: 6px 12px;
                            border-radius: 8px;
                            font-size: 12px;
                            display: flex;
                            align-items: center;
                            gap: 4px;
                        `;
                        friendIndicator.innerHTML = '✓ Friends';

                        participantDiv.appendChild(badge);
                        participantDiv.appendChild(friendIndicator);
                    } else {
                        // Show "Add Friend" button
                        const addFriendBtn = document.createElement('button');
                        addFriendBtn.style.cssText = `
                            background: #6B4423;
                            color: white;
                            border: none;
                            padding: 6px 12px;
                            border-radius: 8px;
                            font-size: 12px;
                            cursor: pointer;
                            transition: all 0.2s;
                        `;
                        addFriendBtn.textContent = '+ Add Friend';
                        addFriendBtn.onclick = () => addParticipantAsFriend(name, userId, addFriendBtn);

                        participantDiv.appendChild(badge);
                        participantDiv.appendChild(addFriendBtn);
                    }
                }

                participantsNames.appendChild(participantDiv);
            }
        }
    } else {
        // Only the current user, show waiting message
        if (participantsLoading) participantsLoading.style.display = 'block';
        if (participantsDisplay) participantsDisplay.style.display = 'none';
    }
}

// Add participant as friend
async function addParticipantAsFriend(participantName, participantId, button) {
    if (!currentUser || !participantId) return;

    try {
        // Get participant's user data
        const userDoc = await db.collection('users').doc(participantId).get();

        if (!userDoc.exists) {
            // If user doc doesn't exist, try to find by name
            const userQuery = await db.collection('users')
                .where('name', '==', participantName)
                .get();

            if (userQuery.empty) {
                console.log('User not found:', participantName);
                button.textContent = 'Not Found';
                button.style.background = '#E53935';
                return;
            }

            const friendDoc = userQuery.docs[0];
            var friendData = friendDoc.data();
        } else {
            var friendData = userDoc.data();
        }

        // Add friend bidirectionally
        const friendInfo = {
            friendId: friendData.userId,
            friendName: friendData.name,
            friendEmail: friendData.email,
            addedAt: Date.now(),
            status: 'ACCEPTED'
        };

        // Add friend to current user's list
        await db.collection('kopi').doc(currentUser.uid)
            .collection('friends').doc(friendData.userId).set(friendInfo);

        // Also add current user to friend's list
        const currentUserInfo = {
            friendId: currentUser.uid,
            friendName: currentUser.displayName || currentUser.email.split('@')[0],
            friendEmail: currentUser.email,
            addedAt: Date.now(),
            status: 'ACCEPTED'
        };

        await db.collection('kopi').doc(friendData.userId)
            .collection('friends').doc(currentUser.uid).set(currentUserInfo);

        button.textContent = 'Added!';
        button.disabled = true;
        button.style.background = '#4CAF50';

        console.log(`Successfully added ${participantName} as friend`);
    } catch (error) {
        console.error('Error adding participant as friend:', error);
        button.textContent = 'Error';
        button.style.background = '#E53935';
    }
}

// Listen for participants joining the session
function listenForParticipants() {
    if (!walkingSession.sessionId) return;

    console.log('👥 Listening for participants in session:', walkingSession.sessionId);

    // Listen to the active session for changes
    const unsubscribe = db.collection('activeSessions')
        .doc(walkingSession.sessionId)
        .onSnapshot((doc) => {
            if (doc.exists) {
                const sessionData = doc.data();
                const participantNames = sessionData.participantNames || [];
                const participantIds = sessionData.participants || [];

                console.log('Participants updated:', participantNames, participantIds);

                // Update the UI with both names and IDs
                displayParticipants({
                    names: participantNames,
                    ids: participantIds
                });

                // Update walking session data
                if (participantNames.length > 1) {
                    walkingSession.isGroupWalk = true;
                    walkingSession.groupParticipants = participantIds;
                    walkingSession.groupParticipantNames = participantNames;
                }
            }
        });

    // Store unsubscribe function to clean up later
    walkingSession.participantsListener = unsubscribe;
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

// Language Functions - Make them globally available
window.toggleLanguageMenu = function() {
    const dropdown = document.getElementById('languageDropdown');
    if (dropdown) {
        dropdown.classList.toggle('hidden');
    }
}

window.setLanguage = function(lang) {
    currentLanguage = lang;
    localStorage.setItem('selectedLanguage', lang);
    updateLanguageDisplay();
    updateAllText();

    // Close the dropdown
    const dropdown = document.getElementById('languageDropdown');
    if (dropdown) {
        dropdown.classList.add('hidden');
    }
}

function updateLanguageDisplay() {
    // Update the current language flag display
    const flagDisplay = document.querySelector('.flag-display');
    if (flagDisplay) {
        const flags = {
            'en': '🇬🇧',
            'zh': '🇨🇳',
            'ms': '🇲🇾'
        };
        flagDisplay.textContent = flags[currentLanguage] || '🇬🇧';
    }
}

function t(key, params = {}) {
    let text = translations[currentLanguage]?.[key] || translations.en[key] || key;

    // Replace parameters in the text
    Object.keys(params).forEach(param => {
        text = text.replace(`{${param}}`, params[param]);
    });

    return text;
}

function updateAllText() {
    // Update login screen
    const loginTitle = document.querySelector('#loginScreen h1');
    if (loginTitle) loginTitle.textContent = t('loginTitle');

    const loginTagline = document.querySelector('#loginScreen .tagline');
    if (loginTagline) loginTagline.textContent = t('tagline');

    const emailInput = document.getElementById('email');
    if (emailInput) emailInput.placeholder = t('emailPlaceholder');

    const passwordInput = document.getElementById('password');
    if (passwordInput) passwordInput.placeholder = t('passwordPlaceholder');

    const loginButton = document.querySelector('#loginForm button');
    if (loginButton) loginButton.textContent = t('loginButton');

    // Update signup screen
    const signupTitle = document.querySelector('#signupScreen h1');
    if (signupTitle) signupTitle.textContent = t('joinTitle');

    const signupName = document.getElementById('signupName');
    if (signupName) signupName.placeholder = t('namePlaceholder');

    const signupEmail = document.getElementById('signupEmail');
    if (signupEmail) signupEmail.placeholder = t('emailPlaceholder');

    const signupPassword = document.getElementById('signupPassword');
    if (signupPassword) signupPassword.placeholder = t('passwordPlaceholder');

    const signupButton = document.querySelector('#signupForm button');
    if (signupButton) signupButton.textContent = t('signupButton');

    // Update dashboard screen
    const dashboardTitle = document.querySelector('#dashboardScreen h2');
    if (dashboardTitle) dashboardTitle.textContent = t('dashboardTitle');

    const dashboardTagline = document.querySelector('#dashboardScreen .tagline');
    if (dashboardTagline) dashboardTagline.textContent = t('tagline');

    // Update dashboard description
    const description = document.querySelector('.description');
    if (description) description.textContent = t('dashboardDescription');

    // Update bottom text
    const bottomText = document.querySelector('.bottom-decoration p');
    if (bottomText) bottomText.textContent = t('bottomText');

    const joinSessionBtn = document.getElementById('joinSessionBtn');
    if (joinSessionBtn) joinSessionBtn.textContent = t('joinSessionButton');

    const scheduleBtn = document.querySelector('button[onclick="showSchedule()"]');
    if (scheduleBtn) scheduleBtn.textContent = t('scheduleButton');

    const friendsBtn = document.querySelector('button[onclick="showFriends()"]');
    if (friendsBtn) friendsBtn.textContent = t('friendsButton');

    const historyBtn = document.querySelector('button[onclick="showHistory()"]');
    if (historyBtn) historyBtn.textContent = t('historyButton');

    const rewardsBtn = document.querySelector('button[onclick="showRewards()"]');
    if (rewardsBtn) rewardsBtn.textContent = t('rewardsButton');

    const logoutBtn = document.querySelector('.logout-btn');
    if (logoutBtn) logoutBtn.textContent = t('logoutButton');

    // Update QR scanner screen
    const qrTitle = document.querySelector('#qrScreen .join-title');
    if (qrTitle) qrTitle.textContent = t('joinSessionTitle');

    const startCameraBtn = document.getElementById('startCamera');
    if (startCameraBtn) startCameraBtn.textContent = t('scanButton');

    const qrInfoText = document.querySelector('.qr-info-text');
    if (qrInfoText) qrInfoText.innerHTML = t('qrInfoText');

    const manualEntryText = document.querySelector('.manual-entry-text');
    if (manualEntryText) manualEntryText.textContent = t('manualEntryText');

    const sessionCodeInput = document.getElementById('sessionCode');
    if (sessionCodeInput) sessionCodeInput.placeholder = t('sessionPlaceholder');

    const joinBtn = document.querySelector('.btn-join');
    if (joinBtn) joinBtn.textContent = t('joinButton');

    // Update friends screen
    const friendsTitle = document.querySelector('#friendsScreen .header h2');
    if (friendsTitle) friendsTitle.textContent = t('friendsTitle');

    const friendEmailInput = document.getElementById('friendEmail');
    if (friendEmailInput) friendEmailInput.placeholder = t('usernamePlaceholder');

    const addFriendBtn = document.querySelector('.add-friend button');
    if (addFriendBtn) addFriendBtn.textContent = t('addFriendButton');

    // Update history screen
    const historyTitle = document.querySelector('#historyScreen .header h2');
    if (historyTitle) historyTitle.textContent = t('historyTitle');

    const historySubtitle = document.querySelector('.history-subtitle');
    if (historySubtitle) historySubtitle.textContent = t('historySubtitle');

    const refreshBtn = document.querySelector('button[onclick="refreshHistory()"]');
    if (refreshBtn) refreshBtn.textContent = t('refreshButton');

    const sessionsHeader = document.querySelector('.sessions-header');
    if (sessionsHeader) sessionsHeader.textContent = t('sessionsHeader');

    // Update rewards screen
    const rewardsTitle = document.querySelector('#rewardsScreen .header h2');
    if (rewardsTitle) rewardsTitle.textContent = t('rewardsTitle');

    // Update schedule screen
    const scheduleTitle = document.querySelector('#scheduleScreen .header h2');
    if (scheduleTitle) scheduleTitle.textContent = t('scheduleTitle');

    const allWalksTab = document.getElementById('allWalksTab');
    if (allWalksTab) allWalksTab.textContent = t('allWalksTab');

    const scheduleWalkTab = document.getElementById('scheduleWalkTab');
    if (scheduleWalkTab) scheduleWalkTab.textContent = t('scheduleWalkTab');

    // Update completion screen
    const completionTitle = document.querySelector('.completion-title');
    if (completionTitle) completionTitle.textContent = t('completionTitle');

    const backToHomeBtn = document.querySelector('.completion-home-btn');
    if (backToHomeBtn) backToHomeBtn.textContent = t('backToHomeButton');

    // Update all back buttons
    const backButtons = document.querySelectorAll('.back-btn, .completion-back-btn');
    backButtons.forEach(btn => {
        btn.textContent = t('backButton');
    });

    // Update username display if user is logged in
    if (currentUser && currentUser.email) {
        const username = currentUser.email.split('@')[0];
        const usernameText = document.getElementById('usernameText');
        if (usernameText) {
            usernameText.textContent = t('usernameText') + username;
        }
    }
}

// Initialize language on page load
function initializeLanguage() {
    // Update the current language flag display
    updateLanguageDisplay();

    // Update all text to the selected language
    updateAllText();
}

// Chat Functions
let currentChatFriendId = null;
let chatListener = null;

function openChat(friendId, friendName) {
    currentChatFriendId = friendId;
    document.getElementById('chatFriendName').textContent = friendName;
    showScreen('chatScreen');
    loadMessages(friendId);
}
// Make chat functions available globally for onclick handlers
window.openChat = openChat;
window.sendMessage = sendMessage;

async function sendMessage() {
    if (!currentUser || !currentChatFriendId) return;

    const input = document.getElementById('chatInput');
    const message = input.value.trim();
    if (!message) return;

    // Check message safety if available
    if (typeof messageSafety !== 'undefined') {
        // Use message safety module to check and potentially block the message
        const wasSent = await messageSafety.processMessage(message, async (safeMessage) => {
            // This callback is only called if the message is safe to send
            await actualSendMessage(safeMessage);
        });

        if (wasSent) {
            input.value = ''; // Clear input only if message was sent
        }
    } else {
        // Fallback if message safety module is not loaded
        await actualSendMessage(message);
        input.value = '';
    }
}

// Actual message sending logic separated for reuse
async function actualSendMessage(message) {
    if (!currentUser || !currentChatFriendId) return;

    try {
        // Create unique chat ID (sorted user IDs to ensure consistency)
        const chatId = [currentUser.uid, currentChatFriendId].sort().join('_');

        const messageData = {
            senderId: currentUser.uid,
            senderName: currentUser.displayName || currentUser.email.split('@')[0],
            text: message,
            timestamp: Date.now()
        };

        // Add message to Firestore
        await db.collection('chats').doc(chatId)
            .collection('messages').add(messageData);

        console.log('Message sent successfully');
    } catch (error) {
        console.error('Error sending message:', error);
        showMessage('Failed to send message', true);
    }
}

async function loadMessages(friendId) {
    if (!currentUser || !friendId) return;

    // Clear existing listener
    if (chatListener) {
        chatListener();
    }

    try {
        const chatId = [currentUser.uid, friendId].sort().join('_');
        const messagesContainer = document.getElementById('chatMessages');

        // Listen for messages in real-time
        chatListener = db.collection('chats').doc(chatId)
            .collection('messages')
            .orderBy('timestamp', 'asc')
            .onSnapshot(snapshot => {
                messagesContainer.innerHTML = '';

                if (snapshot.empty) {
                    messagesContainer.innerHTML = '<div style="text-align: center; padding: 20px; color: #8B5A3C;">Start a conversation!</div>';
                    return;
                }

                snapshot.forEach(doc => {
                    const msg = doc.data();
                    const messageDiv = document.createElement('div');
                    const isOwnMessage = msg.senderId === currentUser.uid;

                    messageDiv.className = isOwnMessage ? 'message own-message' : 'message friend-message';

                    const time = new Date(msg.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});

                    messageDiv.innerHTML = `
                        <div class="message-content">
                            ${!isOwnMessage ? `<div class="message-sender">${msg.senderName}</div>` : ''}
                            <div class="message-text">${msg.text}</div>
                            <div class="message-time">${time}</div>
                        </div>
                    `;

                    messagesContainer.appendChild(messageDiv);
                });

                // Scroll to bottom
                messagesContainer.scrollTop = messagesContainer.scrollHeight;
            });
    } catch (error) {
        console.error('Error loading messages:', error);
        document.getElementById('chatMessages').innerHTML = '<div style="text-align: center; padding: 20px; color: #E53935;">Failed to load messages</div>';
    }
}

// Schedule Functions (defined before DOMContentLoaded so they're available globally)
function switchTab(tabName) {
    // Hide all tab contents
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });

    // Remove active class from all tabs
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });

    // Show selected tab content
    if (tabName === 'allWalks') {
        document.getElementById('allWalksContent').classList.add('active');
        document.getElementById('allWalksTab').classList.add('active');
        loadMyWalks();
        loadNearbyWalks();
    } else if (tabName === 'scheduleWalk') {
        document.getElementById('scheduleWalkContent').classList.add('active');
        document.getElementById('scheduleWalkTab').classList.add('active');
    }
}

async function loadMyWalks() {
    if (!currentUser) {
        console.log('No current user for loadMyWalks');
        return;
    }

    const myWalksList = document.getElementById('myWalksList');
    if (!myWalksList) {
        console.error('myWalksList element not found');
        return;
    }

    myWalksList.innerHTML = '<div style="padding: 20px; color: #8B5A3C;">Loading...</div>';

    try {
        const now = Date.now();
        console.log('Loading walks for user:', currentUser.uid);

        // Query only walks created by current user
        console.log('Querying walks where createdBy ==', currentUser.uid);
        const snapshot = await db.collection('scheduledWalks')
            .where('createdBy', '==', currentUser.uid)
            .get();

        console.log('Firebase query returned', snapshot.size, 'documents');

        const walks = [];
        const expiredWalks = [];

        console.log('Found', snapshot.size, 'walks in database');

        snapshot.forEach(doc => {
            const walk = { id: doc.id, ...doc.data() };
            const walkTime = walk.scheduledTime;
            console.log('Walk:', walk.locationName, 'Time:', new Date(walkTime), 'CreatedBy:', walk.createdBy, 'CurrentUser:', currentUser.uid);

            // Check if createdBy matches current user
            const isMyWalk = walk.createdBy === currentUser.uid;
            console.log('Is my walk?', isMyWalk, 'Match:', walk.createdBy, '===', currentUser.uid);

            // Check if walk is expired (30 minutes past scheduled time)
            if (walkTime < (now - 30 * 60 * 1000)) {
                console.log('Walk is expired, adding to delete list');
                expiredWalks.push(doc.id);
            } else {
                console.log('Walk is not expired, adding to walks list');
                walks.push(walk);
            }
        });

        console.log('Total walks after filtering:', walks.length);
        console.log('Walks to delete (expired):', expiredWalks.length);

        // Sort walks by scheduled time
        walks.sort((a, b) => a.scheduledTime - b.scheduledTime);

        // Delete expired walks
        for (const walkId of expiredWalks) {
            await db.collection('scheduledWalks').doc(walkId).delete();
            console.log('Deleted expired walk:', walkId);
        }

        if (walks.length === 0) {
            myWalksList.innerHTML = '<div style="padding: 20px; color: #8B5A3C; text-align: center;">No scheduled walks</div>';
            return;
        }

        myWalksList.innerHTML = walks.map(walk => {
            const date = new Date(walk.scheduledTime);
            const timeUntil = walk.scheduledTime - now;
            const minutesUntil = Math.floor(timeUntil / (60 * 1000));
            const hoursUntil = Math.floor(minutesUntil / 60);

            let timeText = '';
            if (minutesUntil < 0) {
                timeText = 'Started';
            } else if (hoursUntil > 0) {
                timeText = `In ${hoursUntil}h ${minutesUntil % 60}m`;
            } else {
                timeText = `In ${minutesUntil}m`;
            }

            return `
                <div class="walk-item">
                    <div class="walk-location">${walk.locationName}</div>
                    <div class="walk-time">${date.toLocaleString()}</div>
                    <div class="walk-status">${timeText}</div>
                    <button class="join-btn" onclick="joinScheduledWalk('${walk.id}')">Join</button>
                </div>
            `;
        }).join('');

    } catch (error) {
        console.error('Error loading my walks:', error);
        myWalksList.innerHTML = '<div style="padding: 20px; color: #E53935;">Failed to load walks</div>';
    }
}

async function loadNearbyWalks() {
    const nearbyWalksList = document.getElementById('nearbyWalksList');
    if (!nearbyWalksList) {
        console.error('nearbyWalksList element not found');
        return;
    }

    nearbyWalksList.innerHTML = '<div style="padding: 20px; color: #8B5A3C;">Loading...</div>';

    try {
        const now = Date.now();
        console.log('Loading nearby walks...');

        // Get all walks without complex query first
        const snapshot = await db.collection('scheduledWalks')
            .get();

        const walks = [];
        const expiredWalks = [];

        console.log('Found', snapshot.size, 'total walks');

        snapshot.forEach(doc => {
            const walk = { id: doc.id, ...doc.data() };

            // Check if walk is expired (30 minutes past scheduled time)
            if (walk.scheduledTime < (now - 30 * 60 * 1000)) {
                expiredWalks.push(doc.id);
            } else if (walk.scheduledTime > (now - 30 * 60 * 1000)) {
                // Only show walks that haven't started more than 30 mins ago
                // and are not created by current user
                if (walk.createdBy !== currentUser?.uid) {
                    walks.push(walk);
                }
            }
        });

        // Sort walks by scheduled time
        walks.sort((a, b) => a.scheduledTime - b.scheduledTime);

        // Limit to 10 walks
        const limitedWalks = walks.slice(0, 10);

        // Delete expired walks
        for (const walkId of expiredWalks) {
            await db.collection('scheduledWalks').doc(walkId).delete();
            console.log('Deleted expired walk:', walkId);
        }

        if (limitedWalks.length === 0) {
            nearbyWalksList.innerHTML = '<div style="padding: 20px; color: #8B5A3C; text-align: center;">No nearby walks scheduled</div>';
            return;
        }

        nearbyWalksList.innerHTML = limitedWalks.map(walk => {
            const date = new Date(walk.scheduledTime);
            const timeUntil = walk.scheduledTime - now;
            const minutesUntil = Math.floor(timeUntil / (60 * 1000));
            const hoursUntil = Math.floor(minutesUntil / 60);

            let timeText = '';
            if (minutesUntil < 0) {
                timeText = 'Started';
            } else if (hoursUntil > 0) {
                timeText = `In ${hoursUntil}h ${minutesUntil % 60}m`;
            } else {
                timeText = `In ${minutesUntil}m`;
            }

            return `
                <div class="walk-item">
                    <div class="walk-location">${walk.locationName}</div>
                    <div class="walk-time">${date.toLocaleString()}</div>
                    <div class="walk-organizer">By ${walk.createdByName || 'Anonymous'}</div>
                    <div class="walk-status">${timeText}</div>
                    <button class="join-btn" onclick="joinScheduledWalk('${walk.id}')">Join</button>
                </div>
            `;
        }).join('');

    } catch (error) {
        console.error('Error loading nearby walks:', error);
        nearbyWalksList.innerHTML = '<div style="padding: 20px; color: #E53935;">Failed to load walks</div>';
    }
}

async function createWalk() {
    const location = document.getElementById('walkLocation').value;
    const date = document.getElementById('walkDate').value;
    const time = document.getElementById('walkTime').value;

    console.log('Creating walk:', { location, date, time });

    if (!location || !date || !time) {
        showMessage('Please fill in all fields');
        return;
    }

    if (!currentUser) {
        showMessage('Please login first');
        return;
    }

    try {
        const scheduledTime = new Date(`${date}T${time}`).getTime();

        // Don't allow scheduling in the past
        if (scheduledTime < Date.now()) {
            showMessage('Cannot schedule walks in the past');
            return;
        }

        const walkData = {
            locationName: location,
            scheduledTime: scheduledTime,
            createdBy: currentUser.uid,
            createdByName: currentUser.displayName || currentUser.email.split('@')[0],
            createdAt: Date.now(),
            participants: [currentUser.uid]
        };

        console.log('Creating walk with data:', walkData);
        console.log('Current user UID:', currentUser.uid);

        const docRef = await db.collection('scheduledWalks').add(walkData);
        console.log('Walk created with ID:', docRef.id);

        showMessage('Walk scheduled successfully!');

        // Reset form
        document.getElementById('createWalkForm').reset();
        document.getElementById('walkLocation').value = '';
        document.querySelectorAll('.location-btn').forEach(btn => {
            btn.classList.remove('selected');
        });

        // Clear selected location text
        const selectedText = document.getElementById('selectedLocationText');
        if (selectedText) {
            selectedText.textContent = '';
        }

        // Switch to all walks tab
        switchTab('allWalks');

    } catch (error) {
        console.error('Error creating walk:', error);
        showMessage('Failed to schedule walk: ' + error.message, true);
    }
}

async function joinScheduledWalk(walkId) {
    if (!currentUser) {
        showMessage('Please login first');
        return;
    }

    try {
        const walkDoc = await db.collection('scheduledWalks').doc(walkId).get();

        if (!walkDoc.exists) {
            showMessage('Walk not found');
            return;
        }

        const walk = walkDoc.data();

        // Start walking session with this location
        walkingSession.locationName = walk.locationName;
        walkingSession.isGroupWalk = true;
        walkingSession.scheduledWalkId = walkId;

        // Navigate to find friends screen
        showFindFriendsScreen();

    } catch (error) {
        console.error('Error joining walk:', error);
        showMessage('Failed to join walk', true);
    }
}

function selectLocation(location) {
    document.getElementById('walkLocation').value = location;

    // Update button styles
    document.querySelectorAll('.location-btn').forEach(btn => {
        btn.classList.remove('selected');
        if (btn.textContent.includes(location)) {
            btn.classList.add('selected');
        }
    });

    // Update selected location text if it exists
    const selectedText = document.getElementById('selectedLocationText');
    if (selectedText) {
        selectedText.textContent = `Selected: ${location}`;
    }

    console.log('Location selected:', location);
}

// Make functions globally available
window.switchTab = switchTab;
window.loadMyWalks = loadMyWalks;
window.loadNearbyWalks = loadNearbyWalks;
window.createWalk = createWalk;
window.joinScheduledWalk = joinScheduledWalk;
window.selectLocation = selectLocation;

console.log('Schedule functions loaded:', {
    switchTab: typeof window.switchTab,
    createWalk: typeof window.createWalk,
    selectLocation: typeof window.selectLocation
});

// Auto-refresh schedule every minute
setInterval(() => {
    const scheduleScreen = document.getElementById('scheduleScreen');
    if (scheduleScreen && !scheduleScreen.classList.contains('hidden')) {
        loadMyWalks();
        loadNearbyWalks();
    }
}, 60000);

// Initialize app
document.addEventListener('DOMContentLoaded', () => {
    // Initialize language
    initializeLanguage();

    // Add Enter key support for chat input
    const chatInput = document.getElementById('chatInput');
    if (chatInput) {
        chatInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
    }

    // Close language dropdown when clicking outside
    document.addEventListener('click', (e) => {
        const selector = document.querySelector('.language-selector');
        const dropdown = document.getElementById('languageDropdown');

        if (selector && dropdown && !selector.contains(e.target)) {
            dropdown.classList.add('hidden');
        }
    });

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

    // Listen for points updates and refresh UI with debouncing to prevent constant reloads
    let lastHistoryLoad = 0;
    let lastKnownPoints = null;
    let isLoadingHistory = false;

    window.addEventListener('pointsUpdated', (event) => {
        const newPoints = event.detail?.points || 0;
        const now = Date.now();

        console.log('Points updated event:', newPoints, 'Last:', lastKnownPoints);

        // Don't reload if points haven't actually changed
        if (lastKnownPoints !== null && lastKnownPoints === newPoints) {
            console.log('Points unchanged, skipping reload');
            return;
        }

        // Don't reload if we're already loading
        if (isLoadingHistory) {
            console.log('Already loading history, skipping');
            return;
        }

        // Don't reload if less than 5 seconds since last reload
        if (now - lastHistoryLoad < 5000) {
            console.log('Too soon since last reload, skipping');
            return;
        }

        // Don't reload during active walking session
        if (walkingSession && walkingSession.isActive) {
            console.log('Walking session active, skipping reload');
            return;
        }

        // Update tracking variables
        lastKnownPoints = newPoints;
        lastHistoryLoad = now;

        // Check if history screen is visible using classList
        const historyScreen = document.getElementById('historyScreen');
        if (historyScreen && !historyScreen.classList.contains('hidden')) {
            console.log('History screen visible, reloading...');
            isLoadingHistory = true;
            loadHistory().finally(() => {
                isLoadingHistory = false;
            });
        }

        // Check if rewards screen is visible
        const rewardsScreen = document.getElementById('rewardsScreen');
        if (rewardsScreen && !rewardsScreen.classList.contains('hidden')) {
            loadCoupons();
        }
    });
});
