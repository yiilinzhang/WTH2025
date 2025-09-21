# Kopi Kakis Walking Club 🚶‍♂️☕

A gamified walking app that brings communities together through group walks and rewards. Walk with friends, earn points, and redeem free kopi!

## 🌟 Features

### Core Functionality
- **Group Walking Sessions**: Create or join walking sessions at specific locations
- **QR Code Integration**: Scan QR codes at coffee shops, parks, and walking trails
- **Real-time Tracking**: GPS-based distance and route tracking
- **Points System**: Earn 10 points per meter walked
- **Group Bonus**: 20% bonus points when walking with others
- **Rewards**: Redeem points for free drinks (Kopi, Teh, Milo)

### Social Features
- **Friend System**: Add friends from walking sessions
- **Group Chat**: Real-time messaging during walks
- **Participant Notifications**: Get notified when others join your session
- **Walking History**: Track past sessions and achievements
- **Weekly Streaks**: Build consistency with streak tracking

### Technical Features
- **Multi-language Support**: English, Chinese (简体中文), Malay
- **PWA Support**: Works offline, installable on mobile devices
- **Real-time Sync**: Firebase Firestore for instant updates
- **Responsive Design**: Optimized for mobile and desktop

## 🚀 Quick Start

### Prerequisites
- Node.js (v14+)
- Firebase account
- Modern web browser with GPS support

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/kopi-kakis-walking.git
cd kopi-kakis-walking
```

2. Install dependencies:
```bash
# For the Android app
cd app
./gradlew build

# For the PWA
cd pwa-ios
npm install  # if package.json exists
```

3. Configure Firebase:
- Create a Firebase project at https://console.firebase.google.com
- Enable Authentication (Email/Password)
- Enable Firestore Database
- Add your Firebase config to the app

4. Start the development server:
```bash
# PWA development
cd pwa-ios
python3 -m http.server 8080

# Or use Node.js
npx http-server -p 8080
```

5. Open http://localhost:8080 in your browser

## 📱 App Structure

```
WTH2025/
├── app/                    # Android native app
│   └── src/main/java/      # Kotlin source code
├── pwa-ios/               # Progressive Web App
│   ├── index.html         # Main app entry
│   ├── app.js             # Core application logic
│   ├── styles.css         # Styling
│   ├── sw.js              # Service worker
│   └── manifest.json      # PWA manifest
└── README.md              # This file
```

## 🎮 How to Use

### Starting a Walking Session
1. Sign in or create an account
2. Click "JOIN WALKING SESSION"
3. Either:
   - Scan a QR code at a location
   - Select from available locations (East Coast Park, Botanic Garden, Bishan Park)
4. Wait for others to join or start walking immediately
5. Track your distance and earn points
6. End session to save progress

### Group Walking
- **Session Creator**: Starts the session and waits for others
- **Participants**: Join by scanning the same QR or selecting the same location
- **Bonus Points**: All participants get 20% bonus when walking together
- **Real-time Updates**: See who joins instantly with notifications

### Redeeming Rewards
1. Go to Rewards section
2. Select a drink (Kopi: 500 points, Teh: 400 points, Milo: 300 points)
3. Show the generated QR code at participating coffee shops
4. Enjoy your free drink!

## 🔧 Configuration

### Firebase Collections Structure
```
kopi/
├── {userId}/
│   ├── points: number
│   ├── noOfKopiRedeemed: number
│   ├── walkHistory: array
│   ├── friends/
│   │   └── {friendId}/
│   └── coupons/
│       └── {couponId}/

activeSessions/
└── {sessionId}/
    ├── sessionId: string
    ├── locationName: string
    ├── createdBy: string
    ├── participants: array
    ├── participantNames: array
    ├── status: string
    └── createdAt: timestamp
```

### Environment Variables
Create a `.env` file (if using a build system):
```env
FIREBASE_API_KEY=your_api_key
FIREBASE_AUTH_DOMAIN=your_auth_domain
FIREBASE_PROJECT_ID=your_project_id
FIREBASE_STORAGE_BUCKET=your_storage_bucket
FIREBASE_MESSAGING_SENDER_ID=your_sender_id
FIREBASE_APP_ID=your_app_id
```

## 🐛 Debugging

### Common Issues

1. **GPS not working**:
   - Ensure location permissions are granted
   - Use HTTPS for production (required for GPS)
   - Check browser console for errors

2. **Participants not showing**:
   - Clear browser cache
   - Check Firebase console for data
   - Verify session is active (30-minute timeout)

3. **Points not calculating**:
   - Ensure GPS is tracking movement
   - Check console for distance updates
   - Verify group bonus is applying (20% for 2+ people)

### Debug Mode
Open browser DevTools console and look for:
- 📊 Firebase session data logs
- 📋 Participant display logs
- 🔄 Polling status
- 🎉 Join notifications

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- OpenStreetMap for mapping services
- Firebase for real-time database
- QR Code libraries for code generation/scanning
- The kopi kakis community for inspiration

## 📞 Support

For issues and questions:
- Create an issue on GitHub
- Contact the development team
- Check the debugging section above

## 🚀 Deployment

### PWA Deployment
1. Build the production version
2. Deploy to any static hosting (Netlify, Vercel, GitHub Pages)
3. Ensure HTTPS is enabled
4. Test PWA installation on mobile devices

### Android Deployment
1. Build the release APK:
```bash
cd app
./gradlew assembleRelease
```
2. Sign the APK
3. Upload to Google Play Store

## 🔮 Future Features

- [ ] Leaderboards and challenges
- [ ] Route recommendations
- [ ] Weather integration
- [ ] Corporate wellness programs
- [ ] Integration with fitness trackers
- [ ] More reward options
- [ ] Social media sharing

---

**Made with ❤️ for the walking community**