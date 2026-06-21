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
        if (System.getenv("CI") != null) return   // headless CI (e.g. Pages build): nothing to open into
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
        if (System.getenv("CI") != null) return   // headless CI (e.g. Pages build): nothing to open into
        val opener = if (System.getProperty("os.name").lowercase().contains("mac")) "open" else "xdg-open"
        exec.exec { commandLine(opener, f.path) }
    }
}

/**
 * Code → Claude Design (push manifest). Computes the exact set of **Code-owned** files to upload to
 * the claude.ai/design project and their remote paths, then writes `design/.design-push-manifest.json`.
 *
 * It deliberately does NOT perform the upload: the claude.ai/design write goes through Claude's
 * `/design-sync` (DesignSync) tool, authenticated by the user's claude.ai login — there is no public
 * endpoint a Gradle task can POST to. So this task's job is to encode the **policy** once (which globs
 * are Code-owned source-of-truth vs Design-authored, and how local files map to remote paths), so the
 * sync isn't a hand-maintained file list. `/design-sync` then consumes the manifest for a deterministic
 * upload. Run `designExport` first if the staged atlas may be stale (it refreshes `design/assets/`).
 */
abstract class DesignPush : DefaultTask() {

    @get:Inject abstract val layout: ProjectLayout

    @TaskAction
    fun push() {
        val root = layout.projectDirectory.asFile
        val design = File(root, "design").also { if (!it.isDirectory) throw GradleException("Missing design/ folder") }

        // Fixed Code→Design bundle (stable remote paths).
        val bundleFiles = listOf("PROMPT.md", "README.md", "design-tokens.json", "spec.html", "refinement-mockup.html")
        val missing = bundleFiles.filterNot { File(design, it).exists() }
        if (missing.isNotEmpty()) throw GradleException("design/ bundle incomplete — missing: ${missing.joinToString()} (run designExport?)")

        // Warn (don't fail) if the staged atlas drifted from the committed one — designExport refreshes it.
        val stagedAtlas = File(design, "assets/sprites.png")
        val liveAtlas = File(root, "composeApp/src/commonMain/resources/sprites.png")
        if (liveAtlas.exists() && stagedAtlas.exists() && stagedAtlas.length() != liveAtlas.length()) {
            logger.warn("⚠ design/assets/sprites.png differs from the committed atlas — run ./gradlew designExport to refresh before pushing.")
        }

        // Build the push set: fixed bundle + globbed assets/screens. Globs encode the policy, so new
        // screenshots/assets are picked up without editing this task.
        data class Entry(val local: String, val remote: String, val note: String? = null)
        val push = mutableListOf<Entry>()
        bundleFiles.forEach { push += Entry("design/$it", it) }
        listOf("assets/sprites.png", "assets/sprites.txt")
            .filter { File(design, it).exists() }
            .forEach { push += Entry("design/$it", it) }
        // screens/*.png are Code-captured baselines; screens/*.html are Design-authored (NOT pushed).
        File(design, "screens").listFiles().orEmpty()
            .filter { it.isFile && it.extension.equals("png", true) }
            .sortedBy { it.name }
            .forEach { push += Entry("design/screens/${it.name}", "screens/${it.name}") }
        // requests/*.md: the round number is curated on the remote, which this task can't see — flag for
        // /design-sync to assign the next `roundN-` by listing remote requests/ at push time.
        val requests = File(design, "requests").listFiles().orEmpty()
            .filter { it.isFile && it.extension.equals("md", true) }
            .sortedBy { it.name }
            .map { Entry("design/requests/${it.name}", "requests/round<N>-${it.name}", "assign next roundN from remote requests/ at push") }

        // Design-authored (never overwrite from Code).
        val designOwned = listOf("screens/*.html", "foundations/**", "uploads/**", "design_handoff_*/**", "screenshots/**", "styles.css", "_ds_*", ".thumbnail")

        fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
        fun entry(e: Entry) = buildString {
            append("    {\"local\": \"${esc(e.local)}\", \"remote\": \"${esc(e.remote)}\"")
            if (e.note != null) append(", \"note\": \"${esc(e.note)}\"")
            append("}")
        }
        val json = buildString {
            appendLine("{")
            appendLine("  \"_comment\": \"Generated by ./gradlew designPush. Code-owned files for /design-sync to upload to claude.ai/design — the upload itself is done by Claude's DesignSync tool, not Gradle.\",")
            appendLine("  \"projectName\": \"Air War 2142\",")
            appendLine("  \"bundle\": [")
            appendLine(push.joinToString(",\n") { entry(it) })
            appendLine("  ],")
            appendLine("  \"requests\": [")
            appendLine(requests.joinToString(",\n") { entry(it) })
            appendLine("  ],")
            appendLine("  \"designOwned_doNotPush\": [${designOwned.joinToString(", ") { "\"${esc(it)}\"" }}]")
            append("}")
        }
        val manifest = File(design, ".design-push-manifest.json")
        manifest.writeText(json + "\n")

        logger.lifecycle("✓ design push manifest → ${manifest.relativeTo(root)}")
        logger.lifecycle("  bundle:   ${push.size} Code-owned file(s), fixed remote paths")
        logger.lifecycle("  requests: ${requests.size} brief(s) — roundN assigned at push time")
        logger.lifecycle("")
        logger.lifecycle("Upload is NOT done here. Ask Claude to run /design-sync — it reads this manifest")
        logger.lifecycle("and writes to the claude.ai/design project (authenticated by your claude.ai login).")
    }
}
