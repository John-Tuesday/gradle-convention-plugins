import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    `kotlin-dsl`
    signing
}

group = "io.github.john-tuesday"
version = "0.0.0"

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_1_9
        languageVersion = KotlinVersion.KOTLIN_1_9
        progressiveMode = true
        jvmTarget = JvmTarget.JVM_1_8
    }
}

kotlin {
    compilerOptions {
        explicitApi()
    }
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(JavaVersion.VERSION_1_8.majorVersion)
    }
    sourceSets {
        val main by getting {
            dependencies {
                compileOnly(libs.kotlin.gradlePlugin)
                compileOnly(libs.kotlin.multiplatform.gradlePlugin)
            }
        }

        val test by getting {
            dependencies {
                implementation(libs.kotlin.gradlePlugin)
                implementation(libs.kotlin.multiplatform.gradlePlugin)
            }
        }
    }
}

testing {
    suites.withType<JvmTestSuite>().configureEach {
        useKotlinTest()
    }
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class)

        val integrationTest by registering(JvmTestSuite::class) {
            testType = TestSuiteType.INTEGRATION_TEST

            dependencies {
                implementation(project())
            }

            targets.configureEach {
                testTask.configure {
                    shouldRunAfter(test)
                }
            }
        }

        // Create a new test suite
        val functionalTest by registering(JvmTestSuite::class) {
            testType = TestSuiteType.FUNCTIONAL_TEST

            dependencies {
                implementation(project())
            }

            targets.configureEach {
                testTask.configure {
                    shouldRunAfter(test)
                }
            }
        }
    }
}

signing {
    useGpgCmd()
}

gradlePlugin {
    website = "https://github.com/John-Tuesday/gradle-convention-plugins"
    vcsUrl = "https://github.com/John-Tuesday/gradle-convention-plugins"

    plugins {
        val mavenPublishAssist by registering {
            id = "io.github.john-tuesday.maven-publish-assist"
            displayName = "Maven publish assist plugin"
            description = "Simplify and streamline publishing to Maven Central"
            tags = listOf("maven", "publish")
            implementationClass = "io.github.john.tuesday.plugins.MavenPublishAssistPlugin"
        }
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

tasks.check {
    dependsOn(testing.suites.named<JvmTestSuite>("functionalTest"))
}
