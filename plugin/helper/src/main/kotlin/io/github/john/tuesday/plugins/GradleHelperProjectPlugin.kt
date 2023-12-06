package io.github.john.tuesday.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * No-op plugin meant to add the helper api to the classpath in an IDE friendly way
 */
public class GradleHelperProjectPlugin : Plugin<Project> {
    override fun apply(target: Project) {}
}
