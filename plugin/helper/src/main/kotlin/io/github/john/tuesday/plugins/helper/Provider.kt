package io.github.john.tuesday.plugins.helper

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

public fun ProviderFactory.propOrEnv(propertyKey: String, environmentKey: String): ProviderWithError<String> =
    gradleProperty(propertyKey)
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
