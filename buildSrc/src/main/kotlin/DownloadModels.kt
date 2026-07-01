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
 * The Gradle *daemon* has no controlling terminal, so a normal (daemon-backed) invocation
 * can only list models when run without --model (no interactive prompt) — same split as
 * pruneBranches; see TASKS.md #20. Running with `--no-daemon` gives the forked script a
 * real controlling terminal to probe via `/dev/tty` (see download-models.sh — same
 * technique prune-branches.sh already uses), so the picker prompt works; no stdin wiring
 * needed here since the script reads the answer from /dev/tty directly, not fd 0. The
 * `models:pick` npm script (and `scripts/download-models-interactive.sh`) bake the
 * `--no-daemon` flag in so you don't have to remember it. Running `scripts/download-
 * models.sh` directly (no Gradle at all) also works and is faster — this exists for
 * discoverability via `./gradlew tasks --group game` and so the interactive path is
 * reachable without knowing the script exists.
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
