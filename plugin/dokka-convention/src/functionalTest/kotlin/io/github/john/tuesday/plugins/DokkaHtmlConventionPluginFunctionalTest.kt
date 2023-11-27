package io.github.john.tuesday.plugins

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class DokkaHtmlConventionPluginFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val rootBuildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }


    private val childProjectDir by lazy { projectDir.resolve("child") }
    private val childBuildFile by lazy { childProjectDir.resolve("build.gradle.kts") }

    @Test
    fun `basic multi-project apply`() {
        settingsFile.writeText("""
            rootProject.name = "parent"
            include(":child")
        """.trimIndent())
        rootBuildFile.writeText(
            """
            plugins {
                id("io.github.john-tuesday.dokka-html")
            }
        """.trimIndent()
        )
        childProjectDir.mkdir()
        childBuildFile.writeText(
            """
            plugins {
                id("io.github.john-tuesday.dokka-html")
            }
        """.trimIndent()
        )

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withProjectDir(projectDir)
        val result = runner.build()

        println(result.output)
    }
}
