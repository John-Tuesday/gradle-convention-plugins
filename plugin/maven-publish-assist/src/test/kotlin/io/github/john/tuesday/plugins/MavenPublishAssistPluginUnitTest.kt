package io.github.john.tuesday.plugins

import io.github.john.tuesday.plugins.helper.PgpInMemoryKeys
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.findByType
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class MavenPublishAssistPluginUnitTest {
    @Test
    fun `maven quick publish plugin`() {
        System.setProperty(PgpInMemoryKeys.SECRET_KEY_PROPERTY, "")
        System.setProperty(PgpInMemoryKeys.PASSWORD_PROPERTY, "")
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.john-tuesday.maven-publish-assist")

        val publishingExtension = project.extensions.findByType<PublishingExtension>()
        assertNotNull(publishingExtension)

        val signingExtension = project.extensions.findByType<SigningExtension>()
        assertNotNull(signingExtension)

        val publishTask = project.tasks.named("publishToMavenLocal")
        assertNotNull(publishTask.orNull)
    }
}
