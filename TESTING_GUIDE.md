# Testing Guide for Walking App

## Quick Start with Android Emulator

### 1. Create an Emulator
- Open Android Studio
- Tools → AVD Manager
- Create Virtual Device → Choose Pixel 6 → Download Android 13 or 14
- Name it "Test Phone"

### 2. Run the App
- Click the green Play button in Android Studio
- Select your emulator
- Wait for app to install and launch

### 3. Test Location Features

#### Simulating Your Location:
1. Click "..." button on emulator sidebar
2. Go to "Location" tab
3. Search for any address (e.g., "Central Park, New York")
4. Click "Set Location"

#### Simulating Walking:
1. In Location tab, click "Routes"
2. Set a start and end point
3. Click "Play Route"
4. Set playback speed (1x = walking speed)
5. The app will track your "movement"

### 4. Test Multi-User Sessions

#### Create Multiple Users:
1. Launch 2-3 emulators (create more AVDs if needed)
2. On Emulator 1: Click "Start New Session"
3. Note the session code (e.g., "SESSION_ABC123")
4. On Emulator 2: Click "Scan QR Code"
5. Since camera won't work, manually enter the session code
6. Both emulators now share location!

### 5. Test Features Checklist

- [ ] Start a new session
- [ ] Join existing session (use session code)
- [ ] See session participants
- [ ] Start walking session
- [ ] View map with your location
- [ ] Track distance/steps/points
- [ ] Pause/Resume session
- [ ] End session and see results

### 6. Troubleshooting

**Map not showing?**
- Make sure you have internet connection on emulator
- OpenStreetMap needs internet for tiles

**Location not updating?**
- Go to emulator settings → Location → Set to "High Accuracy"
- Make sure you're moving the location in emulator controls

**Firebase not working?**
- Check if google-services.json is in app/ folder
- Make sure Firebase Realtime Database is enabled in Firebase Console

### 7. Quick Test Scenario

1. Start emulator
2. Launch app
3. Click "Start New Session"
4. Click "Start Walking Session"
5. In emulator controls, set a route and play it
6. Watch the distance/steps increase
7. Click "End Session" after 1 minute
8. See your points earned!

## Testing Without Movement

If you just want to see the UI without walking:
1. Start a session
2. The timer will run
3. You can manually change location in emulator to simulate jumps
4. Each location change will add distance/points