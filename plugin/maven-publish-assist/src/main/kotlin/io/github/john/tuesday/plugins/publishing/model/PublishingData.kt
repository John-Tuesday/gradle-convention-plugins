package io.github.john.tuesday.plugins.publishing.model

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.maven.MavenPomLicenseSpec
import org.gradle.kotlin.dsl.assign
import java.net.URI

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
        name = "SonatypeStaging",
        url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/",
    ) {
        override val usernamePropKey: String = "ossrhUsername"
        override val usernameEnvKey: String = "OSSRH_USERNAME"
        override val passwordPropKey: String = "ossrhPassword"
        override val passwordEnvKey: String = "OSSRH_PASSWORD"
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
        override val passwordPropKey: String = "gpr.key"
        override val passwordEnvKey: String = "TOKEN"
    }
}

/***
 * Provider with additional method [getOrError] to give a more transparent error
 */
public class ProviderWithErrorMessage<T>(
    private val provider: Provider<T>,
    public val errorMessage: String,
) : Provider<T> by provider {

    /**
     * Checks if the value is present and returns it via [get] otherwise it runs [error]
     */
    public fun getOrError(): T = if (provider.isPresent) provider.get() else error(errorMessage)
}

/**
 * Convert a regular [Provider] to a [ProviderWithErrorMessage]
 */
public fun <T> Provider<T>.withError(errorMessage: String): ProviderWithErrorMessage<T> {
    return ProviderWithErrorMessage(provider = this, errorMessage = errorMessage)
}


/**
 * Provide username from [ProviderFactory.gradleProperty] or else [ProviderFactory.environmentVariable]
 */
public fun ProviderFactory.username(repo: MavenRepository): ProviderWithErrorMessage<String> {
    return gradleProperty(repo.usernamePropKey)
        .orElse(environmentVariable(repo.usernameEnvKey))
        .withError("Could not find property '${repo.usernamePropKey}' and could not find environment variable '${repo.usernameEnvKey}'")
}

/**
 * Provide password from [ProviderFactory.gradleProperty] or else [ProviderFactory.environmentVariable]
 */
public fun ProviderFactory.password(repo: MavenRepository): ProviderWithErrorMessage<String> {
    return gradleProperty(repo.passwordPropKey)
        .orElse(environmentVariable(repo.passwordEnvKey))
        .withError("Could not find property '${repo.passwordPropKey}' and could not find environment variable '${repo.passwordPropKey}'")
}

/**
 * Create new Maven repository from [repo] with properties resolved by [providers]
 */
public fun RepositoryHandler.maven(repo: MavenRepository, providers: ProviderFactory) {
    maven {
        name = repo.name
        url = URI(repo.url)

        credentials {
            username = providers.username(repo).getOrError()
            password = providers.password(repo).getOrError()
        }
    }
}
