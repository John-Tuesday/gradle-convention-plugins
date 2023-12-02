package io.github.john.tuesday.plugins

import io.github.john.tuesday.plugins.helper.useGpgOrInMemoryPgp
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

/**
 * Opinionated plugin to assist publishing to Sonatype maven repository
 */
public class MavenPublishAssistPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("maven-publish")
                apply<SigningPlugin>()
            }

            val javadocJar by tasks.registering(Jar::class) {
                archiveClassifier = "javadoc"
            }

            val publishing = extensions.getByType<PublishingExtension>()
            publishing.publications.withType<MavenPublication>().configureEach {
                artifact(javadocJar)
            }

            extensions.configure<SigningExtension> {
                useGpgOrInMemoryPgp(providers)
                sign(publishing.publications)
            }

            val check by tasks.existing

            val signTask = tasks.withType<Sign>()
            signTask.configureEach {
                // Must explicitly ensure sign task happens after building, compiling, linking ...
                dependsOn(check)
            }

            tasks.withType<AbstractPublishToMaven>().configureEach {
                dependsOn(check)
                // Must explicitly ensure publish happens after signing
                mustRunAfter(signTask)
            }
        }
    }
}
