package io.github.john.tuesday.plugins.helper

import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.provider.ProviderFactory
import java.net.URI

/**
 * Maven repository for publishing or consuming
 */
public sealed interface MavenRepository {
    /**
     * Default name used when registering the repository
     */
    public val defaultName: String

    /**
     * Property name yielding the username
     */
    public val usernamePropertyKey: String

    /**
     * Environment variable name yielding the username
     */
    public val usernameEnvironmentKey: String

    /**
     * Property name yielding the password
     */
    public val passwordPropertyKey: String

    /**
     * Environment variable name yielding the password
     */
    public val passwordEnvironmentKey: String

    /**
     * Url to the repository
     */
    public val url: URI
}

/**
 * Construct a [MavenRepository]
 */
public fun MavenRepository(
    defaultName: String,
    usernamePropertyKey: String,
    usernameEnvironmentKey: String,
    passwordPropertyKey: String,
    passwordEnvironmentKey: String,
    url: URI,
): MavenRepository = MavenRepositoryImplementation(
    defaultName = defaultName,
    usernamePropertyKey = usernamePropertyKey,
    usernameEnvironmentKey = usernameEnvironmentKey,
    passwordPropertyKey = passwordPropertyKey,
    passwordEnvironmentKey = passwordEnvironmentKey,
    url = url,
)

/**
 * Builder interface for building [MavenRepository]
 */
public sealed interface MavenRepositoryBuilder : MavenRepository {
    override var defaultName: String
    override var usernamePropertyKey: String
    override var usernameEnvironmentKey: String
    override var passwordPropertyKey: String
    override var passwordEnvironmentKey: String
    override var url: URI
}

internal class MavenRepositoryImplementation(
    override val defaultName: String,
    override val usernamePropertyKey: String = "",
    override val usernameEnvironmentKey: String = "",
    override val passwordPropertyKey: String = "",
    override val passwordEnvironmentKey: String = "",
    override val url: URI = URI(""),
) : MavenRepository

internal class MavenRepositoryBuilderImplementation(
    override var defaultName: String = "",
    override var usernamePropertyKey: String = "",
    override var usernameEnvironmentKey: String = "",
    override var passwordPropertyKey: String = "",
    override var passwordEnvironmentKey: String = "",
    override var url: URI = URI(""),
) : MavenRepositoryBuilder

/**
 * Sonatype Staging [MavenRepository]
 */
public val SonatypeStaging: MavenRepository = MavenRepository(
    defaultName = "SonatypeStaging",
    usernamePropertyKey = "ossrhUsername",
    usernameEnvironmentKey = "OSSRH_USERNAME",
    passwordPropertyKey = "ossrhPassword",
    passwordEnvironmentKey = "OSSRH_PASSWORD",
    url = URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"),
)

/**
 * John-Tuesday GitHub Packages maven repository (all)
 */
public val GitHubPackages: MavenRepository = MavenRepository(
    defaultName = "GitHubPackages",
    url = URI("https://maven.pkg.github.com/john-tuesday/*"),
    usernamePropertyKey = "gpr.user",
    usernameEnvironmentKey = "GPR_USERNAME",
    passwordPropertyKey = "gpr.key",
    passwordEnvironmentKey = "GPR_TOKEN",
)

/**
 * Create a [MavenRepository] using [GitHubPackages] as the base, but adjust the exact repository to [repositoryName]
 */
public fun GitHubPackages(repositoryName: String, builder: MavenRepositoryBuilder.() -> Unit = {}): MavenRepository {
    return MavenRepositoryBuilderImplementation(
        defaultName = "GitHubPackages",
        url = URI("https://maven.pkg.github.com/john-tuesday/$repositoryName"),
        usernamePropertyKey = "gpr.user",
        usernameEnvironmentKey = "GPR_USERNAME",
        passwordPropertyKey = "gpr.key",
        passwordEnvironmentKey = "GPR_TOKEN",
    ).apply(builder)
}

/**
 * Set all values according to [repository] and [providers]
 */
@OptIn(ExperimentalProviderWithErrorMessageApi::class)
public fun MavenArtifactRepository.usePreset(repository: MavenRepository, providers: ProviderFactory) {
    name = repository.defaultName
    url = repository.url
    credentials {
        username = providers
            .propertyOrEnvironment(
                propertyKey = repository.usernamePropertyKey,
                environmentKey = repository.usernameEnvironmentKey
            )
            .get()
        password = providers
            .propertyOrEnvironment(
                propertyKey = repository.passwordPropertyKey,
                environmentKey = repository.passwordEnvironmentKey,
            )
            .get()
    }
}
