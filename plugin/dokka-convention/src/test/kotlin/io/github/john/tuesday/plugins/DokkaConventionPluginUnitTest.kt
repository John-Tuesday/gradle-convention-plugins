package io.github.john.tuesday.plugins

import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertTrue

class DokkaConventionPluginUnitTest {
    @Test
    fun `apply plugin by type`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(DokkaConventionPlugin::class)

        assertTrue("Could not find plugin ${DokkaBaseConventionPlugin::class.qualifiedName}") {
            project.plugins.hasPlugin(DokkaBaseConventionPlugin::class)
        }
    }
}
