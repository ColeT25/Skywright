# Skywright v1.3 — Glide Momentum

The doorway now opens **centered on the target marker** after a burst hits the glowing breakable band. The burst still travels at the moth's release height, so aiming matters, but the opening no longer shifts away from the target.

The opening pace is slightly slower. The sunrise starts at 110 wall-speed units per second instead of 120 and reaches the same late-run speed. A glide descends more steeply and builds a temporary speed boost the longer you hold it. On release, that boost eases back to the current sunrise speed. The skyline's motion follows the changing wall speed.

## Install

Download `Skywright-v1.3.apk` from this release on an Android 8.0 or newer phone, then open it. If Android asks, allow installation from the app you used to open the file. The game works offline. This APK uses the same signing key as earlier versions, so it can update an existing installation.

## Verification

- Twelve core simulation tests passed, including target-centered openings, the glide speed build and return, and a guided five-minute win route.
- Release build and Android lint completed; lint has 0 errors and 4 nonblocking warnings.
- The signed APK passed zip alignment and Android signature verification.
- The signed v1.3 APK installed as an update over signed v1.2 and launched on a Pixel 4 XL Android 11 emulator.
- The target-centered doorway was visually checked on the emulator. Physical phone play testing remains to be done.
