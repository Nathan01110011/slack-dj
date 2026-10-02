#!/usr/bin/env bash
set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "setupMopidyMac requires macOS" >&2
  exit 1
fi

command -v brew >/dev/null || { echo "Homebrew is required" >&2; exit 1; }
brew tap mopidy/mopidy
brew trust --formula mopidy/mopidy/mopidy
brew trust --formula mopidy/mopidy/mopidy-spotify
brew install mopidy/mopidy/mopidy-spotify

plugin_dir="$(pwd)/.local/mopidy/plugins"
if [[ -f "$plugin_dir/libgstspotify.dylib" ]]; then
  GST_PLUGIN_PATH="$plugin_dir" gst-inspect-1.0 spotifyaudiosrc >/dev/null && exit 0
fi

brew install rust pkgconf
source_dir="$(pwd)/build/mopidy/gst-plugins-rs"
mkdir -p "$(dirname "$source_dir")" "$plugin_dir"
if [[ ! -d "$source_dir/.git" ]]; then
  git clone --filter=blob:none --no-checkout https://github.com/GStreamer/gst-plugins-rs.git "$source_dir"
fi

source_commit="a18e4843bf472ffd4f84a86c709866d2813a522c"
git -C "$source_dir" fetch --depth 1 origin "$source_commit"
git -C "$source_dir" checkout --detach "$source_commit"
available_kb="$(df -Pk "$source_dir" | awk 'NR == 2 { print $4 }')"
if (( available_kb < 5 * 1024 * 1024 )); then
  echo "Building the Spotify plugin needs at least 5 GB of free disk space" >&2
  exit 1
fi
(cd "$source_dir" && CARGO_INCREMENTAL=0 CARGO_PROFILE_RELEASE_DEBUG=0 cargo build --release --package gst-plugin-spotify)
cp "$source_dir/target/release/libgstspotify.dylib" "$plugin_dir/"
GST_PLUGIN_PATH="$plugin_dir" gst-inspect-1.0 spotifyaudiosrc >/dev/null
echo "Mopidy and the Spotify playback plugin are ready."
