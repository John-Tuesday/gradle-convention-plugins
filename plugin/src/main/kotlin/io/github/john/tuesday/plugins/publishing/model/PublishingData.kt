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
    public data object SonatypeStaging : MavenRepository(
        name = "sonatypeStaging",
        url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/",
    ) {
        const val USERNAME_PROPERTY_KEY: String = "ossrhUsername"
        const val PASSWORD_PROPERTY_KEY: String = "ossrhPassword"
    }
}
