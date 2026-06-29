# Aircraft Reference Collection — Complete & Organized

## 📐 **16 3-View Schematics** (4.5 MB)

Full orthographic projections for 3D modeling. Located in `/schematics/` — all suitable for Blender MCP generation.

**Format:** 14 SVG files + 2 bitmaps (PNG/JPG) pending conversion via task #32 pipeline

### Axis Aircraft
- `messerschmitt-bf-109-schematic.svg` — German fighter (3-view)
- `messerschmitt-me-262-schematic.svg` — First jet fighter (3-view)
- `junkers-ju-88-schematic.svg` — Fast dive bomber (3-view)
- `heinkel-he-111-schematic.svg` — Medium bomber (3-view)

### Allied Aircraft
- `northamerican-p-51_mustang-schematic.svg` — Escort fighter (3-view)
- `supermarine-spitfire-schematic.svg` — Interceptor (3-view)
- `lockheed-p-38_lightning-schematic.svg` — Twin-boom fighter (3-view)
- `boeing-b-17-schematic.svg` — Heavy bomber (3-view)
- `northamerican-b-25_mitchell-schematic.svg` — Medium bomber (3-view)
- `douglas-a-26_invader-schematic.png` — Light attack bomber (3-view)
- `republic-p-47_thunderbolt-schematic.svg` — Heavy fighter (3-view)

### Modern/Novelty
- `lockheed-sr-71-schematic.svg` — Reconnaissance (3-view)
- `lockheed-f-117_nighthawk-schematic.svg` — Stealth fighter (3-view)
- `lockheed-ac-130-schematic.svg` — Gunship (3-view)
- `fokker-dr-1-schematic.jpg` — WWI Red Baron triplane (3-view)
- `wright-flyer-1903-schematic.svg` — First powered aircraft (3-view)

---

## 🎨 **12 Design Reference Images** (37 MB + 4 side-view SVGs for texture inspiration)

High-resolution museum/airshow photos (8) + side-view schematics (4) for texture/skin design inspiration, organized by biome.

### Temperate / European (8 photos + 4 side-views)
**Photos (8):**
- `messerschmitt-bf-109g4-red7-airworthy.jpg` — 6 MB, restored, pristine RLM colors
- `northamerican-p-51d-american-beauty-1944.jpg` — 814 KB, USAAF official 1944 photo
- `supermarine-spitfire-dday-invasion-stripes.jpg` — 988 KB, RAF invasion markings overlay
- `boeing-b-17-sentimental-journey-nose-art.jpg` — 15 MB ⭐ **Highest detail** — Betty Grable nose art
- `heinkel-he-111h-technik-museum-sinsheim.jpg` — 6.35 MB, pristine museum restoration

**Side-view Reference SVGs (4) — texture/skin design inspiration:**
- `douglas-a-26_invader-sideview-reference.svg` — Attack bomber profile (sourced from Wikimedia)
- `fokker-dr-1-sideview-reference.svg` — WWI triplane profile for color reference
- `messerschmitt-bf-109-sideview-reference.svg` — Luftwaffe profile for camouflage reference
- `supermarine-spitfire-sideview-reference.svg` — RAF profile for elliptical wing design reference

### Tropical (1 photo)
- `messerschmitt-bf-109g2-trop-raf-museum.jpg` — 1.4 MB, tropical-rated variant at RAF Museum

### Arctic / Winter (1 photo)
- `mikoyan-mig3-winter-distemper-camouflage.jpg` — 117 KB, white distemper wash (eroded texture)

### Temperate Airworthy (1 photo)
- `republic-p-47d-nellie-b-airworthy.jpg` — 4.8 MB, composite airframe at 2019 Duxford

---

## 📚 **Documentation**

### `PAINT_SCHEMES.md`
Complete reference guide organized by 5 biomes:
- **Temperate/European** — Dark Green + Dark Earth (RAF), RLM 70/71 (Luftwaffe), Olive Drab (USAAF)
- **Desert/Arid** — Dark Earth + Middle Stone + Azure Blue (RAF), Sand Yellow + Olive (Luftwaffe)
- **Arctic/Winter** — White distemper, eroded field-applied wash
- **Tropical/Jungle** — Dark Green + Azure Blue undersides, tan variants
- **Urban/Industrial** — RLM 74/75 splinters, Natural Metal, Ocean Grey

Includes squadron markings, nose art, museum references, and implementation ideas.

### `README.md`
Quick inventory and usage notes for schematics.

---

## 🔧 **What Was Fixed**

✅ **Fixed 5 broken HTML error pages** from Wikimedia Commons (User-Agent issue)
- Fokker Dr.I — downloaded proper SVG (132 KB)
- SR-71 Blackbird — downloaded proper SVG (168 KB)  
- F-117 Nighthawk — downloaded proper SVG (58 KB)
- AC-130 Gunship — downloaded C-130H variant (56 KB)
- Wright Flyer 1903 — downloaded proper SVG (188 KB)

✅ **Applied consistent naming scheme** to all 16 schematics
- Pattern: `manufacturer-model_variant-schematic.{svg,png}`
- Example: `douglas-a26invader` → `douglas-a-26_invader`
- Updated all references in README.md and INDEX.md

✅ **Organized into clean directory structure**
- `/schematics/` — All 16 files with consistent naming (4.3 MB total)
- `/photos/{temperate,tropical,arctic}/` — Paint references organized by biome

---

## 💡 **Ready For**

1. **Sprite generation** — SVGs scale to any resolution
2. **Paint scheme application** — Photo references with biome organization
3. **Color extraction** — Use photo picker on B-17 (15 MB, extreme detail)
4. **Game implementation** — Biome-locked paint variants, squad markings as cosmetics

---

**Collection complete. Total size: 42 MB. All files sourced from Wikimedia Commons (CC0/CC-BY-SA/public domain).**
