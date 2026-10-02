package com.slackdj

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

data class Track(val uri: String, val name: String, val artist: String) {
    val label: String get() = "$name by $artist"
}

interface MusicServer {
    fun state(): String
    fun currentTrack(): Track?
    fun imageUrl(uri: String): String? = null
    fun search(query: String): List<Track>
    fun lookup(uri: String): List<Track>
    fun artistSongs(query: String): List<Track>
    fun belters(): List<Track>
    fun add(uri: String)
    fun play()
    fun pause()
    fun next()
    fun queuedTracks(): List<Track>
    fun setConsume(enabled: Boolean)
}

class MopidyClient(
    endpoint: String = "http://localhost:6680/mopidy/rpc",
    private val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
    private val mapper: ObjectMapper = ObjectMapper(),
) : MusicServer {
    private val uri = URI.create(endpoint)
    private val ids = AtomicLong()

    private fun call(method: String, params: Map<String, Any> = emptyMap(), timeout: Duration = Duration.ofSeconds(5)): JsonNode {
        val body = mapper.writeValueAsString(mapOf("jsonrpc" to "2.0", "id" to ids.incrementAndGet(), "method" to "core.$method", "params" to params))
        val request = HttpRequest.newBuilder(uri)
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) error("Mopidy returned HTTP ${response.statusCode()}")
        val json = mapper.readTree(response.body())
        if (json.hasNonNull("error")) error("Mopidy error: ${json["error"]}")
        return json["result"] ?: error("Mopidy returned no result")
    }

    override fun state() = call("playback.get_state").asText()
    override fun currentTrack() = call("playback.get_current_track").toTrack()
    override fun imageUrl(uri: String): String? = call("library.get_images", mapOf("uris" to listOf(uri)))
        .path(uri)
        .mapNotNull { image ->
            image.path("uri").asText("").takeIf { it.startsWith("https://") }
                ?.let { image.path("width").asInt(0) to it }
        }
        .maxByOrNull { (width, _) -> width }
        ?.second
    override fun search(query: String) = call("library.search", mapOf("query" to mapOf("any" to listOf(query)), "uris" to listOf("spotify:"))).searchTracks()
    override fun lookup(uri: String): List<Track> = call("library.lookup", mapOf("uris" to listOf(uri)), Duration.ofSeconds(30))
        .path(uri).mapNotNull { it.toTrack() }
    override fun artistSongs(query: String): List<Track> {
        val artistUri = SpotifyInput.uri(query, "artist")
        val tracks = if (artistUri != null) lookup(artistUri) else call(
            "library.search",
            mapOf("query" to mapOf("artist" to listOf(query)), "uris" to listOf("spotify:")),
            Duration.ofSeconds(20),
        ).searchTracks()
        return tracks.distinctBy { SongIdentity.key(it) }.take(10)
    }
    override fun belters() = call("library.search", mapOf("query" to mapOf("uri" to listOf(BELTER_PLAYLIST)), "uris" to listOf("spotify:"))).searchTracks()
    override fun add(uri: String) { call("tracklist.add", mapOf("uris" to listOf(uri))) }
    override fun play() { call("playback.play") }
    override fun pause() { call("playback.pause") }
    override fun next() { call("playback.next") }
    override fun setConsume(enabled: Boolean) { call("tracklist.set_consume", mapOf("value" to enabled)) }

    override fun queuedTracks(): List<Track> {
        val tracks = call("tracklist.get_tracks").mapNotNull { it.toTrack() }
        val index = call("tracklist.index").takeUnless { it.isNull }?.asInt() ?: -1
        return tracks.drop(index + 1)
    }

    private fun JsonNode.searchTracks(): List<Track> = flatMap { result ->
        result.path("tracks").mapNotNull { it.toTrack() }
    }

    private fun JsonNode.toTrack(): Track? {
        if (isNull || isMissingNode) return null
        val uri = path("uri").asText("")
        if (uri.isBlank()) return null
        return Track(uri, path("name").asText("Unknown track"), path("artists").firstOrNull()?.path("name")?.asText() ?: "Unknown artist")
    }

    companion object {
        private const val BELTER_PLAYLIST = "spotify:playlist:3xVCCnIZV6a2gvmvcYWOSP"
    }
}
