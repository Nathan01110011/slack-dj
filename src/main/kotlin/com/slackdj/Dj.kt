package com.slackdj

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.random.Random

class Dj(private val music: MusicServer, private val random: Random = Random.Default) {
    private val pending = mutableMapOf<String, Track>()
    private val played = mutableSetOf<String>()
    private val requesters = mutableMapOf<String, String>()
    private val awaitingPlayback = linkedMapOf<String, Long>()
    private val voters = mutableSetOf<String>()
    private var votedTrackUri: String? = null
    private var skipCount = 0

    data class QueueResult(val queued: Boolean, val started: Boolean = false)

    @Synchronized
    fun queue(track: Track, requester: String? = null): QueueResult {
        if (track.uri in played) return QueueResult(false)
        val tlid = music.add(track.uri)
        awaitingPlayback[track.uri] = tlid
        if (requester != null) requesters[track.uri] = requester
        played.add(track.uri)
        val started = music.state() == "stopped"
        if (started) music.play(tlid)
        return QueueResult(true, started)
    }

    @Synchronized
    fun observedPlaying(track: Track) {
        awaitingPlayback.remove(track.uri)
    }

    /** Mopidy can stop just after a queue-time state check. Start the oldest still-pending TLID. */
    @Synchronized
    fun recoverStoppedPlayback(): String? {
        val (uri, tlid) = awaitingPlayback.entries.firstOrNull() ?: return null
        if (music.state() != "stopped") return null
        music.play(tlid)
        return uri
    }

    @Synchronized
    fun requesterFor(track: Track): String? = requesters[track.uri]

    @Synchronized
    fun command(user: String, text: String): String {
        val parts = text.trim().split(Regex("\\s+"), limit = 2)
        val action = parts.firstOrNull()?.lowercase().orEmpty()
        val option = parts.getOrNull(1)?.trim().orEmpty()
        return when (action) {
            "help" -> HELP
            "quote" -> QUOTES.random(random)
            "what" -> music.currentTrack()?.let { "${it.label} is currently ${music.state()}!" } ?: "No music is currently playing!"
            "music" -> when (option.lowercase()) {
                "pause" -> { music.pause(); "Music now *_not_* playing!" }
                "play" -> { music.play(); "Music now playing!" }
                "" -> "What do you want to do with the music? Play/Pause?"
                else -> "Unrecognised command."
            }
            "next" -> music.queuedTracks().mapIndexed { index, track -> "${index + 1}) ${track.label}" }
                .joinToString("\n").ifEmpty { "Nothing queued at the minute." }
            "belter" -> music.belters().randomOrNull(random)?.let {
                val result = queue(it, user)
                if (result.queued) "Coming up: ${it.label}" else "This has already been played today."
            } ?: "Couldn't find a belter right now."
            "play" -> {
                if (option.isEmpty()) "Specify something to play."
                else {
                    val track = music.search(option).firstOrNull()
                    if (track == null) "Can't find anything like that :thinking_face:"
                    else { pending[user] = track; "<@$user> So that's ${track.label} to be played?" }
                }
            }
            "yes" -> {
                val track = pending[user] ?: return "Have you requested a song yet?"
                pending.remove(user)
                val result = queue(track, user)
                when {
                    !result.queued -> "This has already been played today."
                    result.started -> "Playing that now!"
                    else -> "Song added to the queue."
                }
            }
            "no" -> if (pending.remove(user) != null) "oh dear" else "Have you requested a song yet?"
            "skip", "keep" -> vote(user, action)
            else -> "Sorry, that doesn't seem like a valid command."
        }
    }

    private fun vote(user: String, action: String): String {
        val track = music.currentTrack() ?: return "No music is currently playing!"
        if (votedTrackUri != track.uri) {
            votedTrackUri = track.uri
            voters.clear()
            skipCount = 0
        }
        if (!voters.add(user)) return "Only one vote allowed per song I'm afraid."
        skipCount += if (action == "skip") 1 else -1
        if (skipCount > 2) {
            music.next()
            votedTrackUri = null
            voters.clear()
            skipCount = 0
            return "Okay then, I hope that wasn't an absolute *_tune and a half!_*"
        }
        return "Skip count is at $skipCount."
    }

    companion object {
        private const val HELP = "For song requests, use `/song <title>` or `/artist <name>`; choices stay private until you confirm. Legacy mention commands:\n\n" +
            "*play* \"_insert search term_\" : Queue up a song - respond with a yes or no.\n" +
            "*music* \"_pause/play_\" : Start or stop playback\n" +
            "*what* : I'll tell you what's currently playing\n" +
            "*next* : Shows the current track list\n" +
            "*belter* : Queues up an absolute belter\n" +
            "*skip* : Vote to skip the current song\n" +
            "*keep* : Vote to keep the current song\n" +
            "*quote* : One of my classic quotes"

        private val QUOTES: List<String> = requireNotNull(Dj::class.java.getResourceAsStream("/quotes.json")) {
            "quotes.json is missing"
        }.use { stream -> ObjectMapper().readTree(stream).map { it.asText() } }
    }
}
