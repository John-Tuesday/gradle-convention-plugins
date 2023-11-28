package io.github.john.tuesday.plugins

import io.github.john.tuesday.plugins.publishing.model.withError
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
                val gpgKeyName = providers.gradleProperty("signing.gnupg.keyName")
                val gpgPassphrase = providers.gradleProperty("signing.gnupg.passphrase")
                if (gpgKeyName.isPresent && gpgPassphrase.isPresent)
                    useGpgCmd()
                else {
                    val keyId = providers
                        .gradleProperty("signing.keyId")
                        .orElse(providers.environmentVariable("GPG_SIGNING_KEY_ID"))
                    val password = providers
                        .gradleProperty("signing.password")
                        .orElse(providers.environmentVariable("GPG_SIGNING_PASSWORD"))
                        .withError("Expected to find property 'signing.password' or environment variable 'GPG_SIGNING_PASSWORD' ")
                    val key = providers
                        .gradleProperty("GPG_SECRET_KEY")
                        .orElse(providers.environmentVariable("GPG_SECRET_KEY"))
                        .withError("Expected to find property 'GPG_SECRET_KEY' or environment variable 'GPG_SECRET_KEY' ")
                    if (keyId.isPresent)
                        useInMemoryPgpKeys(keyId.get(), key.getOrError(), password.getOrError())
                    else
                        useInMemoryPgpKeys(key.getOrError(), password.getOrError())
                }
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
