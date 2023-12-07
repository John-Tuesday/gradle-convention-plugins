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
                implementation(libs.dokka.gradlePlugin)
                implementation(libs.dokka.base)
                implementation(libs.dokka.version.plugin)
            }
        }
    }
}

gradlePlugin {
    plugins {
        val dokkaBaseConventionPlugin by registering {
            id = pluginLibs.plugins.dokka.convention.base.get().pluginId
            displayName = "Dokka base convention plugin"
            description = "Simplify and streamline generating documentation"
            implementationClass = "io.github.john.tuesday.plugins.DokkaBaseConventionPlugin"
        }
        val dokkaConventionPlugin by registering {
            id = pluginLibs.plugins.dokka.convention.convention.get().pluginId
            displayName = "Dokka convention plugin"
            description = "Simplify and streamline generating documentation"
            implementationClass = "io.github.john.tuesday.plugins.DokkaConventionPlugin"
        }
        val dokkaHtmlConventionPlugin by registering {
            id = pluginLibs.plugins.dokka.convention.html.get().pluginId
            displayName = "Dokka html convention plugin"
            description = "Simplify and streamline generating multi-module html documentation"
            implementationClass = "io.github.john.tuesday.plugins.DokkaHtmlConventionPlugin"
        }
        val dokkaVersioningConventionPlugin by registering {
            id = pluginLibs.plugins.dokka.convention.versioning.get().pluginId
            displayName = "Dokka Versioning convention plugin"
            description = "Simplify and streamline configuring versioning"
            implementationClass = "io.github.john.tuesday.plugins.DokkaVersioningConventionPlugin"
        }
    }
}
