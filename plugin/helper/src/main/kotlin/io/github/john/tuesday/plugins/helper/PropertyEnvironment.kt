package io.github.john.tuesday.plugins.helper

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

internal fun notFoundString(propertyKey: String, environmentKey: String): String =
    "Expected property with key '$propertyKey' or environment variable '$environmentKey' to be set."

/**
 * Tries to find a value set by using [propertyKey] to check [ProviderFactory.gradleProperty] and
 * [ProviderFactory.systemProperty] and [environmentKey] to check [ProviderFactory.environmentVariable], in that order.
 * If a value is found, it will be yielded; otherwise, an error will be raised detailing the keys and areas checked.
 */
@ExperimentalProviderWithErrorMessageApi
public fun Project.propertyOrEnvironment(
    propertyKey: String,
    environmentKey: String,
): ProviderWithErrorMessage<String> =
    propertyOrEnvironment(
        provider { propertyKey },
        provider { environmentKey },
    )

/**
 * Tries to find a value set by using [propertyKey] to check [ProviderFactory.gradleProperty] and
 * [ProviderFactory.systemProperty] and [environmentKey] to check [ProviderFactory.environmentVariable], in that order.
 * If a value is found, it will be yielded; otherwise, an error will be raised detailing the keys and areas checked.
 */
@ExperimentalProviderWithErrorMessageApi
public fun Project.propertyOrEnvironment(
    propertyKey: Provider<String>,
    environmentKey: Provider<String>,
): ProviderWithErrorMessage<String> =
    providers
        .propertyOrEnvironment(propertyKey = propertyKey, environmentKey = environmentKey)
        .orElse(provider { if (hasProperty(propertyKey.get())) property(propertyKey.get()).toString() else null }.filter { it != null })
        .withErrorMessage(propertyKey.zip(environmentKey) { prop, env -> notFoundString(prop, env) })

/**
 * Tries to find a value set by using [propertyKey] to check [ProviderFactory.gradleProperty] and
 * [ProviderFactory.systemProperty] and [environmentKey] to check [ProviderFactory.environmentVariable], in that order.
 * If a value is found, it will be yielded; otherwise, an error will be raised detailing the keys and areas checked.
 */
@ExperimentalProviderWithErrorMessageApi
public fun ProviderFactory.propertyOrEnvironment(
    propertyKey: Provider<String>,
    environmentKey: Provider<String>,
): ProviderWithErrorMessage<String> =
    gradleProperty(propertyKey)
        .orElse(systemProperty(propertyKey))
        .orElse(environmentVariable(environmentKey))
        .withErrorMessage(propertyKey.zip(environmentKey) { prop, env -> notFoundString(prop, env) })

/**
 * Tries to find a value set by using [propertyKey] to check [ProviderFactory.gradleProperty] and
 * [ProviderFactory.systemProperty] and [environmentKey] to check [ProviderFactory.environmentVariable], in that order.
 * If a value is found, it will be yielded; otherwise, an error will be raised detailing the keys and areas checked.
 */
@ExperimentalProviderWithErrorMessageApi
public fun ProviderFactory.propertyOrEnvironment(
    propertyKey: String,
    environmentKey: String,
): ProviderWithErrorMessage<String> =
    gradleProperty(propertyKey)
        .orElse(systemProperty(propertyKey))
        .orElse(environmentVariable(environmentKey))
        .withErrorMessage(provider { notFoundString(propertyKey, environmentKey) })
