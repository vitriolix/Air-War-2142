# 3D Aircraft Models

This directory contains (or will contain) production-ready 3D aircraft models sourced from Sketchfab and other platforms. Models are intentionally **not committed to git** because they are large binary files (50–500 MB each).

## Why Not in Git?

- **Repo size:** 3D models bloat the repository significantly
- **Merge conflicts:** Binary files don't merge cleanly
- **Bandwidth:** Cloning becomes slow and wasteful for developers who don't need models locally

## Getting the Models

### Using the Gradle Task

Sketchfab requires manual download for free models (no direct API access for authenticated downloads). A Gradle task provides a helper interface:

```bash
# List available models
./gradlew downloadModels

# Download a specific model — by number, short name, or full name
./gradlew downloadModels --model=4
./gradlew downloadModels --model="Bf-109"
./gradlew downloadModels --model="Messerschmitt Bf-109"

# Download all models
./gradlew downloadModels --all
```

Each model will provide instructions on where to save the downloaded `.glb` file.

### Interactive Picker (run directly)

`./gradlew downloadModels` can't prompt — task execution always happens in a
forked, terminal-detached Gradle daemon process, even with `--no-daemon`
(same limitation as `pruneBranches`; see `TASKS.md` #20 — this is inherent to
Gradle's architecture, not fixable via flags). For an interactive "enter a
number" picker, run the script directly instead:

```bash
./scripts/download-models.sh
```

### Manual Download (Alternative)

For direct links, see `design/ASSETS_MANIFEST.md` (lists all Sketchfab URLs).

## Organized Structure

Once downloaded, organize models by aircraft type:

```
models/
├── fighters/
│   ├── bf109.glb
│   ├── p51d_mustang.glb
│   ├── spitfire_mk14.glb
│   ├── fokker_dr1.glb
│   └── a26_invader.glb
├── bombers/
│   ├── b17_flying_fortress.glb
│   ├── b25_mitchell.glb
│   ├── he111.glb
│   └── ju88.glb
└── README.md (this file)
```

## Models Are Dev-Box-Only — Code↔Design Handoff

The `.glb`/`.fbx`/`.blend` source models never leave a developer's machine (not committed
— see "Why Not in Git?" above — and not sent to Design as files; they're 50–500 MB each).
Instead:

- **Claude Desktop** (with Blender MCP) owns the full 3D models locally. It drives Blender
  directly for any **geometry** work — proportions, damage-variant meshes, pose/rig
  changes, camera setup for batch rendering. See `BLENDER_HANDOFF.md` for the
  stylize/reskin/render pipeline.
- **Claude Design** never receives model files. It works on **textures, materials, and
  animations** from a **prompt + pixel-perfect screenshots/renders** exported by
  Desktop/Blender (the same per-screen-spec discipline as the 2D `design/` round-trip —
  see `design/PROMPT.md`). Design's texture/material decisions get described back as a
  spec (paint scheme, weathering, decal placement — see `COLOR_SWATCHES.md`) for Desktop
  to apply in Blender, rather than Design touching geometry directly.
- This keeps the repo lean (no binary model bloat) while still giving Design enough
  visual fidelity (renders, not the raw mesh) to make stylization calls.

## Usage in Game

3D models are **never loaded at runtime** — KorGE doesn't have a mature 3D-model-loading
module (the experimental `korge-k3d` only generates procedural primitives, no
GLB/glTF import), and runtime 3D would blow the perf gate anyway. Once models are
finished in Blender:

1. Batch-render each (chonkyness/stretch/damage/livery) variant to PNG at game scale
   (see `BLENDER_HANDOFF.md` Phase 4)
2. Bake the renders into a sprite atlas via the `resvg` / sprite-atlas pipeline
3. Place the resulting PNG + metadata in `assets/sprites/aircraft/` and reference by
   type in gameplay code, same as any other pre-baked sprite (architecture decision #8)

## Attribution

All models require proper attribution in game credits. See `design/ASSETS_MANIFEST.md` for creator names and license terms.

**License:** CC-BY-4.0 (credit required)

## Future: Automation

When the CI/asset-ingestion pipeline is built (task #5–#11 in TASKS.md):
- Sketchfab direct links → download via API
- Quality assessment → Claude integration
- Batch rendering → Blender automation
- Sprite atlas generation → resvg pipeline

For now, models are downloaded manually on-demand.
