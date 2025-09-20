#!/bin/bash

# Firebase Database Rules Deployment Script
# This script deploys the database rules to Firebase Realtime Database

echo "======================================"
echo "Firebase Database Rules Deployment"
echo "======================================"

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo "❌ Firebase CLI is not installed."
    echo "Please install it by running: npm install -g firebase-tools"
    exit 1
fi

# Check if database.rules.json exists
if [ ! -f "database.rules.json" ]; then
    echo "❌ database.rules.json file not found!"
    exit 1
fi

echo "📋 Found database.rules.json"
echo ""
echo "🔐 Please login to Firebase (if not already logged in):"
firebase login

echo ""
echo "🚀 Deploying database rules..."
firebase deploy --only database

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Database rules deployed successfully!"
    echo ""
    echo "======================================"
    echo "Next steps:"
    echo "1. Open Firebase Console: https://console.firebase.google.com"
    echo "2. Select your project: WTH2025"
    echo "3. Go to Realtime Database"
    echo "4. Verify the rules are applied"
    echo "======================================"
else
    echo ""
    echo "❌ Failed to deploy database rules."
    echo "Please check your Firebase configuration."
fi