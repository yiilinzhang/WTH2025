# Firebase Authentication Setup Instructions

## Current Status
The app has a fallback authentication system that allows testing without Firebase being fully configured. The test account (test@test.com / testtest) will work even without Firebase.

## To Enable Full Firebase Authentication:

### 1. Go to Firebase Console
- Visit: https://console.firebase.google.com/
- Select your project: **wth2025**

### 2. Enable Authentication
1. Click on "Authentication" in the left sidebar
2. Click "Get started" if you haven't already
3. Go to the "Sign-in method" tab
4. Enable "Email/Password" authentication:
   - Click on "Email/Password"
   - Toggle "Enable" to ON
   - Click "Save"

### 3. Optional: Enable Google Sign-In
1. In the same "Sign-in method" tab
2. Click on "Google"
3. Toggle "Enable" to ON
4. Add your project support email
5. Click "Save"

### 4. Add SHA-1 Certificate (for Google Sign-In)
1. In Firebase Console, go to Project Settings (gear icon)
2. Scroll to "Your apps" section
3. Find your Android app
4. Add SHA-1 fingerprint:
   ```bash
   # To get your debug SHA-1:
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Copy the SHA-1 value and paste it in Firebase Console
6. Download the updated `google-services.json`
7. Replace the file in `app/google-services.json`

### 5. Test the App
After enabling Firebase Authentication:
- The app will use Firebase for real authentication
- You can create new accounts with any email
- All authentication will be handled by Firebase

## Fallback Mode
If Firebase is not configured or encounters errors:
- The app will automatically fall back to local authentication
- Test account (test@test.com / testtest) will still work
- Other accounts can be created but won't persist across app reinstalls

## Troubleshooting
If you see "CONFIGURATION_NOT_FOUND" error:
1. Make sure Authentication is enabled in Firebase Console
2. Ensure `google-services.json` is in the `app/` directory
3. Try cleaning and rebuilding: `./gradlew clean assembleDebug`
4. Check that the package name in Firebase matches: `com.example.myapplication`