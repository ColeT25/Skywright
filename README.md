# Skywright

**Make the gap. Then make it through.** Skywright is a one-thumb Android arcade game about a moth flying through sealed paper walls. Tap to flap, hold to glide and aim, then release to send an air burst through a wall's glowing breakable band. Survive until sunrise to win.

![Skywright menu on a tall Android screen](docs/responsive-menu-v1.4.png)

## Download and install

Download the signed [Skywright v1.4 APK](https://github.com/ColeT25/Skywright/releases/download/v1.4/Skywright-v1.4.apk) on an Android phone running Android 8.0 or newer. Open the downloaded file and allow installation from that source if Android asks. The APK installs directly; it does not require a Play Store account or an internet connection to play. Version 1.4 uses the same signing key as earlier releases, so it can install as an update.

SHA-256: `5ed235cabf583ff64843d483cadcd947af42aba983d6438804ea78e3673f42ac`

## How to play

- A quick tap flaps upward without firing. Gravity pulls you back down.
- Keep a finger down for about a fifth of a second to unfold the moth's wings and glide. The glide slopes downward and gradually speeds up the passing walls; releasing eases the walls back to the current sunrise pace. A dashed line shows where a burst would go and turns cyan when aligned with a glowing band.
- Release a glide to fire at the moth's height **at release**. The burst targets the nearest visible closed wall. A miss flashes red; a hit cuts a fixed doorway **centered on that wall's target marker**. Only one burst can be in flight, and the gust takes 0.9 seconds to recharge.
- Releasing when no closed wall is visible or the gust is recharging simply ends the glide. The charge bar below the sunrise meter shows when another burst is ready.
- Fly through doorways to score. Breakable bands and doorways narrow as dawn approaches, while the walls move faster. Each wall keeps the width it had when it appeared.
- After the opening stretch, marked updrafts and downdrafts may appear between walls. Their arrows show which way the wind will carry the moth. Gusts become slightly more common toward sunrise, never appear in consecutive wall gaps, and leave extra room to recover before the following wall.
- On tall phones, the sky and foreground extend to the edges while the moth, walls, and controls keep their familiar scale. The HUD moves into the extra top space while avoiding display cutouts.
- Survive five minutes of active play to raise the sun fully and win. A collision ends the current run and restarts the sunrise on the next attempt.
- Tap the pause icon in the top-right corner during a run. Sound and vibration switches are on the menu, pause, and result screens. Best score and settings stay on the device.

![Skywright gameplay on a tall Android screen](docs/responsive-gameplay-v1.4.png)

## Build from source

This is a native Kotlin Android project with original Canvas artwork, a fixed-step simulation, and no external game engine or runtime permissions. The Gradle wrapper is included. With Android SDK Platform 36, Build Tools 36.0.0, and Java 17 installed:

```sh
bash ./gradlew testDebugUnitTest lintRelease assembleDebug
```

The signed release APK was built locally with a project-specific signing key. The key is intentionally outside this repository. See [GAME_PLAN.md](GAME_PLAN.md) for the original game concept and [RELEASE_NOTES_v1.4.md](RELEASE_NOTES_v1.4.md) for the latest tuning.

## Verification for v1.4

- Core simulation tests cover target-centered openings, glide speed, wind direction and spacing, shooting, collision, pause cancellation, a guided five-minute route that encounters gusts, and screen mapping at tall and short aspect ratios.
- Android build, lint, APK alignment, signature, and emulator checks are recorded in the [v1.4 release notes](RELEASE_NOTES_v1.4.md). Physical phone play testing remains to be done.

## Project structure

- `GameCore.kt`: Android-free physics, pulse targeting, wind gusts, wall state, collision, and scoring.
- `SkywrightView.kt`: touch input, fixed-step frame pacing, lifecycle pause, and settings.
- `ScreenLayout.kt`: responsive canvas and touch coordinate mapping.
- `Renderer.kt`: original paper-sky visuals drawn with Canvas.
- `GameAudio.kt`: local synthesized feedback.
