# Google Auth Demo App

This Android application demonstrates how to implement Google Sign-In using Firebase Authentication.

## Features

- Sign in with Google account
- Display user information (name and email) after login
- Sign out functionality

## Setup Instructions

1. Create a Firebase project:
   - Go to the [Firebase Console](https://console.firebase.google.com/)
   - Click "Add project" and follow the setup steps
   - Register your Android app with package name `com.kritavya.googleauth`
   - Download the `google-services.json` file

2. Add the `google-services.json` file:
   - Place the downloaded file in the `app/` directory
   - This file is required for Firebase Authentication to work

3. Set up Google Sign-In in Firebase Console:
   - In your Firebase project, go to Authentication > Sign-in method
   - Enable Google as a sign-in provider
   - Configure the OAuth consent screen in the Google Cloud Console if prompted

4. Build and run the application:
   - Open the project in Android Studio
   - Connect an Android device or use an emulator
   - Click Run

## Implementation Details

The app uses:
- Firebase Authentication for handling user authentication
- Google Sign-In API for the sign-in process
- Material Design components for UI

## Requirements

- Android SDK 24 or higher
- Google Play Services
- Internet connection for authentication 