plugins {
    id("convention.plugins")
}

group = "io.github.john-tuesday"
version = "0.0.0-SNAPSHOT"

kotlin {
    sourceSets {
        val main by getting {
            dependencies {
                implementation(libs.dokka.gradlePlugin)
            }
        }
    }
}

gradlePlugin {
    plugins {
        val dokkaConventionPlugin by registering {
            id = "io.github.john-tuesday.dokka-base-convention"
            displayName = "Dokka base convention plugin"
            description = "Simplify and streamline generating documentation"
            implementationClass = "io.github.john.tuesday.plugins.DokkaBaseConventionPlugin"
        }
        val dokkaHtmlMultiModuleConventionPlugin by registering {
            id = "io.github.john-tuesday.dokka-html"
            displayName = "Dokka html convention plugin"
            description = "Simplify and streamline generating multi-module html documentation"
            implementationClass = "io.github.john.tuesday.plugins.DokkaHtmlMultiModuleConventionPlugin"
        }
    }
}
