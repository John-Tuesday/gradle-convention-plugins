dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        val libs by creating {
            from(files("../gradle/libs.versions.toml"))
        }
        val pluginLibs by creating {
            from(files("plugin.libs.versions.toml"))
        }
    }
}
pluginManagement {
    includeBuild("../build-logic")
}

plugins {
    id("build-logic.constants")
}

rootProject.name = "plugin"
include(":maven-publish-assist")
include(":dokka-convention")
include(":helper")
