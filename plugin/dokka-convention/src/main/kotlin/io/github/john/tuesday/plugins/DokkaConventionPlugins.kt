package io.github.john.tuesday.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.gradle.*
import java.net.URL

/**
 * Configuration options for [DokkaBaseConventionPlugin]
 */
public interface RepositoryDocumentation {
    /**
     * URL of the source code used in source link generation.
     */
    public val sourceBaseUrl: Property<String>
}

/**
 * Applies [DokkaPlugin] and includes "Module.md" per [DokkaTaskPartial] and per [DokkaMultiModuleTask].
 * SourceLink base directory is `./src` so [RepositoryDocumentation.sourceBaseUrl] should be `.` because `/src`
 * is automatically added.
 *
 * Source base url default is
 *
 *     "https://github.com/John-Tuesday/${rootProject.name}/tree/main${project.path.replace(':', '/')}"
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
                sourceBaseUrl.convention(
                    "https://github.com/John-Tuesday/${rootProject.name}/tree/main${
                        project.path.replace(':', '/')
                    }"
                )
            }


            tasks.withType<AbstractDokkaLeafTask>().configureEach {
                dokkaSourceSets.configureEach {
                    val moduleDoc = layout.projectDirectory.file("Module.md").asFile
                    if (moduleDoc.exists() && moduleDoc.isFile) includes.from(moduleDoc)

                    sourceLink {
                        localDirectory.convention(layout.projectDirectory.dir("src").asFile)
                        remoteUrl.convention(repositoryDocumentation.sourceBaseUrl.map { URL("$it/src") })
                        remoteLineSuffix.convention("#L")
                    }
                }
            }

            val dokkaMultiModuleTask = tasks.withType<DokkaMultiModuleTask>()
            dokkaMultiModuleTask.configureEach {
                outputDirectory.convention(rootProject.layout.projectDirectory.dir("docs/documentation"))
                val moduleDoc = layout.projectDirectory.file("Module.md").asFile
                if (moduleDoc.exists() && moduleDoc.isFile) includes.from(moduleDoc)
            }
        }
    }
}

/**
 * Applies [DokkaBaseConventionPlugin]. Configures each [AbstractDokkaTask] and sets configuration convention for
 * [DokkaBase]. Sets each [PublishToMavenRepository] to dependsOn the rootProject's dokkaHtmlMultiModule task.
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

            tasks.withType<AbstractDokkaTask>().configureEach {
                pluginsMapConfiguration.convention(
                    mapOf(DokkaBase::class.qualifiedName!! to DOKKA_BASE_CONFIGURATION_DEFAULT)
                )
            }

            tasks.withType<PublishToMavenRepository>().configureEach {
                dependsOn(dokkaHtmlMultiModuleTask.get())
            }
        }
    }

    internal companion object {
        internal const val DOKKA_BASE_CONFIGURATION_DEFAULT: String = """
                {
                    "separateInheritedMembers": true 
                }
                """
    }
}
