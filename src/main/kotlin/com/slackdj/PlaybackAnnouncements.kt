package com.slackdj

/** Tracks playback transitions so polling doesn't repeat an announcement on pause/resume. */
internal class PlaybackAnnouncements {
    private var lastUri: String? = null

    fun observe(state: String, track: Track?): Track? {
        if (state == "stopped") lastUri = null
        if (state != "playing" || track == null) return null
        if (track.uri == lastUri) return null
        lastUri = track.uri
        return track
    }

    fun disconnected() { lastUri = null }
}
