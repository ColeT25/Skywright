# Skywright

**Make the gap. Then make it through.** Skywright is a one-thumb Android arcade game about a moth flying through sealed paper walls. Every tap flaps upward and sends a pulse toward the next visible wall. Aim at the glowing band: a pulse can cut an opening only through that part of the wall. Survive until sunrise to win.

![Skywright menu](docs/menu.png)

## Download and install

Download the signed [Skywright v1.1 APK](https://github.com/ColeT25/Skywright/releases/download/v1.1/Skywright-v1.1.apk) on an Android phone running Android 8.0 or newer. Open the downloaded file and allow installation from that source if Android asks. The APK installs directly; it does not require a Play Store account or an internet connection to play. Version 1.1 uses the same signing key as v1.0, so it can install as an update.

SHA-256: `c651f7937dd30a9921bb6eae827b38ce8f395cca848e13df59ad0d74e81f3bf2`

## How to play

- Tap anywhere to flap. Gravity pulls you back down.
- Your flap sends a pulse to the earliest visible closed wall. It samples the moth's height when you tap. The pulse cuts a doorway only if that height falls inside the wall's glowing band when the pulse arrives.
- A red flash means the pulse missed the breakable band. You can try that wall again while there is time. After a successful cut, the doorway stays fixed; later taps cannot move it.
- Fly through doorways to score. Breakable bands and doorways narrow as dawn approaches, while the walls move faster. Each wall keeps the width it had when it appeared.
- Survive five minutes of active play to raise the sun fully and win. A collision ends the current run and restarts the sunrise on the next attempt.
- Tap the pause icon in the top-right corner during a run. Sound and vibration switches are on the menu, pause, and result screens. Best score and settings stay on the device.

![A pulse-cut doorway during a run](docs/gameplay.png)

## Build from source

This is a native Kotlin Android project with original Canvas artwork, a fixed-step simulation, and no external game engine or runtime permissions. The Gradle wrapper is included. With Android SDK Platform 36, Build Tools 36.0.0, and Java 17 installed:

```sh
bash ./gradlew testDebugUnitTest lintRelease assembleDebug
```

The signed release APK was built locally with a project-specific signing key. The key is intentionally outside this repository. See [GAME_PLAN.md](GAME_PLAN.md) for the original game concept and [RELEASE_NOTES_v1.1.md](RELEASE_NOTES_v1.1.md) for the sunrise update.

## Verification for v1.1

- Core simulation tests cover targeting, misses and retries, collision, scoring, pause behavior, and a guided five-minute win path.
- Android build, lint, APK alignment, signature, and emulator update checks are recorded in the [v1.1 release notes](RELEASE_NOTES_v1.1.md). Physical phone play testing remains to be done.

## Project structure

- `GameCore.kt`: Android-free physics, pulse targeting, wall state, collision, and scoring.
- `SkywrightView.kt`: touch input, fixed-step frame pacing, lifecycle pause, and settings.
- `Renderer.kt`: original paper-sky visuals drawn with Canvas.
- `GameAudio.kt`: local synthesized feedback.
