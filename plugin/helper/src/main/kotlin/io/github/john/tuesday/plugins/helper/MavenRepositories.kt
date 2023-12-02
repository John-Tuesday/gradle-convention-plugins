package io.github.john.tuesday.plugins.helper

import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.provider.ProviderFactory
import java.net.URI

public sealed interface MavenRepository {
    public val defaultName: String
    public val usernamePropertyKey: String
    public val usernameEnvironmentKey: String
    public val passwordPropertyKey: String
    public val passwordEnvironmentKey: String
    public val url: URI
}

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

public val SonatypeStaging: MavenRepository = MavenRepository(
    defaultName = "SonatypeStaging",
    usernamePropertyKey = "ossrhUsername",
    usernameEnvironmentKey = "OSSRH_USERNAME",
    passwordPropertyKey = "ossrhPassword",
    passwordEnvironmentKey = "OSSRH_PASSWORD",
    url = URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"),
)

public val GitHubPackages: MavenRepository = MavenRepository(
    defaultName = "GitHubPackages",
    url = URI("https://maven.pkg.github.com/john-tuesday/*"),
    usernamePropertyKey = "gpr.user",
    usernameEnvironmentKey = "GPR_USERNAME",
    passwordPropertyKey = "gpr.key",
    passwordEnvironmentKey = "GPR_TOKEN",
)

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

public fun MavenArtifactRepository.usePreset(repository: MavenRepository, providers: ProviderFactory) {
    name = repository.defaultName
    url = repository.url
    credentials {
        username = providers
            .propOrEnv(
                propertyKey = repository.usernamePropertyKey,
                environmentKey = repository.usernameEnvironmentKey
            )
            .get()
        password = providers
            .propOrEnv(
                propertyKey = repository.passwordPropertyKey,
                environmentKey = repository.passwordEnvironmentKey,
            )
            .get()
    }
}
