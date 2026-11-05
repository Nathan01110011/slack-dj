plugins {
    kotlin("jvm") version "2.2.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.slack.api:bolt-socket-mode:1.52.0")
    implementation("javax.websocket:javax.websocket-api:1.1")
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:1.20")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    implementation("org.slf4j:slf4j-simple:1.7.36")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.slackdj.MainKt")
}

distributions {
    main {
        contents {
            from("deploy") { into("deploy") }
        }
    }
}

tasks.register<Exec>("setupMopidyMac") {
    group = "application"
    description = "Install Mopidy and build the Spotify playback plugin on macOS"
    commandLine("bash", "scripts/setup-mopidy-macos.sh")
}

tasks.register<Exec>("setupSpotifyPlaybackAuthMac") {
    group = "application"
    description = "Create local Spotify playback credentials using the official librespot helper"
    commandLine("bash", "scripts/auth-spotify-macos.sh")
}

tasks.register<JavaExec>("runLocal") {
    group = "application"
    description = "Start Mopidy locally, then run Slack DJ"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(application.mainClass)
    environment("MOPIDY_START_LOCAL", "true")
}

tasks.test {
    useJUnitPlatform()
}
