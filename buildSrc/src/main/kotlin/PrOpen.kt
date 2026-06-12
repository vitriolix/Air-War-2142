import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Open a GitHub pull request's page in the browser (`gh pr view <target> --web`).
 * Defaults to the current branch's PR; pass `--pr=<number|url|branch>` for a specific one.
 */
abstract class PrOpen : DefaultTask() {

    @get:Option(option = "pr", description = "PR number, URL, or branch to open (default: current branch).")
    @get:Input
    @get:Optional
    abstract val pr: Property<String>

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun open() {
        val target = pr.orNull?.takeIf { it.isNotBlank() }
        exec.exec {
            commandLine(buildList {
                addAll(listOf("gh", "pr", "view"))
                if (target != null) add(target)
                add("--web")
            })
        }
    }
}
