package io.github.john.tuesday.plugins.publishing.model

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

    public companion object
}

/**
 * Configuration data for a Maven Repository. For use with maven-publish
 */
public sealed class MavenRepository(
    /**
     * Name of this repository in the owning container
     */
    public val name: String,

    /**
     * URI pointing to the repository
     */
    public val url: String,
) {
    /**
     * Gradle property key for username
     */
    public abstract val usernamePropKey: String

    /**
     * Environment variable name for username
     */
    public abstract val usernameEnvKey: String

    /**
     * Gradle property key for password
     */
    public abstract val passwordPropKey: String

    /**
     * Environment variable name for password
     */
    public abstract val passwordEnvKey: String

    /**
     * Sonatype Maven Repository for Staging
     */
    public data object SonatypeStaging : MavenRepository(
        name = "sonatypeStaging",
        url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/",
    ) {
        override val usernamePropKey: String = "ossrhUsername"
        override val usernameEnvKey: String = usernamePropKey
        override val passwordPropKey: String = "ossrhPassword"
        override val passwordEnvKey: String = passwordPropKey
    }

    /**
     * GitHub Package Gradle registry
     */
    public class GitHubPackage(
        owner: String,
        repository: String,
        name: String = "GitHubPackages",
    ) : MavenRepository(
        name = name,
        url = "https://maven.pkg.github.com/$owner/$repository",
    ) {
        override val usernamePropKey: String = "gpr.user"
        override val usernameEnvKey: String = "USERNAME"
        override val passwordPropKey: String = "grp.key"
        override val passwordEnvKey: String = "TOKEN"
    }
}
