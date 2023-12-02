package io.github.john.tuesday.build.logic.constants

import io.github.john.tuesday.build.logic.helpers.propOrEnv
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.provider.ProviderFactory
import java.net.URI

public sealed class MavenRepository(
    public val defaultName: String,
    public val usernamePropertyKey: String,
    public val usernameEnvironmentKey: String,
    public val passwordPropertyKey: String,
    public val passwordEnvironmentKey: String,
    public val url: URI,
)

public data object SonatypeStaging : MavenRepository(
    defaultName = "SonatypeStaging",
    usernamePropertyKey = "ossrhUsername",
    usernameEnvironmentKey = "OSSRH_USERNAME",
    passwordPropertyKey = "ossrhPassword",
    passwordEnvironmentKey = "OSSRH_PASSWORD",
    url = URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"),
)

public data object GitHubPackages : MavenRepository(
    defaultName = "GitHubPackages",
    url = URI("https://maven.pkg.github.com/john-tuesday/*"),
    usernamePropertyKey = "gpr.user",
    usernameEnvironmentKey = "GPR_USERNAME",
    passwordPropertyKey = "gpr.key",
    passwordEnvironmentKey = "GPR_TOKEN",
)

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
