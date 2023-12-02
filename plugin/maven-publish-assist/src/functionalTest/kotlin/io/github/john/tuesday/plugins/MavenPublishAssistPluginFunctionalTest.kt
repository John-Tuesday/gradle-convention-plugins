package io.github.john.tuesday.plugins

import io.github.john.tuesday.plugins.helper.PgpInMemoryKeys
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class MavenPublishAssistPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

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
}
