plugins {
    id("convention.plugins") apply false
    id(libs.plugins.dokka.get().pluginId)
}

version = pluginLibs.versions.shared.get()

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory = rootProject.layout.projectDirectory.dir("../docs/documentation/$version")
}

val publishChildren by tasks.registering {
    group = "publishing"
    description = "publish all publications produced by subprojects to all repositories"
}
