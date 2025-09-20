# Database Sync Setup

## Overview
This document explains how to sync the app's backend with Firebase Realtime Database.

## Database Structure
```
firebase-database/
├── users/{userId}/
│   ├── totalPoints: number
│   ├── sessionsCompleted: number
│   ├── lastActivityTime: timestamp
│   └── completedSessions/{sessionId}/
│       ├── distance, duration, points, completedAt
├── sessions/{sessionId}/
│   ├── status: "active" | "completed"
│   ├── participants/{userId}/
│   └── endTime, totalDistance, totalDuration
├── recentSessions/{sessionId}/
│   └── userId, userName, distance, duration, points, completedAt, location
└── leaderboard/{userId}/
    └── userId, userName, totalPoints, sessionsCompleted, lastUpdated
```

## Setup Instructions

### 1. Deploy Database Rules
```bash
# Make sure you have Firebase CLI installed
npm install -g firebase-tools

# Deploy the database rules
./deploy-database-rules.sh
```

### 2. Manual Database Sync
To manually sync the database from Firebase Console:

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project: **WTH2025**
3. Navigate to **Realtime Database**
4. Click **Import JSON** and upload the sample data if needed

### 3. Test Data Seeding
The app automatically seeds test data on first run. To manually trigger:

```kotlin
// In any Activity or Fragment
DatabaseInitializer.seedTestData { success ->
    if (success) {
        Toast.makeText(context, "Test data loaded", Toast.LENGTH_SHORT).show()
    }
}
```

### 4. Force Sync Data
To force sync user data:

```kotlin
// In any Activity or Fragment
DatabaseSyncManager.forceSyncNow(context)
```

## Key Features Implemented

### Session Completion
When a walking session ends:
1. Points calculated (1 point per 100 meters)
2. User's total points updated
3. Session added to completedSessions
4. Entry added to recentSessions for leaderboard
5. Leaderboard entry updated with new totals

### Real-time Updates
The LeaderboardFragment listens to:
- `recentSessions/` for latest walking sessions
- `users/{userId}/` for personal stats and streak

### Data Persistence
- All session data persisted to Firebase
- Automatic cleanup of sessions older than 30 days
- User profiles created on first login
- Leaderboard entries maintained separately for fast queries

## Testing

### Test Walking Session
1. Start app and login
2. Join a walking session (use code: SESSION_TEST99)
3. Walk/simulate movement
4. End session
5. Check leaderboard - should show updated points and recent session

### Verify Database Updates
1. Open Firebase Console
2. Navigate to Realtime Database
3. Check these nodes after ending a session:
   - `/users/{userId}/totalPoints` - should increase
   - `/recentSessions/` - should have new entry
   - `/leaderboard/{userId}/` - should update

## Troubleshooting

### Data Not Syncing
1. Check Firebase configuration in `google-services.json`
2. Verify database URL: `https://wth2025-default-rtdb.firebaseio.com/`
3. Check internet connection
4. Review Logcat for errors with tag "DatabaseSyncManager"

### Points Not Updating
1. Ensure SessionCompletionActivity receives correct data
2. Check userId is not null
3. Verify Firebase rules allow write access

### Test Data Not Loading
1. Clear app data: Settings > Apps > WTH2025 > Clear Data
2. Restart app to trigger first run
3. Check Firebase Console for test entries

## Security Notes
- Database rules enforce user authentication
- Users can only modify their own data
- Leaderboard is read-only for all authenticated users
- Session participants can only be added, not removed