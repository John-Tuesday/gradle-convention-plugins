package io.github.john.tuesday.plugins

import io.github.john.tuesday.plugins.MavenPublishAssistPlugin.TestFlags.useFilterTargetExt
import io.github.john.tuesday.plugins.helper.ExperimentalProviderWithErrorMessageApi
import io.github.john.tuesday.plugins.helper.propertyOrEnvironment
import io.github.john.tuesday.plugins.helper.useGpgOrInMemoryPgp
import io.github.john.tuesday.plugins.maven.publish.assist.model.FilterTargetExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
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

    /**
     * Set Ignore case regex option property name
     *
     * "true" or "false"
     * default "true"
     */
    public const val IGNORE_CASE_PROPERTY: String = "targetFilter.ignoreCase"

    /**
     * Set Ignore case regex option environment variable name
     *
     * "true" or "false"
     * default "true"
     */
    public const val IGNORE_CASE_ENVIRONMENT: String = "TARGET_FILTER_IGNORE_CASE"
}

/**
 * Opinionated plugin to assist publishing to Sonatype maven repository
 *
 * [Sign] and [AbstractPublishToMaven] tasks can be skipped/disabled based on regex patterns.
 *
 * Property [FilterTargetKeys.EXCLUDE_PROPERTY] ("targetFilter.exclude") or environment variable
 * [FilterTargetKeys.EXCLUDE_ENVIRONMENT] ("TARGET_FILTER_EXCLUDE") can specify a regex pattern of which [Sign] and
 * [AbstractPublishToMaven] tasks with a matching target are skipped/disabled
 *
 * Property [FilterTargetKeys.INCLUDE_PROPERTY] ("targetFilter.include") or environment variable
 * [FilterTargetKeys.INCLUDE_ENVIRONMENT] ("TARGET_FILTER_INCLUDE") can specify a regex pattern of which [Sign] and
 * [AbstractPublishToMaven] tasks without a matching target are skipped/disabled
 *
 * Property [FilterTargetKeys.IGNORE_CASE_PROPERTY] ("targetFilter.ignoreCase") or environment variable
 * [FilterTargetKeys.IGNORE_CASE_ENVIRONMENT] ("TARGET_FILTER_IGNORE_CASE") toggles case sensitivity. Can be "true" or
 * "false" and the default is "true"
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

            val filterTargetExtension = extensions.create<FilterTargetExtension>(FilterTargetExtension.Default.NAME)
            filterTargetExtension.init(target)

            val useFilterTarget = useFilterTargetExt
            setSignTaskRelations()
            setPublishTaskRelations()
            if (useFilterTarget.get()) {
                logger.info("Using ${FilterTargetExtension::class.simpleName}")
                filterTargets(filterTargetExtension)
            }

            val ignoreCase = propertyOrEnvironment(
                propertyKey = FilterTargetKeys.IGNORE_CASE_PROPERTY,
                environmentKey = FilterTargetKeys.IGNORE_CASE_ENVIRONMENT,
            ).orElse("true").map { if (it == "false") setOf() else setOf(RegexOption.IGNORE_CASE) }
            val shouldExclude = propertyOrEnvironment(
                propertyKey = FilterTargetKeys.EXCLUDE_PROPERTY,
                environmentKey = FilterTargetKeys.EXCLUDE_ENVIRONMENT,
            ).zip(ignoreCase) { pattern, options -> Regex(pattern, options) }
            val shouldInclude = propertyOrEnvironment(
                propertyKey = FilterTargetKeys.INCLUDE_PROPERTY,
                environmentKey = FilterTargetKeys.INCLUDE_ENVIRONMENT,
            ).zip(ignoreCase) { pattern, options -> Regex(pattern, options) }

            fun shouldRun(targetName: String) = provider {
                val includeMatched = shouldInclude.map { it.matches(targetName) }.getOrElse(true)
                val excludeMatched = shouldExclude.map { it.matches(targetName) }.getOrElse(false)
                includeMatched && !excludeMatched
            }

            if (!useFilterTarget.get()) {
                logger.info("Using old target filter")
                val check by tasks.existing

                val signTask = tasks.withType<Sign>()
                signTask.configureEach {
                    val targetName = name.substringAfter("sign").substringBefore("Publication")
                    val shouldRun = shouldRun(targetName)
                    enabled = shouldRun.get()
                    onlyIf { shouldRun.get() }
                }
                tasks.withType<AbstractPublishToMaven>().configureEach {
                    val targetName = name.substringAfter("publish").substringBefore("Publication")
                    val shouldRun = shouldRun(targetName)
                    enabled = shouldRun.get()
                    onlyIf { shouldRun.get() }
                }
            }
        }
    }

    internal data object TestFlags {
        const val USE_FILTER_TARGET_EXT: String = "useFilterTargetExt"

        val Project.useFilterTargetExt: Provider<Boolean>
            get() = provider {
                if (hasProperty(USE_FILTER_TARGET_EXT))
                    property(USE_FILTER_TARGET_EXT).toString() == "true"
                else
                    false
            }

        const val PUBLISH_RELATES_CHECK: String = "publishRelatesCheck"

        enum class RelationType {
            DependsOn,
            MustRunAfter,
            None,
        }

        val Project.publishRelatesToCheck: Provider<RelationType>
            get() = provider {
                if (hasProperty(PUBLISH_RELATES_CHECK)) {
                    val value = property(PUBLISH_RELATES_CHECK).toString()
                    RelationType.entries.firstOrNull { it.name.lowercase() == value.lowercase() }
                        ?: RelationType.DependsOn
                } else
                    RelationType.DependsOn
            }

        const val SIGN_RELATES_CHECK: String = "signRelatesCheck"
        val Project.signRelatesToCheck: Provider<RelationType>
            get() = provider {
                if (hasProperty(SIGN_RELATES_CHECK)) {
                    val value = property(SIGN_RELATES_CHECK).toString()
                    RelationType.entries.firstOrNull { it.name.lowercase() == value.lowercase() }
                        ?: RelationType.DependsOn
                } else
                    RelationType.DependsOn
            }
    }
}

internal fun Project.setSignTaskRelations() {
    val check by tasks.existing
    tasks.withType<Sign>().configureEach {
        with(MavenPublishAssistPlugin.TestFlags) {
            // Must explicitly ensure sign task happens after building, compiling, linking ...
            when (signRelatesToCheck.get()) {
                MavenPublishAssistPlugin.TestFlags.RelationType.DependsOn -> dependsOn(check)
                MavenPublishAssistPlugin.TestFlags.RelationType.MustRunAfter -> mustRunAfter(check)
                MavenPublishAssistPlugin.TestFlags.RelationType.None -> Unit
            }
        }
    }
}

internal fun Project.setPublishTaskRelations() {
    val check by tasks.existing
    val signTasks = tasks.withType<Sign>()
    tasks.withType<AbstractPublishToMaven>().configureEach {
        with(MavenPublishAssistPlugin.TestFlags) {
            when (publishRelatesToCheck.get()) {
                MavenPublishAssistPlugin.TestFlags.RelationType.DependsOn -> dependsOn(check)
                MavenPublishAssistPlugin.TestFlags.RelationType.MustRunAfter -> mustRunAfter(check)
                MavenPublishAssistPlugin.TestFlags.RelationType.None -> Unit
            }
        }
        // Must explicitly ensure publish happens after signing
        mustRunAfter(signTasks)
    }
}

internal fun Project.filterTargets(filterTargetExtension: FilterTargetExtension) {
    tasks.withType<Sign>().configureEach {
        filterTargets(filterTargetExtension)
    }
    tasks.withType<AbstractPublishToMaven>().configureEach {
        filterTargets(filterTargetExtension)
    }
}

internal fun Sign.filterTargets(filterTargetExtension: FilterTargetExtension) {
    val targetName = name.substringAfter("sign").substringBefore("Publication")
    val shouldRun = filterTargetExtension.shouldRun(targetName)
    enabled = shouldRun.get()
    onlyIf { shouldRun.get() }
}

internal fun AbstractPublishToMaven.filterTargets(filterTargetExtension: FilterTargetExtension) {
    val targetName = name.substringAfter("publish").substringBefore("Publication")
    val shouldRun = filterTargetExtension.shouldRun(targetName)
    enabled = shouldRun.get()
    onlyIf { shouldRun.get() }
}
