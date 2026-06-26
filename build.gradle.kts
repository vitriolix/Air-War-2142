plugins {
    alias(libs.plugins.korge) apply false
}

// ──────────────────────────────────────────────────────────────────────────────
// Dev / ops task surface — the canonical commands for this project.
//   Run with:  ./gradlew <task>     (e.g. ./gradlew playWeb, ./gradlew release)
//   npm run <alias> is a thin shim that just calls ./gradlew <task>.
//
// Build/test/run tasks delegate to the real :composeApp tasks. Shell-heavy
// orchestration (launch browser, adb, git, gh) wraps the scripts in scripts/.
// ──────────────────────────────────────────────────────────────────────────────
val gameGroup = "game"
fun org.gradle.api.tasks.Exec.runScript(vararg args: String) {
    group = gameGroup
    workingDir = rootDir
    commandLine(listOf("bash") + args)
}

// --- Launch / play ---------------------------------------------------------------
tasks.register("playJvm") {
    group = gameGroup; description = "Launch the JVM desktop app."
    dependsOn(":composeApp:runJvm")
}
tasks.register<Exec>("playWeb") {
    description = "Build, serve, and open the web (JS) app in a browser."
    runScript("scripts/play.sh", "web")
}
tasks.register<Exec>("playWebHeadless") {
    description = "Serve the web (JS) app without opening a browser (prints the URL)."
    runScript("scripts/play.sh", "web", "--headless")
}
tasks.register<Exec>("playWasm") {
    description = "Build, serve, and open the web (Wasm) app in a browser."
    runScript("scripts/play.sh", "wasm")
}
tasks.register<Exec>("playWasmHeadless") {
    description = "Serve the web (Wasm) app without opening a browser (prints the URL)."
    runScript("scripts/play.sh", "wasm", "--headless")
}
tasks.register<Exec>("playAndroid") {
    description = "Install & launch on a running Android emulator/device (adb)."
    runScript("scripts/play.sh", "android")
}
tasks.register<Exec>("playNative") {
    description = "No Kotlin/Native desktop target — explains the alternative."
    runScript("scripts/play.sh", "native")
    isIgnoreExitValue = true // it intentionally exits non-zero with guidance
}

// --- Tests -----------------------------------------------------------------------
tasks.register("testJvm")     { group = gameGroup; description = "Run JVM tests.";          dependsOn(":composeApp:jvmTest") }
tasks.register("testWeb")     { group = gameGroup; description = "Run JS tests.";           dependsOn(":composeApp:jsTest") }
tasks.register("testWasm")    { group = gameGroup; description = "Run Wasm tests.";         dependsOn(":composeApp:wasmJsTest") }
tasks.register("testAndroid") { group = gameGroup; description = "Run Android unit tests."; dependsOn(":composeApp:testDebugUnitTest") }
tasks.register("testAll")     { group = gameGroup; description = "Run all tests.";          dependsOn(":composeApp:allTests") }

// --- Housekeeping ----------------------------------------------------------------
tasks.register<WebConsole>("webConsole") { group = gameGroup; description = "Boot the web build and stream its browser console + errors to the terminal. --headed for a visible Chrome for Testing window; --target=wasm for the Wasm build." }
// Native custom-task helpers (buildSrc) — logic lives in the Gradle task context, not bash.
tasks.register<RenderDocs>("renderDocs") { group = gameGroup; description = "Render Markdown docs (README + docs/) to HTML and open the index. --file=<path> for one." }
tasks.register<PrOpen>("openPr")         { group = gameGroup; description = "Open a GitHub PR's page in the browser. --pr=<number|branch> (default: current branch)." }
tasks.register<DesignExport>("designExport") { group = gameGroup; description = "Code → Claude Design: stage the design/ handoff bundle (refresh assets, open spec, print push steps)." }
tasks.register<DesignImport>("designImport") { group = gameGroup; description = "Claude Design → Code: review refined files dropped in design/incoming/ and print how to apply them." }
tasks.register<DesignPush>("designPush")     { group = gameGroup; description = "Code → Claude Design: compute the Code-owned push manifest (design/.design-push-manifest.json) for /design-sync to upload." }
tasks.register<Exec>("captureScreens") {
    group = gameGroup
    description = "Recapture design/screens/*.png from the live web build. HEADED + 1:1 only — never headless (SwiftShader fakes SDF text slivers); see scripts/capture-screens.js."
    dependsOn("playWebHeadless"); finalizedBy("killServers")   // serve the JS build, then stop it after
    workingDir = rootDir
    commandLine("node", "scripts/capture-screens.js")
}
tasks.register<SyncDocTasks>("syncDocTasks") { group = gameGroup; description = "Regenerate each doc's Tasks block from TASKS.md (single source). --check fails if stale (CI)." }
tasks.register<SyncDocTasks>("checkDocTasks") { group = gameGroup; description = "Backstop: fail if any doc's Tasks block is out of sync with TASKS.md. Run by tidyGit/releaseCheckGit."; check.set(true) }
tasks.register<Exec>("installGitHooks") { group = gameGroup; description = "Point git at the committed hooks (scripts/hooks) — auto-syncs doc task blocks on commit."; workingDir = rootDir; commandLine("git", "config", "core.hooksPath", "scripts/hooks") }
tasks.register("renderApiDocs") { group = gameGroup; description = "Render the Kotlin API reference (Dokka HTML) → build/api/index.html. Pairs with renderDocs."; dependsOn(":composeApp:dokkaHtml") }
tasks.register<Exec>("tidyGit")     { description = "Verify a clean git working tree + push state."; dependsOn("checkDocTasks"); runScript("scripts/tidy-git.sh") }
tasks.register<Exec>("killServers") { description = "Stop the JS/Wasm dev servers and runJvm.";      runScript("scripts/kill-servers.sh") }
tasks.register<Exec>("createPr")    { description = "Push the current branch and open a GitHub PR (gh)."; runScript("scripts/create-pr.sh") }
tasks.register<Exec>("pruneBranches") { description = "Delete local branches whose upstream is gone AND merged; prompt about any not-yet-merged ones. (script: --dry-run)"; runScript("scripts/prune-branches.sh") }

// --- Release prep (each step standalone) -----------------------------------------
tasks.register<Exec>("releaseCheckGit") { description = "Release: ensure git is tidy.";        dependsOn("checkDocTasks"); runScript("scripts/tidy-git.sh") }
tasks.register("releaseTest")           { group = gameGroup; description = "Release: run all tests."; dependsOn(":composeApp:allTests") }
tasks.register("releaseBuild") {
    group = gameGroup; description = "Release: build JVM jar + web prod bundle + Android release APK."
    dependsOn(":composeApp:jvmJar", ":composeApp:jsBrowserProductionWebpack", ":composeApp:assembleRelease")
}
tasks.register<Exec>("releaseVersion") { description = "Release: print the app version.";              runScript("scripts/release.sh", "version") }
tasks.register<Exec>("releaseBranch")  { description = "Release: create/switch to release/<version>."; runScript("scripts/release.sh", "branch") }
tasks.register<Exec>("releaseTag")     { description = "Release: tag v<version> (after committing).";  runScript("scripts/release.sh", "tag") }

// Ordered pipeline: check-git → test → build
tasks.named("releaseTest").configure  { mustRunAfter("releaseCheckGit") }
tasks.named("releaseBuild").configure { mustRunAfter("releaseTest") }
tasks.register("release") {
    group = gameGroup; description = "Release prep: check-git → test → build, in order."
    dependsOn("releaseCheckGit", "releaseTest", "releaseBuild")
}
