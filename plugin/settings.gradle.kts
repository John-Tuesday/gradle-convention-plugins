dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
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

rootProject.name = "plugin"
include(":maven-publish-assist")
include(":dokka-convention")
