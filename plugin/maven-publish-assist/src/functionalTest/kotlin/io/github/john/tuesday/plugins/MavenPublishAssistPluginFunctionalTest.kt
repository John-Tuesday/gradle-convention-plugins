package io.github.john.tuesday.plugins

import io.github.john.tuesday.plugins.helper.PgpInMemoryKeys
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.*

internal const val MAVEN_ASSIST_ID = "io.github.john-tuesday.maven-publish-assist"
internal const val ENABLE_FILTER_EXT_ENV = "ORG_GRADLE_PROJECT_${MavenPublishAssistPlugin.TestFlags.USE_FILTER_TARGET_EXT}"
internal const val SIGN_RELATES_CHECK_ENV = "ORG_GRADLE_PROJECT_${MavenPublishAssistPlugin.TestFlags.SIGN_RELATES_CHECK}"
internal const val PUBLISH_RELATES_CHECK_ENV = "ORG_GRADLE_PROJECT_${MavenPublishAssistPlugin.TestFlags.PUBLISH_RELATES_CHECK}"

internal val passOutcomes = listOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE)

private fun GradleRunner.assertSign(target: String, outcomes: List<TaskOutcome>) {
    val result = withArguments(":sign${target}Publication").build()

    val checkTask = result.task(":check")
    when (environment?.get(SIGN_RELATES_CHECK_ENV)) {
        null, MavenPublishAssistPlugin.TestFlags.RelationType.DependsOn.name -> {
            assertNotNull(checkTask)
            assertContains(passOutcomes, checkTask.outcome)
        }
        else -> assertNull(checkTask)
    }

    val signTask = result.task(":sign${target}Publication")
    assertNotNull(signTask)
    assertContains(outcomes, signTask.outcome)
}

private fun GradleRunner.assertSignExcluded(target: String) = assertSign(target, listOf(TaskOutcome.SKIPPED))

private fun GradleRunner.assertSignIncluded(target: String) = assertSign(target, passOutcomes)

private fun GradleRunner.assertLocalPublish(target: String, outcome: TaskOutcome, signOutcomes: List<TaskOutcome>) {
    val result = withArguments(":publish${target}PublicationToMavenLocal").build()

    val signTask = result.task(":sign${target}Publication")
    if (environment?.containsKey(PgpInMemoryKeys.SECRET_KEY_ENVIRONMENT) == true) {
        assertNotNull(signTask)
        assertContains(signOutcomes, signTask.outcome)
    } else
        assertNull(signTask)

    val checkTask = result.task(":check")
    when (environment?.get(PUBLISH_RELATES_CHECK_ENV)) {
        null, MavenPublishAssistPlugin.TestFlags.RelationType.DependsOn.name -> {
            assertNotNull(checkTask)
            assertContains(passOutcomes, checkTask.outcome)
        }
        else -> {
            val signCheckRelation = environment?.get(SIGN_RELATES_CHECK_ENV)
            when {
                signTask != null && (signCheckRelation == MavenPublishAssistPlugin.TestFlags.RelationType.DependsOn.name || signCheckRelation == null) -> {
                    assertNotNull(checkTask)
                    assertContains(passOutcomes, checkTask.outcome)
                }
                else -> assertNull(checkTask)
            }

        }
    }

    val publishTask = result.task(":publish${target}PublicationToMavenLocal")
    assertNotNull(publishTask)
    assertEquals(outcome, publishTask.outcome)
}

private fun GradleRunner.assertLocalPublishExcluded(target: String) = assertLocalPublish(target, TaskOutcome.SKIPPED, listOf(TaskOutcome.SKIPPED))
private fun GradleRunner.assertLocalPublishIncluded(target: String) = assertLocalPublish(target, TaskOutcome.SUCCESS, passOutcomes)

private fun GradleRunner.assertIncluded(target: String) {
    assertLocalPublishIncluded(target)
    if (environment?.containsKey(PgpInMemoryKeys.SECRET_KEY_ENVIRONMENT) == true)
        assertSignIncluded(target)
}

private fun GradleRunner.assertExcluded(target: String) {
    assertLocalPublishExcluded(target)
    if (environment?.containsKey(PgpInMemoryKeys.SECRET_KEY_ENVIRONMENT) == true)
        assertSignExcluded(target)
}

private fun GradleRunner.matrix(
    constants: Map<String, String> = environment ?: mapOf(),
    variables: Map<String, List<String?>> = mapOf(
        ENABLE_FILTER_EXT_ENV to listOf("true", "false"),
        SIGN_RELATES_CHECK_ENV to listOf(null, "None", "DependsOn", "MustRunAfter"),
        PUBLISH_RELATES_CHECK_ENV to listOf(null, "None", "DependsOn", "MustRunAfter"),
    ),
    groups: List<Map<String, String>> = listOf(mapOf(), PGP_ENV),
): Sequence<GradleRunner> {
    val variableIndices = variables
        .map { (key, opts) ->
            Triple(key, opts.size, 0)
        }
        .let { it + Triple("", groups.size, 0) }
        .toMutableList()
    return sequence {
        do {
            val variableEnv = variableIndices
                .dropLast(1)
                .mapNotNull {  (key, _, i) ->
                    variables[key]!![i]?.let { key to it }
                }
                .toMap()
                .let { it + groups[variableIndices.last().third] }
            yield(withEnvironment(constants + variableEnv))
            for (index in variableIndices.indices) {
                val old = variableIndices[index]
                variableIndices[index] = old.copy(third = (old.third + 1) % old.second)
                if (variableIndices[index].third != 0)
                    break
            }
        } while (!variableIndices.all { (_, _, i) -> i == 0 })
    }
}

internal fun GradleRunner.printEnvironment() {
    println("With environment:")
    val tag = "+-- "
    for ((key, value) in environment ?: mapOf()) {
        println("$tag'$key' : '$value'")
    }
}

class MavenPublishAssistPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    private val kotlinMultiplatformBuildFileString =
        """
            plugins {
                kotlin("multiplatform") version "${KotlinVersion.CURRENT}"
                id("$MAVEN_ASSIST_ID")
            }
            
            group = "test.group"
            version = "0.1.0"
            
            kotlin {
                jvm()
                linuxX64()
                js(IR) {
                    nodejs()
                }
            }
        """.trimIndent()
    private val kotlinMultiplatformSettingsFileString =
        """
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }
        """.trimIndent()

    private fun kmmRunner(): GradleRunner {
        settingsFile.writeText(kotlinMultiplatformSettingsFileString)
        buildFile.writeText(kotlinMultiplatformBuildFileString)

        return GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
    }

    @Test
    fun `basic single project check`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id("$MAVEN_ASSIST_ID")
            }
        """.trimIndent()
        )

        val runners = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments(":check")
            .matrix()
        for (runner in runners) {
            runner.build()
        }
    }

    @Test
    fun `exclude targets`() {
        val runners = kmmRunner()
            .matrix(constants = mapOf(FilterTargetKeys.EXCLUDE_ENVIRONMENT to "(Jvm)|(LinuxX64)"))
        for (runner in runners) {
            runner.printEnvironment()
            runner.assertIncluded("KotlinMultiplatform")
            runner.assertExcluded("Jvm")
        }
    }

    @Test
    fun `include targets`() {
        val runners = kmmRunner()
            .matrix(constants = mapOf(FilterTargetKeys.INCLUDE_ENVIRONMENT to "(Jvm)|(LinuxX64)"))
        for (runner in runners) {
            runner.printEnvironment()
            runner.assertIncluded("Jvm")
            runner.assertExcluded("KotlinMultiplatform")
        }
    }

    @Test
    fun `include and exclude prioritize excluding over including`() {
        val runners = kmmRunner()
            .matrix(
                constants = mapOf(
                    FilterTargetKeys.INCLUDE_ENVIRONMENT to "(Jvm)|(KotlinMultiplatform)",
                    FilterTargetKeys.EXCLUDE_ENVIRONMENT to "(Jvm)|(Js)",
                ),
            )
        for (runner in runners) {
            runner.printEnvironment()
            runner.assertExcluded("Jvm")
            runner.assertIncluded("KotlinMultiplatform")
            runner.assertExcluded("Js")
        }
    }
}

const val PGP_KEY = """
-----BEGIN PGP PRIVATE KEY BLOCK-----

lFgEZYBkzhYJKwYBBAHaRw8BAQdAcfCl6fuFcF4P0D14cntJ5EQCSGkTo2SaCF0G
apbbEqIAAQD4sdjpVGV5soSHO6G/cuvG8JrOcXs5P4urje7N/V58MBNZtBN0dWVz
ZGF5LW9yZy10ZXN0aW5niJkEExYKAEEWIQRuso8JY+2NM0qzlE19cRjvf1QIYgUC
ZYBkzgIbAwUJBaOagAULCQgHAgIiAgYVCgkICwIEFgIDAQIeBwIXgAAKCRB9cRjv
f1QIYslnAQCKsKjT6JLoj6o3mYQ1sC0PJolXVsQqGO6G4J2wO5K0RQD/TfH+5ooh
iA5L8fXPuTMg0ag6RqzRYMm1PQ+eN3AtjwCcXQRlgGTOEgorBgEEAZdVAQUBAQdA
ct/LVB66Xcv9RNVx+RQK7qw0yH6wHCnc1XAGBrADDjoDAQgHAAD/UvuBI+/R1GPB
j4ebbBGv30YVSHcFXlBeNhALmKcq5TgPZoh4BBgWCgAgFiEEbrKPCWPtjTNKs5RN
fXEY739UCGIFAmWAZM4CGwwACgkQfXEY739UCGIdDQD+JOdxl1eldppfQTziPIGB
cN/fsy54W2RhV4Uo3N2nU34BANikvewaOyYXfnxZb4ceqIBvTKzv4RIjSlLN9g9J
VBsA
=sTZQ
-----END PGP PRIVATE KEY BLOCK-----
"""
const val PGP_PASSWORD = ""
internal val PGP_ENV = mapOf(
    PgpInMemoryKeys.PASSWORD_ENVIRONMENT to PGP_PASSWORD,
    PgpInMemoryKeys.SECRET_KEY_ENVIRONMENT to PGP_KEY,
)
