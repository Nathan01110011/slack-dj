package com.slackdj

import com.slack.api.app_backend.interactive_components.response.ActionResponse
import com.slack.api.model.block.ActionsBlock
import com.slack.api.model.block.CardBlock
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import com.slack.api.model.block.composition.PlainTextObject
import com.slack.api.model.block.element.ButtonElement
import com.slack.api.model.block.element.ImageElement

internal object SlackViews {
    fun dismissal(): ActionResponse = ActionResponse.builder().deleteOriginal(true).build()

    fun replacement(view: RequestView): ActionResponse = ActionResponse.builder()
        .responseType("ephemeral")
        .replaceOriginal(true)
        .text(fallback(view))
        .blocks(blocks(view))
        .build()

    fun blocks(view: RequestView): List<LayoutBlock> = when (view) {
        is RequestView.Notice -> listOf(section(view.text), actions(dismissButton()))
        is RequestView.Queued -> listOf(section("Queued *${safe(view.track.name)}*."), actions(dismissButton()))
        is RequestView.Confirm -> listOf(
            section("Queue *${safe(view.track.name)}* by ${safe(view.track.artist)}? Only you can see this choice."),
            actions(
                button("Queue it", "dj_confirm", view.id),
                dismissButton(view.id),
            ),
        )
        is RequestView.Choices -> buildList {
            val lines = view.tracks.mapIndexed { offset, (_, track) ->
                "${offset + 1}. *${safe(track.name)}* — ${safe(track.artist)}"
            }.joinToString("\n")
            add(section("Songs for *${safe(view.artist)}*:\n$lines"))
            add(actions(*view.tracks.mapIndexed { offset, (index, _) ->
                button("${offset + 1}", "dj_select_$offset", "${view.id}:$index")
            }.toTypedArray()))
            add(actions(*(if (view.showMore) arrayOf(button("Show five other songs", "dj_more", view.id), dismissButton(view.id))
                else arrayOf(dismissButton(view.id)))))
            if (view.suggestSongSearch) add(section("Looking for a specific song? Try `/song <title or Spotify track link>`."))
        }
    }

    fun fallback(view: RequestView): String = when (view) {
        is RequestView.Choices -> "Choose a song for ${view.artist}"
        is RequestView.Confirm -> "Queue ${view.track.label}?"
        is RequestView.Queued -> "Queued ${view.track.name}"
        is RequestView.Notice -> view.text
    }

    fun safe(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    fun nowPlaying(track: Track, imageUrl: String?, requester: String?): List<LayoutBlock> {
        val card = CardBlock.builder()
            .title(MarkdownTextObject.builder().text("*${safe(track.name)}*").build())
            .subtitle(MarkdownTextObject.builder().text(safe(track.artist)).build())
            .body(MarkdownTextObject.builder().text(":musical_note: *Now playing*" +
                (requester?.let { " · Requested by <@${safe(it)}>" } ?: "")).build())
        if (imageUrl != null) card.heroImage(ImageElement.builder()
            .imageUrl(imageUrl).altText("Album art for ${track.name}").build())
        return listOf(card.build())
    }

    private fun dismissButton(value: String? = null): ButtonElement = button("Dismiss", "dj_dismiss", value)

    private fun section(text: String): SectionBlock = SectionBlock.builder()
        .text(MarkdownTextObject.builder().text(text).build()).build()

    private fun actions(vararg buttons: ButtonElement): ActionsBlock = ActionsBlock.builder()
        .elements(buttons.toList()).build()

    private fun button(label: String, action: String, value: String?): ButtonElement {
        val builder = ButtonElement.builder()
            .actionId(action)
            .text(PlainTextObject.builder().text(label).build())
        if (value != null) builder.value(value)
        return builder.build()
    }
}
