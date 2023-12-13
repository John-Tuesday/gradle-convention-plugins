package io.github.john.tuesday.plugins

import io.github.john.tuesday.plugins.DokkaBaseConventionPlugin.Companion.defaultSourceLinkLocalDirectory
import io.github.john.tuesday.plugins.DokkaBaseConventionPlugin.Companion.remoteUrl
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.GradleSourceLinkBuilder
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private fun Project.assertDefaultSourceLinks(repositoryDocumentation: RepositoryDocumentation) {
    fun GradleSourceLinkBuilder.assertDefaults() {
        assertTrue(localDirectory.isPresent)
        assertEquals(project.defaultSourceLinkLocalDirectory().get(), localDirectory.get())
        assertTrue(remoteUrl.isPresent)
        assertEquals(URL(repositoryDocumentation.remoteUrl.get()), remoteUrl.get())
        assertTrue(remoteLineSuffix.isPresent)
        assertEquals(DokkaConventionDefaults.REMOTE_LINE_SUFFIX, remoteLineSuffix.get())
    }

    project.tasks.withType<AbstractDokkaLeafTask>().onEach { task ->
        task.dokkaSourceSets.onEach {  sourceSet ->
            sourceSet.sourceLinks.get().onEach {
                it.assertDefaults()
            }
        }
    }

    project.tasks.withType<DokkaTask>().onEach { task ->
        task.dokkaSourceSets.onEach {  sourceSet ->
            sourceSet.sourceLinks.get().onEach {
                it.assertDefaults()
            }
        }
    }
}

private fun Project.assertDefaultOutputDirectoryMultiModuleDokkaTasks(repositoryDocumentation: RepositoryDocumentation) {
    val dokkaTasks = project.tasks.withType<DokkaMultiModuleTask>()
    assertTrue {
        dokkaTasks.size > 0
    }
    for (task in dokkaTasks) {
        assertTrue {
            task.outputDirectory.isPresent
        }
        assertEquals(repositoryDocumentation.outputDir.get(), task.outputDirectory.get())
    }
}

private fun Project.assertDefaultOutputDirectorySingleModuleDokkaTasks(repositoryDocumentation: RepositoryDocumentation) {
    val dokkaTasks = project.tasks.withType<DokkaTask>()
    assertTrue {
        dokkaTasks.size > 0
    }
    for (task in dokkaTasks) {
        assertTrue {
            task.outputDirectory.isPresent
        }
        assertEquals(repositoryDocumentation.outputDir.get(), task.outputDirectory.get())
    }
}

private fun Project.assertDefaultRepositoryDocumentation(): RepositoryDocumentation {
    val repositoryDocumentation = project.extensions.findByType<RepositoryDocumentation>()
    assertNotNull(repositoryDocumentation)
    assertEquals(
        repositoryDocumentation,
        project.extensions.getByName(DokkaBaseConventionPlugin.REPOSITORY_DOCUMENTATION_NAME)
    )
    return repositoryDocumentation
}

class DokkaBaseConventionPluginUnitTest {
    @Test
    fun `single project defaults`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(DokkaBaseConventionPlugin::class)

        val repositoryDocumentation = project.assertDefaultRepositoryDocumentation()

        project.assertDefaultOutputDirectorySingleModuleDokkaTasks(repositoryDocumentation)
        project.assertDefaultSourceLinks(repositoryDocumentation)
    }

    @Test
    fun `multi project defaults`() {
        val parent = ProjectBuilder.builder().build()
        val child = ProjectBuilder.builder().withParent(parent).build()
        parent.pluginManager.apply(DokkaBaseConventionPlugin::class)
        child.pluginManager.apply(DokkaBaseConventionPlugin::class)

        with(parent) {
            val repositoryDocumentation = assertDefaultRepositoryDocumentation()
            assertDefaultOutputDirectorySingleModuleDokkaTasks(repositoryDocumentation)
            assertDefaultOutputDirectoryMultiModuleDokkaTasks(repositoryDocumentation)
            assertDefaultSourceLinks(repositoryDocumentation)
        }
        with(child) {
            val repositoryDocumentation = assertDefaultRepositoryDocumentation()
            assertDefaultOutputDirectorySingleModuleDokkaTasks(repositoryDocumentation)
            assertDefaultSourceLinks(repositoryDocumentation)
        }
    }
}
