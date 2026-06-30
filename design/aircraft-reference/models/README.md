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

# Download a specific model
./gradlew downloadModels --model="Bf-109"

# Download all models
./gradlew downloadModels --all
```

Each model will provide instructions on where to save the downloaded `.glb` file.

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

## Usage in Game

Once models are downloaded:
1. Place in the appropriate subdirectory above
2. Create a KorGE model loader in `src/common/render/ModelAssets.kt`
3. Reference models by type in gameplay code
4. Test rendering + sprite baking via Blender

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
