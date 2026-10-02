# Slack DJ

A Kotlin bot that controls a local [Mopidy](https://mopidy.com/) server from a Slack channel. It uses [Bolt for Java](https://docs.slack.dev/tools/java-slack-sdk/) in Socket Mode and Mopidy's HTTP JSON-RPC API.

## Setup

Install Java 21. For local macOS playback, this project can install and start Mopidy alongside the bot (see below). For another host, install Mopidy and its Spotify extension there and set `MOPIDY_RPC_URL` to that host's HTTP RPC endpoint.

Create a Slack app with Socket Mode enabled. Give its app-level token the `connections:write` scope. Add the `chat:write` and `commands` bot scopes, create `/song` and `/artist` under Slash Commands, enable **Interactivity & Shortcuts** for the confirmation buttons, and invite the bot to the channel. Socket Mode does not need public Request URLs. Reinstall the app after adding the `commands` scope. The older mention commands remain available if you keep `app_mentions:read` and the `app_mention` bot event subscription (or `channels:history` with `message.channels`).

Fill in the gitignored `.localenv` file in the project root, or set these environment variables directly. Environment variables take precedence over `.localenv`; see `.localenv.example` for a shareable template.

| Variable | Purpose |
| --- | --- |
| `SLACK_BOT_TOKEN` | Bot token (`xoxb-`); `SLACK_TOKEN` also works for migration |
| `SLACK_APP_TOKEN` | Socket Mode app-level token (`xapp-`) |
| `SLACK_CHANNEL` | Channel ID where commands and replies belong |
| `SLACK_DJ_ID` | Optional bot user ID for reference; the app obtains the correct ID from Slack at startup |
| `BAN_USER` | Optional comma or space separated list of blocked user IDs |
| `MOPIDY_RPC_URL` | Optional Mopidy RPC URL |
| `MOPIDY_SPOTIFY_CLIENT_ID` | Spotify client ID from [Mopidy authentication](https://mopidy.com/ext/spotify/#authentication), required for `runLocal` |
| `MOPIDY_SPOTIFY_CLIENT_SECRET` | Matching Spotify client secret, required for `runLocal` |
| `MOPIDY_AUDIO_OUTPUT` | Optional GStreamer audio output; defaults to `autoaudiosink` |

The Slack tokens and Spotify client secret are secrets. Keep `.localenv` private. `runLocal` writes a temporary Mopidy config with the Spotify values, restricts it to the current user, and removes it at shutdown.

### Local macOS playback

Run `./gradlew setupMopidyMac` once. It installs Mopidy and Mopidy-Spotify with Homebrew and builds the GStreamer Spotify playback plugin in `.local/mopidy/plugins`. The setup may take a while because it compiles native dependencies. Then fill in the Spotify fields in `.localenv` and run `./gradlew runLocal`. This starts Mopidy and the Kotlin bot together; stopping the bot also stops the Mopidy process it started. Audio goes to the Mac's default output unless you set `MOPIDY_AUDIO_OUTPUT`.

Spotify search can work even when playback authentication fails with `GStreamer error: Resource not found`. Mopidy-Spotify [documents this upstream issue](https://github.com/mopidy/mopidy-spotify/issues/437). If it happens, stop `runLocal`, run `./gradlew setupSpotifyPlaybackAuthMac`, complete the Spotify sign-in shown by the librespot helper, then restart `./gradlew runLocal`. This creates a separate playback credential cache at `~/Library/Application Support/mopidy/spotify/credentials-cache/credentials.json`; your Spotify client ID and secret in `.localenv` remain unchanged. The task backs up any existing credential file and restricts file permissions. Spotify Premium is required for Mopidy-Spotify playback.

On a future host, configure Mopidy there and run the bot with `./gradlew run`; it will connect to `MOPIDY_RPC_URL` without starting a local Mopidy process. Run tests with `./gradlew test`.

## Requesting music

Use `/song <song name>` for the first matching track, or pass a Spotify track URL, `spotify:track:...` URI, or bare track ID. The bot privately shows the match with **Queue it** and **Dismiss** buttons. Confirming updates your private card; the channel gets one public now-playing card when the song actually starts.

Use `/artist <artist name>` (or a Spotify artist URL, URI, or bare artist ID) to see five private song choices. **Show five other songs** shows one more page of five. Duplicate titles from different albums and remasters are collapsed. After that, use `/song` if you want a specific track. Selecting a number replaces the choices with a private confirmation step. Every private card has **Dismiss**, which removes it without queueing anything. Searches and choices expire after 30 minutes and reset when the bot restarts.

When Mopidy starts playing a new track, the bot automatically posts a single now-playing card in the DJ channel. It includes the requester when the bot queued the track, plus larger album art at the top of the card when Mopidy provides it. Tracks added outside the bot have no requester shown. There is no `/nowplaying` slash command to configure. Playback is checked every two seconds, so the announcement may be slightly delayed.

Slash command results and button responses are ephemeral: only the requester sees them, and Slack may discard them after a refresh. The bot only uses the configured `SLACK_CHANNEL`. The Gradle window logs command and button events, music availability, and posting errors.

## Legacy mention commands

In the configured channel, mention the bot followed by a command, for example `<@U123456> play The Beatles`. The bot asks you to confirm the first search result with `yes` or reject it with `no` (mention it again for that response).

The bot no longer posts public startup and online/offline greetings. Mention commands below still work as a secondary workflow and reply publicly. If mentions produce no `Received ... event` line, check that Event Subscriptions are enabled and `app_mention` is subscribed in the Slack app settings.

| Command | Action |
| --- | --- |
| `play <search terms>` | Search Spotify and propose a track |
| `yes` / `no` | Confirm or reject your pending track |
| `music play` / `music pause` | Control playback |
| `what` | Show the current track and playback state |
| `next` | Show queued tracks |
| `belter` | Queue a random track from the configured Spotify playlist |
| `skip` / `keep` | Cast one vote per current track; three net skip votes advance it |
| `quote` | Post a random quote |
| `help` | Show command help |

Pending tracks, played track history, and votes are held in memory and reset when the process restarts. The bot checks Mopidy connectivity every two seconds and enables consume mode when Mopidy comes online.
