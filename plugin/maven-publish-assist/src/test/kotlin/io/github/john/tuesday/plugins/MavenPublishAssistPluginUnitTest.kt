package io.github.john.tuesday.plugins

import io.github.john.tuesday.plugins.helper.PgpInMemoryKeys
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MavenPublishAssistPluginUnitTest {
    private val defaultProperties = mapOf (
        PgpInMemoryKeys.SECRET_KEY_PROPERTY to "",
        PgpInMemoryKeys.PASSWORD_PROPERTY to "",
 )

    private fun defaultProject(): Project {
        val project = ProjectBuilder.builder().build()
        for ((key, value) in defaultProperties)
            project.extraProperties.set(key, value)
        return project
    }

    @Test
    fun `maven quick publish plugin`() {
        val project = defaultProject()
        project.pluginManager.apply(MavenPublishAssistPlugin::class)

        val publishingExtension = project.extensions.findByType<PublishingExtension>()
        assertNotNull(publishingExtension)

        val signingExtension = project.extensions.findByType<SigningExtension>()
        assertNotNull(signingExtension)

        val publishTask = project.tasks.named("publishToMavenLocal")
        assertNotNull(publishTask.orNull)
    }

    @Test
    fun `exclude targets`() {
        val project = defaultProject()
        project.extraProperties.set(FilterTargetKeys.EXCLUDE_PROPERTY, "(Jvm)|(KotlinMultiplatform)")
        project.pluginManager.apply(KotlinMultiplatformPluginWrapper::class)
        project.pluginManager.apply(MavenPublishAssistPlugin::class)

        project.extensions.configure<KotlinMultiplatformExtension> {
            linuxX64()
            jvm()
        }

        assertFalse {
            val jvmPublishTask by project.tasks.named("publishJvmPublicationToMavenLocal")
            jvmPublishTask.enabled
        }
        assertTrue {
            val publishLinuxX64PublicationToMavenLocal by project.tasks.getting
            publishLinuxX64PublicationToMavenLocal.enabled
        }
    }

    @Test
    fun `include targets`() {
        val project = defaultProject()
        project.extraProperties.set(FilterTargetKeys.INCLUDE_PROPERTY, "(Jvm)|(KotlinMultiplatform)")
        project.pluginManager.apply(KotlinMultiplatformPluginWrapper::class)
        project.pluginManager.apply(MavenPublishAssistPlugin::class)

        project.extensions.configure<KotlinMultiplatformExtension> {
            linuxX64()
            jvm()
        }

        assertTrue {
            val jvmPublishTask by project.tasks.named("publishJvmPublicationToMavenLocal")
            jvmPublishTask.enabled
        }
        assertFalse {
            val publishLinuxX64PublicationToMavenLocal by project.tasks.getting
            publishLinuxX64PublicationToMavenLocal.enabled
        }
    }

    @Test
    fun `include and exclude targets prioritizes exclude over include`() {
        val project = defaultProject()
        project.extraProperties.set(FilterTargetKeys.EXCLUDE_PROPERTY, "(Jvm)|(KotlinMultiplatform)")
        project.extraProperties.set(FilterTargetKeys.INCLUDE_PROPERTY, "(Jvm)|(KotlinMultiplatform)")
        project.pluginManager.apply(KotlinMultiplatformPluginWrapper::class)
        project.pluginManager.apply(MavenPublishAssistPlugin::class)

        project.extensions.configure<KotlinMultiplatformExtension> {
            linuxX64()
            jvm()
        }

        assertFalse {
            val jvmPublishTask by project.tasks.named("publishJvmPublicationToMavenLocal")
            jvmPublishTask.enabled
        }
        assertFalse {
            val publishLinuxX64PublicationToMavenLocal by project.tasks.getting
            publishLinuxX64PublicationToMavenLocal.enabled
        }
    }
}
