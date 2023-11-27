package io.github.john.tuesday.plugins

import org.gradle.kotlin.dsl.findByType
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class DokkaHtmlMultiModuleConventionPluginUnitTest {
    @Test
    fun `apply by type`() {
        val rootProject = ProjectBuilder.builder().withName("parent").build()
        val project = ProjectBuilder.builder().withParent(rootProject).build()
        rootProject.plugins.apply(DokkaHtmlMultiModuleConventionPlugin::class.java)
        project.plugins.apply(DokkaHtmlMultiModuleConventionPlugin::class.java)

        val repositoryDocumentation = project.extensions.findByType<RepositoryDocumentation>()
        assertNotNull(repositoryDocumentation)
    }
}
