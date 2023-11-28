import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.TestSuiteType
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun ProviderFactory.propertyOrEnv(
    propertyKey: String,
    environmentKey: String = propertyKey,
): Provider<String> = gradleProperty(propertyKey)
    .orElse(environmentVariable(environmentKey))
    .orElse(provider { error("Expected to find property '$propertyKey' or environment variable '$environmentKey' but found nothing.") })

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
                        name = "SonatypeStaging"
                        url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                        credentials {
                            username = providers.propertyOrEnv(
                                propertyKey = "ossrhUsername",
                                environmentKey = "OSSRH_USERNAME",
                            ).get()
                            password = providers.propertyOrEnv(
                                propertyKey = "ossrhPassword",
                                environmentKey = "OSSRH_PASSWORD",
                            ).get()
                        }
                    }
                    maven {
                        name = "GitHubPackages"
                        url = uri("https://maven.pkg.github.com/john-tuesday/gradle-convention-plugins")
                        credentials {
                            username = providers.propertyOrEnv(
                                propertyKey = "gpr.user",
                                environmentKey = "USERNAME",
                            ).get()
                            password = providers.propertyOrEnv(
                                propertyKey = "gpr.key",
                                environmentKey = "TOKEN",
                            ).get()
                        }
                    }
                }
            }

            publishingExtension.publications.withType<MavenPublication>().configureEach {
                artifact(javadocJar)
            }

            val signingExtension = extensions.getByType<SigningExtension>()
            signingExtension.apply {
                val gpgKeyName = providers.gradleProperty("signing.gnupg.keyName")
                val gpgPassphrase = providers.gradleProperty("signing.gnupg.passphrase")
                if (gpgKeyName.isPresent && gpgPassphrase.isPresent)
                    useGpgCmd()
                else {
                    val keyId = providers
                        .gradleProperty("signing.keyId")
                        .orElse(providers.environmentVariable("GPG_SIGNING_KEY_ID"))
                    val password = providers.propertyOrEnv(
                        propertyKey = "signing.password",
                        environmentKey = "GPG_SIGNING_PASSWORD",
                    )
                    val key = providers.propertyOrEnv(
                        propertyKey = "GPG_SECRET_KEY",
                        environmentKey = "GPG_SECRET_KEY",
                    )
                    if (keyId.isPresent)
                        useInMemoryPgpKeys(keyId.get(), key.get(), password.get())
                    else
                        useInMemoryPgpKeys(key.get(), password.get())
                }
                sign(publishingExtension.publications)
            }

            val testingExtension = extensions.getByType<TestingExtension>()
            val testSuite = testingExtension.suites.named<JvmTestSuite>("test")
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

            val check by tasks.existing
            check.configure {
                dependsOn(functionalTest.get())
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
