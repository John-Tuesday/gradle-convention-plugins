package io.github.john.tuesday.plugins

import org.gradle.kotlin.dsl.findByType
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class DokkaBaseConventionPluginUnitTest {
    @Test
    fun `apply plugin by type`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(DokkaBaseConventionPlugin::class.java)

        val repositoryDocumentation = project.extensions.findByType<RepositoryDocumentation>()
        assertNotNull(repositoryDocumentation)
    }
}
