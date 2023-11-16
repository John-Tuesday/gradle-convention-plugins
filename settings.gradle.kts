dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
pluginManagement {
    includeBuild("build-logic")
    includeBuild("plugin")
}
rootProject.name = "gradle-convention-plugins"
include(":sample")


