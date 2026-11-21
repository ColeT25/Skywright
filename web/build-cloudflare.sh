#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
exec bash web/with-build-java.sh bash ./gradlew -p web --no-daemon jsBrowserDistribution
