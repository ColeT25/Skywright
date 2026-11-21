# Skywright

Skywright is a browser arcade game about a moth flying through sealed paper walls. Tap, click, or press Space to flap. Hold to glide and aim, then release an air burst through a glowing band.

## Run locally

From the repository root, use Java 21, Node.js 24 with npm, and Python 3.

```sh
bash ./gradlew -p web jsNodeTest jsBrowserDistribution
python3 -m http.server 8000 --bind 127.0.0.1 --directory web/build/site
```

Open `http://localhost:8000/`. Press Escape or use the pause button to pause. The browser saves your best score and settings. On HTTPS or localhost, the game can be played offline after the service worker caches it.

## How to play

Choose **Tutorial** from the start menu or the loss screen to practice flapping, gliding, and clearing a wall.

- **Levels** starts with First Flight, followed by Paper Maze and Crosswinds. Clear the last wall to unlock the next level. Retrying keeps the same layout.
- **Endless** continues through procedural walls until you crash. Difficulty increases for the first five minutes of active play, then stays at the sunrise pace. Each wall you clear counts toward your Endless best.
- A quick tap flaps upward without firing. Gravity pulls you back down.
- Keep a finger down for about a fifth of a second to unfold the moth's wings and glide. The glide slopes downward and gradually speeds up the passing walls. Releasing eases the walls back to the current pace. A dashed line shows where a burst would go and turns cyan when aligned with a glowing band.
- Release a glide to fire at the moth's height **at release**. The burst targets the nearest visible closed wall. A miss flashes red; a hit cuts a fixed doorway **centered on that wall's target marker**. Only one burst can be in flight, and the gust takes 0.9 seconds to recharge.
- Releasing when no closed wall is visible or the gust is recharging simply ends the glide. The charge bar shows when another burst is ready.
- Fly through doorways to score. In Endless, newly generated walls get narrower openings and move faster as dawn approaches.
- Marked updrafts and downdrafts push the moth in the direction of their arrows. Crosswinds places them at fixed points. In Endless, wind can spawn after the first 25 seconds and becomes more common toward sunrise.
- On tall phones, the sky and foreground extend to the edges while the moth, walls, and controls keep their familiar scale. The HUD moves into the extra top space while avoiding display cutouts.
- A collision ends the current run. Tap the pause icon in the top-right corner to pause. Level unlocks, the Endless best score, and settings stay on the device.

## Quality checks

GitHub Actions runs the checks on pull requests and pushes to `main`. To run the same checks locally:

```sh
npm --prefix web ci
npm --prefix web run format:check
bash ./gradlew -p web --warning-mode fail spotlessCheck jsBrowserDistribution
npm --prefix web run test:coverage
npm --prefix web exec -- playwright install chromium
npm --prefix web run test:browser
```

Use `bash ./gradlew -p web spotlessApply` to format Kotlin and `npm --prefix web run format` to format the handwritten web files. Kotlin compilation enables progressive mode and treats warnings as errors.

The Node tests must cover at least 95% of lines in each of `GameCore.kt` and `ScreenLayout.kt`. Coverage runs disable incremental Kotlin/JS linking so the source maps include unchanged Kotlin files. The check verifies that execution maps back to both files and writes the report to `web/build/coverage/report/index.html`.

Browser tests cover keyboard and touch controls, clearing a wall and restarting, canceling a held shot by pausing, saving the sound setting, and reloading offline.

## Deploy

Configure Cloudflare Git builds from the repository root with `bash web/build-cloudflare.sh`. It verifies the pinned Eclipse Temurin JDK archive and builds to `web/build/site`. For a Worker, point Wrangler's `--assets` flag at that directory. For Pages, use it as the build output directory. [Cloudflare Workers static assets](https://developers.cloudflare.com/workers/static-assets/) · [Cloudflare Pages Git integration](https://developers.cloudflare.com/pages/get-started/git-integration/)

## Project structure

- `web/src/jsMain/kotlin/` contains the game simulation, layout, renderer, audio, and browser input.
- `web/src/jsMain/resources/` contains the static page, icons, and offline service worker.
- `web/src/jsTest/kotlin/` contains the Kotlin game and layout tests.
- `web/tests/` contains the Playwright browser tests.

## Archived Android releases

The current project builds for browsers. The last Android APK is [Skywright v1.4](https://github.com/ColeT25/Skywright/releases/tag/v1.4). Earlier APKs are in the [release archive](https://github.com/ColeT25/Skywright/releases).
