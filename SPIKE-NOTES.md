# Spike: upgrade KorGE 6 → 7 (Kotlin 2.0.21 → modern)

**Branch:** `spike/korge7-bump` · investigation-only (no build changes) · 2026-06

**Question:** is moving to a KorGE that rides a modern Kotlin (≥2.1, needed for current
Compose-Multiplatform) feasible? **Verdict: not currently — it's a blocked path, not just a big one.**

## Why it's blocked

- Latest **released** KorGE is **6.0.0** (May 2025), pinned to **Kotlin 2.0.21** — what we're on.
- KorGE `main` is **7.0.0-SNAPSHOT**: **Kotlin 2.3.21 / Gradle 9.4.1 / AGP 9.x**, maven namespace
  moving `com.soywiz.korge` → `org.korge.korlibs`. The major infra-rewrite commit landed ~2026-06-21
  ("Upgrade project to Gradle and AGP 9 and korlibs 7.0.0-SNAPSHOT") — it's actively churning.
- **The engine + Gradle plugin are not published anywhere as v7.** Maven Central snapshots, checked:

  | Artifact | v7 snapshot? |
  |---|---|
  | `org.korge.korlibs:korlibs-image` | ✅ `7.0.0-SNAPSHOT` |
  | `org.korge.korlibs:korlibs-core` | ❌ 404 |
  | `org.korge.korlibs:korge` (engine) | ❌ 404 |
  | `org.korge.korlibs:korge-core` | ❌ 404 |
  | `org.korge.korlibs:korge-gradle-plugin` (what you `apply`) | ❌ 404 |

  KorGE's own `main` builds the plugin from source via `includeBuild("korge-gradle-plugins")`, and
  the docs have **no snapshot/nightly install path**. There is no published KorGE 7 plugin to depend on.

## What an upgrade would actually entail (once it's possible)

1. Today: build the engine + Gradle plugin from source and `publishToMavenLocal`, re-vendoring on every
   upstream churn — i.e. become a KorGE-from-source shop tracking an in-flight rewrite.
2. Our build: Gradle 8.14 → 9.4.1 + AGP 8.2 → 9.x (new KMP DSL `com.android.kotlin.multiplatform.library`;
   the `builtInKotlin`/`newDsl` flags already in `gradle.properties` were pre-staged for this).
3. Kotlin 2.0.21 → 2.3.21 (three minor jumps).
4. Namespace change across every KorGE dependency + plugin id.
5. KorGE 6 → 7 **breaking API** surface — re-verify the protected input layer per CLAUDE.md
   (`HeldKey` deferred release, `singleTouch` Anywhere, per-backend grave key, `stage.keys{}`),
   scene/view APIs, `CANVAS_HEIGHT` coupling, sprite-atlas path.
6. Full perf-gate re-A/B vs the web baseline (~3.9ms / p95 ~7.3ms) on all four targets.

## Recommendation

Wait for a published KorGE 7 **alpha/beta with documented coordinates** (v6 took ~11 months
alpha→stable; v7 has no tag yet), then treat it as a normal large upgrade. Revisit modern
Compose-MP at that point.

If Android/iOS Compose menus are the real goal, the **CMP 1.7.3 path works today on the current
KorGE 6 / Kotlin 2.0.21 stack** (see branch `spike/compose-korge-interop`) — none of this v7 pain.
