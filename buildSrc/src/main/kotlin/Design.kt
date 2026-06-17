import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Code → Claude Design. Stages the handoff bundle under design/ so it can be handed to a
 * claude.ai/design project (see design/PROMPT.md). Refreshes design/assets/ from the committed
 * sprite atlas, checks the committed bundle files exist, opens spec.html, and prints next steps.
 * Logic lives here (not bash) per the project's tooling convention.
 */
abstract class DesignExport : DefaultTask() {

    @get:Inject abstract val exec: ExecOperations
    @get:Inject abstract val layout: ProjectLayout

    @TaskAction
    fun export() {
        val root = layout.projectDirectory.asFile
        val design = File(root, "design").also { if (!it.isDirectory) throw GradleException("Missing design/ folder") }

        // The committed bundle Claude Design needs.
        val required = listOf("PROMPT.md", "design-tokens.json", "spec.html", "refinement-mockup.html")
        val missing = required.filterNot { File(design, it).exists() }
        if (missing.isNotEmpty()) throw GradleException("design/ bundle incomplete — missing: ${missing.joinToString()}")

        // Refresh design/assets/ from the live, committed atlas so sprite sizes never drift.
        val resources = File(root, "composeApp/src/commonMain/resources")
        val assets = File(design, "assets").apply { mkdirs() }
        var copied = 0
        for (name in listOf("sprites.png", "sprites.txt")) {
            val src = File(resources, name)
            if (!src.exists()) throw GradleException("Sprite atlas not found: ${src.relativeTo(root)} (run :composeApp:bakeAtlas)")
            src.copyTo(File(assets, name), overwrite = true); copied++
        }

        logger.lifecycle("✓ design bundle staged")
        logger.lifecycle("  bundle:  ${required.joinToString()}  (in design/)")
        logger.lifecycle("  assets:  refreshed $copied file(s) → design/assets/ from the sprite atlas")
        logger.lifecycle("")
        logger.lifecycle("Next — hand off to Claude Design (claude.ai/design):")
        logger.lifecycle("  1. open a design-system project and paste design/PROMPT.md")
        logger.lifecycle("  2. attach design-tokens.json, spec.html (+ screens/), refinement-mockup.html, assets/sprites.{png,txt}")
        logger.lifecycle("  (or use the /design-sync skill to push design/** for you)")

        open(File(design, "spec.html"))
    }

    private fun open(f: File) {
        val opener = if (System.getProperty("os.name").lowercase().contains("mac")) "open" else "xdg-open"
        exec.exec { commandLine(opener, f.path) }
    }
}

/**
 * Claude Design → Code. Reviews whatever was pulled back into design/incoming/ from a claude.ai/design
 * project: lists the files, opens any HTML for review, and prints how to apply the changes (tokens
 * first, then the owning scene). It does not auto-apply — UI changes land via hand-edits + a run.
 */
abstract class DesignImport : DefaultTask() {

    @get:Inject abstract val exec: ExecOperations
    @get:Inject abstract val layout: ProjectLayout

    @TaskAction
    fun import() {
        val root = layout.projectDirectory.asFile
        val incoming = File(root, "design/incoming")
        if (!incoming.isDirectory) throw GradleException("Missing design/incoming/ — create it and drop refined files there")

        val files = incoming.walkTopDown()
            .filter { it.isFile && it.name != ".gitkeep" && !it.name.startsWith(".") }
            .sortedBy { it.path }
            .toList()

        if (files.isEmpty()) {
            logger.lifecycle("design/incoming/ is empty.")
            logger.lifecycle("Pull refined component files from your Claude Design project into design/incoming/")
            logger.lifecycle("(or let the /design-sync skill write them there), then re-run designImport.")
            return
        }

        logger.lifecycle("✓ ${files.size} file(s) in design/incoming/:")
        files.forEach { logger.lifecycle("    ${it.relativeTo(incoming).path}  (${it.length()} bytes)") }
        logger.lifecycle("")
        logger.lifecycle("Apply changes:")
        logger.lifecycle("  1. reconcile any changed/added token into design/design-tokens.json (the contract)")
        logger.lifecycle("  2. edit the owning scene: MenuScene.kt / GameScene.kt / SettingsScene.kt")
        logger.lifecycle("  3. verify by RUNNING the app (./gradlew playWeb|playJvm|playAndroid)")

        files.filter { it.extension.equals("html", ignoreCase = true) }.forEach { open(it) }
    }

    private fun open(f: File) {
        val opener = if (System.getProperty("os.name").lowercase().contains("mac")) "open" else "xdg-open"
        exec.exec { commandLine(opener, f.path) }
    }
}
