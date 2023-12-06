package io.github.john.tuesday.plugins

import io.github.john.tuesday.plugins.helper.ExperimentalProviderWithErrorMessageApi
import io.github.john.tuesday.plugins.helper.propertyOrEnvironment
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
 * Property and environment variable names for configuring target filtering
 */
public data object FilterTargetKeys {
    /**
     * Exclude targets property name
     */
    public const val EXCLUDE_PROPERTY: String = "targetFilter.exclude"

    /**
     * Exclude targets environment variable name
     */
    public const val EXCLUDE_ENVIRONMENT: String = "TARGET_FILTER_EXCLUDE"

    /**
     * Include targets property name
     */
    public const val INCLUDE_PROPERTY: String = "targetFilter.include"

    /**
     * Include targets environment variable name
     */
    public const val INCLUDE_ENVIRONMENT: String = "TARGET_FILTER_INCLUDE"
}

/**
 * Opinionated plugin to assist publishing to Sonatype maven repository
 *
 * [Sign] and [AbstractPublishToMaven] tasks can be skipped/disabled based on regex patterns.
 * Property [FilterTargetKeys.EXCLUDE_PROPERTY] ("targetFilter.exclude") or environment variable
 * [FilterTargetKeys.EXCLUDE_ENVIRONMENT] ("TARGET_FILTER_EXCLUDE") can specify a regex pattern of which [Sign] and
 * [AbstractPublishToMaven] tasks with a matching target are skipped/disabled
 *
 * Property [FilterTargetKeys.INCLUDE_PROPERTY] ("targetFilter.include") or environment variable
 * [FilterTargetKeys.INCLUDE_ENVIRONMENT] ("TARGET_FILTER_INCLUDE") can specify a regex pattern of which [Sign] and
 * [AbstractPublishToMaven] tasks without a matching target are skipped/disabled
 */
public class MavenPublishAssistPlugin : Plugin<Project> {
    @OptIn(ExperimentalProviderWithErrorMessageApi::class)
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
                if (useGpgOrInMemoryPgp())
                    sign(publishing.publications)
            }

            val shouldExclude = propertyOrEnvironment(
                propertyKey = FilterTargetKeys.EXCLUDE_PROPERTY,
                environmentKey = FilterTargetKeys.EXCLUDE_ENVIRONMENT,
            ).map { Regex(it) }
            val shouldInclude = propertyOrEnvironment(
                propertyKey = FilterTargetKeys.INCLUDE_PROPERTY,
                environmentKey = FilterTargetKeys.INCLUDE_ENVIRONMENT,
            ).map { Regex(it) }

            fun shouldRun(targetName: String) = provider {
                val includeMatched = shouldInclude.map { it.matches(targetName) }.getOrElse(true)
                val excludeMatched = shouldExclude.map { it.matches(targetName) }.getOrElse(false)
                includeMatched && !excludeMatched
            }

            val check by tasks.existing

            val signTask = tasks.withType<Sign>()
            signTask.configureEach {
                // Must explicitly ensure sign task happens after building, compiling, linking ...
                dependsOn(check)

                val targetName = name.substringAfter("sign").substringBefore("Publication")
                val shouldRun = shouldRun(targetName)
                enabled = shouldRun.get()
                onlyIf { shouldRun.get() }
            }

            tasks.withType<AbstractPublishToMaven>().configureEach {
                dependsOn(check)
                // Must explicitly ensure publish happens after signing
                mustRunAfter(signTask)

                val targetName = name.substringAfter("publish").substringBefore("Publication")
                val shouldRun = shouldRun(targetName)
                enabled = shouldRun.get()
                onlyIf { shouldRun.get() }
            }
        }
    }
}
