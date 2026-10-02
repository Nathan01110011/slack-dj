package com.slackdj

import com.slack.api.model.block.ActionsBlock
import com.slack.api.model.block.element.ButtonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SongRequestsTest {
    private val tracks = (1..10).map { Track("spotify:track:${it.toString().padStart(22, '0')}", "Song $it", "Artist") }

    @Test
    fun spotifyLinksUrisAndBareIdsAreAcceptedOnlyForTheirType() {
        val id = "4UrmmXStaqiT5sSC7QO6HK"
        assertEquals("spotify:track:$id", SpotifyInput.uri("https://open.spotify.com/track/$id?si=abc", "track"))
        assertEquals("spotify:track:$id", SpotifyInput.uri("<https://open.spotify.com/track/$id|Spotify>", "track"))
        assertEquals("spotify:track:$id", SpotifyInput.uri("spotify:track:$id", "track"))
        assertEquals("spotify:track:$id", SpotifyInput.uri(id, "track"))
        assertEquals("spotify:artist:$id", SpotifyInput.uri("https://open.spotify.com/artist/$id", "artist"))
        assertEquals(null, SpotifyInput.uri("spotify:artist:$id", "track"))
    }

    @Test
    fun songIsPrivateUntilItsOwnerConfirms() {
        val music = FakeMusic().apply { songResults = listOf(tracks[0]) }
        val dj = Dj(music)
        val requests = SongRequests(music, dj)
        val prompt = assertIs<RequestView.Confirm>(requests.song("U1", "C1", "Song 1"))
        assertTrue(music.added.isEmpty())
        assertIs<RequestView.Notice>(requests.confirm("U2", "C1", prompt.id))
        assertIs<RequestView.Notice>(requests.confirm("U1", "C2", prompt.id))
        assertTrue(music.added.isEmpty())
        val queued = assertIs<RequestView.Queued>(requests.confirm("U1", "C1", prompt.id))
        assertEquals(tracks[0], queued.track)
        assertEquals(listOf(tracks[0].uri), music.added)
        assertEquals("U1", dj.requesterFor(tracks[0]))
        assertIs<RequestView.Notice>(requests.confirm("U1", "C1", prompt.id))
        assertEquals(1, music.added.size)
    }

    @Test
    fun artistShowsTwoPagesOfFiveThenSuggestsSongSearch() {
        val music = FakeMusic().apply { artistResults = tracks }
        val requests = SongRequests(music, Dj(music))
        val first = assertIs<RequestView.Choices>(requests.artist("U1", "C1", "Artist"))
        assertEquals((0..4).toList(), first.tracks.map { it.first })
        assertTrue(first.showMore)
        assertFalse(first.suggestSongSearch)
        assertEquals(3, SlackViews.blocks(first).size)
        val buttons = (SlackViews.blocks(first)[1] as ActionsBlock).elements.filterIsInstance<ButtonElement>()
        assertEquals(5, buttons.size)
        assertEquals(5, buttons.map { it.actionId }.toSet().size)
        assertTrue(music.added.isEmpty())

        val second = assertIs<RequestView.Choices>(requests.more("U1", "C1", first.id))
        assertEquals((5..9).toList(), second.tracks.map { it.first })
        assertFalse(second.showMore)
        assertTrue(second.suggestSongSearch)
        assertEquals(4, SlackViews.blocks(second).size)

        val selected = assertIs<RequestView.Confirm>(requests.select("U1", "C1", "${first.id}:7"))
        assertEquals(tracks[7], selected.track)
        assertTrue(music.added.isEmpty())
        assertIs<RequestView.Queued>(requests.confirm("U1", "C1", first.id))
        assertEquals(listOf(tracks[7].uri), music.added)
    }

    @Test
    fun duplicateAlbumCopiesAreCollapsedBeforePagination() {
        val music = FakeMusic().apply {
            artistResults = listOf(
                tracks[0],
                Track("spotify:track:other000000000000000", "Song 1 - 2009 Remaster", "Artist"),
                Track("spotify:track:another0000000000000", "SONG 1", "Artist"),
            ) + tracks.drop(1)
        }
        val requests = SongRequests(music, Dj(music))
        val first = assertIs<RequestView.Choices>(requests.artist("U1", "C1", "Artist"))
        val second = assertIs<RequestView.Choices>(requests.more("U1", "C1", first.id))
        assertEquals((0..4).toList(), first.tracks.map { it.first })
        assertEquals((5..9).toList(), second.tracks.map { it.first })
        assertEquals(tracks.map { it.uri }, (first.tracks + second.tracks).map { it.second.uri })
        assertEquals(SongIdentity.key(tracks[0]), SongIdentity.key(Track("different", "Song 1 (2009 Remaster)", "Artist")))
        assertFalse(SongIdentity.key(tracks[0]) == SongIdentity.key(Track("different", "Song 1 - Live", "Artist")))
    }

    @Test
    fun buttonResponsesReplacePromptsAndKeepOnlyDismissWhenDone() {
        val confirm = RequestView.Confirm("id", tracks[0])
        val confirmResponse = SlackViews.replacement(confirm)
        assertTrue(confirmResponse.isReplaceOriginal)
        assertEquals("ephemeral", confirmResponse.responseType)
        assertTrue(confirmResponse.blocks.any { it is ActionsBlock })

        val queuedResponse = SlackViews.replacement(RequestView.Queued(tracks[0], false))
        assertTrue(queuedResponse.isReplaceOriginal)
        assertEquals(listOf("dj_dismiss"), actionIds(queuedResponse.blocks))
        val cancelResponse = SlackViews.replacement(RequestView.Notice("Cancelled"))
        assertEquals(listOf("dj_dismiss"), actionIds(cancelResponse.blocks))
        assertEquals(listOf("dj_confirm", "dj_dismiss"), actionIds(confirmResponse.blocks))
        assertTrue(SlackViews.dismissal().isDeleteOriginal)
    }

    @Test
    fun dismissRemovesOnlyOwnersSessionWithoutQueueing() {
        val music = FakeMusic().apply { songResults = listOf(tracks[0]) }
        val requests = SongRequests(music, Dj(music))
        val prompt = assertIs<RequestView.Confirm>(requests.song("U1", "C1", "Song 1"))
        requests.dismiss("U2", "C1", prompt.id)
        val queued = assertIs<RequestView.Queued>(requests.confirm("U1", "C1", prompt.id))
        assertEquals(tracks[0], queued.track)

        val another = assertIs<RequestView.Confirm>(requests.song("U1", "C1", "Song 1"))
        requests.dismiss("U1", "C1", another.id)
        assertIs<RequestView.Notice>(requests.confirm("U1", "C1", another.id))
        assertEquals(1, music.added.size)
    }

    private fun actionIds(blocks: List<com.slack.api.model.block.LayoutBlock>): List<String> = blocks
        .filterIsInstance<ActionsBlock>()
        .flatMap { it.elements.filterIsInstance<ButtonElement>().map(ButtonElement::getActionId) }

    @Test
    fun spotifyIdUsesLookupAndExpiredChoicesCannotQueue() {
        val music = FakeMusic().apply { lookupResults = listOf(tracks[0]) }
        var time = 0L
        val requests = SongRequests(music, Dj(music)) { time }
        val prompt = assertIs<RequestView.Confirm>(requests.song("U1", "C1", "4UrmmXStaqiT5sSC7QO6HK"))
        assertEquals("spotify:track:4UrmmXStaqiT5sSC7QO6HK", music.lookedUp)
        time = 31 * 60 * 1000L
        assertIs<RequestView.Notice>(requests.confirm("U1", "C1", prompt.id))
        assertTrue(music.added.isEmpty())
    }

    private class FakeMusic : MusicServer {
        var songResults = emptyList<Track>()
        var artistResults = emptyList<Track>()
        var lookupResults = emptyList<Track>()
        var lookedUp: String? = null
        val added = mutableListOf<String>()
        override fun state() = "playing"
        override fun currentTrack(): Track? = null
        override fun search(query: String) = songResults
        override fun lookup(uri: String): List<Track> { lookedUp = uri; return lookupResults }
        override fun artistSongs(query: String) = artistResults
        override fun belters() = emptyList<Track>()
        override fun add(uri: String): Long { added.add(uri); return added.size.toLong() }
        override fun play(tlid: Long?) = Unit
        override fun pause() = Unit
        override fun next() = Unit
        override fun queuedTracks() = emptyList<Track>()
        override fun setConsume(enabled: Boolean) = Unit
    }
}
