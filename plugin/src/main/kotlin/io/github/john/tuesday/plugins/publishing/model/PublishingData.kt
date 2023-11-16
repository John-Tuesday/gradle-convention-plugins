package io.github.john.tuesday.plugins.publishing.model

import org.gradle.api.publish.maven.MavenPomLicenseSpec
import org.gradle.kotlin.dsl.assign

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

public sealed class LicensePreset(
    public val name: String,
    public val url: String,
    public val comments: String? = null,
    public val distribution: String? = null,
) {
    public data object MIT : LicensePreset(name = "MIT", url = "https://opensource.org/licenses/MIT")

    public companion object
}

public sealed class MavenRepository(
    public val name: String,
    public val url: String,
) {
    public abstract val UsernamePropKey: String
    public abstract val UsernameEnvKey: String
    public abstract val PasswordPropKey: String
    public abstract val PasswordEnvKey: String

    public data object SonatypeStaging : MavenRepository(
        name = "sonatypeStaging",
        url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/",
    ) {
        override val UsernamePropKey: String = "ossrhUsername"
        override val UsernameEnvKey: String = UsernamePropKey
        override val PasswordPropKey: String = "ossrhPassword"
        override val PasswordEnvKey: String = PasswordPropKey
    }

    public class GitHubPackage internal constructor(
        owner: String,
        repository: String,
        name: String = "GitHubPackages",
    ) : MavenRepository(
        name = name,
        url = "https://maven.pkg.github.com/$owner/$repository",
    ) {
        override val UsernamePropKey: String = "gpr.user"
        override val UsernameEnvKey: String = "USERNAME"
        override val PasswordPropKey: String = "grp.key"
        override val PasswordEnvKey: String = "TOKEN"
    }
}
