package com.slackdj

import com.slack.api.Slack
import com.slack.api.bolt.App
import com.slack.api.bolt.AppConfig
import com.slack.api.bolt.socket_mode.SocketModeApp
import com.slack.api.model.event.AppMentionEvent
import com.slack.api.model.event.MessageEvent
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private fun Map<String, String>.required(name: String): String = get(name)?.takeIf { it.isNotBlank() }
    ?: error("Set $name in .localenv or the environment")

fun main() {
    val log = LoggerFactory.getLogger("SlackDJ")
    val config = loadConfig()
    val botToken = config["SLACK_BOT_TOKEN"]?.takeIf { it.isNotBlank() } ?: config.required("SLACK_TOKEN")
    val appToken = config.required("SLACK_APP_TOKEN")
    require(botToken.startsWith("xoxb-")) { "SLACK_BOT_TOKEN must be a bot OAuth token (xoxb-)" }
    require(appToken.startsWith("xapp-")) { "SLACK_APP_TOKEN must be an app-level token (xapp-)" }
    val channel = config.required("SLACK_CHANNEL")
    val banned = config["BAN_USER"].orEmpty().split(Regex("[,;\\s]+|[\\[\\]\"']+"))
        .filter { it.isNotBlank() }.toSet()
    val music = MopidyClient(config["MOPIDY_RPC_URL"]?.takeIf { it.isNotBlank() }
        ?: "http://localhost:6680/mopidy/rpc")
    if (config["MOPIDY_START_LOCAL"] == "true") startLocalMopidy(config, music)
    val dj = Dj(music)
    val requests = SongRequests(music, dj)
    val slack = Slack.getInstance().methods(botToken)
    val auth = slack.authTest { it }
    check(auth.isOk) { "Slack auth.test failed: ${auth.error}" }
    val botId = auth.userId ?: error("Slack auth.test did not return a bot user ID")
    val configuredBotId = config["SLACK_DJ_ID"]?.removePrefix("<@")?.removeSuffix(">")
    if (!configuredBotId.isNullOrBlank() && configuredBotId != botId) {
        log.warn("SLACK_DJ_ID does not match the bot token; using Slack's bot user ID {}", botId)
    }
    log.info("Authenticated as bot user {}; listening in channel {}", botId, channel)
    val commands = Executors.newFixedThreadPool(4)
    val monitor = Executors.newSingleThreadScheduledExecutor()
    val online = AtomicBoolean(false)
    val checked = AtomicBoolean(false)
    val announcements = PlaybackAnnouncements()
    val receivedEvent = AtomicBoolean(false)
    val recentEvents = LinkedHashSet<String>()

    fun post(message: String) {
        try {
            val response = slack.chatPostMessage { it.channel(channel).text(message) }
            if (response.isOk) log.info("Posted Slack reply to {}", channel)
            else log.error("Slack post failed: {}", response.error)
        } catch (error: Exception) {
            log.error("Slack post failed", error)
        }
    }

    fun postNowPlaying(track: Track) {
        val requester = dj.requesterFor(track)
        val imageUrl = try { music.imageUrl(track.uri) } catch (error: Exception) {
            log.warn("Could not load album art for {}", track.uri, error)
            null
        }
        try {
            val response = slack.chatPostMessage {
                it.channel(channel).text(SlackViews.nowPlayingFallback(track, requester))
                    .blocks(SlackViews.nowPlaying(track, imageUrl, requester))
            }
            if (response.isOk) log.info("Announced now playing: {}", track.label)
            else log.error("Now-playing Slack post failed: {}", response.error)
        } catch (error: Exception) {
            log.error("Now-playing Slack post failed", error)
        }
    }

    fun postPrivate(user: String, view: RequestView) {
        try {
            val response = slack.chatPostEphemeral {
                it.channel(channel).user(user).text(SlackViews.fallback(view)).blocks(SlackViews.blocks(view))
            }
            if (response.isOk) log.info("Posted private Slack response to {}", user)
            else log.error("Private Slack response failed: {}", response.error)
        } catch (error: Exception) {
            log.error("Private Slack response failed", error)
        }
    }

    fun handleEvent(source: String, eventChannel: String?, user: String?, text: String?, ts: String?) {
        receivedEvent.set(true)
        log.info("Received {} event: channel={}, user={}", source, eventChannel, user)
        if (eventChannel != channel || user == null || user == botId) {
            log.info("Ignoring event outside the configured channel or without a human user")
            return
        }
        val message = text.orEmpty().trim()
        val mention = Regex("^<@${Regex.escape(botId)}(?:\\|[^>]+)?>\\s+(.+)$", RegexOption.DOT_MATCHES_ALL)
            .matchEntire(message)?.groupValues?.get(1)
        val raw = if (message.startsWith("$botId ")) message.removePrefix("$botId ") else null
        val command = mention ?: raw
        if (command == null) {
            log.info("Ignoring message without a leading bot mention")
            return
        }
        if (ts != null) {
            val key = "$eventChannel:$ts"
            val fresh = synchronized(recentEvents) {
                if (!recentEvents.add(key)) false else {
                    if (recentEvents.size > 256) recentEvents.remove(recentEvents.first())
                    true
                }
            }
            if (!fresh) {
                log.info("Ignoring duplicate event {}", key)
                return
            }
        }
        log.info("Handling command '{}' from {}", command.substringBefore(' '), user)
        commands.execute {
            val response = when {
                user in banned -> "Sorry, I've been ordered to ignore you :grinning:"
                !online.get() -> "Music server not up yet!"
                else -> try { dj.command(user, command) }
                    catch (error: Exception) {
                        log.error("Command failed", error)
                        "Don't know what's happened here, try again later ¯\\_(ツ)_/¯"
                    }
            }
            post(response)
        }
    }

    val app = App(AppConfig.builder().singleTeamBotToken(botToken).build())
    for (commandName in listOf("/song", "/artist")) {
        app.command(commandName) { request, context ->
            val payload = request.payload
            val user = payload.userId
            val eventChannel = payload.channelId
            val input = payload.text.orEmpty().trim()
            log.info("Received {} command: channel={}, user={}", commandName, eventChannel, user)
            if (eventChannel != channel) return@command context.ack("Use this command in the configured DJ channel.")
            commands.execute {
                val view = when {
                    user in banned -> RequestView.Notice("Sorry, I can't take requests from you.")
                    !online.get() -> RequestView.Notice("Music server not up yet!")
                    else -> try {
                        if (commandName == "/song") requests.song(user, channel, input)
                        else requests.artist(user, channel, input)
                    } catch (error: Exception) {
                        log.error("{} search failed", commandName, error)
                        RequestView.Notice("The music search failed. Please try again in a moment.")
                    }
                }
                postPrivate(user, view)
            }
            context.ack()
        }
    }
    fun action(actionId: String, perform: (String, String, String) -> RequestView) {
        app.blockAction(actionId) { request, context ->
            val user = request.payload.user.id
            val eventChannel = request.payload.channel?.id ?: request.payload.container?.channelId
            val value = request.payload.actions.firstOrNull()?.value.orEmpty()
            val hasResponseUrl = !request.payload.responseUrl.isNullOrBlank()
            log.info("Received {} action: channel={}, user={}", actionId, eventChannel, user)
            commands.execute {
                if (eventChannel == channel) {
                    val view = try {
                        perform(user, eventChannel, value)
                    } catch (error: Exception) {
                        log.error("{} action failed", actionId, error)
                        RequestView.Notice("That action failed. Please try your request again.")
                    }
                    try {
                        if (hasResponseUrl) {
                            val response = context.respond(SlackViews.replacement(view))
                            if (response.code != 200) log.error("Replacing private Slack prompt failed: HTTP {} ({})", response.code, response.body)
                            else log.info("Replaced private Slack prompt for {}", user)
                        } else {
                            log.warn("Slack action did not include a response URL; posting a new private response")
                            postPrivate(user, view)
                        }
                    } catch (error: Exception) {
                        log.error("Replacing private Slack prompt failed", error)
                        postPrivate(user, view)
                    }
                    if (view is RequestView.Queued) {
                        post("${SlackViews.safe(view.track.name)} queued by <@$user>")
                    }
                }
            }
            context.ack()
        }
    }
    for (index in 0..4) action("dj_select_$index", requests::select)
    action("dj_more", requests::more)
    action("dj_confirm", requests::confirm)
    action("dj_cancel", requests::cancel)
    app.blockAction("dj_dismiss") { request, context ->
        val user = request.payload.user.id
        val eventChannel = request.payload.channel?.id ?: request.payload.container?.channelId
        val id = request.payload.actions.firstOrNull()?.value.orEmpty()
        log.info("Received dismiss action: channel={}, user={}", eventChannel, user)
        val ack = context.ack()
        if (eventChannel == channel) {
            commands.execute {
                requests.dismiss(user, channel, id)
                try {
                    val response = context.respond(SlackViews.dismissal())
                    if (response.code != 200) log.error("Dismissing private Slack prompt failed: HTTP {} ({})", response.code, response.body)
                } catch (error: Exception) {
                    log.error("Dismissing private Slack prompt failed", error)
                }
            }
        }
        ack
    }

    app.event(MessageEvent::class.java) { request, context ->
        val event = request.event
        if (event.subtype == null && event.botId == null) {
            handleEvent("message", event.channel, event.user, event.text, event.ts)
        }
        context.ack()
    }
    app.event(AppMentionEvent::class.java) { request, context ->
        val event = request.event
        handleEvent("app_mention", event.channel, event.user, event.text, event.ts)
        context.ack()
    }

    val socket = SocketModeApp(appToken, app)
    log.info("Starting Socket Mode; /song and /artist are ready for slash commands")
    socket.startAsync()

    monitor.schedule({
        if (!receivedEvent.get()) {
            log.warn("No Slack events received yet. If you mentioned the bot, enable Event Subscriptions and add the app_mention bot event in Slack app settings.")
        }
    }, 30, TimeUnit.SECONDS)

    monitor.scheduleWithFixedDelay({
        val state = try { music.state() } catch (_: Exception) { null }
        val available = state != null
        val previous = online.getAndSet(available)
        if (!checked.get() || available != previous) log.info("Mopidy is {}", if (available) "online" else "offline")
        if (available && (!previous || !checked.get())) {
            try { music.setConsume(true) } catch (error: Exception) {
                log.error("Could not enable consume mode", error)
            }
        } else if (!available && previous) {
            log.warn("Mopidy went offline")
        }
        if (state == null) announcements.disconnected()
        else {
            val track = if (state == "playing") try { music.currentTrack() } catch (error: Exception) {
                log.warn("Could not read current Mopidy track", error)
                null
            } else null
            announcements.observe(state, track)?.let(::postNowPlaying)
        }
        checked.set(true)
    }, 0, 2, TimeUnit.SECONDS)

    Runtime.getRuntime().addShutdownHook(Thread {
        monitor.shutdownNow()
        commands.shutdownNow()
    })
}
