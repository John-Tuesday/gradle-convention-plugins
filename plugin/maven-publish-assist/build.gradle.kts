plugins {
    id("convention.plugins")
}

group = "io.github.john-tuesday"
version = "0.1.0-SNAPSHOT"
version = pluginLibs.versions.mavenPublishAssist.get()

gradlePlugin {
    plugins {
        val mavenPublishAssist by registering {
            id = pluginLibs.plugins.maven.publish.assist.get().pluginId
            displayName = "Maven publish assist plugin"
            description = "Simplify and streamline publishing to Maven Central"
            tags = listOf("maven", "publish")
            implementationClass = "io.github.john.tuesday.plugins.MavenPublishAssistPlugin"
        }
    }
}
