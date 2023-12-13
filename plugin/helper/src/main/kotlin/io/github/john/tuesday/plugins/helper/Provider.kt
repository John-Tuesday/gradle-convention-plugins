package io.github.john.tuesday.plugins.helper

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

/**
 * Annotation marking use of [ProviderWithErrorMessage]
 */
@RequiresOptIn
@MustBeDocumented
public annotation class ExperimentalProviderWithErrorMessageApi

@Deprecated(
    message = "This function is deprecated and will be made an error in 0.1.0-alpha05.",
    replaceWith = ReplaceWith("ProviderFactory.propertyOrEnvironment"),
    level = DeprecationLevel.WARNING,
)
public fun ProviderFactory.propOrEnv(propertyKey: String, environmentKey: String): ProviderWithError<String> =
    gradleProperty(propertyKey)
        .orElse(systemProperty(propertyKey))
        .orElse(environmentVariable(environmentKey))
        .withError("Expected property with key '$propertyKey' or environment variable '$environmentKey' to be set.")

@Deprecated(
    message = "This function is deprecated and will be made an error in 0.1.0-alpha05",
    replaceWith = ReplaceWith("Provider<T>.withErrorMessage"),
    level = DeprecationLevel.WARNING,
)
public fun <T> Provider<T>.withError(message: String): ProviderWithError<T> = ProviderWithError(
    errorMessage = message,
    provider = this,
)

@Deprecated(
    message = "Will be made an ERROR in 0.1.0-alpha05. Will be removed in 0.1.0-beta.",
    replaceWith = ReplaceWith("ProviderWithErrorMessage"),
    level = DeprecationLevel.WARNING,
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
 * Essentially just a [Provider] which will show a more helpful message when [get] is called with no value set.
 */
@ExperimentalProviderWithErrorMessageApi
public sealed class ProviderWithErrorMessage<T>(
    private val value: Provider<T>,
) : Provider<T> by value {
    protected abstract val message: Provider<String>

    /**
     * When the value [isPresent], it returns the value; otherwise, it raises an error with the preset message.
     */
    override fun get(): T & Any {
        if (isPresent)
            return value.get()
        else
            error(message.get())
    }

    /**
     * When the value [isPresent], it returns the value; otherwise, it raises an error with the preset message.
     *
     * This behaves identically to [get] but is more explicit
     */
    public fun getOrError(): T = if (value.isPresent) value.get() else error(message.get())

    internal companion object {
        const val NO_MESSAGE: String = "Error message not set."
    }
}

@ExperimentalProviderWithErrorMessageApi
internal class ProviderWithErrorMessageImplementation<T>(
    value: Provider<T>,
    messageProvider: Provider<String>,
) : ProviderWithErrorMessage<T>(value) {
    override val message: Provider<String> = messageProvider.orElse(NO_MESSAGE)
}

@ExperimentalProviderWithErrorMessageApi
internal class ProviderWithErrorMessageInjectImplementation<T>(
    value: Provider<T>,
    messageString: String,
) : ProviderWithErrorMessage<T>(value) {
    @get:Inject
    protected val providers: ProviderFactory
        get() = error("ProviderFactory not injected")

    override val message: Provider<String> = providers.provider { messageString }
}

/**
 * Create a [ProviderWithErrorMessage] whose error message will be [message]
 */
@ExperimentalProviderWithErrorMessageApi
public fun <T> ProviderWithErrorMessage(
    value: Provider<T>,
    message: Provider<String>,
): ProviderWithErrorMessage<T> = ProviderWithErrorMessageImplementation(value, message)

/**
 * Create a [ProviderWithErrorMessage] whose error message will be [message]
 */
@ExperimentalProviderWithErrorMessageApi
public fun <T> ProviderWithErrorMessage(
    value: Provider<T>,
    message: String,
): ProviderWithErrorMessage<T> = ProviderWithErrorMessageInjectImplementation(value, message)

/**
 * Convert `this` to a [ProviderWithErrorMessage] whose error message is [message]
 */
@ExperimentalProviderWithErrorMessageApi
public fun <T> Provider<T>.withErrorMessage(message: Provider<String>): ProviderWithErrorMessage<T> =
    ProviderWithErrorMessage(value = this, message = message)

/**
 * Convert `this` to a [ProviderWithErrorMessage] whose error message is [message]
 */
@ExperimentalProviderWithErrorMessageApi
public fun <T> Provider<T>.withErrorMessage(message: String): ProviderWithErrorMessage<T> =
    ProviderWithErrorMessage(value = this, message = message)
