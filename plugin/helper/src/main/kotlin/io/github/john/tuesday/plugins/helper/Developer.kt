package io.github.john.tuesday.plugins.helper

import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import org.gradle.kotlin.dsl.assign

/**
 * Add John Tuesday's developer information
 */
public fun MavenPomDeveloperSpec.johnTuesday() {
    developer {
        id = "John-Tuesday"
        name = "John Tuesday"
        email = "calamarfederal.messyink@gmail.com"
    }
}
