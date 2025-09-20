# Kopi Kakis PWA - iPhone Testing Version

## Quick Setup for iPhone Testing

### Option 1: Simple HTTP Server (Fastest)
1. Open Terminal and navigate to this folder:
   ```bash
   cd /Users/yilinzhang/AndroidStudioProjects/WTH2025/pwa-ios
   ```

2. Start a local server:
   ```bash
   # Using Python (if available)
   python3 -m http.server 8000

   # OR using Node.js (if available)
   npx http-server -p 8000

   # OR using PHP (if available)
   php -S localhost:8000
   ```

3. Find your Mac's IP address:
   ```bash
   ifconfig | grep "inet " | grep -v 127.0.0.1
   ```

4. On your iPhone, open Safari and go to:
   ```
   http://YOUR_MAC_IP:8000
   ```
   (Replace YOUR_MAC_IP with the IP from step 3)

5. **Add to Home Screen:**
   - Tap the Share button in Safari
   - Tap "Add to Home Screen"
   - The app will work like a native app!

### Option 2: Firebase Hosting (More Permanent)
1. Install Firebase CLI:
   ```bash
   npm install -g firebase-tools
   ```

2. Login and initialize:
   ```bash
   firebase login
   firebase init hosting
   ```

3. Deploy:
   ```bash
   firebase deploy
   ```

## Features Working on iPhone

✅ **Firebase Authentication** - Same login as Android app
✅ **Same Database** - All your walking data syncs
✅ **QR Scanner** - Camera works in Safari
✅ **GPS Tracking** - Location tracking during walks
✅ **Friends System** - Add friends by email
✅ **Walking History** - View past sessions
✅ **Rewards System** - Redeem coffee with points
✅ **PWA Features** - Works offline, home screen icon

## Test with Your QR Codes

The app works with your existing QR codes:
- East Coast Park
- Botanic Garden
- Bishan Park

Just scan or type the location name!

## Notes

- **Camera Permission:** iPhone will ask for camera permission for QR scanning
- **Location Permission:** iPhone will ask for location permission for GPS tracking
- **Same Firebase Project:** Uses your existing wth2025 Firebase project
- **Offline Support:** Works without internet after first load

## Troubleshooting

**Can't access from iPhone?**
- Make sure iPhone and Mac are on same WiFi
- Check Mac's firewall settings
- Try http (not https) for local testing

**QR Scanner not working?**
- Allow camera permission in Safari
- Make sure you're using Safari (not Chrome)

**GPS not working?**
- Allow location permission when prompted
- Make sure you're using HTTPS or localhost

Your Android app and iPhone PWA share the same Firebase backend, so all your data syncs perfectly! 🎉