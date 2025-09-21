# Testing the Group Walk Fix

## Steps to Clear Cache and Test:

### 1. Clear Safari/Chrome Cache Completely:

#### For Safari on iOS:
1. Go to Settings > Safari
2. Tap "Clear History and Website Data"
3. Confirm the action

#### For Chrome:
1. Open Chrome DevTools (F12 or right-click > Inspect)
2. Go to Application tab
3. Under "Storage", click "Clear site data"
4. OR: Hold Shift and click the Reload button for hard refresh

### 2. Unregister Service Worker:
1. Open DevTools (F12)
2. Go to Application tab
3. Click on "Service Workers" on the left
4. Find your service worker and click "Unregister"
5. OR add `?nocache=${Date.now()}` to your URL

### 3. Test the Fix:

#### Test Case 1: Session Creator Sees Themselves
1. Open the app in Browser 1 (use incognito/private mode)
2. Sign in as User A
3. Go to "Find Friends/Group"
4. Scan QR code or select location
5. **Expected**: You should see "Waiting for others to join..." message
6. Check console for: "Created walking session" and participant data

#### Test Case 2: Others Joining
1. Open the app in Browser 2 (different browser or incognito)
2. Sign in as User B
3. Join the same session (scan same QR or select same location)
4. **Expected**:
   - User A should now see User B in participants list
   - User B should see User A in participants list

#### Test Case 3: 20% Bonus Points
1. Start walking with both users in the session
2. Walk for a bit to accumulate distance
3. End the walk
4. **Expected**: Points should show 20% bonus (check if points = distance × 10 × 1.2)

### 4. Alternative Testing Method:

If cache is persistent, try:
```bash
# Start a local server with cache busting
cd pwa-ios
python3 -m http.server 8080 --bind localhost
```

Then access: `http://localhost:8080/?v=${timestamp}`

### 5. Check Console Logs:

Open browser console and look for:
- "👥 Listening for participants in session:"
- "Participants updated:"
- "Created walking session:"
- Group participants array should include the creator

### 6. Verify in Firebase:

1. Go to Firebase Console
2. Check Firestore > activeSessions collection
3. Verify the session document has:
   - `participants` array with creator's UID
   - `participantNames` array with creator's name

## Troubleshooting:

If changes don't appear:
1. Check if app.js file size is ~115KB (indicating updates)
2. In DevTools Network tab, ensure app.js is fetched (not from cache)
3. Try opening in a completely different browser
4. Check for JavaScript errors in console
5. Verify Firebase permissions allow reading/writing activeSessions

## Quick Debug:

Add this to browser console to check current session:
```javascript
console.log('Current session:', walkingSession);
console.log('Group participants:', walkingSession.groupParticipants);
console.log('Is group walk:', walkingSession.isGroupWalk);
```