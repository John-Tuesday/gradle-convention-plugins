package io.github.john.tuesday.plugins.helper


import org.gradle.api.publish.maven.MavenPomLicenseSpec
import org.gradle.kotlin.dsl.assign

/**
 * Create, configure, and add license according to [license]
 */
public fun MavenPomLicenseSpec.license(license: LicensePreset): Unit {
    license {
        name = license.name
        url = license.url
        license.comments?.let {
            comments = it
        }
        license.distribution?.let {
            distribution = it
        }
    }
}

/**
 * Configuration data for software licenses. For use with maven-publish
 */
public sealed class LicensePreset(
    /**
     * Name of the license
     */
    public val name: String,

    /**
     * URL pointing the license template
     */
    public val url: String,

    /**
     * comments
     */
    public val comments: String? = null,

    /**
     * distribution
     */
    public val distribution: String? = null,
) {
    /**
     * MIT license
     */
    public data object MIT : LicensePreset(name = "MIT", url = "https://opensource.org/licenses/MIT")
}
