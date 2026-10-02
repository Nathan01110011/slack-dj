package com.slackdj

import java.nio.file.Files
import java.nio.file.Path

internal fun loadConfig(
    file: Path = Path.of(".localenv"),
    environment: Map<String, String> = System.getenv(),
): Map<String, String> {
    val local = if (Files.exists(file)) {
        Files.readAllLines(file).mapIndexedNotNull { index, line ->
            val entry = line.trim()
            if (entry.isEmpty() || entry.startsWith("#")) return@mapIndexedNotNull null
            val separator = entry.indexOf('=')
            require(separator > 0) { "Invalid ${file.fileName} entry on line ${index + 1}" }
            val key = entry.substring(0, separator).trim()
            require(key.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                "Invalid ${file.fileName} key on line ${index + 1}"
            }
            val raw = entry.substring(separator + 1).trim()
            val value = if (raw.length >= 2 && raw.first() == raw.last() && raw.first() in "\"'") {
                raw.substring(1, raw.length - 1)
            } else raw
            key to value
        }.toMap()
    } else emptyMap()
    return local + environment.filterValues { it.isNotBlank() }
}
