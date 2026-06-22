plugins {
    alias(libs.plugins.korge)
    // SPIKE (spike/compose-korge-interop): Compose-MP host shell applied alongside the KorGE plugin.
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // No version: Dokka is already on the build classpath (pulled in transitively by KorGE),
    // so re-declaring a version errors ("already on the classpath with an unknown version").
    id("org.jetbrains.dokka")
}

// SPIKE: Compose dependencies for the Android host shell only (the rest of the targets keep the
// pure-KorGE UI for now). Proves the Compose-MP + KorGE plugins coexist in one :composeApp module.
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation("androidx.activity:activity-compose:1.9.3")
        }
        // SPIKE: probe whether Compose-MP UI compiles for the Wasm web target alongside KorGE.
        wasmJsMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
        }
    }
}

// SPIKE: Compose 1.7.x's Android artifacts require compileSdk 34+ (KorGE defaults to 33).
// The top-level `android {}` accessor collides with Compose's extension, so configure AGP's
// extension explicitly by its common supertype. In afterEvaluate so it wins over KorGE's setup.
afterEvaluate {
    extensions.configure<com.android.build.gradle.BaseExtension>("android") {
        compileSdkVersion(34)
    }
}

// API reference (Dokka — the Kotlin Javadoc). Output to the root build/api/ so the README
// link (build/api/index.html) is short. Generate via the root `apiDocs` task.
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    moduleName.set("Air-War-2142")
    outputDirectory.set(rootProject.layout.buildDirectory.dir("api"))
}

korge {
    id = "com.vitriolix.airwar2142"
    version = "1.0.0"
    name = "Air War 2142"
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
// Runs com.vitriolix.airwar2142.bake.BakeAtlas (in jvmMain) on the JVM compilation classpath.
tasks.register<JavaExec>("bakeAtlas") {
    group = "game"
    description = "Render vector shapes to a sprite atlas (build-time)."
    val jvmMainComp = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn(jvmMainComp.compileTaskProvider)
    classpath = files(jvmMainComp.output.allOutputs, jvmMainComp.runtimeDependencyFiles)
    mainClass.set("com.vitriolix.airwar2142.bake.BakeAtlasKt")
    val resDir = layout.projectDirectory.dir("src/commonMain/resources")
    args(resDir.asFile.absolutePath)
    outputs.dir(resDir)
}
