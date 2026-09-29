# Skywright v1.4 — Wind and Full-Screen Sky

The moth descends a touch faster while gliding. Its glide gravity increases from 350 to 365 logical units/s² and its fall limit from 185 to 195 units/s. Flapping, aiming, and the five-minute sunrise are unchanged.

After the first 25 seconds, some gaps between walls contain an updraft or downdraft. Colored moving arrows show the wind's direction before the moth reaches it. The wind carries the moth vertically while taps and glides remain available. Gusts become gently more likely toward sunrise (about 6% to 16% per generated gap), cannot occur in consecutive gaps, and reserve at least 350 logical units of recovery space before the next wall. At the highest possible scroll speed, that is at least 1.4 seconds to recover and aim.

The sky and foreground now fill tall portrait screens instead of leaving canvas letterboxing. The HUD moves into the extra top space and respects display-cutout safe insets. The world keeps its horizontal scale and wall lookahead.

## Install

Download `Skywright-v1.4.apk` from this release on an Android 8.0 or newer phone, then open it. If Android asks, allow installation from the app you used to open the file. The game works offline. This release uses the same signing key as earlier versions, so it can install as an update.

SHA-256: `5ed235cabf583ff64843d483cadcd947af42aba983d6438804ea78e3673f42ac`

## Verification

- All 18 unit tests passed. They cover glide tuning, wind direction and recovery spacing, no introductory gusts, the five-minute guided win route with gusts, and screen mapping at tall and short aspect ratios.
- The release build and Android lint completed. Lint found 0 errors and 4 existing warnings.
- The APK passed zip alignment and Android v2/v3 signature checks; its package metadata reports `com.skywright.game`, version code 5, version 1.4, minimum Android 8.0.
- The signed APK installed with `adb install -r` and launched on a Pixel 4 XL Android 11 emulator. The native 1440 × 3040 display was visually checked in menu and gameplay; the sky and foreground fill its tall viewport.
- Physical Samsung Galaxy S26 play testing remains to be done.
