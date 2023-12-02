package io.github.john.tuesday.build.logic.helpers

import io.github.john.tuesday.build.logic.constants.GpgKeys
import io.github.john.tuesday.build.logic.constants.PgpInMemoryKeys
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.plugins.signing.SigningExtension

public fun ProviderFactory.propOrEnv(propertyKey: String, environmentKey: String): ProviderWithError<String> =
    gradleProperty(propertyKey)
        .orElse(systemProperty(propertyKey))
        .orElse(environmentVariable(environmentKey))
        .withError("Expected property with key '$propertyKey' or environment variable '$environmentKey' to be set.")

public fun <T> Provider<T>.withError(message: String): ProviderWithError<T> = ProviderWithError(
    errorMessage = message,
    provider = this,
)

public class ProviderWithError<T> internal constructor(
    public val errorMessage: String,
    private val provider: Provider<T>,
) : Provider<T> by provider {
    override fun get(): T & Any {
        if (provider.isPresent)
            return provider.get()
        else
            error(errorMessage)
    }

    public fun getOrError(): T = if (provider.isPresent) provider.get() else error(errorMessage)
}

/**
 * Use [SigningExtension.useGpgCmd] if the appropriate values are set. See [GpgKeys] for key values. Otherwise,
 * use [SigningExtension.useInMemoryPgpKeys]. Gets values using [PgpInMemoryKeys]. KeyId is optional.
 */
public fun SigningExtension.useGpgOrInMemoryPgp(providers: ProviderFactory) {
    val gpgKeyName = providers.gradleProperty(GpgKeys.KEY_NAME_PROPERTY)
    val gpgPassphrase = providers.gradleProperty(GpgKeys.PASSPHRASE_PROPERTY)
    if (gpgKeyName.isPresent && gpgPassphrase.isPresent)
        useGpgCmd()
    else {
        val keyId = providers.propOrEnv(
            propertyKey = PgpInMemoryKeys.KEY_ID_PROPERTY,
            environmentKey = PgpInMemoryKeys.KEY_ID_ENVIRONMENT,
        )
        val password = providers.propOrEnv(
            propertyKey = PgpInMemoryKeys.PASSWORD_PROPERTY,
            environmentKey = PgpInMemoryKeys.PASSWORD_ENVIRONMENT,
        )
        val key = providers.propOrEnv(
            propertyKey = PgpInMemoryKeys.SECRET_KEY_PROPERTY,
            environmentKey = PgpInMemoryKeys.SECRET_KEY_ENVIRONMENT,
        )
        if (keyId.isPresent)
            useInMemoryPgpKeys(keyId.get(), key.get(), password.get())
        else
            useInMemoryPgpKeys(key.get(), password.get())
    }
}
