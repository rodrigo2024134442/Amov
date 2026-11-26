# CityFix

Native **Android** app where citizens report urban issues (potholes, broken lights, litter...) and municipal operators handle them from report to resolution.
Developed for **Mobile Architectures** at ISEC - Polytechnic University of Coimbra (2026/27).

## Features

- **Three profiles**: citizen, municipal operator and anonymous guest, with registration, login and operator account validation
- **Report incidents** with category, description, photos and videos, and a location picked on **Google Maps** or taken from GPS
- **Ticket lifecycle** for operators: status changes with history, proof of resolution, messages between citizen and operator, reopen, and automatic closing
- **Push notifications** (Firebase Cloud Messaging) when an incident you follow changes
- **Geofencing**: a banner warns you when you are near a reported incident
- **Impact detection** with the device sensors
- **Municipal dashboard** with statistics, and groups of related incidents
- Background uploads and reminders with **WorkManager**, offline support with Firestore cache
- English and Portuguese, portrait and landscape layouts

## Architecture

- **MVVM**: Jetpack Compose screens, ViewModels with StateFlow, repositories over Firebase
- **Firebase**: Authentication, Firestore, Storage, Cloud Messaging and **Cloud Functions** (Node.js triggers on incident create/update and a scheduled job to close resolved incidents)
- Firestore and Storage security rules in `firestore.rules` and `storage.rules`

## Tech

Kotlin, Jetpack Compose, Material 3, MVVM, Coroutines and StateFlow, Navigation Compose, WorkManager, Coil, Google Maps Compose, Play Services Location, Firebase (Auth, Firestore, Storage, FCM, Cloud Functions)

## Run

1. Open the project in Android Studio.
2. Add your own `google-services.json` (Firebase project) to `app/`.
3. Create `secrets.properties` in the project root with your Google Maps key: `MAPS_API_KEY=...`
4. Run on a device or emulator with Google Play services.

The full report is in [`docs/`](docs).

## Authors

- Rodrigo Costa ([@rodrigo2024134442](https://github.com/rodrigo2024134442))
- Marco Marcelo ([@Marco2024114817](https://github.com/Marco2024114817))