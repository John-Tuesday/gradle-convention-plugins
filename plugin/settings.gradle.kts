dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
pluginManagement {
    includeBuild("../build-logic")
}

rootProject.name = "plugin"
