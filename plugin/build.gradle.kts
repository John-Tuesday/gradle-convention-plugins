import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin

plugins {
    id("convention.plugins") apply false
    id(libs.plugins.dokka.get().pluginId)
    base
}

dependencies {
    dokkaPlugin(libs.dokka.version.plugin)
}

version = pluginLibs.versions.shared.get()

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory = rootProject.layout.projectDirectory.dir("../docs/documentation/$version")

    pluginConfiguration<VersioningPlugin, VersioningConfiguration> {
        version = pluginLibs.versions.shared.get()
        olderVersionsDir = rootProject.layout.projectDirectory.dir("../docs/documentation/$version").asFile
    }
}

val publishChildren by tasks.registering {
    group = "publishing"
    description = "publish all publications produced by subprojects to all repositories"

    dependsOn(tasks.check)
}
