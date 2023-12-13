import io.github.john.tuesday.build.logic.constants.PUBLISH_GROUP

plugins {
    id("convention.plugins")
}

group = PUBLISH_GROUP
version = pluginLibs.versions.shared.get()

kotlin {
    sourceSets {
        val main by getting {
            dependencies {
                api(project(":helper"))
            }
        }
    }
}

gradlePlugin {
    @Suppress("UnstableApiUsage")
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
