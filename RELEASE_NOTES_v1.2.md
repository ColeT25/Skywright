# Skywright v1.2 — Glide

Quick taps now flap without shooting. Hold a touch to unfold the moth's wings and slow its descent, then release to fire an air burst at the moth's current height. A dashed aim line and wider wings show when you are gliding; the line turns cyan when it crosses a breakable band. Shots have a 0.9-second cooldown and only one burst can be in flight, so extra flaps cannot accidentally cut the next wall.

Walls are spaced farther apart to leave room for aiming and one recovery attempt. The five-minute sunrise, increasing wall speed, narrowing breakable bands, scoring, and local settings remain.

## Install

Download `Skywright-v1.2.apk` from this release on an Android 8.0 or newer phone, then open it. If Android asks, allow installation from the app you used to open the file. The game works offline. This APK uses the same signing key as earlier versions, so it can update an existing installation.

## Verification

- Eleven core simulation tests passed, including quick-tap and hold-release rules, a canceled glide, and a guided five-minute win route.
- Release build and Android lint completed; lint has 0 errors and 4 nonblocking warnings.
- The signed APK passed zip alignment and Android signature verification.
- The signed v1.2 APK installed as an update over signed v1.1 and launched on a Pixel 4 XL Android 11 emulator.
- The menu, glide aiming guide, and a doorway cut by releasing a glide were visually checked on the emulator.
- Physical phone play testing remains to be done.
