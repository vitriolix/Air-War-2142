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
 * Task execution always happens inside a forked "Gradle build daemon" JVM (even with
 * `--no-daemon`, which only controls whether that daemon is *kept alive for reuse*, not
 * whether one is forked at all), and that daemon has no OS-level controlling terminal —
 * so `/dev/tty`-based detection inside a forked script never works here, no matter the
 * flags. BUT Gradle's client↔daemon protocol *can* forward the real keystrokes typed at
 * the launching terminal: wiring `standardInput = System.\`in\`` below pipes whatever the
 * client forwards into the script's stdin. That forwarding isn't auto-detected reliably
 * from inside a daemon-executed task (no real console to probe), so it's opt-in via
 * `--force-prompt`, set only by the dedicated wrapper (`scripts/download-models-
 * interactive.sh` / `npm run models:pick`) — a human running that wrapper is presumed to
 * be at a real terminal. Plain `./gradlew downloadModels` (no `--force-prompt`) is
 * untouched: always safe, lists and exits, never blocks waiting for input that isn't
 * coming (CI-safe).
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

    @get:Option(option = "force-prompt", description = "Forward this terminal's stdin to the picker so it can prompt (only meaningful with --no-daemon --console=plain; see scripts/download-models-interactive.sh).")
    @get:Input
    @get:Optional
    abstract val forcePrompt: Property<Boolean>

    @get:Inject
    abstract val exec: ExecOperations

    private val scriptPath = "scripts/download-models.sh"

    @TaskAction
    fun download() {
        val modelName = model.orNull?.takeIf { it.isNotBlank() }
        val downloadAll = all.orNull == true
        val prompting = forcePrompt.orNull == true

        // Check if script exists
        if (!java.io.File(scriptPath).exists()) {
            throw IllegalStateException("Script not found: $scriptPath")
        }

        // Build command
        val cmd = mutableListOf("bash", scriptPath)
        when {
            downloadAll -> cmd.add("all")
            modelName != null -> cmd.add(modelName)
            // else: list available models (no arg) — or prompt, if forcePrompt forwards stdin
        }

        println("📥 Running: ${cmd.joinToString(" ")}")
        println("")

        exec.exec {
            commandLine(cmd)
            if (prompting) {
                standardInput = System.`in`
                environment("AIR_WAR_2142_FORCE_PROMPT", "1")
            }
        }
    }
}
