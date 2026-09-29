# Skywright — Android game plan

## The pitch

**Skywright** is a one-thumb, portrait arcade game. A small moth flies through an endless city of sealed paper walls. Tapping flaps its wings, as in *Flappy Bird*, but each flap also sends a visible sound pulse forward. The first pulse to reach a wall cuts a doorway at the moth's height **when the player tapped**. The player must then fly through the doorway they chose a moment earlier.

The twist changes the central question from “Can I reach this preset gap?” to “Where should I put the next gap, and can I meet it?” It uses the same immediate, readable tap-to-rise and gravity-to-fall control, while making every timely flap a small act of level creation. There are no weapons, upgrades, or second control gesture.

## What a run feels like

1. A sealed wall enters from the right. Its center seam and an empty doorway outline show that it can be cut.
2. The player taps. The moth jumps upward; a ring travels toward the earliest visible, uncut wall. A thin horizontal guide shows the height captured at the instant of the tap.
3. When the ring reaches that wall, the paper peels open into a gap centered on the guide. The gap stays put until the wall leaves the screen.
4. The player keeps flapping or gliding to reach the opening. Further taps can cut later visible walls, but cannot move an opening already made.
5. Passing a wall earns one point. Hitting paper, the ceiling, or the floor ends the run. A single tap restarts after the result screen.

The early game teaches “tap to place, then fly to meet it.” Later walls move faster and arrive closer together, so the player begins placing the next opening while navigating the current one. A missed pulse is visible before it becomes a collision; failure should feel attributable to a decision, not to hidden geometry.

## Rules and tuning targets

The numbers below are starting values for a **360 × 720 logical portrait playfield**, not promises about final difficulty. Scale coordinates uniformly to the phone's drawable area and keep collision rules in logical units.

| System | Initial target |
| --- | --- |
| Moth | Fixed horizontal center at `x = 96`; circular collision radius `12` |
| Flight | Gravity `850 units/s²`; each tap sets upward velocity to `-280 units/s`; downward speed capped at `430 units/s` |
| Walls | Width `44`; opening height `160` for the first three walls, then `148`; opening center clamped so the full opening fits between ceiling and floor |
| Scroll | `120 units/s` initially; increase by `8` every five cleared walls, capped at `184` |
| Spacing | At least `300` logical units between wall leading edges; tune with play tests to preserve a reachable route |
| Pulse | Travels right at `520 units/s` relative to the screen; collides with a wall moving left, so a last-second tap may arrive too late |
| Target | Earliest **visible, uncut** wall ahead of the moth; each wall accepts one pulse. Taps with no eligible target still flap normally |
| Score | `+1` after the wall's trailing edge passes the moth, once per wall |

Store the sampled opening height on the pulse, not the moth's later position. A pulse arriving after the wall's leading edge reaches the moth cannot save the player. A wall with no opening remains solid. Walls cannot be cut twice. The first wall should arrive with enough time for the player to see, cut, and reach an opening; use a short three-wall introduction at the slower speed.

**Fairness gate for procedural generation:** use hand-tuned spacing and speed patterns in the first playable build. Before adding random patterns, simulate candidate spacing from representative legal flight states and reject patterns that offer no reachable opening height or too little time for a pulse to arrive. The player, rather than the generator, supplies each opening's height.

## Look and sound

- High-contrast paper silhouettes against a dusk gradient. The moth is a bright, simple shape with two wing poses. Obstacles are recognizable even with sound muted.
- A pulse is a clean expanding ring and a traveling line, followed by a quick paper-fold animation when its opening appears. The opening does not flash or conceal the collision edge.
- A soft wing note on tap, a rising chime when a wall opens, and a small percussion hit on score. All sounds are optional; sound and vibration toggles persist locally.
- Clear score at the top, current best on the result screen, and a short “Tap to flap and cut a door” tutorial on first launch. The first wall demonstrates the guide line before the player can fail.

## Android implementation

Build a small native Kotlin Android app with no account, network access, ads, or asset downloads. Use a single Activity and a custom View with Canvas drawing for the menu, gameplay, and result overlay. Keep simulation separate from Android drawing so the physics and collision logic can be tested without a device. Use Android's frame callbacks to pace rendering and a fixed `1/60 s` simulation step; cap catch-up after a pause so returning to the app never skips through a wall. Pause automatically when the Activity loses focus.

Suggested project shape:

```text
Skywright/
  app/src/main/java/.../MainActivity.kt
  app/src/main/java/.../SkywrightView.kt
  app/src/main/java/.../GameCore.kt       # state, fixed-step update, scoring
  app/src/main/java/.../WallSpawner.kt    # spacing and reachable patterns
  app/src/main/java/.../Renderer.kt       # Canvas art and feedback
  app/src/main/java/.../Audio.kt          # short effects and mute setting
  app/src/test/java/.../GameCoreTest.kt
  app/src/main/AndroidManifest.xml
  README.md
```

`GameCore` owns the moth position and velocity, walls, in-flight pulses, score, and run state (`ready`, `playing`, `paused`, `game over`). Touch input is an event queued for the next simulation step; a tap sets upward velocity and spawns at most one pulse. The pulse records its target wall ID and sampled Y coordinate. The game update moves moth, walls, and pulses; resolves pulse-wall arrivals; checks circle-versus-wall collision against the open gap; then awards points and removes offscreen objects. Draw from the resulting state. Keep input, simulation, and rendering on clearly defined threads or synchronize snapshots, so touch events cannot alter a wall halfway through a physics step.

Persist only best score and settings with local preferences. Use original primitive/vector artwork and generated short sound effects, with no third-party game assets. Support common portrait phone aspect ratios, display cutouts, app background/return, and both 60 Hz and high-refresh screens. The game remains playable offline.

## Build sequence and acceptance checks

1. **Prototype the mechanic:** create the Activity, game loop, one wall, flap physics, pulse travel, and cut-on-arrival behavior. Render the tap-height guide and collision outline immediately.
2. **Make a full run:** add wall spawning, score, game-over/restart, introduction walls, and persistent best score. Tune difficulty on a real phone; adjust numbers based on recorded failures rather than increasing randomness.
3. **Finish the presentation:** paper-fold animation, original shapes, sound, mute and vibration settings, pause handling, and a short tutorial.
4. **Verify the rules:** unit-test pulse height sampling, one-cut-per-wall, late-pulse failure, collision boundaries, score counted once, and pause/resume timing. Run a long automated simulation with varied tap timings to catch impossible wall sequences and state bugs.
5. **Verify the device build:** build and install the APK on an Android phone or emulator; play several full runs with sound on/off, background and resume, restart, and screen-size checks. Record the tested device and Android version. A successful build alone does not count as device verification.

### Final deliverable: directly installable APK

The completed game should be handed over as a **single signed APK**, for example `Skywright-v1.0.apk`, plus the source project and a short install note. Produce a release build, sign it with a project-specific key kept out of version control, and verify its signature and package metadata. Keep that signing key securely so later APK updates can install over the first version. If a release key is not yet available during development, use an automatically signed debug APK for early phone tests, then produce the signed release APK for delivery.

Before handoff, confirm the APK exists, its checksum is recorded, and it installs and launches on a device. Provide the exact APK path and simple sideload steps (transfer it to the phone, open it, and allow installation from that source if Android prompts). The APK, rather than an Android App Bundle or a Play Store listing, is the required end product.

## Scope for version 1

One endless mode, one-thumb controls, score and best score, local settings, a brief tutorial, and a signed APK. Additional modes, cosmetics, online rankings, purchases, and procedural themes can be considered after the central flap-and-cut loop is fun and the first APK is working.

## Version 1.1 addendum: Dawn

The original endless scoring loop became a five-minute survival ascent. A visible breakable band turns each wall into an aiming challenge. A tap samples the moth's height and sends a pulse toward the earliest closed wall; the wall opens only when the sampled height lies inside its band. A miss gives immediate red feedback and permits another attempt. As the sun rises, new bands and doorways narrow and wall speed increases. Existing walls keep their spawned geometry. Survive one continuous run until the sun reaches its highest point to win; score remains an additional measure of how far you flew.

## Version 1.2 addendum: Glide and release

Quick taps still flap. Every touch starts with a flap; after 0.18 seconds of holding, the moth unfolds its wings and glides with lower gravity and a slower fall limit. A live dashed guide tracks the moth's height and changes color when it lines up with the nearest closed wall's breakable band. Releasing after a glide fires one horizontal burst at the moth's release height. A quick tap never fires. Shots have a 0.9-second cooldown, and only one burst can be in flight. A hold can still be used for movement when no shot is available. Walls are spaced farther apart to give players time to aim and recover as the sunrise accelerates the game. Future obstacle types can make the choice between flapping and gliding more varied without changing these controls.

## Version 1.3 addendum: Targeted openings and glide momentum

A successful burst still has to cross the glowing breakable band, but the doorway now opens at the wall's marked center instead of at the burst's impact height. This makes the marker a reliable preview of the opening location. The sunrise's starting wall speed is reduced from 120 to 110 logical units per second; it still reaches 210 by the five-minute goal. Gliding has a steeper fall and gradually adds up to 40 units per second of temporary wall speed. The extra speed eases away over roughly 0.4 seconds after the glide ends, returning to the current sunrise pace. Background parallax follows the actual distance traveled.

## Version 1.4 addendum: Responsive sky and wind lanes

The glide falls slightly faster: glide gravity rises from 350 to 365 logical units/s² and its fall limit from 185 to 195 units/s. The 360 × 720 physics field, wall scale, and horizontal lookahead stay fixed. On taller phones, the renderer extends the sky and foreground into the extra vertical area instead of leaving letterbox strips, moves the HUD toward the top edge, and respects the display-cutout safe inset. Touches use the same screen transform as the artwork.

After 25 seconds, a wall gap can contain a wind lane. Its full-height translucent ribbon and moving arrows indicate an updraft or downdraft before the moth reaches it. Inside the 112-unit lane, wind adds a steady vertical drift of 205 units/s; flapping and gliding still work normally. Gust chance starts at about 6% per generated gap and approaches 16% by sunrise, with no consecutive gust gaps. A gust gap is at least 600 units wide, leaving at least 350 units between the wind lane and its following wall—at least 1.4 seconds at the maximum wall speed of 250 units/s. The lane begins after the prior wall has passed, so the player gets a distinct recovery and aiming interval.

## Android documentation used for the implementation path

- [Build and install APKs from the command line](https://developer.android.com/build/building-cmdline) — debug APKs, release signing, and device installation.
- [Android game-loop rendering](https://developer.android.com/games/develop/gameloops) — frame pacing considerations.
- [APK signature verification with apksigner](https://developer.android.com/tools/apksigner) — checking the signed deliverable.
