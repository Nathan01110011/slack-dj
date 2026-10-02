#!/usr/bin/env bash
set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "This launcher is for local macOS playback; use the packaged launcher on Linux." >&2
  exit 1
fi

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

# Gradle finishes before the long-running app starts, so there is no perpetual :run task.
./gradlew --console=plain installDist
export MOPIDY_START_LOCAL=true
exec ./build/install/slack-dj/bin/slack-dj
