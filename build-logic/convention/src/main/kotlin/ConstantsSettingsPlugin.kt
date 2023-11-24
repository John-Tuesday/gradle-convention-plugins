import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * Plugin which merely adds shared constant values
 */
public class ConstantsSettingsPlugin : Plugin<Settings> {
    override fun apply(target: Settings) {}
}
