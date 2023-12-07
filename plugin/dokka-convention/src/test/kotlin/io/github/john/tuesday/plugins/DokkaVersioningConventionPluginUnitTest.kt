package io.github.john.tuesday.plugins

import io.github.john.tuesday.plugins.DokkaVersioningConventionPlugin.Companion.versioningMapConfiguration
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaVersion
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.versioning.VersioningPlugin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun Project.assertVersioningDependency() {
    val config = project.configurations.named("dokkaPlugin").get()
    assertTrue("Could not find 'org.jetbrains.dokka:versioning-plugin:${DokkaVersion.version}'") {
        config.allDependencies.any {
            it.group == "org.jetbrains.dokka" && it.name == "versioning-plugin" && it.version == DokkaVersion.version
        }
    }
}

private fun Project.assertPluginConfiguration() {
    val tasks = project.tasks.withType<AbstractDokkaTask>()
    assertTrue("Expected at least 1 task of type ${AbstractDokkaTask::class.simpleName}") {
        tasks.size > 0
    }

    for (it in tasks) {
        val key = VersioningPlugin::class.qualifiedName!!
        val versioningConfig = it.pluginsMapConfiguration.getting(key)
        assertTrue("Expected pluginsMapConfiguration to have item defined at key '$key'") {
            versioningConfig.isPresent
        }
        assertEquals(project.versioningMapConfiguration().get()[key]!!, versioningConfig.get())
    }
}

class DokkaVersioningConventionPluginUnitTest {
    @Test
    fun `single project apply`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(DokkaVersioningConventionPlugin::class)

        project.assertVersioningDependency()
        project.assertPluginConfiguration()
    }

    @Test
    fun `multi project apply`() {
        val parent = ProjectBuilder.builder().withName("parent").build()
        val childA = ProjectBuilder.builder().withParent(parent).build()
        parent.pluginManager.apply(DokkaVersioningConventionPlugin::class)
        childA.pluginManager.apply(DokkaVersioningConventionPlugin::class)

        parent.assertVersioningDependency()
        childA.assertVersioningDependency()
        parent.assertPluginConfiguration()
        childA.assertPluginConfiguration()
    }
}
