# Kopi Kakis PWA - iOS/Android Testing Version

## Quick Setup for Mobile Testing

### Option 1: HTTPS Server (Recommended for Full PWA Features)

1. **Navigate to project folder:**
   ```bash
   cd /Users/yilinzhang/AndroidStudioProjects/WTH2025/pwa-ios
   ```

2. **Generate SSL certificates (one-time setup):**
   ```bash
   # Install mkcert if not already installed
   brew install mkcert
   mkcert -install

   # Generate certificates
   mkcert localhost 127.0.0.1 ::1
   ```

3. **Start HTTPS server:**
   ```bash
   # Using npx http-server with SSL
   npx http-server -p 8443 -a 0.0.0.0 --ssl --cert localhost.pem --key localhost-key.pem
   ```

4. **Find your Mac's IP address:**
   ```bash
   ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -1
   ```

5. **Access on mobile device:**
   - Open Safari (iOS) or Chrome (Android)
   - Go to: `https://YOUR_MAC_IP:8443`
   - Accept the certificate warning (it's safe - it's your local server)
   - **Add to Home Screen:**
     - iOS: Tap Share → Add to Home Screen
     - Android: Menu → Add to Home screen

### Option 2: Simple HTTP Server (Quick Testing)

1. **Navigate to project folder:**
   ```bash
   cd /Users/yilinzhang/AndroidStudioProjects/WTH2025/pwa-ios
   ```

2. **Start HTTP server:**
   ```bash
   # Using Node.js
   npx http-server -p 8080 -a 0.0.0.0

   # OR using Python
   python3 -m http.server 8080 --bind 0.0.0.0
   ```

3. **Access on mobile:**
   - Go to: `http://YOUR_MAC_IP:8080`
   - Note: Some features (camera, GPS) may require HTTPS

## Features Working on Mobile

✅ **Message Safety System** - AI-powered scam prevention
✅ **Firebase Authentication** - Synced across all devices
✅ **Real-time Chat** - With automatic message filtering
✅ **QR Scanner** - Camera access for location check-ins
✅ **GPS Tracking** - Live location during walks
✅ **Friends System** - Add friends, see who's walking
✅ **Walking Sessions** - Join friends at same location
✅ **Rewards System** - Earn points, redeem coffee
✅ **Multi-language** - English, Chinese, Malay
✅ **PWA Features** - Offline support, app-like experience

## New Safety Features

### Message Protection
- **Blocks sensitive info**: Credit cards, SSNs, passwords, phone numbers
- **Detects scam patterns**: Urgent money requests, suspicious links
- **Filters inappropriate content**: Automatic profanity blocking
- **Silent protection**: Messages blocked without sending

## Test Locations (QR Codes)

The app recognizes these locations:
- East Coast Park
- Botanic Garden
- Bishan Park
- Marina Bay
- Jurong Lake Gardens

## Required Permissions

**iOS Safari:**
- Camera (for QR scanning)
- Location (for GPS tracking)
- Notifications (optional)

**Android Chrome:**
- Camera permission
- Location permission
- Install app permission

## Troubleshooting

### Can't connect from phone?
```bash
# Check firewall is allowing connections
sudo pfctl -d  # Temporarily disable macOS firewall (if needed)

# Ensure both devices on same network
ping YOUR_PHONE_IP
```

### Certificate warnings?
- This is normal for local development
- Tap "Advanced" → "Proceed to site"
- The connection is still encrypted

### Features not working?
- **Camera/GPS need HTTPS** - Use Option 1 setup
- **Clear browser cache** - Settings → Safari → Clear History
- **Service Worker issues** - Close all tabs and retry

### App crashes or shows old version?
1. Clear all website data:
   - iOS: Settings → Safari → Advanced → Website Data → Remove All
   - Android: Chrome → Settings → Privacy → Clear browsing data
2. Close all browser tabs
3. Re-open the URL fresh

## Database Structure

**Firebase Collections:**
- `kopi` - User profiles, points, friends
- `activeSessions` - Live walking sessions
- `scheduledWalks` - Planned future walks
- `messages` - Chat messages (filtered for safety)

## Development Notes

**Key Files:**
- `app.js` - Main application logic
- `message-safety.js` - AI message filtering
- `sw.js` - Service worker for offline support
- `index.html` - Main UI structure
- `styles.css` - All styling

**Testing Message Safety:**
- Open chat with a friend
- Try sending: phone numbers, credit cards, passwords
- Messages will be blocked automatically

## Support

Your Android app and iPhone PWA share the same Firebase backend, so all data syncs in real-time! 🎉

For issues or questions, check the browser console for errors (Safari → Develop → iPhone → Console).