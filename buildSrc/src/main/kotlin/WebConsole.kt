import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Boot the web build and stream its browser console + errors to the terminal
 * (wraps scripts/web-console.sh, which serves the dev build then drives it with Playwright).
 *
 * `--headed` opens a visible "Chrome for Testing" window; default is headless.
 * `--target=wasm` streams the Wasm build instead of the default JS build.
 */
abstract class WebConsole : DefaultTask() {

    @get:Option(option = "headed", description = "Open a visible Chrome for Testing window (default: headless).")
    @get:Input
    abstract val headed: Property<Boolean>

    @get:Option(option = "target", description = "Web target to stream: js (default) or wasm.")
    @get:Input
    @get:Optional
    abstract val target: Property<String>

    @get:Inject
    abstract val exec: ExecOperations

    init {
        headed.convention(false)
    }

    @TaskAction
    fun run() {
        // web-console.sh sources _common.sh, which cd's to the repo root itself, so no workingDir needed.
        val tgt = target.orNull?.takeIf { it.isNotBlank() } ?: "js"
        exec.exec {
            commandLine(buildList {
                addAll(listOf("bash", "scripts/web-console.sh", tgt))
                if (headed.get()) add("--headed")
            })
        }
    }
}
