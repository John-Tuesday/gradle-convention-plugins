import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    `kotlin-dsl`
}

group = "io.github.john.tuesday.gradle.plugins.build-logic"

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_1_9
        languageVersion = KotlinVersion.KOTLIN_1_9
        progressiveMode = true
        jvmTarget = JvmTarget.JVM_1_8
    }
}

kotlin {
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_1_9
        languageVersion = KotlinVersion.KOTLIN_1_9
        progressiveMode = true
        explicitApi()
    }

    sourceSets {
        val main by getting {
            dependencies {
                implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")
                compileOnly(libs.kotlin.gradlePlugin)
                compileOnly(libs.kotlin.multiplatform.gradlePlugin)
            }
        }
    }
}

gradlePlugin {
    plugins {
        val pluginsConventionPlugin by registering {
            id = "convention.plugins"
            implementationClass = "PluginsConventionPlugin"
            description = "Kotlin dsl plugin development and publishing convention plugin. Applies kotlin-dsl, maven-publish, and signing"
            displayName = "Kotlin-dsl convention plugin"
        }
    }
}
