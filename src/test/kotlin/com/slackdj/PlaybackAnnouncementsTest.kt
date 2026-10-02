package com.slackdj

import com.slack.api.model.block.CardBlock
import com.slack.api.model.block.SectionBlock
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
        val withArt = SlackViews.nowPlaying(first, "https://i.scdn.co/image/example")
        assertEquals(2, withArt.size)
        val card = withArt[1] as CardBlock
        assertEquals("https://i.scdn.co/image/example", (card.icon as ImageElement).imageUrl)
        assertTrue(card.title.toString().contains("First"))
        assertTrue(card.subtitle.toString().contains("Artist"))
        assertEquals(1, SlackViews.nowPlaying(first, null).size)
        assertTrue(SlackViews.nowPlaying(first, null).single() is SectionBlock)
    }
}
