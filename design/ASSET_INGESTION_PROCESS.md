# Asset Ingestion Process — Comprehensive Pipeline

**Purpose:** Standardized, automated workflow for sourcing and preparing 3D models, schematics, textures, and reference materials for ANY game asset type (aircraft, vehicles, buildings, weapons, characters, etc.).

**Philosophy:** Start with high-quality 3D geometry, supplement with technical specs, gather reference/inspiration materials, build design consistency via stylization parameters.

---

## 🎯 Design Language: Stylization Parameters

All assets use consistent stylization variables to create unified visual flavor:

### Core Parameters

#### **Chonkyness** (0.0 – 2.0, default 1.0)
- How "thick" or "chunky" the model is
- 0.5 = lean/sleek (thin fuselage, narrow wings)
- 1.0 = realistic/canonical proportions
- 1.5–2.0 = exaggerated/cartoonish (thick fuselage, beefy components)
- **Applied to:** fuselage diameter, wing thickness, engine nacelle size, overall mass perception
- **Consistency:** Spitfire might be 0.8 (sleek), B-17 might be 1.2 (sturdy), cartoonified version all at 1.5

#### **Stretch** (0.5 – 2.0, default 1.0)
- How elongated or compressed the model is
- 0.7 = compact (shorter fuselage, squat profile)
- 1.0 = canonical proportions
- 1.3+ = stretched (exaggerated length, "noodle-like")
- **Applied to:** fuselage length, wing span, engine placement
- **Consistency:** Fighters default 1.0, transport aircraft might be 1.1, racing variants 0.9

#### **Damage Level** (0 – 5)
- 0 = Pristine factory condition
- 1 = Battle-worn (minor scuffs, weathering)
- 2 = Light damage (flak scuff marks, paint chipping)
- 3 = Moderate damage (small bullet holes, torn control surface)
- 4 = Heavy damage (large structural damage, collapsed gear)
- 5 = Destroyed (charred, fire damage, cosmetic only)
- **Applied to:** bullet hole placement, control surface tears, paint condition, panel weathering
- **Consistency:** All aircraft share same damage progression framework

#### **Era** (time period aesthetic adjustment)
- Early War (1939–1942): Natural metal, basic camouflage
- Mid War (1942–1943): Established camo patterns, some fading
- Late War (1943–1945): Heavy weathering, complex patterns
- Post-War (1945+): Glossy paint, museum condition
- **Applied to:** Paint scheme selection, weathering intensity, marking style

#### **Theater** (geographic operation theater)
- European: Dark green + dark earth (RAF), RLM 70/71 (Luftwaffe)
- Pacific/Tropical: Dark green + azure blue undersides
- Mediterranean: Sand yellow + olive
- Arctic: White distemper wash
- Desert/North Africa: Sand + blue
- **Applied to:** Base color palette, camouflage pattern, decal style

### Parameter Consistency Rules

1. **All stylization applied uniformly across asset types**
   - A "chonky=1.5" B-17 bomber and "chonky=1.5" Tiger tank should have similar visual weight
   - A "stretch=0.8" Bf-109 and "stretch=0.8" Zero should both appear compact

2. **Stylization orthogonal to damage**
   - Damage Level is independent of Chonkyness/Stretch
   - "Chonky=1.5, Damage=2" means thick body with moderate combat scars

3. **Baked into sprite variants**
   - Store each (Chonkyness, Stretch, Damage Level, Era, Theater) combo as a separate sprite variant
   - e.g., `bf-109_chonky-1.5_stretch-1.0_dmg-2_era-midwar_theater-european.png`

---

## 📋 Asset Ingestion Workflow

### Phase 1: Source the Best 3D Model

**Goal:** Locate highest-quality 3D model available (free + open source preferred).

**Search Hierarchy (ranked by quality, completeness, openness):**

1. **Sketchfab (Free, CC-BY-4.0)**
   - 3D model community, free downloads
   - Quality: Medium-to-High
   - Openness: CC-BY-4.0 (free commercial use with attribution)
   - Coverage: Aircraft, vehicles, characters, weapons
   - Best for: Game-ready models with textures

2. **Thingiverse / Printables (Free, open source)**
   - 3D printing community, often parametric
   - Quality: Medium (designed for printing, not rendering)
   - Openness: CC-BY / public domain
   - Coverage: Vehicles, buildings, props
   - Best for: High-polygon accuracy reference

3. **Turbosquid Free (Free, various licenses)**
   - Professional 3D asset marketplace, free section
   - Quality: High
   - Openness: Varies (check license per model)
   - Coverage: Professional assets
   - Best for: High-quality geometry, rigged models

4. **CGTrader Free (Free, various licenses)**
   - Professional marketplace, free section
   - Quality: High
   - Openness: Varies (check license)
   - Coverage: Professional 3D assets
   - Best for: Complex, detailed models

5. **Poly Haven (Free, CC0/open source)**
   - Open source asset library
   - Quality: High
   - Openness: CC0 (fully open)
   - Coverage: Limited aircraft/vehicles, strong textures
   - Best for: Textures + some geometry

6. **Quaternius (Free, CC0)**
   - Low-poly 3D models
   - Quality: Medium (stylized low-poly)
   - Openness: CC0
   - Coverage: Vehicles, characters, props
   - Best for: Stylized reference, proportion guides

7. **Open source game assets (itch.io, OpenGameArt)**
   - Community game asset sites
   - Quality: Highly variable
   - Openness: Mostly CC0/CC-BY
   - Coverage: Game-ready but often niche
   - Best for: Already-stylized reference

8. **Museum/Technical Resources**
   - CAD downloads from aircraft museums (if available)
   - Digitized historical models
   - Quality: High (dimensionally accurate)
   - Openness: Varies
   - Coverage: Limited but authoritative
   - Best for: Historical accuracy baseline

9. **Commercial (Last Resort)**
   - Gumroad, Artstation, commercial marketplaces
   - Quality: Usually highest
   - Openness: Often proprietary
   - Cost: $5–50+ per model
   - Best for: Only if free alternatives insufficient

**Search Process:**
```bash
# Example: search for aircraft model
for AIRCRAFT in "Spitfire" "Bf-109" "P-51 Mustang"; do
  # Search each site in order
  sketchfab_search "$AIRCRAFT" "3D model" "game ready"
  thingiverse_search "$AIRCRAFT" aircraft
  turbosquid_search "$AIRCRAFT" free
  # ... continue hierarchy
  # Download top 2-3 candidates per site
  # Store with source metadata
done
```

**Selection Criteria:**
- ✅ Proportions match historical specs (within 5%)
- ✅ Game-ready polygon count (5k–100k triangles depending on sprite detail)
- ✅ Textures included (or easily removable for retexturing)
- ✅ Recognizable silhouette (distinctive features visible)
- ✅ License allows commercial game use
- ✅ Open source preferred, CC-BY acceptable, proprietary last resort

**Acceptance Threshold:**
- If excellent 3D model found: USE IT as primary reference
- If good 3D model found: USE IT, supplement with schematic for dimensions
- If no acceptable 3D model: Proceed to Phase 2 (schematic sourcing)

---

### Phase 2: Source Technical Specifications (if needed)

**Goal:** Gather 3-view schematics, technical drawings, and dimensional references.

**Only pursue if:**
- No acceptable 3D model found, OR
- 3D model lacks dimension data, OR
- Need technical accuracy baseline for stylization parameters

**Search Hierarchy (ranked by format + quality):**

1. **Wikimedia Commons (Free, CC0/CC-BY)**
   - SVG 3-view technical drawings
   - Quality: High (vector, infinitely scalable)
   - Openness: CC0 (fully open)
   - Coverage: Extensive WWII aircraft, some modern
   - Best for: Vector graphics, scalable reference

2. **The Blueprints (Free, vector drawings)**
   - Aircraft blueprint collection
   - Quality: High (hand-drawn vector)
   - Openness: Free to view/download
   - Coverage: Aircraft, vehicles, buildings
   - Best for: Accurate proportions, vector format

3. **Drawing Database (Free, bitmap blueprints)**
   - High-resolution blueprint scans
   - Quality: Medium-High (raster)
   - Openness: Free
   - Coverage: Aircraft primarily
   - Best for: Detailed technical specs

4. **Museum Archives (Free, institution-specific)**
   - Museum of Flight, Smithsonian, RAF Museum digital collections
   - Quality: High (authoritative)
   - Openness: Public domain or CC-licensed
   - Coverage: Historical aircraft/vehicles
   - Best for: Accurate historical dimensions

5. **AirCorps Library (Free + Paid)**
   - Comprehensive aircraft blueprint collection
   - Quality: High (original manufacturing specs)
   - Openness: Mixed (some free, some subscription)
   - Coverage: Extensive WWII + modern aircraft
   - Best for: Precise manufacturing dimensions

6. **Internet Archive (Free)**
   - Scanned flight manuals, technical docs
   - Quality: Medium (OCR'd text + scans)
   - Openness: Public domain
   - Coverage: Military aircraft, vehicles
   - Best for: Specification sheets, dimension data

7. **Scale Model Plans (Free + Paid)**
   - RC model plans, scale drawings
   - Quality: High (dimensionally accurate)
   - Openness: Varies
   - Coverage: Niche (specific aircraft/vehicles)
   - Best for: Accurate 3-view reference with dimensions

8. **SVG Conversion Pipeline (In-house)**
   - Use task #32: bitmap→SVG tracing tool (potrace)
   - If no SVG available: source highest-quality bitmap, convert via potrace
   - Quality: Good (for silhouettes)
   - Cost: Processing time
   - Best for: Converting found bitmaps to scalable vectors

**Download Hierarchy (by format):**
1. **SVG (vector)** — infinitely scalable, easiest to modify
2. **PNG (lossless bitmap)** — high quality without artifacts
3. **JPG (lossy bitmap)** — acceptable if no PNG available (note quality loss)

**Dimensional Data Extraction:**
- Use 3-view proportions to calculate wingspan/length ratio
- Extract measurements from schematic labels (if present)
- Cross-reference with historical spec sheets (Wikipedia, technical manuals)
- Document source and measurement uncertainty

---

### Phase 3: Gather Reference & Inspiration Materials

**Goal:** Collect texture references, paint schemes, liveries, and design inspiration (5–10 examples per type).

#### 3A: Reference Photos (5–10 examples)

**Search Hierarchy:**

1. **Museum/Airshow Photography**
   - RAF Museum, Smithsonian, Technik Museum, etc.
   - Search: "[Object] museum restoration photo"
   - Quality: Excellent (professional restoration)
   - Openness: Public domain or CC-licensed
   - Best for: Paint condition, wear patterns, details

2. **Historical Military Photos (Public Domain)**
   - USAAF records, RAF archives, NARA
   - Search: "[Object] WWII military photo"
   - Quality: High (historical documentation)
   - Openness: Public domain
   - Best for: Original paint schemes, combat markings

3. **Aircraft Recognition Manuals (Historical)**
   - Digitized WWI/WWII recognition guides
   - Search on Internet Archive
   - Quality: Medium (printing artifacts)
   - Openness: Public domain
   - Best for: Multiple variants, camouflage patterns

4. **Wikipedia / Wikimedia Commons**
   - Aircraft/vehicle category pages
   - Search: "[Object] Wikipedia images"
   - Quality: Variable
   - Openness: CC-licensed
   - Best for: Diverse examples, curated photos

5. **Scale Model Builds (Community Forums)**
   - Hyperscale modeling forums, build logs
   - Search: "[Object] scale model build log"
   - Quality: High (detailed photography)
   - Openness: Varies (contact modeler for permission)
   - Best for: Weathering detail, wear patterns

6. **3D Model Preview Renders (from 3D model source)**
   - Sketchfab preview images, turbosquid galleries
   - Quality: Medium-High (digital render, not photo)
   - Openness: Tied to model license
   - Best for: Texture examples, material references

7. **Commercial Game Assets / Clipart (Last Resort)**
   - Game asset sites (itch.io, OpenGameArt)
   - Search: "[Object] game sprite reference"
   - Quality: Medium (already stylized)
   - Openness: Varies (check license)
   - Best for: Inspiration for stylization, existing solutions

**Documentation for Each Photo:**
```
filename: b-17_museum-sentimental-journey_001.jpg
source: Museum of Flight, Airshow Documentation
url: https://museumofflight.org/[photo-url]
license: CC0 / Public Domain
photographer: Unknown / [Name if available]
date: 2019-07-15 (or circa 1944 for historical)
notes: Pristine restoration, Betty Grable nose art, visible rivet detail
content: Full aircraft profile, temperate theater, excellent paint condition
```

#### 3B: Texture & Material References (5–10 examples)

**Source from:**

1. **3D Model Textures (from Phase 1 models)**
   - Extract texture maps from downloaded 3D models
   - Organize by material type (metal, paint, weathering)
   - Check license for texture reuse

2. **Poly Haven Textures (Free, CC0)**
   - Open source PBR texture library
   - Search: "metal", "paint", "rust", "weathering"
   - Quality: Professional (game-ready PBR)
   - Openness: CC0
   - Best for: Base materials, weathering

3. **Ambient CG (Free, CC0)**
   - High-quality PBR texture scans
   - Quality: Professional (photogrammetry)
   - Openness: CC0
   - Best for: Realistic wear, rust, corrosion

4. **OpenGameArt Textures (Free, CC0/CC-BY)**
   - Community texture library
   - Quality: Variable
   - Openness: Mostly CC0
   - Best for: Aircraft-specific paint jobs, military camo

5. **Wikimedia Commons (Free, CC0/CC-BY)**
   - High-res photos of materials
   - Search: "military camouflage", "aircraft paint", "rust texture"
   - Quality: Photo-based, variable
   - Openness: CC-licensed
   - Best for: Authentic camo patterns, weathering

6. **3D Scanning Communities (Sketchfab, Poly Haven)**
   - Photogrammetry scans of real objects
   - Quality: Excellent (real-world capture)
   - Openness: CC0/CC-BY
   - Best for: Authentic weathering, panel shadows

**Documentation for Each Texture:**
```
filename: paint_rlm-70-green_reference.jpg
source: Poly Haven / museum restoration photo
material_type: paint, camouflage
color_values: RGB #47593A / LAB 35 -8 12
usage: Base coat, Luftwaffe aircraft temperate theater
authenticity: Historical RLM 70 spec
```

#### 3C: Livery & Paint Job Examples (5–10 examples)

**Source from:**

1. **Museum Restorations (Photo refs)**
   - Each museum aircraft = unique livery example
   - Often well-documented with squadron markings

2. **Historical Squadron Records**
   - USAAF, RAF, Luftwaffe squadron archives
   - Search: "[Squadron Name] aircraft markings"
   - Quality: Authentic period documentation
   - Openness: Public domain or military historical

3. **Scale Model References**
   - Hyperscale modelers document liveries in detail
   - Often include technical paint scheme diagrams
   - Search: "[Aircraft] hyperscale model build"

4. **Aviation Art & Illustration**
   - Professional aviation artists document authentic schemes
   - Search: "[Object] aviation art authentic livery"
   - Check license (some available for reference, some restricted)

5. **Game Asset Clipart**
   - Sites like itch.io, OpenGameArt often have pre-styled aircraft
   - Can extract color palettes, marking placement
   - Search: "[Object] game sprite asset"

**Documentation for Each Livery:**
```
filename: bf-109_jg26-red7_001.jpg
source: Museum photo / Historical documentation
aircraft_code: Bf-109F-4
squadron: JG 26 (German fighter wing)
pilot: [Name if known]
era: Mid-War (1942–1943)
theater: European
base_color: RLM 70/71 (green/brown)
markings: Red 7 fuselage code, Balkenkreuz (German cross), squadron emblems
note: Authentic Luftwaffe fighter, combat veteran
```

---

### Phase 4: Prepare for Stylization & Reskinning

**Goal:** Establish stylization parameters and prepare assets for multi-livery rendering.

#### 4A: Define Stylization Parameters

For each asset, document baseline values:

```yaml
# Example: Messerschmitt Bf-109
aircraft: Messerschmitt Bf-109
base_model: sketchfab/manilov-ap/bf-109
stylization:
  chonkyness: 1.0  # Canonical proportions
  stretch: 1.0     # Standard length
  damage_level: 0  # Start pristine
  era: "mid-war"
  theater: "european"

# Stylization variants to render:
variants:
  - chonkyness: 0.8, stretch: 1.0  # "Sleek 109"
  - chonkyness: 1.2, stretch: 1.0  # "Sturdy 109"
  - chonkyness: 1.0, stretch: 1.2  # "Stretched 109" (experimental variant)
  - damage_level: [1, 2, 3, 4, 5]  # Damage progression
```

#### 4B: Prepare for Blender Modeling & Reskinning

1. **Asset Organization Structure:**
   ```
   assets/
   ├── aircraft/
   │   ├── bf-109/
   │   │   ├── 3d_models/
   │   │   │   ├── base_model.blend
   │   │   │   ├── bf-109_base.fbx (original download)
   │   │   │   └── reference_photos/
   │   │   ├── schematics/
   │   │   │   ├── bf-109-schematic-3view.svg
   │   │   │   └── dimensions-reference.txt
   │   │   ├── textures/
   │   │   │   ├── reference/
   │   │   │   │   ├── paint_rlm-70_reference.jpg
   │   │   │   │   └── weathering_reference.jpg
   │   │   │   └── generated/
   │   │   │       ├── bf-109_rlm-70-71_base.png
   │   │   │       ├── bf-109_usaaf-olive-drab_base.png
   │   │   │       └── bf-109_worn_weathering.png
   │   │   └── liveries/
   │   │       ├── jg26-red7.yaml  (metadata)
   │   │       └── 
   │   ├── spitfire/
   │   └── p-51-mustang/
   └── vehicles/
       ├── tanks/
       │   ├── tiger-1/
       │   └── t-34/
       └── ships/
           └── aircraft-carrier/
   ```

2. **Blender Workflow:**
   - Import base 3D model → apply stylization modifiers
   - Create material slots for each livery/paint scheme
   - Build UV layout for reskinning
   - Test render with different material variants
   - Export sprite atlas for game engine

3. **Reskinning Automation (Script):**
   - Build library of base material/color definitions
   - Script to apply material variants to model
   - Batch render all color/livery combinations
   - Store as sprite atlas (see Phase 5)

---

### Phase 5: Generate Sprite Atlases

**Goal:** Render all stylization + damage + livery combinations to game-ready sprite PNG files.

**Process:**
1. Set up camera angles (isometric, 3/4 view, side view)
2. Batch render for each (chonkyness, stretch, damage_level, livery) combo
3. Bake to sprite atlas via resvg / sprite-baking pipeline
4. Generate sprite metadata (frame positions, collision bounds)
5. Test in game

**Output Structure:**
```
sprites/
├── bf-109_chonky-1.0_stretch-1.0_dmg-0_rlm-70-71.png
├── bf-109_chonky-1.0_stretch-1.0_dmg-1_rlm-70-71.png
├── bf-109_chonky-1.0_stretch-1.0_dmg-2_rlm-70-71.png
├── bf-109_chonky-1.0_stretch-1.0_dmg-0_usaaf-olive.png
├── bf-109_chonky-1.2_stretch-1.0_dmg-0_rlm-70-71.png
└── ... (all combinations)
```

---

## 🔍 Quality Assessment Framework

### For 3D Models: Evaluation Criteria

```
Criterion                Weight    Evaluation (1–5)
─────────────────────────────────────────────────
Geometric Accuracy       25%       Proportions match historical specs
Silhouette Distinctness  20%       Recognizable shape from all angles
Polygon Efficiency       15%       Appropriate detail level (not over/under)
Texture Quality          15%       Included textures are usable/high-res
License Flexibility      15%       Commercial use allowed, open source preferred
Completeness            10%       All major parts modeled (no major gaps)

Quality Score = (criterion_score × weight) × 100
Acceptance Threshold: 75/100 or higher
```

### For Schematics: Evaluation Criteria

```
Criterion                Weight    Evaluation (1–5)
─────────────────────────────────────────────────
Format Quality          30%       SVG > PNG > JPG
Proportional Accuracy   25%       Matches historical specs
Scale Clarity           20%       Dimensions labeled or measurable
Vector/Raster Quality   15%       Sharp lines, no artifacts
Open Source/License     10%       CC0/CC-BY preferred

Quality Score = (criterion_score × weight) × 100
Acceptance Threshold: 70/100 or higher
```

### For Reference Photos: Evaluation Criteria

```
Criterion                Weight    Evaluation (1–5)
─────────────────────────────────────────────────
Detail Level            25%       Can discern texture, wear, markings
Historical Authenticity 20%       Period-accurate or identified era
Paint/Material Clarity  20%       Colors, weathering visible
Angle/Coverage          20%       Shows useful viewing angle
License Compliance      15%       Free/open source preferred

Quality Score = (criterion_score × weight) × 100
Acceptance Threshold: 70/100 or higher
```

---

## 🤖 Automation Strategy

### CLI Tool: `ingest-game-asset`

```bash
# Usage: ingest-game-asset [OBJECT_TYPE] [OBJECT_NAME] [--auto]

# Example: Ingest a new fighter aircraft
ingest-game-asset aircraft "Messerschmitt Bf-109"

# Example: Ingest a tank
ingest-game-asset vehicle/tank "Tiger I"

# Example: Ingest building
ingest-game-asset building "German Bunker Type A"

# Auto mode (script does everything, minimal user input)
ingest-game-asset aircraft "Supermarine Spitfire" --auto
```

### Script Steps:

1. **Search Phase (Automated)**
   ```bash
   # Search each source in hierarchy
   - sketchfab_api_search "$OBJECT" "game ready 3D model"
   - thingiverse_search "$OBJECT"
   - turbosquid_search "$OBJECT"
   - wikimedia_search "$OBJECT" "3-view schematic"
   - the-blueprints_search "$OBJECT"
   # Download top 3 candidates per source
   # Store with metadata
   ```

2. **Quality Assessment Phase (Claude + Manual)**
   ```bash
   # For each downloaded asset:
   - Use Claude to evaluate quality against criteria
   - User selects top 1–3 candidates per category
   # Script generates summary report
   ```

3. **Organization Phase (Automated)**
   ```bash
   # Create directory structure
   mkdir -p assets/[TYPE]/[OBJECT_NAME]/{3d_models,schematics,textures/reference,reference_photos,liveries}
   # Move/link downloaded files
   # Generate metadata files (YAML manifest)
   ```

4. **Metadata Generation (Automated + Manual)**
   ```bash
   # Script creates ASSET_ENTRY.yaml:
   - Object name, type, category
   - 3D model source, license, creator
   - Schematic source, license, format
   - Reference photos (5–10), sources, licenses
   - Stylization parameters baseline
   - Notes on quality, known issues
   ```

5. **Preview Phase (Claude + Manual)**
   ```bash
   # User reviews downloaded models/photos
   # Claude provides quality assessment
   # User approves or requests additional sources
   ```

### Script Outputs:

```
assets/
└── aircraft/
    └── bf-109/
        ├── ASSET_MANIFEST.yaml  # Complete metadata
        ├── 3d_models/
        ├── schematics/
        ├── textures/reference/
        ├── reference_photos/
        └── quality_assessment.txt  # Evaluation notes
```

---

## 📄 Asset Manifest Format (YAML)

Each asset gets a comprehensive manifest:

```yaml
asset_type: aircraft
object_name: Messerschmitt Bf-109
categories: [fighter, wwii, axis, compact]
date_ingested: 2026-06-29

# 3D MODEL (PRIMARY)
3d_model:
  source: Sketchfab
  creator: manilov.ap
  url: https://sketchfab.com/3d-models/bf109-109ef10964dc404089507fc33aad2982
  license: CC-BY-4.0
  cost: free
  attribution_required: yes
  file_format: [FBX, OBJ, Blend]
  polygon_count: 47000  # triangles
  includes_textures: yes
  quality_score: 78/100
  notes: Game-ready geometry, no rigging, textures removable for retexturing
  downloaded_files:
    - bf-109_base.fbx
    - bf-109_textures.zip

# TECHNICAL SPECIFICATIONS (SECONDARY)
schematics:
  primary:
    type: SVG 3-view
    source: Wikimedia Commons
    url: https://commons.wikimedia.org/wiki/File:Messerschmitt_Bf_109.svg
    license: CC0
    cost: free
    quality_score: 82/100
    file: bf-109-schematic-3view.svg
  secondary:
    type: PNG bitmap
    source: Aircraft Reports database
    url: https://example.com/[schematic]
    quality_score: 75/100
    file: bf-109-schematic-bitmap.png

# REFERENCE PHOTOGRAPHY (5–10 EXAMPLES)
reference_photos:
  - filename: bf-109_museum-restoration-001.jpg
    source: Deutsches Technikmuseum Berlin
    url: https://example.com/photo
    license: CC0
    date_taken: circa 1990 (restoration documentation)
    content: Full aircraft, RLM 70/71 authentic paint, museum condition
    quality_score: 88/100
  
  - filename: bf-109_raf-museum-001.jpg
    source: RAF Museum
    url: https://example.com/photo
    license: CC-BY (museum photo)
    content: Captured aircraft, RAF maintenance condition, weathered
    quality_score: 85/100
  
  - filename: bf-109_historical-1943-001.jpg
    source: USAAF Records / National Archives
    url: https://archives.org/[photo]
    license: Public Domain
    date_taken: 1943-06-15
    content: Combat operations, captured aircraft, operational condition
    quality_score: 79/100
  
  # ... 7 more examples

# TEXTURE & MATERIAL REFERENCES
texture_references:
  paint_schemes:
    - name: RLM 70/71 (Luftwaffe Temperate)
      color_rgb: ["#47593A", "#4D4037"]  # RLM 70, RLM 71
      source: Poly Haven, museum restoration photos
      reference_files: [paint_rlm-70_ref.jpg, paint_rlm-71_ref.jpg]
      authenticity: Historical specification
    
    - name: USAAF Olive Drab (captured aircraft)
      color_rgb: "#556B2F"
      source: USAAF records, captured aircraft documentation
      authenticity: Captured variant, non-original

  weathering:
    - name: Battle-worn (light damage)
      source: Historical combat photos, scale model references
      reference_files: [weathering_combat-light_001.jpg, weathering_combat-light_002.jpg]
      characteristics: Paint chipping at panel lines, UV fading, minor scuffs

    - name: Heavy wear (long service)
      source: RAF Museum (long-serving captured variant)
      reference_files: [weathering_heavy-wear_001.jpg]
      characteristics: Significant paint loss, corrosion traces, patched damage

# LIVERY & PAINT JOB EXAMPLES
liveries:
  - name: JG 26 "Schlageter" (Red 7)
    era: Mid-War (1942–1943)
    theater: European
    squadron: JG 26 (Jagdgeschwader 26)
    pilot: Walter Krupinski or similar
    source: Historical documentation, museum restoration
    reference_files: [livery_jg26-red7_001.jpg]
    description: Red fuselage number 7, Balkenkreuz, squadron emblems
    authenticity: Combat veteran documented variant

  - name: Captured USAAF Test Aircraft
    era: Late-War (1944)
    theater: USA (test)
    source: USAAF captured aircraft records
    description: USAAF Olive Drab overpaint, U.S. markings, test codes
    note: Non-combat variant, experimental evaluation

  # ... 8+ more examples

# STYLIZATION PARAMETERS
stylization_baseline:
  chonkyness: 1.0
  stretch: 1.0
  damage_level: 0
  era: mid-war
  theater: european

stylization_variants_planned:
  - chonkyness: 0.8, stretch: 1.0, name: "Sleek 109"
  - chonkyness: 1.2, stretch: 1.0, name: "Sturdy 109"
  - chonkyness: 1.0, stretch: 1.2, name: "Stretched 109"
  - damage_level: [1, 2, 3, 4, 5]

# LICENSES & ATTRIBUTION
licenses_summary:
  3d_model: CC-BY-4.0 (attribution required)
  schematics: CC0 (no attribution required, but credited per policy)
  reference_photos: CC0 + CC-BY (check individual photos)
  textures: CC0 (Poly Haven)

attribution_required:
  - helijah (3D model, CC-BY-4.0)
  - Wikimedia Commons (schematics)
  - Various museums (reference photos, per individual)

# NOTES & ISSUES
quality_notes: |
  3D model geometry accurate to historical specs. Textures are game-ready
  but designed for realistic rendering. Plan to retexture for stylized version.
  
known_issues: |
  Schematic bitmap quality is medium (lossy JPG). Consider SVG conversion
  via task #32 if higher fidelity needed for blueprint extraction.
  
  Reference photo from RAF Museum has slight color cast (aged photo).
  Cross-reference with museum restoration photos for authentic color.

next_steps: |
  1. Import 3D model into Blender
  2. Apply stylization modifiers (chonkyness 1.0–1.2)
  3. Create material slots for livery variants
  4. Batch render sprite atlas
  5. Test in game engine
```

---

## 📋 Task Checklist

- [ ] CLI script `ingest-game-asset` (Phase 1–4 automation)
- [ ] API wrappers for source sites (Sketchfab, Wikimedia, The Blueprints)
- [ ] Quality assessment module (Claude integration)
- [ ] Blender batch rendering script (Phase 5)
- [ ] Asset manifest generation (YAML templating)
- [ ] Metadata validation & completeness checker

---

## 📝 Asset Scope (Not Just Aircraft)

This process applies to **ALL game asset types**:

### Aircraft
- Fighter (Bf-109, P-51, Spitfire, etc.)
- Bomber (B-17, He-111, Ju-88, etc.)
- Reconnaissance (SR-71, etc.)
- Helicopter (modern variants)
- Transport (C-47, etc.)

### Ground Vehicles
- **Tanks:** Tiger I, T-34, M4 Sherman, Panther, KV-1, etc.
- **APCs/IFVs:** Half-track, Stug, M10 Tank Destroyer, etc.
- **Jeeps/Trucks:** Willys, Kubelwagen, Mercedes unimog, etc.
- **Rail:** Locomotives, armored trains, artillery trains

### Water Vessels
- **Warships:** Battleships, cruisers, destroyers, corvettes
- **Carriers:** Aircraft carriers, escort carriers
- **Submarines:** U-boats, Japanese fleet subs
- **Support:** Transports, tankers, landing craft

### Structures
- **German:** Bunkers (Tobruk, casemate), airfields, flak towers, radar stations
- **Soviet:** Pillboxes, blockhouses, trenches, artillery positions
- **Japanese:** Pillboxes, cave fortifications, coastal defenses
- **Industrial:** Factories, refineries, power plants, airfields

### Weapons & Installations
- **Anti-Aircraft:** Flak 88, Bofors 40mm, Quad .50, etc.
- **Artillery:** 88mm gun, 25-pounder, 76mm gun, etc.
- **Coastal Defense:** Ship turrets (salvaged), coastal guns
- **Infantry:** Machine gun nests, anti-tank guns, mortars

### Characters & Uniforms
- **Axis:** German soldier, SS, Luftwaffe pilot, etc.
- **Allied:** USAAF pilot, British soldier, Soviet soldier, etc.
- **Japanese:** Soldier, Naval officer, etc.
- **Cosmetic variants:** Rank insignia, unit markings, damage states

### Miscellaneous
- **Terrain objects:** Crates, barrels, sandbags, obstacles
- **Environmental:** Trees, buildings shells, smoke effects
- **UI assets:** Fonts, icons, emblems, heraldry

---

## 🎬 Process Workflow Diagram

```
START: Ingest asset type X, object Y
  ↓
[PHASE 1] Search 3D model sources
  ├─ Sketchfab, Thingiverse, TurboSquid, CGTrader, Poly Haven, etc.
  └─ Download top 3 candidates, assess quality
  ↓
[DECISION] Found acceptable 3D model?
  ├─ YES → Use as primary reference, proceed to Phase 3
  └─ NO → Proceed to Phase 2
  ↓
[PHASE 2] Search technical specifications (if no 3D model)
  ├─ Wikimedia Commons, The Blueprints, archives
  └─ Download SVG/PNG schematics
  ↓
[PHASE 3] Gather reference & inspiration
  ├─ Reference photos (5–10)
  ├─ Texture references
  └─ Livery/paint job examples (5–10)
  ↓
[PHASE 4] Prepare for stylization
  ├─ Define stylization parameters
  └─ Organize asset directory structure
  ↓
[QUALITY ASSESSMENT] Claude evaluates all assets
  ├─ 3D model quality score
  ├─ Schematic/spec quality
  ├─ Reference material quality
  └─ User approves or requests additional sources
  ↓
[PHASE 5] Generate sprite atlases
  ├─ Import to Blender
  ├─ Apply stylization variants
  ├─ Batch render
  └─ Generate sprite sheets
  ↓
[OUTPUT] Asset ready for game integration
  ├─ 3D model (Blender source)
  ├─ Sprite atlases (PNG with metadata)
  └─ ASSET_MANIFEST.yaml (complete metadata)
  ↓
END: Asset integrated into game
```

---

**Next:** Create tasks for implementing this pipeline.
