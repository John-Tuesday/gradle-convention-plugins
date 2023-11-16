dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
pluginManagement {
    includeBuild("plugin")
}
rootProject.name = "gradle-convention-plugins"
includeBuild("build-logic")
include(":sample")


