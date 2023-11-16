plugins {
    alias(libs.plugins.kotlin.multiplatform)
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

