package com.slackdj

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.time.Duration

internal fun startLocalMopidy(config: Map<String, String>, music: MusicServer): Process {
    fun setting(name: String): String = config[name]?.takeIf { it.isNotBlank() }
        ?: error("Set $name in .localenv to use runLocal")

    val clientId = setting("MOPIDY_SPOTIFY_CLIENT_ID")
    val clientSecret = setting("MOPIDY_SPOTIFY_CLIENT_SECRET")
    val audioOutput = config["MOPIDY_AUDIO_OUTPUT"]?.takeIf { it.isNotBlank() } ?: "autoaudiosink"
    for (value in listOf(clientId, clientSecret, audioOutput)) {
        require(!value.contains('\n') && !value.contains('\r')) { "Mopidy settings must be single-line values" }
    }

    val pluginPath = Path.of(".local/mopidy/plugins").toAbsolutePath()
    require(Files.isDirectory(pluginPath)) {
        "Spotify playback plugin is missing; run ./gradlew setupMopidyMac first"
    }
    val tempDir = Files.createTempDirectory("slack-dj-mopidy-")
    val configFile = tempDir.resolve("mopidy.conf")
    try {
        Files.writeString(configFile, """
            [http]
            hostname = 127.0.0.1
            port = 6680

            [spotify]
            client_id = $clientId
            client_secret = $clientSecret

            [audio]
            output = $audioOutput
        """.trimIndent() + "\n")
        Files.setPosixFilePermissions(configFile, setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))

        val builder = ProcessBuilder("mopidy", "--config", configFile.toString()).inheritIO()
        val existingPluginPath = System.getenv("GST_PLUGIN_PATH").orEmpty()
        builder.environment()["GST_PLUGIN_PATH"] = listOf(pluginPath.toString(), existingPluginPath)
            .filter { it.isNotBlank() }.joinToString(":")
        val process = builder.start()
        try {
            var ready = false
            for (attempt in 0 until 60) {
                if (!process.isAlive) error("Mopidy exited during startup (exit ${process.exitValue()})")
                try {
                    music.state()
                    ready = true
                    break
                } catch (_: Exception) {
                    Thread.sleep(500)
                }
            }
            if (!ready) error("Mopidy did not become ready at the configured RPC URL")
            Runtime.getRuntime().addShutdownHook(Thread {
                process.destroy()
                process.waitFor(Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                if (process.isAlive) process.destroyForcibly()
                Files.deleteIfExists(configFile)
                Files.deleteIfExists(tempDir)
            })
            return process
        } catch (error: Exception) {
            process.destroy()
            process.waitFor(Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            if (process.isAlive) process.destroyForcibly()
            throw error
        }
    } catch (error: Exception) {
        Files.deleteIfExists(configFile)
        Files.deleteIfExists(tempDir)
        throw error
    }
}
