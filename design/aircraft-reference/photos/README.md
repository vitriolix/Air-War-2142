# Paint Scheme Reference Photos

High-resolution museum and airshow photos of actual aircraft showing historical paint schemes and camouflage patterns by biome.

## TEMPERATE / EUROPEAN

### messerschmitt-bf109g4-red7-airworthy.jpg
- **Aircraft:** Messerschmitt Bf 109G-4 'Red 7' (D-FWME)
- **Source:** Flying Legends airshow, Duxford, UK (July 2015)
- **Status:** Restored Spanish-built Hispano Buchon, airworthy
- **Resolution:** 3,735 × 2,490 px (6 MB) — pristine restoration
- **Paint Scheme:** Dark green & dark earth upper surfaces, light blue undersides
- **Photographer:** Alan Wilson
- **License:** CC BY-SA 2.0

**Reference Value:** Shows authentic RLM color splitter pattern on a flying example; pristine condition ideal for "factory fresh" game variant.

---

### northamerican-p51d-american-beauty-1944.jpg
- **Aircraft:** North American P-51D-15-NA Mustang (44-15459)
- **Pilot:** Captain John Voll, 308th Fighter Squadron / 31st Fighter Group
- **Year:** 1944 USAAF official photo
- **Resolution:** 1,298 × 851 px (814 KB)
- **Paint Scheme:** Olive Drab upper with Neutral Gray lower surfaces
- **Squadron Mark:** Yellow group identifier
- **License:** Public domain (U.S. Army Air Forces)

**Reference Value:** Canonical USAAF temperate scheme; shows squadron markings and natural color photos from operational aircraft.

---

### supermarine-spitfire-dday-invasion-stripes.jpg
- **Aircraft:** Supermarine Spitfire, Battle of Britain Memorial Flight (BBMF)
- **Event:** D-Day 80th Anniversary markings (October 2013)
- **Resolution:** 3,042 × 2,068 px (988 KB)
- **Paint Scheme:** RAF temperate (dark green & dark earth) + black/white invasion stripes
- **Photographer:** Sgt Pete George MA ABIPP / MOD
- **License:** Open Government Licence v1.0

**Reference Value:** Iconic invasion markings overlay on standard RAF scheme; shows how seasonal/campaign markings were applied.

---

### boeing-b17-sentimental-journey-nose-art.jpg
- **Aircraft:** Boeing B-17G Flying Fortress "Sentimental Journey"
- **Nose Art:** Betty Grable (WWII-era pinup reference)
- **Source:** Chico Air Museum, California (Commemorative Air Force fleet)
- **Resolution:** 5,798 × 4,480 px (15.11 MB) — extreme detail
- **Date:** September 2021 (modern restoration)
- **Photographer:** Frank Schulenburg
- **License:** CC BY-SA 4.0

**Reference Value:** Highest resolution reference; shows exact pinup art style, aging, wear, and natural color under daylight. **Essential for nose art generation/rendering**.

---

## TROPICAL / JUNGLE

### messerschmitt-bf109g2-trop-raf-museum.jpg
- **Aircraft:** Messerschmitt Bf 109G-2/Trop 'Black 6'
- **Location:** RAF Museum Hendon (London) — static display
- **Resolution:** 2,816 × 2,112 px (1.38 MB)
- **Paint Scheme:** Tropical variant — modifications for desert/tropical ops
- **Museum:** RAF Hendon
- **License:** Public domain
- **Date:** June 2007 museum photo

**Reference Value:** Shows tropical-rated variant with specific modifications; useful for differentiating tropical aircraft from temperate variants in-game.

---

## ARCTIC / WINTER

### mikoyan-mig3-winter-distemper-camouflage.jpg
- **Aircraft:** Mikoyan-Gurevich MiG-3 Soviet fighter
- **Period:** 1941-1942 (WWII)
- **Paint Scheme:** White distemper camouflage over base colors (badly eroded/weathered)
- **Resolution:** 1,000 × 493 px
- **Photographer:** Unknown Soviet military photographer
- **License:** Public domain (Russian copyright law)

**Reference Value:** Only color arctic camouflage reference; shows **realistic weathering & paint degradation** from field use. The eroded texture is historically accurate—distemper wash was field-applied and would degrade with exposure.

---

## Usage for Game Implementation

### Color Extraction
- Use high-res photos (especially B-17 at 15 MP) with color picker to extract hex values
- Cross-reference with paint guide sites (Tamiya, Vallejo) for exact RGB/Hex formulations
- **Suggested workflow:** Color-pick from museum photos → validate against paint databases → generate palette

### Texture/Wear
- B-17 nose art shows wear, fading, UV damage
- MiG-3 shows realistic distemper erosion pattern
- Can use as reference for procedural weathering/aging shader

### Squadron/Unit Markings
- P-51 "American Beauty" shows squadron yellow band
- Spitfire D-Day stripes overlay on base camouflage
- B-17 nose art as procedural/unlockable cosmetic layer

### Biome Spawning Logic
- **Temperate:** Bf 109G-4 or P-51D colors
- **Arctic:** MiG-3 winter scheme (white distemper)
- **Tropical:** Bf 109G-2/Trop variant
- **Desert:** Extend collection with desert photos (future)
- **Urban/Industrial:** Late-war RLM 74/75 splinters (reference guide has colors)

