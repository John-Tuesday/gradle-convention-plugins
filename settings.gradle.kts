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
include(":sample")


