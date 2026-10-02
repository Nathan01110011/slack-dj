package com.slackdj

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DjTest {
    private val first = Track("spotify:track:first", "First", "Artist")
    private val second = Track("spotify:track:second", "Second", "Artist")

    @Test
    fun confirmationsAreScopedToEachUser() {
        val music = FakeMusic().apply {
            results["first"] = listOf(first)
            results["second"] = listOf(second)
        }
        val dj = Dj(music)

        dj.command("A", "play first")
        dj.command("B", "play second")
        assertEquals("oh dear", dj.command("A", "no"))
        assertEquals("Song added to the queue.", dj.command("B", "yes"))
        assertEquals(listOf(second.uri), music.added)
        assertEquals("B", dj.requesterFor(second))
        assertEquals(null, dj.requesterFor(first))
        assertEquals("Have you requested a song yet?", dj.command("A", "yes"))
    }

    @Test
    fun skipVotesResetWhenTrackChanges() {
        val music = FakeMusic().apply { current = first }
        val dj = Dj(music)

        assertEquals("Skip count is at 1.", dj.command("A", "skip"))
        assertEquals("Only one vote allowed per song I'm afraid.", dj.command("A", "keep"))
        assertEquals("Skip count is at 2.", dj.command("B", "skip"))
        music.current = second
        assertEquals("Skip count is at 1.", dj.command("A", "skip"))
        dj.command("B", "skip")
        assertTrue(dj.command("C", "skip").startsWith("Okay then"))
        assertEquals(1, music.skips)
    }

    @Test
    fun stoppedPlaybackStartsAfterConfirmation() {
        val music = FakeMusic().apply {
            playbackState = "stopped"
            results["first"] = listOf(first)
        }
        val dj = Dj(music)
        dj.command("A", "play first")
        assertEquals("Playing that now!", dj.command("A", "yes"))
        assertEquals(1, music.plays)
        assertEquals("This has already been played today.", run {
            dj.command("B", "play first")
            dj.command("B", "yes")
        })
    }

    @Test
    fun directQueueRecordsRequesterOnlyWhenAccepted() {
        val music = FakeMusic()
        val dj = Dj(music)
        assertTrue(dj.queue(first, "U1").queued)
        assertEquals("U1", dj.requesterFor(first))
        assertTrue(!dj.queue(first, "U2").queued)
        assertEquals("U1", dj.requesterFor(first))
    }

    @Test
    fun queuedSongStartsIfPlaybackStopsJustAfterItWasAdded() {
        val music = FakeMusic().apply { current = first }
        val dj = Dj(music)
        dj.queue(first, "U1")
        dj.observedPlaying(first)

        assertTrue(dj.queue(second, "U2").queued)
        assertEquals(0, music.plays)
        music.playbackState = "stopped"

        assertEquals(second.uri, dj.recoverStoppedPlayback())
        assertEquals(listOf<Long?>(2L), music.playedTlids)
        assertEquals("playing", music.playbackState)
        dj.observedPlaying(second)
        music.playbackState = "stopped"
        assertEquals(null, dj.recoverStoppedPlayback())
    }

    @Test
    fun recoveryDoesNotOverridePausedPlayback() {
        val music = FakeMusic()
        val dj = Dj(music)
        dj.queue(first, "U1")
        music.playbackState = "paused"
        assertEquals(null, dj.recoverStoppedPlayback())
        assertEquals(0, music.plays)
    }

    private class FakeMusic : MusicServer {
        val results = mutableMapOf<String, List<Track>>()
        val added = mutableListOf<String>()
        var current: Track? = null
        var playbackState = "playing"
        var skips = 0
        var plays = 0
        val playedTlids = mutableListOf<Long?>()

        override fun state() = playbackState
        override fun currentTrack() = current
        override fun search(query: String) = results[query].orEmpty()
        override fun lookup(uri: String) = results[uri].orEmpty()
        override fun artistSongs(query: String) = results[query].orEmpty()
        override fun belters() = emptyList<Track>()
        override fun add(uri: String): Long { added.add(uri); return added.size.toLong() }
        override fun play(tlid: Long?) { plays++; playedTlids.add(tlid); playbackState = "playing" }
        override fun pause() { playbackState = "paused" }
        override fun next() { skips++ }
        override fun queuedTracks() = emptyList<Track>()
        override fun setConsume(enabled: Boolean) = Unit
    }
}
