dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        google()
    }
    versionCatalogs {
        val pluginLibs by creating {
            from(files("plugin/plugin.libs.versions.toml"))
        }
    }
}
pluginManagement {
    includeBuild("build-logic")
    includeBuild("plugin")
}
rootProject.name = "gradle-convention-plugins"
include(":sample")


