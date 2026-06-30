# Asset Ingestion Process — 3D Models

## Overview

This document describes the workflow for sourcing and ingesting aircraft 3D models into the game. The process prioritizes quality and authenticity over custom generation.

---

## Sourcing Strategy: Prioritized by Quality

### Priority 1: Download Existing High-Quality Models
- **Source:** Sketchfab, TurboSquid, CGTrader, museum/archive 3D scans
- **Format:** `.glb`, `.fbx`, `.obj` with materials
- **Criteria:** (a) proportionally accurate silhouette, (b) damage variants available or easy to create, (c) license permits derivative use (CC-BY, CC0, or commercial)
- **Effort:** ~2-3 hours per aircraft (evaluation + cleanup + export)
- **Output:** Directly usable `.glb` → sprite atlas pipeline

**→ Use this path when a suitable model exists.**

### Priority 2: AI-Assisted Model Generation (Future)
- **Approach:** Meshy, Roboflow, or similar AI tools — text/image → 3D model
- **Input:** high-res schematic photos + color references from `PAINT_SCHEMES.md`
- **Criteria:** Same as Priority 1
- **Effort:** ~4-6 hours (prompt iteration, cleanup, materials)
- **Output:** Exported `.glb` → sprite atlas pipeline

**→ Use this if no existing model meets quality bar.**

### Priority 3: Manual Blender MCP Generation (Last Resort)
- **Approach:** Blender + Claude Desktop MCP; handcraft geometry from schematics
- **Input:** Schematic SVGs + paint/photo reference from `design/aircraft-reference/`
- **Effort:** ~16-24 hours per aircraft (geometry + materials + damage variants)
- **Output:** Exported `.glb` + damage variant `.glb` files → sprite atlas pipeline
- **Docs:** See `design/aircraft-reference/BLENDER_HANDOFF.md` for workflow details

**→ Use ONLY if Priority 1 + 2 fail AND the aircraft is critical for gameplay/story.**

---

## Decision Tree

```
Do we have a quality 3D model already (Sketchfab, museum scan, etc.)?
├─ YES → Download, clean up materials, export .glb
└─ NO → Does AI model generation produce acceptable results?
    ├─ YES → Use Meshy/Roboflow, iterate on prompt, export .glb
    └─ NO → Is this aircraft essential for the game?
        ├─ YES → Use Blender MCP (BLENDER_HANDOFF.md workflow)
        └─ NO → Defer to later; use placeholder or simpler aircraft
```

---

## Workflow: Post-Ingestion

Regardless of source, all models follow this pipeline:

1. **Format check:** Ensure `.glb` with embedded materials + textures
2. **Proportions validation:** Wingspan ÷ fuselage length within ±5% of real aircraft
3. **Silhouette review:** Distinctive shape recognizable (Spitfire elliptical wing, B-17 4-engine, etc.)
4. **Material cleanup:** Remove irrelevant armature, flatten material hierarchy, bake textures if needed
5. **Damage variants:** Clone base model; add 3–4 damage states (battle-worn, light damage, heavy damage)
6. **Sprite rendering:** Camera setup in Blender → render to PNG at game scale (64×64, 128×128, or match existing sprite size)
7. **Atlas baking:** Combine damage variants + paint schemes into sprite atlas via `resvg` / atlas pipeline
8. **In-game load:** Wire into `assets/models/aircraft/` + KorGE model loader + test visual fidelity

---

## Rationale

- **Existing models save 16+ hours per aircraft** — why handcraft if quality pre-made models exist?
- **AI generation is faster than manual** — 4–6 hours vs 16–24 hours, acceptable quality for non-hero aircraft
- **Manual Blender is the fallback** — use only for critical aircraft (hero/iconic planes, or unique requirements)
- **Damage variants are essential** — they provide visual feedback in gameplay; all ingestion paths must support them

---

## Example: Bf-109 (German Fighter)

| Step | Path | Status |
|------|------|--------|
| Source research | Sketchfab → found 3 models, evaluated quality | TBD |
| If found: Download + cleanup | Export `.glb` → validate proportions | TBD |
| If not found: AI generation | Prompt: "WWII Messerschmitt Bf-109 German fighter, accurate schematic proportions" | TBD |
| If failed: Manual generation | Blender MCP → `BLENDER_HANDOFF.md` workflow | TBD |
| Damage variants | Clone base; add 4 states | TBD |
| Sprite rendering | Isometric camera, render to PNG | TBD |
| Atlas bake | Combine variants + paint schemes → atlas PNG | TBD |
| In-game test | Load + verify camera framing, silhouette, colors match art style | TBD |

---

**Next:** Evaluate Priority 1 sources (Sketchfab, TurboSquid) for the five priority aircraft. If none found, escalate to Priority 2 (AI) or Priority 3 (Blender MCP).
