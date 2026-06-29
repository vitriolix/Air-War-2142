# Claude Desktop Prompt — Aircraft 3D Generation via Blender MCP

**Use this prompt in Claude Desktop (after Blender + MCP are running) to start generating aircraft models.**

---

## Test Prompt #1: Bf-109 (Simplest, Validation)

Copy and paste into Claude Desktop:

```
Generate a 3D Messerschmitt Bf-109 (G variant) aircraft model in Blender via MCP.

REFERENCE PACKAGE OVERVIEW:
- Schematics: /Users/josh/src/antigravity-explorations/1942-android-clone/design/aircraft-reference/schematics/messerschmitt-bf109-schematic.svg (3-view orthographic: top, side, front)
- Paint reference photo: /Users/josh/src/antigravity-explorations/1942-android-clone/design/aircraft-reference/photos/temperate/messerschmitt-bf109g4-red7-airworthy.jpg (museum-restored, pristine RLM colors)
- Dimensional specs: DIMENSIONAL_SPECS.md → Bf-109 section (length 8.64m, span 9.87m, fuselage Ø 0.76m, dihedral 6°)
- Color codes: COLOR_SWATCHES.md → Luftwaffe section (RLM 70 Green #47593A, RLM 71 Brown #4D4037, RLM 65 Light Blue #B8CCDB)
- Workflow guide: BLENDER_HANDOFF.md → Phase 1 & 2 (core geometry + detail pass)

MODELING APPROACH:

1. **Fuselage (core geometry)**
   - Create a cylinder (radius 0.38m [0.76m diameter], length 8.64m)
   - Taper linearly from cockpit (at ~2.5m from nose) to tail cone
   - Add a subtle elliptical cross-section (not perfectly round) to match schematic

2. **Wing (from top-view schematic)**
   - Span: 9.87m (wing root at fuselage centerline; extends 4.935m per side)
   - Wing profile: Cambered (not flat); use a basic airfoil shape or blend between geometry nodes
   - Root chord: ~1.5m; tip chord: ~0.8m (tapered)
   - Dihedral angle: 6° (slight upward tilt per schematic side-view perspective)
   - Attachment: Wing root at fuselage, aligned to centerline, 1.2m aft of nose

3. **Tail Surfaces**
   - Vertical stabilizer: ~1.2m tall, ~0.6m chord, single fin (centered on fuselage)
   - Horizontal stabilizer: ~2.5m span, ~0.5m chord (smaller than wing)
   - Elevator/rudder: Simple planes (not detailed articulation for now)
   - Attachment: Tail cone rear (at ~8.0m from nose)

4. **Engine Nacelle**
   - Single in-line engine (Daimler-Benz DB605)
   - Cowling: Cylindrical, ~0.6m diameter, ~0.8m length
   - Positioned: 0.5m forward of wing root, centered on fuselage
   - Propeller: 3-blade, 3.0m diameter, static (not rotating)

5. **Landing Gear (retractable)**
   - Main wheels: Pair, retracts inward into fuselage (shows cavity when retracted)
   - Position: Slightly forward of wing root, track width ~3.6m
   - Wheel diameter: ~0.5m; extended length (gear down): ~0.85m below fuselage
   - Tail wheel: Non-retractable, trailing aft of fuselage, single small wheel

6. **Canopy / Cockpit**
   - Simple dome: ~0.6m wide, ~0.5m tall, positioned at 2.2m from nose
   - Shape: Curved plexiglas (low-detail; geometric dome sufficient)
   - Transparency: 0.6 (slightly opaque to suggest glazing)

MATERIALS & COLORS:

Base paint (temperate European camouflage):
   - Upper fuselage: RLM 70 Green (#47593A)
   - Upper wing: RLM 70 Green (#47593A)
   - Lower wing: RLM 71 Brown (#4D4037)
   - Engine cowling: Flat Black (#1A1A1A)
   - Undersides: RLM 65 Light Blue (#B8CCDB, with 30% metallic overlay to suggest worn patchy finish)
   - Cockpit canopy: Pale blue transparency

Weathering overlay (subtle):
   - Exhaust staining (dark gray, ~20% opacity) around engine cowling + upper fuselage
   - Panel line shadows (very subtle, 8–12% opacity)
   - No battle damage on pristine variant

EXPORT:

Once complete, export as:
   - Format: `.glb` (GL Transmission Format, game-ready)
   - Filename: `messerschmitt-bf109-pristine.glb`
   - Location: `/tmp/aircraft-models/` (or report the path; we'll copy to the repo)
   - Include: All geometry + materials (colors baked into material properties; no external textures needed yet)

VALIDATION CHECKLIST:

Before exporting, verify in Blender viewport:
   [ ] Aspect ratio (length ÷ span) = 8.64 ÷ 9.87 = 0.87 (compare visual side-by-side with fuselage-to-wing ratio in schematic)
   [ ] Silhouette recognizable as Bf-109 (compact fuselage, elliptical wing, single tail)
   [ ] Colors correct (RLM Green on top, Brown on tail, Blue underneath)
   [ ] Landing gear retracts cleanly (no z-fighting, cavity visible)
   [ ] No inverted surface normals (mesh interior faces should be culled)
   [ ] Proportions: Span 9.87m, length 8.64m, fuselage Ø 0.76m — validate dimensions in scene

OPTIONAL (LATER VARIANT):

After pristine is approved, generate "battle-worn" variant with:
   - Same geometry + base colors
   - Add subtle paint chipping (small white primer patches on wings, ~5% coverage)
   - Exhaust staining increased to 35% opacity
   - Panel line wear (20% opacity) + rivet highlights (tiny bright spots, 8% opacity)
   - Export as: `messerschmitt-bf109-battle-worn.glb`

QUESTIONS / CONSTRAINTS:

- Cockpit detail: Geometric dome (current plan) or modeled plexiglas + instruments? (Recommend dome for speed; game won't show interior detail at sprite scale)
- Propeller: Static or rigged for rotation animation? (Recommend static; spinning happens in-game via material animation)
- Landing gear: Both retracted + deployed variants in one model (bone-rigged toggle), or separate models? (Recommend one model, rigged; easier to manage)

Ready? Start by creating the fuselage cylinder and report back with viewport screenshot (use Blender's `bpy.ops.render.opengl()` → save to disk → describe what you see).
```

---

## Test Prompt #2: P-51 Mustang (After Bf-109 Validated)

Once Bf-109 is approved:

```
Generate a North American P-51D Mustang aircraft model in Blender.

REFERENCE:
- Schematic: /design/aircraft-reference/schematics/northamerican-p51mustang-schematic.svg
- Paint photo: /design/aircraft-reference/photos/temperate/northamerican-p51d-american-beauty-1944.jpg
- Specs: DIMENSIONAL_SPECS.md → P-51 section
- Colors: COLOR_SWATCHES.md → USAAF section (Olive Drab #556B2F, Neutral Gray #8B8680, Insignia Blue #3E54A4)

KEY DIFFERENCES FROM Bf-109:
- Length: 9.83m (longer nose, laminar flow fuselage)
- Span: 11.28m (wider, slightly swept wing)
- Aspect ratio: 0.87 (same as Bf-109, but longer profile feel due to bubble canopy placement)
- Fuselage Ø: 0.88m (0.12m wider than Bf-109)
- Canopy: Bubble (0.85m wide, 0.7m tall, high-rise design for rear visibility)
- Engine: Same Merlin as Bf-109, but with intercooler bulge behind cockpit (add ~0.2m height aft of canopy)
- Landing gear: TALL stance (1.10m extended), wheel-door design on retracting legs
- Propeller: 4-blade, 3.40m diameter (larger than Bf-109)

MODELING APPROACH (compared to Bf-109):

1. Fuselage: Cylinder (0.88m Ø, 9.83m long); slightly longer, fatter nose than Bf-109
2. Wing: 11.28m span, laminar flow profile (swept, not elliptical like Spitfire)
3. Canopy: Tall bubble dome (higher profile than Bf-109's open-air canopy)
4. Engine: Same size cowling, but add intercooler bulge (0.2m aft of canopy, adds ~0.3m height)
5. Landing gear: Taller and wider stance (1.10m extended, 4.15m track); wheel doors on retraction

MATERIALS & COLORS:

Paint (USAAF temperate):
   - Upper surfaces: Olive Drab (#556B2F)
   - Upper mottling: Neutral Gray (#8B8680, ~40% opacity noise overlay for splinter effect)
   - Lower surfaces: Insignia Blue (#3E54A4)
   - Engine cowling: Flat Black (#1A1A1A)
   - Propeller: Flat Black (#1A1A1A)
   - Canopy: Pale blue transparent (0.4 opacity)

OPTIONAL (LATE WAR VARIANT):

"Natural metal" variant (bare aluminum, no paint):
   - All external: Aluminum (#C0C0C0, high metallic, roughness 0.4)
   - Engine cowling + wing leading edge: Flat Black (#1A1A1A) for thermal protection
   - Weathering: 15% rust overlay (oxidation on aluminum)

EXPORT:

   - `northamerican-p51d-mustang-pristine.glb`
   - `northamerican-p51d-mustang-bare-metal.glb` (optional, validate after pristine approved)

VALIDATION:

   [ ] Aspect ratio = 9.83 ÷ 11.28 = 0.87 (should feel similar profile to Bf-109, but longer nose)
   [ ] Bubble canopy distinctly higher than Bf-109 (check height vs. fuselage)
   [ ] Tall landing gear visible (validate 1.10m extension)
   [ ] Paint mottling visible but not overwhelming (40% opacity noise is subtle)
   [ ] Wing sweep visible from front view (should have slight back-swept appearance)
```

---

## After Testing Both Models

**Success looks like:**
- [ ] Bf-109 `.glb` exports cleanly, imports into game engine without errors
- [ ] Proportions match schematic aspect ratios (visual inspection + dimension spot-check)
- [ ] Colors are accurate (hex codes from photos match eye-test against reference images)
- [ ] Landing gear articulation works (can toggle retracted ↔ deployed)
- [ ] P-51 is visibly distinct from Bf-109 (longer, taller canopy, different stance)

**If models look good:**
1. Export both as `.glb` to the repo (`assets/models/aircraft/`)
2. Move to **Spitfire** (test elliptical wing accuracy — critical silhouette)
3. Then scale to **He-111** (test asymmetric fuselage, twin engines)
4. Finally **B-17** (heaviest, most complex; 4 engines, detailed gun positions)

**If issues arise:**
- Document the problem (e.g., "proportions off by X%", "colors don't match reference")
- Adjust reference or Blender parameters
- Re-test

---

## Blender MCP Launch Checklist

Before pasting the above prompts into Claude Desktop:

1. **Start Blender 4.5 LTS** (Intel Mac, not 5.x)
   ```bash
   # Verify community addon is installed
   cd ~/.config/blender/4.5/scripts/addons/
   ls -la | grep -i blender.mcp
   # Should show: blender-mcp/ or similar
   ```

2. **Verify MCP server is running:**
   ```bash
   lsof -i :9876
   # Should show Blender process listening on port 9876
   ```

3. **Launch Claude Desktop** (only after Blender is running)
   - Cmd+Space → Claude → launch

4. **Start a new conversation** (fresh context for tool negotiation)

5. **Paste Test Prompt #1** into the chat

6. **Monitor Blender viewport** for updates (Claude will call MCP commands; watch the mesh build)

---

**Good luck! Start with Bf-109. If the pipeline works, the rest should scale smoothly.**

