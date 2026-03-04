# GymApp

GymApp is a feature-complete, offline-first Android application designed for fitness enthusiasts who want to manage their workouts, supplements, and gym access in one place. Built with modern Android technologies, it features advanced NFC capabilities, comprehensive progress tracking, and deep UI customization.

## 🚀 Key Features

### 🛡️ NFC Gym Badge Management
GymApp includes a sophisticated NFC suite designed to replace physical gym badges.
- **Multi-Protocol Scanning**: Support for Mifare Ultralight, NTAG, Mifare Classic, and ISO 15693 (NFC-V) tags.
- **Badge Cloning**: Store full tag data, including memory dumps.
- **Host Card Emulation (HCE)**: Emulate stored badges directly from your phone to access gym facilities.
- **Hardware Interoperability**: Export saved tags in the standard `.nfc` format compatible with Flipper Zero.
- **Hex Viewer**: Inspect raw tag data with a built-in hex dump viewer.

### 🏋️ Workout & Progress Tracking
Stay on top of your strength gains with a flexible tracking system.
- **Custom Splits**: Organize your training into personalized splits (e.g., PPL, Upper/Lower).
- **Exercise Logging**: Track sets, reps, and weight with support for both Metric and Imperial units.
- **Performance Analytics**: Automatically calculates Estimated 1-Rep Max (1RM) using the Epley formula.
- **Detailed Stats**: Monitor personal bests (PBs), total volume, and historical progression.

### 💊 Supplement & Health Tools
- **Inventory Management**: Track supplements with specific dosages, administration timing (e.g., Morning Fasted, Pre-workout), and frequencies.
- **Smart Reminders**: Integrated notification system to ensure you never miss a dose.
- **Peptide Calculator**: A precise reconstitution calculator for determining exact dosages and unit measurements.
- **Intake Logs**: Maintain a complete history of your supplement consumption.

### 🎨 Personalization & Utility
- **Material You**: Full support for dynamic color theming on Android 12+.
- **Advanced Theme Editor**: Deeply customize your experience by overriding individual Material 3 color roles for both light and dark modes.
- **Data Portability**: Complete database backup and restore via JSON.
- **Internal Database Viewer**: A built-in tool for power users to inspect the underlying Room database tables.

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Local Storage**: [Room Persistence Library](https://developer.android.com/training/data-storage/room) (SQLite)
- **Concurrency**: Kotlin Coroutines & Flow
- **NFC**: Native Android NFC API + `HostApduService` for HCE
- **Architecture**: Modern Android Architecture with a focus on offline-first reliability.

## 📦 Getting Started

### Prerequisites
- Android device with NFC hardware (required for badge features).
- Android 7.0 (API level 24) or higher.

### Installation
1. Clone the repository.
2. Open the project in **Android Studio Ladybug** or newer.
3. Build and deploy the `app` module to your device.

---

*Note: This application is intended for personal use and experimental exploration of NFC technology on Android.*
