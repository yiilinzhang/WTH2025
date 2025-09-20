# How to Share Your App for Testing

## Generate APK to Share

### 1. Find Your Debug APK
Your APK is already built at:
```
/Users/yilinzhang/AndroidStudioProjects/WTH2025/app/build/outputs/apk/debug/app-debug.apk
```

### 2. Share via Email/Drive
1. Upload `app-debug.apk` to Google Drive
2. Share link with friends who have Android
3. They download and install

### 3. Share via Direct Transfer
- AirDrop won't work (iPhone → Android)
- Use WhatsApp, Telegram, or WeTransfer
- Send the APK file

## For Your Friends to Install:

1. **Enable Unknown Sources**
   - Settings → Security → Unknown Sources → ON
   - Or Settings → Apps → Special Access → Install Unknown Apps

2. **Download and Install**
   - Download the APK
   - Tap to install
   - Open app

3. **Test Features**
   - Allow location permission
   - Test with their real location
   - Send you screenshots!

## Quick Testing Checklist for Friends:
- [ ] Can join session with code "SESSION_TEST99"
- [ ] Shows their real location on map
- [ ] Distance starts at 0.00 km
- [ ] Distance increases when walking
- [ ] Points accumulate (1 per 10m)

## Alternative: Instant Apps (No Install)
Consider using Android App Bundles for instant testing without installation (requires Play Store setup)