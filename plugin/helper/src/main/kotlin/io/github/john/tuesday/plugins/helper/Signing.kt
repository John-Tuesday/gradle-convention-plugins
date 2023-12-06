package io.github.john.tuesday.plugins.helper

import org.gradle.api.provider.ProviderFactory
import org.gradle.plugins.signing.SigningExtension

/**
 * Property keys and environment variables used to find values when using [SigningExtension.useGpgCmd]
 */
public data object GpgKeys {
    /**
     * Property defining the key name used when signing with GPG [SigningExtension.useGpgCmd]
     */
    public const val KEY_NAME_PROPERTY: String = "signing.gnupg.keyName"

    /**
     * Property defining the passphrase used when signing with GPG [SigningExtension.useGpgCmd]
     */
    public const val PASSPHRASE_PROPERTY: String = "signing.gnupg.passphrase"
}

/**
 * Property keys and environment variables used to find values when using [SigningExtension.useInMemoryPgpKeys]
 */
public data object PgpInMemoryKeys {
    /**
     * Property key holding the secret key used to sign with [SigningExtension.useInMemoryPgpKeys]
     */
    public const val SECRET_KEY_PROPERTY: String = "GPG_SECRET_KEY"

    /**
     * Environment variable holding the secret key used to sign with [SigningExtension.useInMemoryPgpKeys]
     */
    public const val SECRET_KEY_ENVIRONMENT: String = "GPG_SECRET_KEY"

    /**
     * Property key holding the password used to sign with [SigningExtension.useInMemoryPgpKeys]
     */
    public const val PASSWORD_PROPERTY: String = "signing.password"

    /**
     * Environment variable holding password used to sign with [SigningExtension.useInMemoryPgpKeys]
     */
    public const val PASSWORD_ENVIRONMENT: String = "GPG_SIGNING_PASSWORD"

    /**
     * Property key holding the key id used to sign with [SigningExtension.useInMemoryPgpKeys]
     */
    public const val KEY_ID_PROPERTY: String = "signing.keyId"

    /**
     * Environment variable holding key id used to sign with [SigningExtension.useInMemoryPgpKeys]
     */
    public const val KEY_ID_ENVIRONMENT: String = "GPG_SIGNING_KEY_ID"
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

@ExperimentalProviderWithErrorMessageApi
public fun SigningExtension.useGpgOrInMemoryPgp() {
    val gpgKeyName = project.providers.gradleProperty(GpgKeys.KEY_NAME_PROPERTY)
    val gpgPassphrase = project.providers.gradleProperty(GpgKeys.PASSPHRASE_PROPERTY)
    if (gpgKeyName.isPresent && gpgPassphrase.isPresent)
        useGpgCmd()
    else {
        val keyId = project.propertyOrEnvironment(
            propertyKey = PgpInMemoryKeys.KEY_ID_PROPERTY,
            environmentKey = PgpInMemoryKeys.KEY_ID_ENVIRONMENT,
        )
        val password = project.propertyOrEnvironment(
            propertyKey = PgpInMemoryKeys.PASSWORD_PROPERTY,
            environmentKey = PgpInMemoryKeys.PASSWORD_ENVIRONMENT,
        )
        val key = project.propertyOrEnvironment(
            propertyKey = PgpInMemoryKeys.SECRET_KEY_PROPERTY,
            environmentKey = PgpInMemoryKeys.SECRET_KEY_ENVIRONMENT,
        )
        if (keyId.isPresent)
            useInMemoryPgpKeys(keyId.get(), key.get(), password.get())
        else
            useInMemoryPgpKeys(key.get(), password.get())
    }
}
