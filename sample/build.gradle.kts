plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("io.github.john-tuesday.maven-publish-assist")
}

kotlin {
    applyDefaultHierarchyTemplate()
    linuxX64()
    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }
    }
}

