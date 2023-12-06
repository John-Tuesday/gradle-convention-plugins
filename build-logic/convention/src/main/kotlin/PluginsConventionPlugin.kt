import io.github.john.tuesday.build.logic.constants.GitHubPackages
import io.github.john.tuesday.build.logic.constants.SonatypeStaging
import io.github.john.tuesday.build.logic.constants.usePreset
import io.github.john.tuesday.build.logic.helpers.useGpgOrInMemoryPgp
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.TestSuiteType
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.plugins.dsl.KotlinDslPlugin
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.gradle.testing.base.TestingExtension
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

public data object Versions {
    public val java: JavaVersion = JavaVersion.VERSION_1_8
    public val jvmTarget: JvmTarget = JvmTarget.JVM_1_8
    public val kotlin: KotlinVersion = KotlinVersion.KOTLIN_1_9
}

public class PluginsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply<KotlinDslPlugin>()
                apply("maven-publish")
                apply<SigningPlugin>()
                apply<DokkaPlugin>()
            }
            val compileOnly by configurations.existing {
                defaultDependencies {
                    addLater(libs.findLibrary("kotlin-gradlePlugin").get())
                    addLater(libs.findLibrary("kotlin-multiplatform-gradlePlugin").get())
                }
            }
            val testImplementation by configurations.getting {
                defaultDependencies {
                    addLater(libs.findLibrary("kotlin-gradlePlugin").get())
                    addLater(libs.findLibrary("kotlin-multiplatform-gradlePlugin").get())
                }
            }

            val kotlinExtension = extensions.getByType<KotlinJvmProjectExtension>()
            kotlinExtension.apply {
                compilerOptions {
                    explicitApi()
                }

                jvmToolchain {
                    languageVersion = JavaLanguageVersion.of(Versions.java.majorVersion)
                }
            }
            val mainCompilation = kotlinExtension.target.compilations.named("main")

            val javadocJar by tasks.registering(Jar::class) {
                archiveClassifier = "javadoc"
            }

            val publishingExtension = extensions.getByType<PublishingExtension>()
            with(publishingExtension) {
                repositories {
                    maven {
                        usePreset(SonatypeStaging, providers)
                    }
                    maven {
                        usePreset(GitHubPackages, providers)
                    }
                }
            }

            publishingExtension.publications.withType<MavenPublication>().configureEach {
                artifact(javadocJar)
            }

            val signingExtension = extensions.getByType<SigningExtension>()
            signingExtension.apply {
                useGpgOrInMemoryPgp(providers)
                sign(publishingExtension.publications)
            }

            val testingExtension = extensions.getByType<TestingExtension>()
            val testSuite = testingExtension.suites.named<JvmTestSuite>("test")
            testSuite.configure {
                dependencies {
                    implementation(libs.findLibrary("kotlin-gradlePlugin").get())
                    implementation(libs.findLibrary("kotlin-multiplatform-gradlePlugin").get())
                }
            }
            testingExtension.suites.withType<JvmTestSuite>().configureEach {
                useKotlinTest()
            }
            val integrationTest by testingExtension.suites.registering(JvmTestSuite::class) {
                testType = TestSuiteType.INTEGRATION_TEST

                targets.configureEach {
                    testTask.configure {
                        shouldRunAfter(testSuite.get())
                    }
                }

                kotlinExtension.target.compilations.named(name).configure {
                    associateWith(mainCompilation.get())
                }
            }

            val functionalTest by testingExtension.suites.registering(JvmTestSuite::class) {
                testType = TestSuiteType.FUNCTIONAL_TEST

                dependencies {
                    implementation(project())
                    implementation(libs.findLibrary("kotlin-gradlePlugin").get())
                    implementation(libs.findLibrary("kotlin-multiplatform-gradlePlugin").get())
                }

                targets.configureEach {
                    testTask.configure {
                        shouldRunAfter(testSuite.get())
                    }
                }
            }

            val gradlePluginExtension = extensions.getByType<GradlePluginDevelopmentExtension>()
            gradlePluginExtension.apply {
                website = "https://github.com/John-Tuesday/gradle-convention-plugins"
                vcsUrl = "https://github.com/John-Tuesday/gradle-convention-plugins"
            }
            gradlePluginExtension.testSourceSets.add(functionalTest.map { it.sources }.get())
            val publish by tasks.getting
            val publishAllToGitHub = tasks.named("publishAllPublicationsTo${GitHubPackages.defaultName}Repository")
            val publishAllToSonatype = tasks.named("publishAllPublicationsTo${SonatypeStaging.defaultName}Repository")

            val publishChildren by rootProject.tasks.getting
            publishChildren.dependsOn(publish)
            rootProject.tasks.maybeCreate("publishChildrenTo${GitHubPackages.defaultName}Repository").apply {
                dependsOn(publishAllToGitHub)
                group = "publishing"
                description = "publish all publications produced by subprojects to ${GitHubPackages.defaultName}"
            }
            rootProject.tasks.maybeCreate("publishChildrenTo${SonatypeStaging.defaultName}Repository").apply {
                dependsOn(publishAllToSonatype)
                group = "publishing"
                description = "publish all publications produced by subprojects to ${SonatypeStaging.defaultName}"
            }

            val check by tasks.existing
            check.configure {
                dependsOn(functionalTest.get())
            }

            val rootCheck = rootProject.tasks.named("check")
            rootCheck.configure {
                dependsOn(check)
            }

            val signTask = tasks.withType<Sign>()
            signTask.configureEach {
                // Must explicitly ensure sign task happens after building, compiling, linking ...
                dependsOn(check)
            }

            tasks.withType<AbstractPublishToMaven>().configureEach {
                dependsOn(check)
                // Must explicitly ensure publish happens after signing
                mustRunAfter(signTask)
            }

            tasks.withType<KotlinJvmCompile>().configureEach {
                compilerOptions {
                    apiVersion = Versions.kotlin
                    languageVersion = Versions.kotlin
                    progressiveMode = true
                    jvmTarget = Versions.jvmTarget
                }
            }
        }
    }
}
