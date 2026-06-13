plugins {
    alias(libs.plugins.korge)
    // No version: Dokka is already on the build classpath (pulled in transitively by KorGE),
    // so re-declaring a version errors ("already on the classpath with an unknown version").
    id("org.jetbrains.dokka")
}

// API reference (Dokka — the Kotlin Javadoc). Output to the root build/api/ so the README
// link (build/api/index.html) is short. Generate via the root `apiDocs` task.
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    moduleName.set("Air War 2142")
    outputDirectory.set(rootProject.layout.buildDirectory.dir("api"))
}

korge {
    id = "com.example.clone1942"
    version = "1.0.0"
    name = "1942 Retro Clone"
    targetJvm()
    targetAndroid()
    targetJs()
    targetWasmJs()
}

// KorGE doesn't declare createAndroidManifest as a dependency of the Android
// resource tasks, so mergeDebugResources can run before the generated resources exist.
afterEvaluate {
    tasks.matching { it.name.startsWith("merge") && it.name.contains("Resources") }
        .configureEach { dependsOn("createAndroidManifest") }
}

// Build-time sprite-atlas bake — renders the vector shapes to a PNG atlas headlessly.
// Runs com.example.clone1942.bake.BakeAtlas (in jvmMain) on the JVM compilation classpath.
tasks.register<JavaExec>("bakeAtlas") {
    group = "game"
    description = "Render vector shapes to a sprite atlas (build-time)."
    val jvmMainComp = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn(jvmMainComp.compileTaskProvider)
    classpath = files(jvmMainComp.output.allOutputs, jvmMainComp.runtimeDependencyFiles)
    mainClass.set("com.example.clone1942.bake.BakeAtlasKt")
    val resDir = layout.projectDirectory.dir("src/commonMain/resources")
    args(resDir.asFile.absolutePath)
    outputs.dir(resDir)
}
