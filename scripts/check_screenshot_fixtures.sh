#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_DIR"
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew -I scripts/screenshot_classpath.gradle :app:writeCaptureCheckClasspath --no-configuration-cache
CAPTURE_CLASSPATH="$(cat build/looloo-screenshots/fixture-classpath.txt):app/build/intermediates/built_in_kotlinc/debugAndroidTest/compileDebugAndroidTestKotlin/classes"
java -cp "$CAPTURE_CLASSPATH" scripts/CaptureFixtureCheck.java app/src/androidTest/assets/looloo_screenshots.json
