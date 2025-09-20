# Firebase Setup Instructions

## ⚠️ IMPORTANT: Enable Firebase Realtime Database

Your app is failing because Firebase Realtime Database is not enabled. Follow these steps:

### 1. Go to Firebase Console
Visit: https://console.firebase.google.com/project/wth2025

### 2. Enable Realtime Database
1. Click on **"Build"** in the left sidebar
2. Click on **"Realtime Database"**
3. Click **"Create Database"**
4. Choose location (use default: United States)
5. Start in **TEST MODE** (for development)
6. Click **"Enable"**

### 3. Set Database Rules (for testing)
Once created, go to the **Rules** tab and set:

```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

⚠️ **WARNING**: These rules allow anyone to read/write. Only use for testing!

### 4. Your Database URL
Your database URL is: `https://wth2025-default-rtdb.firebaseio.com/`

This has been added to your code already.

### 5. Rebuild and Run
After enabling the database in Firebase Console:
1. Clean build: `./gradlew clean`
2. Rebuild: `./gradlew assembleDebug`
3. Run the app again

## Test Flow
1. Open app
2. Click FAB button
3. Click "Scan QR Code to Join"
4. Enter: `SESSION_TEST99`
5. Click "Join"
6. You should now see the participants page!

## Troubleshooting
If still not working, check Logcat for:
- "Firebase Database connection was forcefully killed" - Database not enabled
- "Successfully joined session" - Everything working!

## Firebase Console Direct Link
https://console.firebase.google.com/project/wth2025/database