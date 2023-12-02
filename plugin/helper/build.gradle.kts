import io.github.john.tuesday.build.logic.constants.PUBLISH_GROUP

plugins {
    id("convention.plugins")
}

group = PUBLISH_GROUP
version = pluginLibs.versions.mavenPublishAssist.get()

gradlePlugin {
    plugins {
        val gradleHelperSettingsPlugin by registering {
            id = "$PUBLISH_GROUP.helper"
            implementationClass = "io.github.john.tuesday.plugins.GradleHelperSettingsPlugin"
        }

        val gradleHelperProjectPlugin by registering {
            id = "$PUBLISH_GROUP.helper.project"
            implementationClass = "io.github.john.tuesday.plugins.GradleHelperProjectPlugin"
        }
    }
}
