dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        gradlePluginPortal()
    }
    versionCatalogs {
        val libs by creating {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
