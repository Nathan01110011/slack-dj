package com.slackdj

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalConfigTest {
    @Test
    fun localValuesLoadAndEnvironmentTakesPrecedence() {
        val file = Files.createTempFile("slack-dj-config", ".env")
        try {
            Files.writeString(file, "# comment\nSLACK_APP_TOKEN='local-token'\nSLACK_CHANNEL=C123\n")
            val config = loadConfig(file, mapOf("SLACK_APP_TOKEN" to "environment-token"))
            assertEquals("environment-token", config["SLACK_APP_TOKEN"])
            assertEquals("C123", config["SLACK_CHANNEL"])
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
