package com.slackdj

import com.slack.api.model.block.CardBlock
import com.slack.api.model.block.element.ImageElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackAnnouncementsTest {
    private val first = Track("spotify:track:first", "First", "Artist")
    private val second = Track("spotify:track:second", "Second", "Artist")

    @Test
    fun announcesEachNewTrackButNotPollsOrResume() {
        val watcher = PlaybackAnnouncements()
        assertNull(watcher.observe("stopped", null))
        assertEquals(first, watcher.observe("playing", first))
        assertNull(watcher.observe("playing", first))
        assertNull(watcher.observe("paused", null))
        assertNull(watcher.observe("playing", first))
        assertEquals(second, watcher.observe("playing", second))
        assertNull(watcher.observe("stopped", null))
        assertEquals(second, watcher.observe("playing", second))
    }

    @Test
    fun nowPlayingUsesAlbumArtWhenAvailable() {
        val withArt = SlackViews.nowPlaying(first, "https://i.scdn.co/image/example", "U123")
        assertEquals(1, withArt.size)
        val card = withArt.single() as CardBlock
        assertEquals("https://i.scdn.co/image/example", (card.heroImage as ImageElement).imageUrl)
        assertNull(card.icon)
        assertTrue(card.title.toString().contains("First"))
        assertTrue(card.subtitle.toString().contains("Artist"))
        assertTrue(card.body.toString().contains("Now playing"))
        assertTrue(card.body.toString().contains("<@U123>"))
        val withoutArt = SlackViews.nowPlaying(first, null, null).single() as CardBlock
        assertNull(withoutArt.heroImage)
        assertTrue(withoutArt.body.toString().contains("Now playing"))
        assertTrue(!withoutArt.body.toString().contains("Requested by"))
    }
}
