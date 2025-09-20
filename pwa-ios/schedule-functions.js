// Schedule Walk Functions

// Location selection function
function selectLocation(location) {
    // Update hidden input
    document.getElementById('walkLocation').value = location;

    // Update selected text
    document.getElementById('selectedLocationText').textContent = '✓ ' + location;

    // Update button styles
    document.querySelectorAll('.location-btn').forEach(btn => {
        btn.classList.remove('selected');
        if (btn.textContent.includes(location)) {
            btn.classList.add('selected');
        }
    });
}

function switchTab(tabName) {
    // Remove active class from all tabs and content
    document.querySelectorAll('.tab-button').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

    // Add active class to selected tab
    document.getElementById(tabName + 'Tab').classList.add('active');
    document.getElementById(tabName + 'Content').classList.add('active');

    // Load data for selected tab
    if (tabName === 'allWalks') {
        loadMyWalks();
        loadNearbyWalks();
    } else if (tabName === 'scheduleWalk') {
        // Set minimum date to today
        const today = new Date().toISOString().split('T')[0];
        document.getElementById('walkDate').min = today;

        // Set default time to 2 hours from now
        const defaultTime = new Date();
        defaultTime.setHours(defaultTime.getHours() + 2);
        const timeString = defaultTime.toTimeString().split(':').slice(0, 2).join(':');
        document.getElementById('walkTime').value = timeString;
    }
}

function showCreateWalk() {
    showScreen('createWalkScreen');

    // Set minimum date to today
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('walkDate').min = today;

    // Set default time to 2 hours from now
    const defaultTime = new Date();
    defaultTime.setHours(defaultTime.getHours() + 2);
    const timeString = defaultTime.toTimeString().split(':').slice(0, 2).join(':');
    document.getElementById('walkTime').value = timeString;
}

async function loadMyWalks() {
    if (!currentUser) return;

    try {
        const myWalksList = document.getElementById('myWalksList');
        myWalksList.innerHTML = '<div style="padding: 20px; text-align: center; color: #8B5A3C;">Loading your walks...</div>';

        // Get walks created by current user
        const createdSnapshot = await db.collection('scheduledWalks')
            .where('creatorId', '==', currentUser.uid)
            .get();

        // Get walks user has RSVP'd to
        const rsvpSnapshot = await db.collection('scheduledWalks')
            .where('currentParticipants', 'array-contains', currentUser.uid)
            .get();

        const walks = [];
        const walkIds = new Set();

        // Add created walks
        createdSnapshot.forEach(doc => {
            const walk = { id: doc.id, ...doc.data(), isCreator: true };
            // Filter by status and future time in JavaScript
            if (walk.status === 'ACTIVE' && walk.scheduledTime > Date.now()) {
                walks.push(walk);
                walkIds.add(doc.id);
            }
        });

        // Add RSVP walks (avoid duplicates)
        rsvpSnapshot.forEach(doc => {
            if (!walkIds.has(doc.id)) {
                const walk = { id: doc.id, ...doc.data(), isCreator: false };
                // Filter by status and future time in JavaScript
                if (walk.status === 'ACTIVE' && walk.scheduledTime > Date.now()) {
                    walks.push(walk);
                }
            }
        });

        // Sort by time (already filtered above)
        const futureWalks = walks.sort((a, b) => a.scheduledTime - b.scheduledTime);

        displayWalks(futureWalks, myWalksList, 'my');

    } catch (error) {
        console.error('Error loading my walks:', error);
        document.getElementById('myWalksList').innerHTML =
            '<div style="padding: 20px; text-align: center; color: #E53935;">Error loading walks</div>';
    }
}

async function loadNearbyWalks() {
    if (!currentUser) return;

    try {
        const nearbyWalksList = document.getElementById('nearbyWalksList');
        nearbyWalksList.innerHTML = '<div style="padding: 20px; text-align: center; color: #8B5A3C;">Loading nearby walks...</div>';

        const currentTime = Date.now();
        const snapshot = await db.collection('scheduledWalks')
            .where('status', '==', 'ACTIVE')
            .get();

        const walks = [];
        snapshot.forEach(doc => {
            const walk = { id: doc.id, ...doc.data() };
            // Filter by future time and exclude user's own walks in JavaScript
            if (walk.scheduledTime > currentTime &&
                walk.creatorId !== currentUser.uid &&
                !walk.currentParticipants.includes(currentUser.uid)) {
                walks.push(walk);
            }
        });

        // Sort by time and limit to 20
        walks.sort((a, b) => a.scheduledTime - b.scheduledTime);
        const limitedWalks = walks.slice(0, 20);

        displayWalks(limitedWalks, nearbyWalksList, 'nearby');

    } catch (error) {
        console.error('Error loading nearby walks:', error);
        document.getElementById('nearbyWalksList').innerHTML =
            '<div style="padding: 20px; text-align: center; color: #E53935;">Error loading walks</div>';
    }
}

function displayWalks(walks, container, type) {
    if (walks.length === 0) {
        const message = type === 'my' ?
            'No upcoming walks. Create your first walk!' :
            'No walks available in your area. Check back later!';
        container.innerHTML = `<div style="padding: 40px; text-align: center; color: #8B5A3C;">${message}</div>`;
        return;
    }

    container.innerHTML = '';
    walks.forEach(walk => {
        const walkCard = createWalkCard(walk, type);
        container.appendChild(walkCard);
    });
}

function createWalkCard(walk, type) {
    const card = document.createElement('div');
    card.className = 'walk-card';

    const dateTime = new Date(walk.scheduledTime);
    const isCreator = walk.creatorId === currentUser.uid;
    const hasJoined = walk.currentParticipants.includes(currentUser.uid);

    card.innerHTML = `
        <div class="walk-header">
            <h3 class="walk-location">${walk.locationName}</h3>
            <span class="walk-status active">
                Open
            </span>
        </div>

        <div class="walk-datetime">
            📅 ${dateTime.toLocaleDateString()} at ${dateTime.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
        </div>

        <div class="walk-participants">
            👥 ${walk.currentParticipants.length} people joined
        </div>

        ${walk.description ? `<div class="walk-description">"${walk.description}"</div>` : ''}

        <div class="walk-creator" style="color: #8B5A3C; font-size: 14px; margin: 8px 0;">
            Created by: ${walk.creatorName}
        </div>

        <div class="walk-actions">
            ${getWalkActions(walk, type, isCreator, hasJoined)}
        </div>
    `;

    return card;
}

function getWalkActions(walk, type, isCreator, hasJoined) {
    if (type === 'my') {
        if (isCreator) {
            return `<button class="walk-action-btn danger" onclick="cancelWalk('${walk.id}')">Cancel Walk</button>`;
        } else if (hasJoined) {
            return `<button class="walk-action-btn secondary" onclick="leaveWalk('${walk.id}')">Leave Walk</button>`;
        }
    } else if (type === 'nearby') {
        return `<button class="walk-action-btn primary" onclick="joinWalk('${walk.id}')">Join Walk</button>`;
    }
    return '';
}

async function joinWalk(walkId) {
    if (!currentUser) return;

    try {
        showLoading();

        const walkRef = db.collection('scheduledWalks').doc(walkId);
        const walkDoc = await walkRef.get();

        if (!walkDoc.exists) {
            showMessage('Walk not found', true);
            return;
        }

        const walk = walkDoc.data();

        // Add user to participants
        await walkRef.update({
            currentParticipants: [...walk.currentParticipants, currentUser.uid]
        });

        showMessage('Successfully joined the walk!');
        loadNearbyWalks(); // Refresh the list

    } catch (error) {
        console.error('Error joining walk:', error);
        showMessage('Failed to join walk', true);
    } finally {
        hideLoading();
    }
}

async function leaveWalk(walkId) {
    if (!currentUser) return;

    if (!confirm('Are you sure you want to leave this walk?')) {
        return;
    }

    try {
        showLoading();

        const walkRef = db.collection('scheduledWalks').doc(walkId);
        const walkDoc = await walkRef.get();

        if (!walkDoc.exists) {
            showMessage('Walk not found', true);
            return;
        }

        const walk = walkDoc.data();
        const updatedParticipants = walk.currentParticipants.filter(id => id !== currentUser.uid);

        await walkRef.update({
            currentParticipants: updatedParticipants
        });

        showMessage('Left the walk successfully');
        loadMyWalks(); // Refresh the list

    } catch (error) {
        console.error('Error leaving walk:', error);
        showMessage('Failed to leave walk', true);
    } finally {
        hideLoading();
    }
}

async function cancelWalk(walkId) {
    if (!confirm('Are you sure you want to cancel this walk? This cannot be undone.')) {
        return;
    }

    try {
        showLoading();

        await db.collection('scheduledWalks').doc(walkId).update({
            status: 'CANCELLED'
        });

        showMessage('Walk cancelled successfully');
        loadMyWalks(); // Refresh the list

    } catch (error) {
        console.error('Error cancelling walk:', error);
        showMessage('Failed to cancel walk', true);
    } finally {
        hideLoading();
    }
}

// Create Walk Form Handler
document.addEventListener('DOMContentLoaded', () => {
    const createWalkForm = document.getElementById('createWalkForm');
    if (createWalkForm) {
        createWalkForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            if (!currentUser) {
                showMessage('Please log in first', true);
                return;
            }

            const location = document.getElementById('walkLocation').value;
            const date = document.getElementById('walkDate').value;
            const time = document.getElementById('walkTime').value;
            const maxParticipants = parseInt(document.getElementById('maxParticipants').value);
            const description = ''; // Removed description field for simplicity

            if (!location || !date || !time) {
                showMessage('Please fill in all required fields', true);
                return;
            }

            // Combine date and time
            const scheduledDateTime = new Date(`${date}T${time}`);

            // Validate future date
            if (scheduledDateTime <= new Date()) {
                showMessage('Please select a future date and time', true);
                return;
            }

            try {
                showLoading();

                const walkData = {
                    walkId: '', // Will be set by Firestore
                    creatorId: currentUser.uid,
                    creatorName: currentUser.displayName || currentUser.email.split('@')[0],
                    locationName: location,
                    scheduledTime: scheduledDateTime.getTime(),
                    maxParticipants: maxParticipants,
                    currentParticipants: [currentUser.uid], // Creator automatically joins
                    description: description,
                    status: 'ACTIVE',
                    createdAt: Date.now(),
                    isRecurring: false,
                    recurringDays: []
                };

                const docRef = await db.collection('scheduledWalks').add(walkData);

                // Update the document with its own ID
                await docRef.update({ walkId: docRef.id });

                showMessage('Walk scheduled successfully!');
                switchTab('allWalks'); // Switch to All Walks tab to see the new walk

            } catch (error) {
                console.error('Error creating walk:', error);
                showMessage('Failed to create walk', true);
            } finally {
                hideLoading();
            }
        });
    }
});