package com.slackdj

import java.util.UUID
import java.util.Locale

object SpotifyInput {
    private val id = Regex("[A-Za-z0-9]{22}")
    private val uri = Regex("spotify:(track|artist):([A-Za-z0-9]{22})", RegexOption.IGNORE_CASE)
    private val url = Regex("https?://open\\.spotify\\.com/(?:intl-[A-Za-z-]+/)?(track|artist)/([A-Za-z0-9]{22})(?:[/?#].*)?", RegexOption.IGNORE_CASE)

    fun uri(input: String, type: String): String? {
        val value = input.trim().removeSurrounding("<", ">").substringBefore('|')
        val match = uri.matchEntire(value) ?: url.matchEntire(value)
        if (match != null) return if (match.groupValues[1].equals(type, ignoreCase = true)) {
            "spotify:$type:${match.groupValues[2]}"
        } else null
        return if (id.matches(value)) "spotify:$type:$value" else null
    }
}

object SongIdentity {
    private val remaster = Regex("(?i)\\s*(?:[-–—]\\s*|\\(\\s*)(?:(?:\\d{4}\\s*)?remaster(?:ed)?(?:\\s*\\d{4})?)\\s*\\)?$")
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    fun key(track: Track): String = "${normalize(track.artist)}:${normalize(track.name.replace(remaster, ""))}"

    private fun normalize(value: String): String = value.lowercase(Locale.ROOT).replace(separators, " ").trim()
}

sealed interface RequestView {
    data class Choices(
        val id: String,
        val artist: String,
        val tracks: List<Pair<Int, Track>>,
        val showMore: Boolean,
        val suggestSongSearch: Boolean,
    ) : RequestView
    data class Confirm(val id: String, val track: Track) : RequestView
    data class Queued(val track: Track, val started: Boolean) : RequestView
    data class Notice(val text: String) : RequestView
}

class SongRequests(
    private val music: MusicServer,
    private val dj: Dj,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private data class Session(
        val user: String,
        val channel: String,
        val artist: String?,
        val tracks: List<Track>,
        val expiresAt: Long,
        var page: Int = 0,
        var selection: Int? = null,
    )

    private val sessions = mutableMapOf<String, Session>()

    @Synchronized
    fun song(user: String, channel: String, input: String): RequestView {
        if (input.isBlank()) return RequestView.Notice("Use `/song <song name, Spotify track link, or track ID>`.")
        val uri = SpotifyInput.uri(input, "track")
        val track = (if (uri != null) music.lookup(uri) else music.search(input)).firstOrNull()
            ?: return RequestView.Notice("I couldn't find that song. Try a more specific title or Spotify track link.")
        val id = store(Session(user, channel, null, listOf(track), now() + TTL_MS, selection = 0))
        return RequestView.Confirm(id, track)
    }

    @Synchronized
    fun artist(user: String, channel: String, input: String): RequestView {
        if (input.isBlank()) return RequestView.Notice("Use `/artist <artist name, Spotify artist link, or artist ID>`.")
        val tracks = music.artistSongs(input).distinctBy { SongIdentity.key(it) }.take(10)
        if (tracks.isEmpty()) return RequestView.Notice("I couldn't find songs for that artist. Try the artist name or a Spotify artist link.")
        val id = store(Session(user, channel, input, tracks, now() + TTL_MS))
        return choices(id, sessions.getValue(id))
    }

    @Synchronized
    fun more(user: String, channel: String, id: String): RequestView {
        val session = session(id, user, channel) ?: return expired()
        if (session.artist == null || session.page != 0 || session.tracks.size <= 5) return expired()
        session.page = 1
        return choices(id, session)
    }

    @Synchronized
    fun select(user: String, channel: String, value: String): RequestView {
        val id = value.substringBefore(':')
        val index = value.substringAfter(':', "").toIntOrNull() ?: return expired()
        val session = session(id, user, channel) ?: return expired()
        if (index !in session.tracks.indices) return expired()
        session.selection = index
        return RequestView.Confirm(id, session.tracks[index])
    }

    @Synchronized
    fun confirm(user: String, channel: String, id: String): RequestView {
        val session = session(id, user, channel) ?: return expired()
        val track = session.selection?.let { session.tracks.getOrNull(it) } ?: return expired()
        sessions.remove(id)
        val result = dj.queue(track)
        return if (result.queued) RequestView.Queued(track, result.started)
        else RequestView.Notice("That song has already been queued today. Try another one.")
    }

    @Synchronized
    fun cancel(user: String, channel: String, id: String): RequestView {
        val session = session(id, user, channel) ?: return expired()
        sessions.remove(id)
        return RequestView.Notice("Cancelled. Nothing was queued.")
    }

    @Synchronized
    fun dismiss(user: String, channel: String, id: String) {
        if (session(id, user, channel) != null) sessions.remove(id)
    }

    private fun choices(id: String, session: Session): RequestView {
        val start = session.page * 5
        return RequestView.Choices(
            id, session.artist.orEmpty(), session.tracks.drop(start).take(5).mapIndexed { i, track -> (start + i) to track },
            showMore = session.page == 0 && session.tracks.size > 5,
            suggestSongSearch = session.page == 1 || session.tracks.size <= 5,
        )
    }

    private fun store(session: Session): String {
        sessions.entries.removeIf { it.value.expiresAt <= now() }
        val id = UUID.randomUUID().toString()
        sessions[id] = session
        return id
    }

    private fun session(id: String, user: String, channel: String): Session? {
        val session = sessions[id] ?: return null
        if (session.expiresAt <= now()) {
            sessions.remove(id)
            return null
        }
        return session.takeIf { it.user == user && it.channel == channel }
    }

    private fun expired() = RequestView.Notice("That choice is no longer available. Run `/song` or `/artist` again.")

    private companion object { const val TTL_MS = 30 * 60 * 1000L }
}
