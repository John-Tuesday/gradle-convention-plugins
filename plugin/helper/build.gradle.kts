import io.github.john.tuesday.build.logic.constants.PUBLISH_GROUP

plugins {
    id("convention.plugins")
}

group = PUBLISH_GROUP
version = pluginLibs.versions.shared.get()

gradlePlugin {
    plugins {
        val gradleHelperSettingsPlugin by registering {
            id = pluginLibs.plugins.helper.settings.get().pluginId
            implementationClass = "io.github.john.tuesday.plugins.GradleHelperSettingsPlugin"
        }

        val gradleHelperProjectPlugin by registering {
            id = pluginLibs.plugins.helper.project.get().pluginId
            implementationClass = "io.github.john.tuesday.plugins.GradleHelperProjectPlugin"
        }
    }
}
