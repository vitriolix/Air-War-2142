# Color Swatches & Material Specifications

Precise hex color codes extracted from museum paint references and official RLM/Air Force color standards. Use these as base colors for Blender materials.

---

## RAF (Royal Air Force) — Temperate / European

### Standard Camouflage Pattern (1941–1945)
**Top surfaces:** Dark Green + Dark Earth  
**Bottom surfaces:** Sky Blue (lighter, for altitude concealment)

| Component | Color Name | Hex Code | RGB | Notes |
|-----------|-----------|----------|-----|-------|
| **Upper fuselage** | RAF Dark Green | `#355E3B` | 53, 94, 59 | Matte, slightly dull |
| **Upper wings** | RAF Dark Green | `#355E3B` | 53, 94, 59 | Same as fuselage for continuity |
| **Lower wings** | RAF Dark Earth | `#6B5D47` | 107, 93, 71 | Slightly lighter than green to break silhouette |
| **Fuselage sides** | RAF Dark Earth | `#6B5D47` | 107, 93, 71 | Transition zone between upper/lower |
| **Underside fuselage** | Sky Blue | `#A6C8D9` | 166, 200, 217 | Pale, weathered slightly |
| **Undersides wings** | Sky Blue | `#A6C8D9` | 166, 200, 217 | Same as fuselage underside |

**Historical Example:** Supermarine Spitfire Mk. V, RAF 19 Squadron, 1941 (preserved at RAF Museum).

### Invasion Stripes (1944 D-Day Campaign)
Applied **over** standard camouflage as **white + black bands** (alternating).

| Component | Color Name | Hex Code | RGB | Notes |
|-----------|-----------|----------|-----|-------|
| **Stripe 1 (white)** | Invasion White | `#F5F5F5` | 245, 245, 245 | High-visibility identification; bright but not pure white |
| **Stripe 2 (black)** | Invasion Black | `#1A1A1A` | 26, 26, 26 | Deep matte black |
| **Stripe width** | 30–40 cm per band | — | — | Typically 3–4 bands per wing + fuselage |

**Historical Example:** Supermarine Spitfire LF. IX, RAF 303 Squadron, June 1944 (D-Day invasion support).

---

## Luftwaffe (Nazi Germany) — Temperate / European

### Standard Camouflage (1939–1945)
**Top surfaces:** RLM 70 (Green) + RLM 71 (Brown)  
**Bottom surfaces:** RLM 65 (Light Blue / Natural Metal mixed)

| Component | Color Name | Hex Code | RGB | Notes |
|-----------|-----------|----------|-----|-------|
| **Upper fuselage (front)** | RLM 70 Green | `#47593A` | 71, 89, 58 | Matte, forest-like |
| **Upper wings (front)** | RLM 70 Green | `#47593A` | 71, 89, 58 | Same as fuselage |
| **Upper wings (rear)** | RLM 71 Brown | `#4D4037` | 77, 64, 55 | Dark, mottled brown |
| **Fuselage aft** | RLM 71 Brown | `#4D4037` | 77, 64, 55 | Transition to rear camouflage |
| **Underside fuselage** | RLM 65 Light Blue | `#B8CCDB` | 184, 204, 219 | Pale, often faded or patchy |
| **Undersides wings** | RLM 65 Light Blue | `#B8CCDB` | 184, 204, 219 | Mixed with natural metal patches (bare duralumin) |

**Weathering Note:** Luftwaffe aircraft often had **patchy undersides** — natural aluminum showing through faded RLM 65. Represent as 60% RLM 65 + 40% natural metal (see below).

**Historical Example:** Messerschmitt Bf-109 G-4, JG 27 (Afrika Korps), North Africa variant with sand overpaint.

### RLM 76 (Light Blue, Northern Europe variant)
Used in high-altitude interception units (1943–1945).

| Component | Color Name | Hex Code | RGB | Notes |
|-----------|-----------|----------|-----|-------|
| **Undersides** | RLM 76 | `#C8D7E8` | 200, 215, 232 | Paler than RLM 65; used in rare variants |

---

## USAAF (United States Army Air Force) — Temperate / European & Pacific

### Standard Camouflage (1942–1945)
**Top surfaces:** Olive Drab + Neutral Gray  
**Bottom surfaces:** Insignia Blue (distinctive to USAAF)

| Component | Color Name | Hex Code | RGB | Notes |
|-----------|-----------|----------|-----|-------|
| **Upper fuselage** | Olive Drab | `#556B2F` | 85, 107, 47 | Matte, yellowish-green |
| **Upper wings** | Olive Drab | `#556B2F` | 85, 107, 47 | Same across all upper surfaces |
| **Fuselage mottling** | Neutral Gray | `#8B8680` | 139, 134, 128 | Applied as irregular splinter pattern over Olive Drab |
| **Wing mottling** | Neutral Gray | `#8B8680` | 139, 134, 128 | Random dappling, not uniform |
| **Undersides fuselage** | Insignia Blue | `#3E54A4` | 62, 84, 164 | Distinctive deep blue; morale color |
| **Undersides wings** | Insignia Blue | `#3E54A4` | 62, 84, 164 | Same as fuselage (uniform) |

**Application Note:** Mottling is irregular and hand-applied. For Blender: use a **noise texture** at 40–50% opacity overlaid on Olive Drab to simulate the Neutral Gray splinters.

**Historical Example:** North American P-51D Mustang, "American Beauty," 354th Fighter Squadron, 1944 (USAAF official photograph in reference photos/).

### Bare Metal (Unpainted Aluminum, Late War)
Used on some P-51D variants (late 1944+) for speed advantage; no paint camouflage.

| Component | Color Name | Hex Code | RGB | Notes |
|-----------|-----------|----------|-----|-------|
| **All external surfaces** | Natural Aluminum | `#C0C0C0` | 192, 192, 192 | Dull metallic; weathered, not shiny |
| **Engine cowling** | Flat Black | `#1A1A1A` | 26, 26, 26 | Engine heat dissipation coating |
| **Wing leading edges** | Flat Black | `#1A1A1A` | 26, 26, 26 | Thermal protection for high-speed flight |

**Weathering:** Bare metal develops patchy corrosion (brownish oxidation). Simulate as **10–20% rust overlay** (see weathering section below).

---

## Soviet Union — Arctic / Winter Camouflage

### White Distemper (Winter Field Application, 1943–1945)
Applied **over** existing camouflage as a washable whitewash for snow concealment.

| Component | Color Name | Hex Code | RGB | Notes |
|-----------|-----------|----------|-----|-------|
| **Primary coating** | White Distemper | `#FFFEF0` | 255, 254, 240 | Off-white (not pure white); chalk-like texture |
| **Weathering** | Distemper Wear | `#E8E4D0` | 232, 228, 208 | Aged, patchy distemper (60% worn) |
| **Exposed paint underneath** | Soviet Olive Drab | `#5C6B47` | 92, 107, 71 | Green base layer showing through worn areas |

**Texture Note:** Distemper is **chalky and matte** — not glossy. Apply with minimal specularity.

**Historical Example:** Mikoyan-Gurevich MiG-3, Soviet Air Force, winter 1942–43 (photograph in reference photos/).

---

## Weathering & Wear Overlays

Use these as texture masks or procedural noise to age the base colors.

### Engine Staining (Exhaust Discoloration)
Applied around engine cowlings and upper fuselage near engines.

| Wear Type | Blend Color | Hex Code | Opacity | Notes |
|-----------|-----------|----------|---------|-------|
| **Light exhaust** | Charcoal Gray | `#3D3D3D` | 15–25% | Subtle smoke staining |
| **Heavy exhaust** | Sooty Black | `#1A1A1A` | 40–60% | Dark streak pattern above engines |

### Panel Line Weathering
Subtle shadows along fuselage panel seams (riveted construction).

| Wear Type | Color | Hex Code | Opacity | Notes |
|-----------|-------|----------|---------|-------|
| **Panel shadow** | Dark overlay (match base ÷ 1.5) | Varies | 8–15% | Very subtle; mostly for close-up detail |
| **Rivet highlight** | Bright overlay (match base × 1.2) | Varies | 5–10% | Tiny specular catch light |

### Chipped Paint / Bare Metal Exposure
Simulates battle damage and erosion.

| Wear Type | Color | Hex Code | Opacity | Notes |
|-----------|-------|----------|---------|-------|
| **Paint chip** | Next-layer color | Depends on aircraft | 20–40% | Random small patches; typically bare metal or primer |
| **Corrosion/rust** | Rust Brown | `#8B4513` | 10–20% | Edges of bare metal patches |

### Battle Damage Discoloration
**For heavy damage variants only** (burned, crash-damaged).

| Damage Type | Color | Hex Code | Opacity | Notes |
|-----------|-------|----------|---------|-------|
| **Burn scorch** | Charred Black | `#0F0F0F` | 30–70% | Around bullet holes, engine damage zones |
| **Fire staining** | Sooty Brown | `#3D2817` | 25–50% | Irregular streaks indicating fire path |
| **Friction burn** | Tan/Brown | `#A0826D` | 20–35% | Where torn fuselage edges oxidize from airflow |

---

## Decal & Marking Colors

### National Insignia (Aircraft Identification)
Applied to fuselage sides and wing undersides.

| Nation | Insignia Design | Primary Color | Hex Code | Notes |
|--------|-----------------|---------------|----------|-------|
| **RAF** | Roundel (circle) | Royal Blue | `#003DA5` | 3-color: blue center + white ring + red outer |
| | | Red | `#E41B17` | Same roundel, nested circles |
| | | White | `#FFFFFF` | Roundel ring |
| **Luftwaffe** | Balkenkreuz (cross) | Black | `#000000` | Thick cross; sometimes with white border |
| | | White border | `#FFFFFF` | Around black cross (optional) |
| **USAAF** | Star | White | `#FFFFFF` | White star in blue circle (Insignia Blue background) |
| | | Insignia Blue | `#3E54A4` | Circle background |
| **Soviet** | Red Star | Soviet Red | `#ED1C24` | 5-pointed star; distinctive marking |

### Nose Art (Fuselage Nose Cone Painting)
Custom squadron/crew artwork. For game purposes, use simplified color schemes:

| Aircraft | Nose Art Style | Colors | Example from Reference Photos |
|----------|----------|--------|------|
| **P-51 Mustang** | Pin-up / Cartoon | Varies; often: `#E41B17` (red), `#F4E4C1` (skin tone), `#000000` (outline) | "American Beauty" (B-17 variant; reference image shows Betty Grable nose art) |
| **Bf-109** | Stylized emblem / Squadron mark | Varies; often: `#FFD700` (gold), `#000000` (black), `#FFFFFF` (white) | Rare in museum examples; mostly tactical markings |
| **B-17** | Pin-up / Squadron name | Varies widely | "Sentimental Journey" (nose art visible in reference photo) |

For game cosmetics, limit nose art to **3–4 pre-designed variants per aircraft** (no unlimited custom art). Use the colors above as guides for tinting.

---

## Biome-Specific Paint Schemes (Summary)

### Temperate / European (Standard)
- **Base:** RAF Dark Green + Dark Earth OR Luftwaffe RLM 70/71 OR USAAF Olive Drab
- **Undersides:** Sky Blue (RAF), RLM 65 (Luftwaffe), Insignia Blue (USAAF)
- **Accents:** National insignia, squadron markings, nose art (P-51)

### Desert / Arid
- **Base:** Sand Yellow (`#E8C547`) + Olive drab
- **Mottling:** Dark Earth patches over sand
- **Undersides:** Light Blue or bare metal
- **Weathering:** Heavy dust overlay, faded colors

### Arctic / Winter
- **Base:** White Distemper wash over existing camouflage
- **Worn state:** Patchy distemper showing base colors underneath
- **Accents:** National insignia (often removed for stealth; red star on white stands out)

### Tropical / Jungle
- **Base:** Dark Green (darker than temperate) + Dark Earth
- **Undersides:** Azure Blue (sky blue variant)
- **Weathering:** Heavy mold/fungus growth (greenish tint overlay)
- **Accents:** Squadron markings in high-visibility bright colors (tropical humidity = faded paint means higher contrast needed)

### Urban / Industrial (Wartime Europe, Defense)
- **Base:** RLM 74/75 (gray-green splinter pattern) OR bare metal
- **Weathering:** Heavy corrosion, soot from ground-based AA fire
- **Accents:** Technical markings (unit codes, kill markings)

---

## Blender Material Setup (Procedural)

For each aircraft variant, create materials as follows:

```
Base Material:
  - Principled BSDF
  - Base Color: [Hex code from above table]
  - Metallic: 0.0 (for painted surfaces) or 0.8 (for bare metal variants)
  - Roughness: 0.7 (matte paint) or 0.4 (polished metal)
  
Weathering Overlay (optional, multiply blend):
  - Color: [Weathering hex code]
  - Mix Factor: 0.1–0.3 (subtle)
  - Texture: Noise or clouds procedural texture
  
Damage/Burn (heavy variants only):
  - Color: Charred Black or Sooty Brown
  - Mix Factor: 0.4–0.8 (pronounced)
  - Texture: Splatter or voronoi for realistic burn patterns
```

---

## Validation Checklist

Before rendering aircraft variants:

- [ ] **Base color applied** — matches hex code from table to ±5% (visual inspection)
- [ ] **Top/bottom distinction** — upper and lower surfaces are visibly different (concealment logic)
- [ ] **Weathering subtle** — exhaust staining and panel lines don't dominate (should be visible only on close inspection)
- [ ] **Metallic vs. matte** — painted surfaces are matte (low specularity), bare metal variants have high specularity
- [ ] **Damage colorization correct** — burn marks are charred black, not gray; corrosion is rust brown
- [ ] **Insignia placement** — national markings on fuselage sides + wing undersides (standard position)
- [ ] **Nose art (if applicable)** — positioned on forward fuselage, readable from front/side angles

