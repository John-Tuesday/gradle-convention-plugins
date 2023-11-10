package io.github.john.tuesday.plugins

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra
import java.util.*

internal abstract class LoadSigningKeyCredentialsTask : DefaultTask() {
    @get:InputFile
    public abstract val inputFile: RegularFileProperty

    @get:Internal
    public abstract val mavenUsername: Property<String>

    @get:Internal
    public abstract val mavenPassword: Property<String>

    init {
        mavenPassword.convention("")
        mavenUsername.convention("")
    }

    @TaskAction
    public fun extract() {
        inputFile.finalizeValue()
        val file = inputFile.get().asFile

        if (!file.exists()) return

        file.reader()
            .use {
                Properties().apply {
                    load(it)
                }
            }
            .onEach { (key, value) ->
                when (val prop = key.toString()) {
                    "ossrhUsername" -> mavenUsername.convention(value.toString()).finalizeValueOnRead()
                    "ossrhPassword" -> mavenPassword.convention(value.toString()).finalizeValueOnRead()
                    else -> extra[prop] = value
                }
            }
    }
}
