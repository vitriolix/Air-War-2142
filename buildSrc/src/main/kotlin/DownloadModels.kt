import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Download 3D aircraft models from Sketchfab.
 * Models are stored in design/aircraft-reference/models/ (ignored by git).
 * Usage: ./gradlew downloadModels [--model=number|short-name|full-name|--all]
 *
 * Task execution always happens inside a forked "Gradle build daemon" JVM — even with
 * `--no-daemon` (which only controls whether that daemon is *kept alive for reuse*, not
 * whether one is forked at all; confirmed via `--info`: Gradle 8 always launches a daemon
 * process with its own baked-in `--add-opens`/`--add-exports` flags that a bootstrap `java`
 * invocation could never organically match). That daemon process is detached from the
 * controlling terminal, so **no combination of flags gets this task an interactive
 * prompt** — this isn't a project-specific limitation (the JDK 21 pin below), it's
 * inherent to Gradle's architecture. Run `scripts/download-models.sh` directly (no Gradle
 * at all) for the interactive "enter a number" picker.
 */
abstract class DownloadModels : DefaultTask() {

    @get:Option(option = "model", description = "Model number, short name, or full name to download (e.g., '4', 'Bf-109', or 'Messerschmitt Bf-109'), or omit to list available models.")
    @get:Input
    @get:Optional
    abstract val model: Property<String>

    @get:Option(option = "all", description = "Download all models.")
    @get:Input
    @get:Optional
    abstract val all: Property<Boolean>

    @get:Inject
    abstract val exec: ExecOperations

    private val scriptPath = "scripts/download-models.sh"

    @TaskAction
    fun download() {
        val modelName = model.orNull?.takeIf { it.isNotBlank() }
        val downloadAll = all.orNull == true

        // Check if script exists
        if (!java.io.File(scriptPath).exists()) {
            throw IllegalStateException("Script not found: $scriptPath")
        }

        // Build command
        val cmd = mutableListOf("bash", scriptPath)
        when {
            downloadAll -> cmd.add("all")
            modelName != null -> cmd.add(modelName)
            // else: list available models (no arg)
        }

        println("📥 Running: ${cmd.joinToString(" ")}")
        println("")

        exec.exec {
            commandLine(cmd)
        }
    }
}
