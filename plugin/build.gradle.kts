plugins {
    id("convention.plugins")
}

group = "io.github.john-tuesday"
version = "0.1.0-SNAPSHOT"

gradlePlugin {
    plugins {
        val mavenPublishAssist by registering {
            id = "io.github.john-tuesday.maven-publish-assist"
            displayName = "Maven publish assist plugin"
            description = "Simplify and streamline publishing to Maven Central"
            tags = listOf("maven", "publish")
            implementationClass = "io.github.john.tuesday.plugins.MavenPublishAssistPlugin"
        }
    }
}
