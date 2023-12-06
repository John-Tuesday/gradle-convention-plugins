package io.github.john.tuesday.plugins

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * No-op plugin meant to add the helper api to the classpath in an IDE friendly way
 */
public class GradleHelperSettingsPlugin : Plugin<Settings> {
    override fun apply(target: Settings) {}
}
