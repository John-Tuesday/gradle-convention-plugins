package io.github.john.tuesday.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.BasePlugin
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

    /**
     * Default output directory for [DokkaTask] and [DokkaMultiModuleTask]
     */
    public val outputDir: DirectoryProperty

}

/**
 * Default constants used by [DokkaBaseConventionPlugin]
 */
internal data object DokkaConventionDefaults {
    /**
     * File name of a markdown module documentation file
     */
    const val MODULE_DOC_FILE_NAME: String = "Module.md"

    /**
     * File name of a markdown package documentation file
     */
    const val PACKAGE_DOC_FILE_NAME: String = "Package.md"

    /**
     * Output directory path relative to root project
     */
    const val OUTPUT_DIR_RELATIVE_PATH: String = "docs/documentation"

    /**
     * Path to source files relative to a project directory
     */
    const val SOURCE_DIR_NAME: String = "src"
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

                outputDir.convention(
                    rootProject.layout.projectDirectory
                        .dir(DokkaConventionDefaults.OUTPUT_DIR_RELATIVE_PATH)
                        .dir(provider { version.toString() })
                )
            }

            val moduleDocProvider = provider { layout.projectDirectory.file(DokkaConventionDefaults.MODULE_DOC_FILE_NAME).asFile }

            fun GradleDokkaSourceSetBuilder.configure() {
                val moduleDoc = moduleDocProvider.get()
                if (moduleDoc.exists() && moduleDoc.isFile) includes.from(moduleDoc)

                sourceLink {
                    localDirectory.convention(layout.projectDirectory.dir(DokkaConventionDefaults.SOURCE_DIR_NAME).asFile)
                    remoteUrl.convention(repositoryDocumentation.sourceBaseUrl.map { URL("$it/${DokkaConventionDefaults.SOURCE_DIR_NAME}") })
                    remoteLineSuffix.convention("#L")
                }
            }

            tasks.withType<AbstractDokkaLeafTask>().configureEach {
                dokkaSourceSets.configureEach { configure() }
            }

            val dokkaMultiModuleTask = tasks.withType<DokkaMultiModuleTask>()
            dokkaMultiModuleTask.configureEach {
                outputDirectory.convention(repositoryDocumentation.outputDir)

                val moduleDoc = moduleDocProvider.get()
                if (moduleDoc.exists() && moduleDoc.isFile) includes.from(moduleDoc)
            }

            val dokkaSingleModuleTask = tasks.withType<DokkaTask>()
            dokkaSingleModuleTask.configureEach {
                outputDirectory.convention(repositoryDocumentation.outputDir)

                dokkaSourceSets.configureEach { configure() }
            }
        }
    }
}

/**
 * Conventions including non-configurable ones
 *
 * 1. Applies [DokkaBaseConventionPlugin] and [BasePlugin]
 * 2. Configures each [AbstractDokkaTask] to [Task.dependsOn] the `check` lifecycle task
 */
public class DokkaConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply<BasePlugin>()
                apply<DokkaBaseConventionPlugin>()
            }

            val check by tasks.existing
            tasks.withType<AbstractDokkaTask>().configureEach {
                dependsOn(check)
            }
        }
    }
}

/**
 * Applies [DokkaConventionPlugin]. Configures each [AbstractDokkaTask] and sets configuration convention for
 * [DokkaBase]. Sets each [PublishToMavenRepository] to dependsOn the rootProject's dokkaHtmlMultiModule task.
 *
 * Assumes rootProject has [DokkaPlugin] applied.
 */
public class DokkaHtmlConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply<DokkaConventionPlugin>()
            }

            tasks.withType<AbstractDokkaTask>().configureEach {
                pluginsMapConfiguration.convention(
                    mapOf(DokkaBase::class.qualifiedName!! to DOKKA_BASE_CONFIGURATION_DEFAULT)
                )
            }
        }
    }

    internal companion object {
        /**
         * JSON configuration default for [DokkaBase] (html output)
         */
        internal const val DOKKA_BASE_CONFIGURATION_DEFAULT: String = """
                {
                    "separateInheritedMembers": true 
                }
                """
    }
}
