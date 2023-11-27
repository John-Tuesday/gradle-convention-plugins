package io.github.john.tuesday.plugins

import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import kotlin.test.*

class DokkaHtmlConventionPluginUnitTest {
    private lateinit var parentProject: Project
    private lateinit var project: Project

    @BeforeTest
    fun setUp() {
        parentProject = ProjectBuilder.builder().withName("parent").build()
        project = ProjectBuilder.builder().withParent(parentProject).build()

        parentProject.plugins.apply(DokkaHtmlConventionPlugin::class.java)
        project.plugins.apply(DokkaHtmlConventionPlugin::class.java)
    }

    @Test
    fun `single project test`() {
        val p = ProjectBuilder.builder().build()
        p.plugins.apply(DokkaHtmlConventionPlugin::class.java)
    }

    @Test
    fun `extensions are present`() {
        val repositoryDocumentation = project.extensions.findByType<RepositoryDocumentation>()
        assertNotNull(repositoryDocumentation)
    }

    @Test
    fun `default html configuration is set`() {
        fun AbstractDokkaTask.assertHtmlDefaultConfig() {
            val configMap = pluginsMapConfiguration.orNull
            assertNotNull(configMap)
            val dokkaBaseConfig = configMap[DokkaBase::class.qualifiedName!!]
            assertNotNull(dokkaBaseConfig)
            assertEquals(dokkaBaseConfig, DokkaHtmlConventionPlugin.DOKKA_BASE_CONFIGURATION_DEFAULT)
        }

        project.tasks.withType<AbstractDokkaTask>()
            .onEach { it.assertHtmlDefaultConfig() }
            .also { assert(it.size > 0) }
        project.rootProject.tasks.withType<AbstractDokkaTask>()
            .onEach { it.assertHtmlDefaultConfig() }
            .also { assert(it.size > 0) }
    }
}
