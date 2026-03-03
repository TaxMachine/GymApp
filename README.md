# GymApp: An AI-Generated Fitness & NFC Companion

> **Warning**: This project was generated entirely by an AI assistant (Google's Gemini). The code is experimental, has not been reviewed by a human developer, and should **not** be used in a production environment without a thorough security and code quality analysis.

GymApp is a feature-rich, offline-first Android application designed for tracking workouts, supplements, and NFC gym badges. It was built to demonstrate advanced capabilities in modern Android development, including complex UI, database management, and hardware interactions like NFC.

## Key Features

-   **Workout & Progress Tracking**:
    -   Create custom workout **Splits** (e.g., Push, Pull, Legs).
    -   Add **Exercises** with weight, reps, and units.
    -   Log progress for each exercise and view historical performance.
    -   Calculates **Estimated 1-Rep Max (1RM)** and total volume.

-   **Performance Analytics**:
    -   Visual **graphs** display weight progression over time.
    -   **Statistics summaries** show total progress, personal bests, and starting vs. current metrics.

-   **Supplement Management**:
    -   Log supplements with custom dosage, unit, timing, and frequency.
    -   Track dosage history with dedicated graphs.
    -   Set up **daily reminders** for supplement intake (requires notification permissions).

-   **NFC Gym Badge Cloning & Emulation**:
    -   **Scan and clone** NFC gym badges (supports MIFARE Ultralight, NTAG, and some MIFARE Classic tags).
    -   Saves a **full memory dump** of the tag, not just the UID.
    -   **Emulate** your cloned badge directly from your phone using Host Card Emulation (HCE).
    -   View the complete memory layout in a **Hexdump** format.
    -   **Export** badge data to the standard `.nfc` file format used by tools like Flipper Zero.

-   **Utilities & Settings**:
    -   **Peptide Reconstitution Calculator**: Easily calculate the correct number of units for a desired dosage.
    -   **Theme Switching**: Instantly switch between Light, Dark, and System Default themes.
    -   **Data Management**:
        -   **Export** the entire database to a single JSON file.
        -   **Import** data from a JSON backup.
        -   **Database Viewer**: An in-app tool to inspect the raw data in every table.
        -   **Delete All Data**: A function to completely wipe the local database.

## Technology Stack

-   **UI**: 100% [Jetpack Compose](https://developer.android.com/jetpack/compose) for a modern, declarative UI.
-   **Architecture**: Offline-first approach using a single-activity pattern.
-   **Database**: [Room](https://developer.android.com/training/data-storage/room) for robust, local SQLite database management.
-   **Asynchronous Operations**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and `Flow` for all database and background tasks.
-   **Hardware**: Android's native `NfcAdapter` and `HostApduService` for reading and emulating NFC tags.

## How to Build

You can build a debug APK directly from the command line using the included Gradle wrapper.

1.  **Open a terminal** in the project's root directory.
2.  **Run the build command**:
    