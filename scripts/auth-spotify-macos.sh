#!/usr/bin/env bash
set -euo pipefail
umask 077

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "setupSpotifyPlaybackAuthMac requires macOS" >&2
  exit 1
fi

if lsof -nP -iTCP:6680 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Stop ./gradlew runLocal before setting up Spotify playback authentication." >&2
  exit 1
fi

command -v cargo >/dev/null || { echo "Rust/Cargo is required; run ./gradlew setupMopidyMac first" >&2; exit 1; }

helper="$HOME/.cargo/bin/get_creds"
if [[ ! -x "$helper" ]]; then
  cargo install --git https://github.com/librespot-org/librespot \
    --no-default-features --features rustls-tls-native-roots --example get_creds
fi

credentials_dir="$HOME/Library/Application Support/mopidy/spotify/credentials-cache"
mkdir -p "$credentials_dir"
chmod 700 "$credentials_dir"

if [[ -f "$credentials_dir/credentials.json" ]]; then
  backup="$credentials_dir/credentials.json.backup.$(date +%Y%m%d%H%M%S)"
  cp "$credentials_dir/credentials.json" "$backup"
  chmod 600 "$backup"
  echo "Backed up the previous Spotify playback credentials."
fi

echo "Complete the Spotify sign-in shown by the librespot helper."
"$helper" "$credentials_dir"
test -s "$credentials_dir/credentials.json" || { echo "No Spotify playback credentials were created" >&2; exit 1; }
chmod 600 "$credentials_dir/credentials.json"
echo "Spotify playback credentials are ready. Restart ./gradlew runLocal and try the track again."
