dependencyResolutionManagement {
    includeBuild("plugin")
    repositories {
        mavenCentral()
        google()
    }
}
rootProject.name = "gradle-convention-plugins"
include(":sample")


