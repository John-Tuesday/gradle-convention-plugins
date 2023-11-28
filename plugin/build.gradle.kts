plugins {
    id("convention.plugins") apply false
    id(libs.plugins.dokka.get().pluginId)
}

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory = rootProject.layout.projectDirectory.dir("../docs/documentation")
}
