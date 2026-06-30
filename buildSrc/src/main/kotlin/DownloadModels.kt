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
 * Usage: ./gradlew downloadModels [--model=name|--all]
 */
abstract class DownloadModels : DefaultTask() {

    @get:Option(option = "model", description = "Model name to download (e.g., 'Bf-109'), or omit to list available models.")
    @get:Input
    @get:Optional
    abstract val model: Property<String>

    @get:Option(option = "all", description = "Download all models.")
    @get:Input
    @get:Optional
    abstract val all: Property<Boolean>

    @get:Inject
    abstract val exec: ExecOperations

    private val modelsDir = "design/aircraft-reference/models"

    @TaskAction
    fun download() {
        val modelName = model.orNull?.takeIf { it.isNotBlank() }
        val downloadAll = all.orNull == true

        // Check if script exists
        val scriptPath = "$modelsDir/download-models.sh"
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
