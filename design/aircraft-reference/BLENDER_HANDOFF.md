# Blender MCP Handoff — 3D Model Stylization & Rendering Pipeline

**Goal:** Use Blender + MCP to stylize, reskin, and render high-quality 3D models into game-ready sprite atlases with consistent design language.

**Philosophy:** 
- **Primary:** Use existing high-quality 3D models (from Sketchfab, Thingiverse, etc.)
- **Secondary:** Supplement with technical schematics for dimension reference only
- **Tertiary:** Use reference photography for texture, paint, and damage inspiration

This is a **stylization + reskinning pipeline**, not a modeling-from-scratch pipeline.

---

## 📦 Input Reference Package

All assets are organized per the Asset Ingestion Process (see `/design/ASSET_INGESTION_PROCESS.md`). Use them together as a unified reference system:

### Schematics (`/schematics/`)
- **14 full 3-view technical drawings** — accurate proportions, scale-neutral vector graphics
- Each aircraft shown in **full 3-view orthographic** (top/side/front) for complete 3D geometry reference
- 12 SVG files + 2 bitmaps (PNG/JPG) — bitmaps pending SVG conversion via task #32 pipeline
- Source: Wikimedia Commons — CC0/CC-BY-SA/public domain
- **Best for:** Wing/fuselage geometry, tail surfaces, engine nacelles, landing gear layout
- **Side-view references:** 4 additional profile SVGs in `/photos/` for texture/skin design inspiration during modeling

**Key aircraft for priority generation:**
1. **Messerschmitt Bf-109** (German fighter — compact, single-engine) — most detailed schematic
2. **North American P-51 Mustang** (US fighter — inline engine, wide stance) — iconic profile
3. **Supermarine Spitfire** (RAF fighter — elliptical wing, curved fuselage) — subtle geometry
4. **Boeing B-17** (Heavy bomber — 4-engine, complex, most photographic detail)
5. **Heinkel He-111** (German medium bomber — asymmetric nose, twin-tail)

### Paint Scheme Reference (`PAINT_SCHEMES.md`)
- 5 geographic biomes with authentic **base colors + camouflage patterns**
- RAF: Dark Green + Dark Earth
- Luftwaffe: RLM 70/71 (green/brown)
- USAAF: Olive Drab
- Arctic: White distemper
- **Best for:** Material/texture color selection and decal placement

### Paint Photos (`/photos/{temperate,tropical,arctic}/`)
- **8 high-resolution museum/airshow photos** — reference for wear, weathering, panel lines
- **Highest detail:** `boeing-b-17-sentimental-journey-nose-art.jpg` (15 MB) — museum-restored, pristine paint, visible rivet detail
- **For realistic:** panel line shadows, paint chipping, metal reflections, nose art placement
- **Best for:** Texture refinement, damage variant planning, decal/marking positions

---

## 🎯 Stylization & Rendering Strategy

### Phase 1: Import & Prepare Base 3D Model

1. **Download high-quality 3D model** (from Sketchfab, Thingiverse, etc.)
   - Use model as-is if proportions are accurate to historical specs (within 5%)
   - No need to model from scratch — existing models are game-ready
2. **Clean up geometry**
   - Remove unnecessary detail (extreme high-poly counts)
   - Verify UV layout is usable
   - Check for degenerate faces, overlapping geometry
3. **Extract/document dimensions**
   - Measure wingspan ÷ length ratio in Blender
   - Cross-reference with schematic (should match within 5%)
   - Document for stylization parameter baseline

### Phase 2: Apply Stylization Parameters

All stylization is done via **modifiers + scaling**, not re-modeling:

1. **Chonkyness Modifier** (0.0–2.0)
   - Scale fuselage diameter, engine nacelles, control surfaces thickness
   - Use proportional editing or weighted scale modifiers
   - Affects "mass perception" without changing proportions fundamentally

2. **Stretch Modifier** (0.5–2.0)
   - Scale fuselage length and wing span proportionally
   - Apply uniformly to maintain proportions
   - Example: "stretch=0.8" = compact variant

3. **Damage Decals** (Level 0–5)
   - Bullet holes: normal-mapped decals placed on fuselage
   - Tears: geometry deformation + alpha mask
   - Weathering: overlay texture with wear patterns
   - Fire damage: charred material + glow effects (cosmetic)

4. **Material Slots for Liveries**
   - Create separate material slots per paint scheme
   - Swap base color + decal textures without re-modeling
   - Example: RLM 70/71, USAAF Olive Drab, captured variants

### Phase 3: Texture & Reskinning

1. **Base Paint Application**
   - Apply color from `PAINT_SCHEMES.md` (RGB values)
   - Create new material for each livery variant
   - Base coat only (no complex shaders needed for sprite rendering)

2. **Weathering & Wear**
   - Layer weathering textures (from museum reference photos)
   - Subtle panel line shadows (use normal maps or baked shadows)
   - Paint chipping at edges, stress points

3. **Decal Placement**
   - Squadron emblems, fuselage codes, markings (from historical reference)
   - Nose art (if applicable)
   - RAF roundels, Balkenkreuz, USAAF stars, etc.

4. **Generate Material Variants**
   - Script to create material combo for each (chonkyness, stretch, livery, damage_level)
   - Export to sprite atlas pipeline (see Phase 4)

### Phase 4: Batch Rendering to Sprite Atlas

1. **Setup Camera Angles**
   - Isometric (45° azimuth, 30° elevation) — preferred for game view
   - Side view (0° azimuth, 0° elevation) — reference/UI
   - Top view (0° azimuth, 90° elevation) — tactical view (optional)

2. **Batch Render All Variants**
   ```
   For each (chonkyness, stretch, damage_level, livery):
     - Set modifier parameters
     - Swap material slots
     - Render at 256×256 or 512×512 (game-scale resolution)
     - Save as bf-109_chonky-1.0_stretch-1.0_dmg-2_rlm-70.png
   ```

3. **Bake to Sprite Atlas**
   - Use resvg / sprite-atlas pipeline (existing tool)
   - Generate sprite metadata (frame positions, collision bounds)
   - Output: one PNG atlas + JSON metadata file

4. **Generate Damage Progression Sequence**
   - 6 frames: pristine → battle-worn → light damage → moderate → heavy → destroyed
   - Animate in-game as damage accumulates
   - Same progression applies across all aircraft (design consistency)

---

## 🔧 Workflow in Blender (Batch Rendering & Scripting)

### Setup

1. **Load Base 3D Model**
   - Open Blender 4.5 LTS
   - Import 3D model (FBX, OBJ, or Blend format)
   - Apply cleaning as needed (remove extra geometry, fix UVs)

2. **Create Stylization Structure**
   - Add empty objects for modifier origins
   - Create material slots for each livery variant
   - Setup modifiers for chonkyness/stretch scaling

3. **Setup Camera & Lighting**
   - Create fixed camera at isometric angle (45°, 30°)
   - Setup 3-point lighting (consistent across all aircraft)
   - Lock camera (no changes during batch render)

### Batch Rendering Script (Python)

Create a Python script in Blender to automate rendering all variants:

```python
# blender_batch_render.py
# Usage: blender -b model.blend --python blender_batch_render.py

import bpy
import json
from pathlib import Path

# Configuration
VARIANTS = [
    {"chonky": 1.0, "stretch": 1.0, "damage": 0, "livery": "rlm-70-71"},
    {"chonky": 1.0, "stretch": 1.0, "damage": 1, "livery": "rlm-70-71"},
    {"chonky": 1.2, "stretch": 1.0, "damage": 0, "livery": "rlm-70-71"},
    # ... all combinations
]

OUTPUT_DIR = "/path/to/sprites/"

def set_modifier_values(obj, chonky, stretch):
    """Apply chonkyness/stretch modifiers"""
    # Set scale on relevant objects
    for mat in obj.data.materials:
        # Material property updates if needed
        pass

def set_material_variant(livery):
    """Swap material for livery"""
    # Change active material to livery variant
    pass

def apply_damage_decals(damage_level):
    """Apply damage texture overlay"""
    # Adjust opacity/position of damage decals
    pass

def render_variant(variant):
    """Render single variant and save"""
    chonky = variant["chonky"]
    stretch = variant["stretch"]
    damage = variant["damage"]
    livery = variant["livery"]
    
    # Apply stylization
    set_modifier_values(bpy.context.active_object, chonky, stretch)
    set_material_variant(livery)
    apply_damage_decals(damage)
    
    # Render
    bpy.context.scene.render.filepath = (
        f"{OUTPUT_DIR}/bf-109_"
        f"chonky-{chonky}_stretch-{stretch}_dmg-{damage}_{livery}.png"
    )
    bpy.ops.render.render(write_still=True)

# Main loop
for variant in VARIANTS:
    render_variant(variant)

print("Batch rendering complete!")
```

### Manual Workflow (if scripting not feasible)

1. **For each variant:**
   - Adjust modifier parameters (chonkyness, stretch)
   - Swap material to livery variant
   - Apply damage decal layer
   - Render to PNG
   - Save with naming convention

2. **Naming Convention:**
   ```
   {aircraft}_{chonky-value}_{stretch-value}_{dmg-value}_{livery}.png
   
   Examples:
   bf-109_chonky-1.0_stretch-1.0_dmg-0_rlm-70-71.png
   bf-109_chonky-1.0_stretch-1.0_dmg-2_rlm-70-71.png
   bf-109_chonky-1.2_stretch-1.0_dmg-0_usaaf-olive.png
   ```

### Import into Game

Once sprite atlas is generated:
1. Place PNG + JSON metadata in `assets/sprites/aircraft/`
2. Write sprite loader in KorGE (frame mapping + collision bounds)
3. Bind to aircraft entity (assign sprite per damage level + livery)

---

## 📐 Precision Notes

### Scaling & Proportions
- Schematics are **dimensionless** but **proportionally accurate**
- Use wingspan ÷ fuselage-length ratios from schematic to size correctly
- Reference aircraft real-world dimensions:
  - **Bf-109:** 8.64 m long, 9.87 m span → 1:1.14 ratio
  - **P-51 Mustang:** 9.83 m long, 11.28 m span → 1:1.15 ratio
  - **B-17:** 20.75 m long, 31.62 m span → 1:1.52 ratio (longer, wider bomber)
  - **Spitfire:** 9.12 m long, 11.23 m span → 1:1.23 ratio

### Dihedral Angles (Wing tilt when viewed from front)
- Most fighters: 5–7° dihedral (slight upward tilt)
- B-17: ~6.5° dihedral
- Spitfire: ~5.5° dihedral (subtly flat wing)

### Engine Positions
- Single-engine (Bf-109, P-51, Spitfire): centerline, 1–2 fuselage diameters forward of wing
- Twin-engine (P-38): dual nacelles, 1–2 m inboard of each wing tip
- 4-engine (B-17): pairs of nacelles at ~1/3 and ~2/3 span

### Color Hex Codes (From PAINT_SCHEMES.md)
- **RAF Dark Green:** `#355E3B`
- **RAF Dark Earth:** `#6B5D47`
- **Luftwaffe RLM 70 (Green):** `#47593A`
- **Luftwaffe RLM 71 (Brown):** `#4D4037`
- **USAAF Olive Drab:** `#556B2F`
- **White distemper (Arctic):** `#FFFEF0` (off-white, weathered)

---

## 📋 Damage Variant Spec

After the base model, generate these damage/wear states:

| Variant | Description | Key Changes |
|---------|-------------|------------|
| **Pristine** | Fresh from factory paint, no wear | Base color, slight gloss |
| **Battle-worn** | 10+ flight hours, minor scuffs | Subtle panel line weathering, slight paint fade |
| **Light damage** | Flak/bullet near-miss: ricochet scuffs | Small bullet holes (non-critical area), scorch marks around wing roots |
| **Moderate damage** | Direct hit: control surface damage | Torn aileron/elevator, larger bullet cluster, smoke staining |
| **Heavy damage** | Mission-kill: severe structural | Collapsed landing gear, fuselage tear (taped), engine nacelle damage, control surface missing |
| **Burning/Crash** | End-state cosmetics | Charred fuselage, engine fire glow, trailing smoke particles (cosmetic VFX only) |

For each variant, export as `.glb` + baked-texture PNG (64×64 or 128×128 base aircraft sprite, damage decal overlay).

---

## ✅ Success Criteria

A generated aircraft model is production-ready when:
1. ✅ **Proportions accurate** — wingspan ÷ length matches schematic ratio (±5%)
2. ✅ **Recognizable silhouette** — distinctive shape (Spitfire elliptical wing, B-17 4-engine, etc.)
3. ✅ **Base paint applied** — solid color from `PAINT_SCHEMES.md` (per biome)
4. ✅ **Panel lines visible** — subtle shading along fuselage/wing panels (not cartoon-harsh)
5. ✅ **Damage variants exported** — at least 3 variants (pristine, battle-worn, heavy) as separate `.glb` files
6. ✅ **Landing gear poseable** — retracted and deployed positions (can toggle in Blender for screenshot verification)
7. ✅ **No T-pose issues** — geometry is sensible (no inverted/backwards surfaces, correct normals facing outward)

---

## 🚀 Next: Sprite Atlas Rendering

Once 3D models are complete and approved:
1. **Camera setup in Blender** — isometric or side-view angles matching game art style
2. **Render to PNG** — each damage variant at game-scale pixels (64×64, 128×128, or match existing sprite size)
3. **Bake into atlas** via `resvg` / sprite-atlas pipeline (separate workflow)
4. **Test in-game** — verify visual fidelity at actual game resolution + camera angles

---

**Reference package ready. Start with Bf-109 (simplest), validate the process, then scale to remaining aircraft.**
