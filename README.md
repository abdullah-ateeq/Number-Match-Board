# Number Match Board 🎯

An offline, turn-based competitive number-matching puzzle game for Android, built with **100% Jetpack Compose** and modern Android architecture.

![Android SDK](https://img.shields.io/badge/API-24%2B-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

---

## 📖 Overview

**Number Match Board** is a strategic board game where you compete turn-by-turn against an intelligent CPU opponent on a $6 \times 6$ grid (36 cells labelled A1 through F6). Your goal is to find and capture pairs of numbers by matching identical numbers or pairing numbers that add up to 10.

The game is completely offline, privacy-first, and features rich animations, haptic feedback, sound effects, custom difficulty levels, detailed statistics, and customizable accessibility settings.

---

## ✨ Features

- 🎮 **Turn-Based CPU Mode**: Play head-to-head against a turn-based AI opponent.
- 🧠 **3 Difficulty Levels**:
  - **Easy**: CPU makes mistakes ($30\%$ mistake rate) and picks pairs randomly.
  - **Normal**: CPU plays naturally with occasional random picks ($8\%$ mistake rate).
  - **Hard**: CPU uses a strategy evaluating future board combinations and number frequencies.
- 🔢 **Pair Matching System**:
  - **Identical Pair** (e.g., $5 + 5$, $7 + 7$): Worth **12 points** (+2 bonus).
  - **Sum to 10 Pair** (e.g., $3 + 7$, $4 + 6$, $1 + 9$): Worth **11 points** (+1 bonus).
- 📊 **Detailed Game Statistics**: Tracks games played, wins, losses, draws, current & best win streaks, best score, total pairs, and match breakdowns.
- 🎨 **Modern Material 3 Design**:
  - Dynamic Color support (Android 12+).
  - System, Light, and Dark themes.
  - Smooth Compose transition animations.
- 🔊 **Rich Sensory Feedback**: Custom sound effects and haptic feedback on pair captures, invalid selections, and game over.
- ♿ **Accessibility & Usability**: Full TalkBack support with semantic coordinate labels (A1–F6) and high-contrast visuals.
- 🔒 **100% Offline & Private**: Zero permissions required. No tracking, ads, or network usage.

---

## 🛠️ Architecture & Tech Stack

The project follows modern Android development best practices and Clean Architecture principles:

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3 design system)
- **Navigation**: Jetpack Navigation Compose
- **Architecture Pattern**: MVVM (Model-View-ViewModel) + Unidirectional Data Flow (UDF)
- **State Management**: Kotlin Coroutines & `StateFlow`
- **Persistence**: `SharedPreferences` via `KeyValueStore` pattern
- **Testing**:
  - Unit Tests for Game Engine, Pair Rules, CPU AI Strategy, Repositories, and ViewModel using `JUnit` and `kotlinx-coroutines-test`.
  - UI & Instrumentation Tests with Compose UI Test library.

### Directory Structure

```text
com.example.numbermatchboard/
├── data/              # Repositories & KeyValueStore for settings & statistics
├── game/
│   ├── ai/            # CPU strategy heuristics (Easy, Normal, Hard)
│   ├── logic/         # Board generator, pair rules engine & game rules
│   └── model/         # Data classes (GameState, Cell, Coordinate, PairType)
├── navigation/        # Compose NavHost & navigation routes
├── ui/
│   ├── components/    # Reusable Compose UI widgets (BoardCell, NumberBoard, GameInfo, etc.)
│   ├── screens/       # GameScreen, HomeScreen, SettingsScreen, AboutScreen, ResultScreen
│   └── theme/         # Color palettes, Typography, and Material3 Theme definition
└── util/              # Accessibility helpers and GameFeedback (Sound & Haptics)
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug / ME2 or newer
- **JDK**: Version 11 or higher
- **Android SDK**: Compile SDK 37, Minimum SDK 24 (Android 7.0+)

### Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/abdullah-ateeq/Number-Match-Board.git
   cd Number-Match-Board
   ```

2. **Open in Android Studio**:
   - Open Android Studio and choose **Open an Existing Project**.
   - Select the `Number-Match-Board` directory.

3. **Build & Run**:
   - Select the `app` run configuration.
   - Run on an emulator or physical device running Android 7.0 (API 24) or higher.

4. **Run Unit Tests**:
   ```bash
   ./gradlew test
   ```

---

## 📜 Game Rules

1. The game board contains **36 numbers** arranged in 6 rows (A to F) and 6 columns (1 to 6).
2. On your turn, select two active cells on the board:
   - **Identical Numbers**: Both cells have the same value (e.g., $4$ and $4$).
   - **Sum to 10**: The values of both cells add up to 10 (e.g., $3$ and $7$).
3. If a valid pair is selected, you capture the pair and **keep your turn**.
4. If an invalid pair is selected, your turn ends and passes to the CPU.
5. The game ends when all pairs on the board are captured. The side with the most captured pairs wins!

---

## 📄 License

This project is open-source under the [MIT License](LICENSE).
