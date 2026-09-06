# LooLoo screenshot automation (v0.26)

Captures real production Compose screens on an Android device. It does not generate mockups, upload images, or build an MP4. Everything is local. Production/release code has no dependency on this tooling.

## Prerequisites

- The existing Android SDK/JDK/Gradle setup, Python 3.9+, Bash, and a host-compatible `adb`.
- USB debugging authorized on an unlocked Android device, or a running emulator.
- Installed local TTS voices and a working audio engine are recommended for music/story states.
- `ADB=/absolute/path/to/adb` overrides SDK discovery. An x86 platform-tools binary will not run natively on an ARM Linux host; use platform-tools appropriate for the host. The script fails clearly instead of claiming capture succeeded.

Connect the tablet or Galaxy S22 and run `adb devices -l`. Use its serial explicitly when more than one physical device is connected. Serials are not tied to profiles or assumed ownership. A single authorized physical device is selected automatically; a lone emulator is selected only when no physical device exists. Offline/unauthorized devices and ambiguity fail safely.

Unlock the device and clear personal notifications first. Capture uses the full display, including real system bars, without opening the notification shade or modifying brightness/settings. The dedicated debug-only activity requests portrait. Review all images before publishing; the manifest contains device serials and should not be published unedited.

## Commands

```bash
# Inspect all 65 entries without ADB
./scripts/capture_looloo_screenshots.sh --list

# Build/install debug app AND instrumentation, then capture tablet inventory
./scripts/capture_looloo_screenshots.sh --serial TABLET_SERIAL --profile tablet --install

# Galaxy S22; reuse previously installed matching APKs
./scripts/capture_looloo_screenshots.sh --serial PHONE_SERIAL --profile phone --no-install

# Emulator
./scripts/capture_looloo_screenshots.sh --serial emulator-5554 --profile emulator --install

# One category, alternate output
./scripts/capture_looloo_screenshots.sh --serial TABLET_SERIAL --profile tablet --category stories --output build/story-captures

# Remove only known inventory PNGs in this profile, then recapture
./scripts/capture_looloo_screenshots.sh --serial TABLET_SERIAL --profile tablet --clean

# Optional Apps/OS app-settings images (review installed-app information)
./scripts/capture_looloo_screenshots.sh --serial TABLET_SERIAL --profile tablet --category home --include-utilities
```

Default category is `all`; categories are `home`, `games`, `learn`, `music`, `stories`. `--no-install` is the default. Use `--install` after code changes. No dependency versions are changed. App data is not cleared. `--clean` removes only known inventory filenames, not arbitrary output contents. Selected PNGs are removed before each new capture to prevent stale images masquerading as new results.

## Inventory and implementation

The canonical inventory is `app/src/androidTest/assets/looloo_screenshots.json`: exactly 65 stable IDs, filenames, titles, categories, and suggested durations. All required groups are represented: Home, Games, Memory, Coloring, Speak & Spell, Animals, Counting, Math, ABC, Shapes, Puzzles, Piano, Drums, Billiards, Story Time, Tic-Tac-Toe, Apps, and Settings.

`CaptureActivity` exists only in `src/debug`, is not exported, and hosts the normal theme and real production screen composables supplied by instrumentation. It has no release counterpart. `CaptureFixtures` lives entirely in `androidTest`. It builds deterministic states with `Random(26)` and existing pure engines. Completion states use the engines' normal transitions. Puzzle progress uses valid placed-piece/tray state; all restoration payloads use existing production codecs/Savers. The first saveable slots of each screen are an explicit test contract, and both saver round trips and consumed/restored slots are checked. A changed contract fails capture rather than silently producing a different state. Private screen Savers are accessed reflectively only in the unminified test target; production code is not changed to expose screenshot hooks.

These are staged showcase states rendered by the actual app UI, not evidence that an end-to-end user solved a game. Home pickers are opened with real semantic clicks. Individual game screens are staged directly to avoid slow or flaky multi-game play-throughs. Navigation callbacks on isolated fixture screens are inert; their rendered UI is the production screen. The root Home/Games fixtures use normal app navigation. `home_story_entry` is the Story Library reached from the Stories action and is intentionally visually equivalent to `story_library`.

The Compose test clock freezes delayed transitions. Layout/semantics waits and measure/draw synchronization replace arbitrary sleeps. The drum demo advances virtual time into its first hit; LooLoo's turn is held before its launch timer. Billiards aiming uses the actual cue ball's semantic bounds and a held slingshot gesture. Read to Me resumes page one using the production Resume action. Story content and billiards physics are untouched. Audio initialization is awaited; device-specific voice/audio failures may prevent a requested transient state and are reported as failures.

`games_main_lower` deliberately captures the scrolled lower viewport (it may overlap the first viewport on an unusually large display). Apps and Settings are all-65 inventory members but **skipped by default** for privacy. Opt-in Settings captures the OS's real app-details screen, which varies by Android/OEM; it is not a fake in-app settings page. No keys, network settings, development menus, or logs are added to screenshot UI. Hardware status bar content must still be reviewed.

## Outputs

```text
build/looloo-screenshots/
  tablet/01_home_main.png ...
  phone/...
  emulator/...
  manifest.json
  showcase_order.txt
  showcase_manifest.json
  instrumentation.log
```

`build/` is already ignored by Git. Do not add generated PNGs unless explicitly requested. A custom output outside `build/` is the caller's responsibility.

Manifest schema version 1 has an `entries` array. Each selected inventory entry includes `filename` (profile-relative), `screen_id`, `id`, `title`, `category`, `status`, device serial/model, orientation, app version/label when available, timestamp, duration, and notes. Status is `captured`, `skipped`, `failed`, or `blocked` (ADB/device preflight failed). **Only `captured` entries have newly exported PNGs.** Partial results survive failures. The script exits nonzero for failed captures or tooling errors. The manifest describes the latest invocation/category; use separate `--output` directories to retain manifests from different devices/runs.

Showcase order contains only successfully captured paths in inventory order. Showcase JSON adds title, duration, and a gentle-fade suggestion. This is ready as input to a future local stitching workflow, but no ffmpeg dependency, video generation, README edit, upload, or publication occurs here.

## Validation and troubleshooting

```bash
python3 -m unittest discover -s scripts/tests
bash -n scripts/capture_looloo_screenshots.sh
./scripts/check_screenshot_fixtures.sh
./gradlew :app:compileDebugKotlin
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew assembleDebugAndroidTest
git diff --check
```

- No executable ADB: set `ADB` to a native binary; inspect host CPU architecture.
- Multiple devices: pass `--serial`; the driver never guesses ownership.
- Unauthorized/offline: unlock, authorize USB debugging, and reconnect.
- Missing runner: use `--install` to install both APKs.
- Saved-state contract or semantic failure: inspect local `instrumentation.log`, update only the test fixture contract, and recapture. Do not substitute a different screen or fabricate a PNG.
- Audio loading never finishes: verify the local audio/TTS setup on that device. The timeout fails instead of taking a loading screen as a successful music state.
- Rotated output: verify the device permits portrait; landscape PNGs are rejected.
- Some captures missing: consult each manifest status; successful captures remain usable.
- Raw instrumentation logs can contain local diagnostic paths; keep logs and device metadata private.
- The local fixture checker constructs all 65 entries on the host JVM and checks production saver/codec round trips. It compiles instrumentation first and uses Java source-file launching with the existing JDK. Its ignored `fixture-classpath.txt` contains local build paths; do not publish it. This test does not render screens or simulate a device.
- The capture activity's portrait-only lint exceptions are scoped to the debug manifest, not production. Android versions that ignore orientation locks are handled by rejecting landscape images.
- The active window must belong to the app (or Settings for its opt-in entry); permission dialogs, a locked device, or other foreground apps fail capture instead of being mislabeled.

Successful host tests, compilation, or APK assembly do not prove hardware screenshots succeeded. A real device run and inspection are required before using the images for README/LinkedIn or regression baselines. Across devices, fonts, emoji, bars, TTS, and installed apps can legitimately differ.
