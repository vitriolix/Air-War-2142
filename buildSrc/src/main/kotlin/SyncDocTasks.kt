import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import javax.inject.Inject

/**
 * Generate each design doc's "Tasks" section from the single TASKS.md source of truth.
 *
 * TASKS.md stays canonical. Tag any task item with an HTML comment naming the doc(s) it belongs
 * to (invisible when rendered) — `- [ ] #18 iОS target … &lt;!-- docs:0004 --&gt;` (one or more,
 * comma/space separated). Each `docs/NNNN-*.md` that contains a managed block (the marker pair
 * below) gets that block rewritten with every TASKS.md item tagged for its number `NNNN`,
 * checkbox state preserved. Edit tasks in TASKS.md, never inside the block.
 *
 *   &lt;!-- TASKS:auto START --&gt; … generated … &lt;!-- TASKS:auto END --&gt;
 *
 * `--check` writes nothing and FAILS if any block is stale (for CI / pre-commit).
 */
abstract class SyncDocTasks : DefaultTask() {

    @get:Option(option = "check", description = "Verify blocks are up to date; fail instead of writing.")
    @get:Input
    @get:Optional
    abstract val check: Property<Boolean>

    @get:Inject abstract val layout: ProjectLayout

    private val startMarker = "<!-- TASKS:auto START -->"
    private val endMarker = "<!-- TASKS:auto END -->"

    @TaskAction
    fun sync() {
        val root = layout.projectDirectory.asFile
        val checking = check.getOrElse(false)

        // ── 1. Collect tagged task lines from TASKS.md, grouped by doc number (in file order) ──
        val tasksFile = File(root, "TASKS.md")
        if (!tasksFile.exists()) throw GradleException("TASKS.md not found at ${tasksFile.path}")

        val tagRe = Regex("""<!--\s*docs:\s*([0-9,\s]+?)\s*-->""")
        val itemRe = Regex("""^\s*- \[[ xX~›]] """)               // a checkbox list item
        val byDoc = LinkedHashMap<String, MutableList<String>>()  // docId -> clean task lines

        tasksFile.forEachLine { line ->
            if (!itemRe.containsMatchIn(line)) return@forEachLine
            val tag = tagRe.find(line) ?: return@forEachLine
            val clean = line.replace(tagRe, "").trimEnd()          // drop the routing comment
            tag.groupValues[1].split(Regex("[,\\s]+")).filter { it.isNotBlank() }.forEach { id ->
                byDoc.getOrPut(id) { mutableListOf() }.add(clean)
            }
        }

        // ── 2. Rewrite the managed block in each opted-in doc (docs/NNNN-*.md) ──
        val blockRe = Regex("$startMarker.*?$endMarker", RegexOption.DOT_MATCHES_ALL)
        val docFiles = File(root, "docs").listFiles { f -> f.extension == "md" }?.sortedBy { it.name }
            ?: emptyList()

        val stale = mutableListOf<String>()
        var wrote = 0
        for (doc in docFiles) {
            val text = doc.readText()
            if (!blockRe.containsMatchIn(text)) continue            // no marker → not managed, skip
            val id = Regex("""^(\d{4})""").find(doc.name)?.groupValues?.get(1) ?: continue

            val newBlock = renderBlock(id, byDoc[id].orEmpty())
            val newText = text.replace(blockRe, Regex.escapeReplacement(newBlock))
            if (newText == text) continue

            if (checking) {
                stale.add(doc.relativeTo(root).path)
            } else {
                doc.writeText(newText)
                wrote++
                logger.lifecycle("synced ${doc.relativeTo(root).path} (${byDoc[id].orEmpty().size} tasks)")
            }
        }

        if (checking && stale.isNotEmpty()) {
            throw GradleException(
                "Doc task lists are out of date:\n  " + stale.joinToString("\n  ") +
                    "\nRun ./gradlew syncDocTasks to regenerate."
            )
        }
        logger.lifecycle(if (checking) "Doc task lists up to date." else "Synced $wrote doc(s) from TASKS.md.")
    }

    private fun renderBlock(id: String, lines: List<String>): String = buildString {
        append(startMarker).append("\n")
        append("## Tasks (from TASKS.md)\n\n")
        append("<!-- Generated from TASKS.md by `./gradlew syncDocTasks` — edit tasks there, not here. -->\n\n")
        if (lines.isEmpty()) {
            append("_No tasks are tagged for this doc yet._\n\n")
        } else {
            lines.forEach { append(it).append("\n") }
            append("\n")
        }
        append(endMarker)
    }
}
