package io.github.john.tuesday.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.*
import java.net.URL

internal val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

public interface RepositoryDocumentation {
    public val documentationBaseUrl: Property<String>
    public val reportUndocumented: Property<Boolean>
}

/**
 * Applies [DokkaPlugin] and includes "Module.md" per [DokkaTaskPartial] and per [DokkaMultiModuleTask].
 * SourceLink base directory is `./src` so [RepositoryDocumentation.documentationBaseUrl] should be `.` because `/src`
 * is automatically added.
 *
 * Output directory is set to `"docs/documentation"`, relative to the root project.
 */
public class DokkaBaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply<DokkaPlugin>()
            }

            val repositoryDocumentation = extensions.create<RepositoryDocumentation>("repositoryDocumentation").apply {
                documentationBaseUrl.convention("https://john-tuesday.github.io")
                reportUndocumented.convention(true)
            }


            tasks.withType<AbstractDokkaLeafTask>().configureEach {
                dokkaSourceSets.configureEach {
                    val moduleDoc = layout.projectDirectory.file("Module.md").asFile
                    if (moduleDoc.exists() && moduleDoc.isFile) includes.from(moduleDoc)
                    reportUndocumented = repositoryDocumentation.reportUndocumented

                    sourceLink {
                        localDirectory = layout.projectDirectory.dir("src").asFile
                        remoteUrl = repositoryDocumentation.documentationBaseUrl.map { URL("$it/src") }
                        remoteLineSuffix = "#L"
                    }
                }
            }

            val dokkaMultiModuleTask = tasks.withType<DokkaMultiModuleTask>()
            dokkaMultiModuleTask.configureEach {
                outputDirectory = rootProject.layout.projectDirectory.dir("docs/documentation")
                val moduleDoc = layout.projectDirectory.file("Module.md").asFile
                if (moduleDoc.exists() && moduleDoc.isFile) includes.from(moduleDoc)
            }
        }
    }
}

/**
 * Applies [DokkaBaseConventionPlugin] then sets each [PublishToMavenRepository] to dependsOn the rootProject's
 * dokkaHtmlMultiModule task.
 *
 * Assumes rootProject has [DokkaPlugin] applied.
 */
public class DokkaHtmlMultiModuleConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply<DokkaBaseConventionPlugin>()
            }

            val dokkaHtmlMultiModuleTask = rootProject.tasks.named<DokkaMultiModuleTask>("dokkaHtmlMultiModule")

            tasks.withType<PublishToMavenRepository>().configureEach {
                dependsOn(dokkaHtmlMultiModuleTask.get())
            }
        }
    }
}
