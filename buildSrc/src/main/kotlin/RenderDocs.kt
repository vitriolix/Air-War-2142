import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Render the project's Markdown docs to HTML via pandoc and open a navigable index.
 * Collects the root README.md, the docs/ folder, nested READMEs (e.g. scripts/README.md),
 * the root README is the landing page (its .md links are rewritten to the rendered .html).
 * Pass `--file=<path.md>` to render and open just one file. Output → build/docs/ (gitignored).
 */
abstract class RenderDocs : DefaultTask() {

    @get:Option(option = "file", description = "Render just this Markdown file and open it.")
    @get:Input
    @get:Optional
    abstract val file: Property<String>

    @get:Inject abstract val exec: ExecOperations
    @get:Inject abstract val layout: ProjectLayout

    private val styleHtml = """
        <style>
          :root { color-scheme: light; }
          body { max-width: 880px; margin: 0 auto; padding: 40px 28px 80px;
                 font: 16px/1.65 -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
                 color: #1f2328; background: #fff; }
          h1,h2,h3 { line-height: 1.25; margin-top: 1.6em; }
          h1 { font-size: 2em; border-bottom: 1px solid #d1d9e0; padding-bottom: .3em; }
          h2 { font-size: 1.5em; border-bottom: 1px solid #d1d9e0; padding-bottom: .3em; }
          h3 { font-size: 1.2em; }
          a { color: #0969da; text-decoration: none; } a:hover { text-decoration: underline; }
          code { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 85%;
                 background: #eff1f3; padding: .2em .4em; border-radius: 6px; }
          pre { background: #f6f8fa; padding: 14px 16px; border-radius: 8px; overflow: auto; line-height: 1.45; }
          pre code { background: none; padding: 0; font-size: 13px; }
          table { border-collapse: collapse; margin: 1em 0; display: block; overflow: auto; }
          th, td { border: 1px solid #d1d9e0; padding: 6px 13px; }
          th { background: #f6f8fa; } tr:nth-child(2n) td { background: #f6f8fa; }
          blockquote { margin: 1em 0; padding: 0 1em; color: #59636e; border-left: .25em solid #d1d9e0; }
          hr { border: 0; border-top: 1px solid #d1d9e0; margin: 2em 0; }
          ul,ol { padding-left: 1.6em; }
          input[type=checkbox] { margin-right: .4em; }
          p.sub { color: #59636e; margin-top: -.3em; }
          ul.idx { list-style: none; padding: 0; }
          ul.idx li { border: 1px solid #d1d9e0; border-radius: 10px; padding: 14px 18px; margin: 12px 0; }
          ul.idx li:hover { border-color: #0969da; background: #f6f8fa; }
          ul.idx .t { font-size: 1.15em; font-weight: 600; }
          ul.idx .f { float: right; color: #8b949e; font-family: ui-monospace, Menlo, monospace; font-size: 12px; }
          ul.idx p { margin: .4em 0 0; color: #59636e; }
        </style>
    """.trimIndent()

    @TaskAction
    fun render() {
        val root = layout.projectDirectory.asFile
        val out = layout.buildDirectory.dir("docs").get().asFile.apply { mkdirs() }
        val styleFile = File(out, "_style.html").apply { writeText(styleHtml) }

        // ── single-file mode ──
        file.orNull?.let { rel ->
            val src = File(rel).takeIf { it.isAbsolute && it.exists() } ?: File(root, rel)
            if (!src.exists()) throw GradleException("No such file: $rel")
            val outFile = File(out, src.nameWithoutExtension + ".html")
            renderOne(src, outFile, styleFile, titleOf(src) ?: src.name)
            open(outFile)
            return
        }

        // ── all-docs mode: root README → docs/*.md → nested READMEs ──
        val rootReadme = File(root, "README.md")
        val docs = buildList {
            if (rootReadme.exists()) {
                add(rootReadme)
                // …and everything the README links (relative .md targets that exist), so the
                // README's links always resolve in the rendered output.
                Regex("]\\(([^)]+\\.md)(?:#[^)]*)?\\)").findAll(rootReadme.readText()).forEach { m ->
                    File(root, m.groupValues[1]).takeIf { it.exists() }?.let(::add)
                }
            }
            File(root, "docs").listFiles { f -> f.extension == "md" }?.sortedBy { it.name }?.let(::addAll)
            root.walkTopDown()
                .onEnter { it.name !in setOf("build", "node_modules", ".git", ".gradle", ".kotlin") }
                .filter { it.isFile && it.name == "README.md" && it.parentFile != root }
                .sortedBy { it.path }
                .forEach { add(it) }
        }.distinctBy { it.canonicalPath }
        if (docs.isEmpty()) throw GradleException("No Markdown docs found (README.md or docs/*.md).")

        val entries = StringBuilder()
        for (src in docs) {
            val rel = src.relativeTo(root).path
            val slug = rel.removeSuffix(".md").replace(Regex("[/ ]+"), "-")  // path-based → collision-free
            val title = titleOf(src) ?: rel
            val outFile = File(out, "$slug.html")
            renderOne(src, outFile, styleFile, title)
            relink(outFile)                                                  // .md cross-links → rendered .html
            entries.append("<li><span class=\"f\">${esc(rel)}</span>")
                .append("<a href=\"$slug.html\"><span class=\"t\">${esc(title)}</span></a>")
                .append("<p>${esc(blurbOf(src))}</p></li>")
            logger.lifecycle("rendered $rel")
        }

        // Root README is the index/landing when present; else generate a standalone index.
        if (rootReadme.exists()) {
            logger.lifecycle("Rendered ${docs.size} docs → build/docs/ (landing: README)")
            open(File(out, "README.html"))
        } else {
            File(out, "index.html").writeText(
                styleHtml +
                    "<h1>📚 Project docs</h1>" +
                    "<p class=\"sub\">No root README.md — generated index.</p>" +
                    "<ul class=\"idx\">$entries</ul>"
            )
            logger.lifecycle("Built docs index (${docs.size} docs) → build/docs/index.html")
            open(File(out, "index.html"))
        }
    }

    private fun renderOne(src: File, outFile: File, style: File, title: String) {
        exec.exec {
            commandLine(
                "pandoc", src.path, "-f", "gfm", "-t", "html5", "-s",
                "--metadata", "title=$title", "--include-in-header", style.path, "-o", outFile.path
            )
        }
    }

    private fun relink(f: File) {
        f.writeText(f.readText().replace(Regex("href=\"([^\"#]+)\\.md(#[^\"]*)?\"")) { m ->
            "href=\"" + m.groupValues[1].replace("/", "-") + ".html" + m.groupValues[2] + "\""
        })
    }

    private fun open(f: File) {
        val opener = if (System.getProperty("os.name").lowercase().contains("mac")) "open" else "xdg-open"
        exec.exec { commandLine(opener, f.path) }
    }

    private fun titleOf(f: File): String? =
        f.useLines { ls -> ls.firstOrNull { it.startsWith("# ") } }?.removePrefix("# ")?.trim()

    private fun blurbOf(f: File): String = f.useLines { seq ->
        var seenHeading = false
        seq.firstNotNullOfOrNull { raw ->
            val line = raw.trim()
            when {
                line.startsWith("#") -> { seenHeading = true; null }
                seenHeading && line.isNotEmpty() && !line.startsWith("![") && !line.matches(Regex("^[-=]+$")) ->
                    line.replace("**", "").replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")
                else -> null
            }
        } ?: ""
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
