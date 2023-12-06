package io.github.john.tuesday.plugins

import io.github.john.tuesday.plugins.helper.PgpInMemoryKeys
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MavenPublishAssistPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    private val kotlinMultiplatformBuildFileString =
        """
            plugins {
                kotlin("multiplatform") version "1.9.21"
                id("io.github.john-tuesday.maven-publish-assist")
            }
            
            kotlin {
                jvm()
                linuxX64()
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
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(projectDir)
    }

    @Test
    fun `maven quick`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id("io.github.john-tuesday.maven-publish-assist")
            }
        """.trimIndent()
        )

        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withEnvironment(
                mapOf(
                    PgpInMemoryKeys.SECRET_KEY_ENVIRONMENT to "",
                    PgpInMemoryKeys.PASSWORD_ENVIRONMENT to "",
                )
            )
            .withArguments(":check")
        val result = runner.build()
    }

    @Test
    fun `exclude targets`() {
        val runner = kmmRunner()
            .withEnvironment(
                mapOf(
                    PgpInMemoryKeys.SECRET_KEY_ENVIRONMENT to "",
                    PgpInMemoryKeys.PASSWORD_ENVIRONMENT to "",
                    FilterTargetKeys.EXCLUDE_ENVIRONMENT to "(Jvm)|(LinuxX64)",
                )
            )
            .withArguments(":publishJvmPublicationToMavenLocal")
        val result = runner.build()

        val signJvmTask = result.task(":signJvmPublication")
        assertNotNull(signJvmTask)
        assertEquals(TaskOutcome.SKIPPED, signJvmTask.outcome)

        val publishJvmTask = result.task(":publishJvmPublicationToMavenLocal")
        assertNotNull(publishJvmTask)
        assertEquals(TaskOutcome.SKIPPED, publishJvmTask.outcome)

        val result2 = runner
            .withArguments(":publishKotlinMultiplatformPublicationToMavenLocal", "--continue", "--exclude-task", ":signKotlinMultiplatformPublication")
            .buildAndFail()

        val signKmpTask = result2.task(":signKotlinMultiplatformPublication")
        assertNull(signKmpTask)

        val publishKmpTask = result2.task(":publishKotlinMultiplatformPublicationToMavenLocal")
        assertNotNull(publishKmpTask)
        assertEquals(TaskOutcome.FAILED, publishKmpTask.outcome)
    }

    @Test
    fun `include targets`() {
        val runner = kmmRunner()
            .withEnvironment(
                mapOf(
                    PgpInMemoryKeys.SECRET_KEY_ENVIRONMENT to "",
                    PgpInMemoryKeys.PASSWORD_ENVIRONMENT to "",
                    FilterTargetKeys.INCLUDE_ENVIRONMENT to "(Jvm)|(LinuxX64)",
                )
            )
            .withArguments(":publishJvmPublicationToMavenLocal", "--continue", "--exclude-task", ":signJvmPublication")
        val result = runner.buildAndFail()

        val signJvmTask = result.task(":signJvmPublication")
        assertNull(signJvmTask)

        val publishJvmTask = result.task(":publishJvmPublicationToMavenLocal")
        assertNotNull(publishJvmTask)
        assertEquals(TaskOutcome.FAILED, publishJvmTask.outcome)

        val result2 = runner
            .withArguments(":publishKotlinMultiplatformPublicationToMavenLocal")
            .build()

        val signKmpTask = result2.task(":signKotlinMultiplatformPublication")
        assertNotNull(signKmpTask)
        assertEquals(TaskOutcome.SKIPPED, signKmpTask.outcome)

        val publishKmpTask = result2.task(":publishKotlinMultiplatformPublicationToMavenLocal")
        assertNotNull(publishKmpTask)
        assertEquals(TaskOutcome.SKIPPED, publishKmpTask.outcome)
    }
}
