package io.github.john.tuesday.plugins.helper

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

internal fun notFoundString(propertyKey: String, environmentKey: String): String =
    "Expected property with key '$propertyKey' or environment variable '$environmentKey' to be set."

@ExperimentalProviderWithErrorMessageApi
public fun ProviderFactory.propertyOrEnvironment(
    propertyKey: Provider<String>,
    environmentKey: Provider<String>,
): ProviderWithErrorMessage<String> =
    gradleProperty(propertyKey)
        .orElse(systemProperty(propertyKey))
        .orElse(environmentVariable(environmentKey))
        .withErrorMessage(propertyKey.zip(environmentKey) { prop, env -> notFoundString(prop, env) })

@ExperimentalProviderWithErrorMessageApi
public fun ProviderFactory.propertyOrEnvironment(
    propertyKey: String,
    environmentKey: String,
): ProviderWithErrorMessage<String> =
    gradleProperty(propertyKey)
        .orElse(systemProperty(propertyKey))
        .orElse(environmentVariable(environmentKey))
        .withErrorMessage(notFoundString(propertyKey, environmentKey))
