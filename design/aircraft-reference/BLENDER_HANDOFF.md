# Blender MCP Handoff — Aircraft 3D Model Generation

**Goal:** Use Blender + MCP to generate production-ready 3D aircraft models from precision technical references.

---

## 📦 Input Reference Package

All assets are in this directory. Use them together as a unified reference system.

### Schematics (`/schematics/`)
- **16 SVG technical drawings** — accurate proportions, scale-neutral vector graphics
- Each aircraft shown in **3-view orthographic** (top/side/front) or **profile** (where 3-view would be ambiguous)
- Source: Wikimedia Commons — CC0/CC-BY-SA/public domain
- **Best for:** Wing/fuselage geometry, tail surfaces, engine nacelles, landing gear layout

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
- **Highest detail:** `boeing-b17-sentimental-journey-nose-art.jpg` (15 MB) — museum-restored, pristine paint, visible rivet detail
- **For realistic:** panel line shadows, paint chipping, metal reflections, nose art placement
- **Best for:** Texture refinement, damage variant planning, decal/marking positions

---

## 🎯 Generation Strategy

### Phase 1: Core Geometry (Per Aircraft)
1. **Sketch the fuselage** using the side-view schematic as a guide
   - Use proportions from schematic measurements (wingspan ÷ length ratios are critical)
   - Create a centerline curve, then build cross-section loops (cylinder → taper toward tail)
2. **Model wings** from the top-view schematic
   - Elliptical or swept geometry per aircraft (Spitfire = elliptical, Mustang = swept)
   - Dihedral angle from side-view perspective
3. **Tail surfaces** (stabilizer, elevator, fin, rudder) from schematic
4. **Landing gear** from front/side views — retractable position + deployed
5. **Engine nacelles** and air intakes from side-view proportions

### Phase 2: Detail Pass
- **Panel lines** from photo reference (subtle geometry or UV-mapped shadows)
- **Cockpit glazing** (windscreen + side windows) — approximate geometry
- **Antenna** and aerials (small but visible, reference photos show placement)
- **Armament hardpoints** (wing pylons, bomb racks, gun positions) — simplified or placeholder

### Phase 3: Material & Texture
- **Base colors** from `PAINT_SCHEMES.md` (use RGB hex codes provided)
- **Weathering** — subtle dirt/wear from museum photo reference (procedural or baked)
- **Panel line shading** — subtle dark line along panel edges (visible in B-17 photo)
- **Rivet detail** — tiny specular reflections (from photo high-frequency detail)

### Phase 4: Damage Variants
- **Bullet holes** — small caliber (fuselage), larger engine damage
- **Burn/scorch marks** — around cockpit/engine from flak
- **Torn control surfaces** — fuselage tears, elevator/aileron damage
- **Undercarriage damage** — collapsed gear, missing leg
- **Nose art** variations (nose cone paintable per aircraft)

---

## 🔧 Workflow in Blender MCP

### Before Starting
- **Verify Blender is running** (Blender 4.5 LTS on Intel Mac)
- **Check port 9876:** `lsof -i :9876` should show Blender process
- **Launch Claude Desktop** after Blender is ready (launch order critical)

### Recommended MCP Commands (Community Addon)
- `blender:add_primitive(type, size)` — start with cylinder/cube shapes
- `blender:create_mesh(name, vertices, faces)` — low-poly geometry from coordinates
- `blender:set_material_color(object, color_hex)` — apply base paint colors
- `blender:add_modifier(object, type, settings)` — bevel edges, subdivision for detail
- `blender:export_model(format)` — export to `.glb` / `.fbx` / `.obj` when done

### Import into Game
Once models are export-ready (`.glb` format preferred):
1. Place in `assets/models/aircraft/`
2. Write a loader in KorGE (model resource + material binding)
3. Render to sprite atlas via the sprite-bake pipeline (later stage — textures → PNG sprites for each damage level + paint variant)

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
