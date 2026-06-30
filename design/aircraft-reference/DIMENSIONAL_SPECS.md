# Aircraft Dimensional Specifications

Precise measurements for modeling. All dimensions in **meters**. Source: official aircraft data sheets (public domain).

## Priority Aircraft (Model in This Order)

### 1. Messerschmitt Bf-109 (G variant) — German Fighter
**Simplest geometry; excellent for validating the pipeline.**

| Dimension | Measurement | Notes |
|-----------|-------------|-------|
| **Length** | 8.64 m | Fuselage nose to tail |
| **Wingspan** | 9.87 m | Tip to tip, loaded (no drop tanks) |
| **Height (tail sitting)** | 2.60 m | Fuselage center to tallest point |
| **Wing area** | 16.2 m² | Single elliptical wing, cambered |
| **Fuselage diameter** | 0.76 m | Approximate tube diameter at widest |
| **Dihedral angle** | 6° | Slight upward tilt per side |
| **Engine** | 1× Daimler-Benz DB605 (1475 hp) | Inline V12, center-mounted |
| **Propeller diameter** | 3.0 m | 3-blade variable-pitch |
| **Landing gear track** | 3.6 m | Wheel-to-wheel distance |
| **Gear extension (deployed)** | 0.85 m | Main wheel center below fuselage |

**Model Priority:** Start with fuselage cylinder (0.76m Ø, 8.64m long), taper tail. Wing from top-view schematic, dihedral 6°. Single engine nacelle forward of cockpit. Retractable gear (simple cylinder landing legs).

---

### 2. North American P-51 Mustang (D variant) — US Fighter
**Iconic profile; slightly more complex (laminar flow wing, inline engine).**

| Dimension | Measurement | Notes |
|-----------|-------------|-------|
| **Length** | 9.83 m | Nose probe to tail |
| **Wingspan** | 11.28 m | Loaded, no external tanks |
| **Height** | 4.17 m | Tall landing gear stance |
| **Wing area** | 21.6 m² | Laminar flow wing (40° sweep at quarter-chord) |
| **Fuselage diameter** | 0.88 m | Slightly wider than Bf-109; monocoque semi-monocoque |
| **Dihedral angle** | 6.5° | Slightly more tilt than Bf-109 |
| **Engine** | 1× Rolls-Royce Merlin (1650 hp) | Inline V12, with intercooler behind cockpit |
| **Propeller diameter** | 3.40 m | 4-blade constant-speed |
| **Landing gear track** | 4.15 m | Wide, stable stance |
| **Gear extension** | 1.10 m | Main wheel lower than Bf-109 |
| **Fuselage taper** | Linear from aft-cockpit; bubble canopy mounted 3.2m from nose | Non-stop taper to tail cone |

**Model Priority:** Laminar wing (swept profile from schematic), monocoque fuselage slightly fatter than Bf-109. Bubble canopy (geometric dome). Single center engine. Tall, spread landing gear for stable ground handling.

---

### 3. Supermarine Spitfire (Mk. IX variant) — RAF Fighter
**Subtle geometry; elliptical wing critical to silhouette.**

| Dimension | Measurement | Notes |
|-----------|-------------|-------|
| **Length** | 9.12 m | Proportionally shorter than P-51 |
| **Wingspan** | 11.23 m | Very close to P-51 (conformal in top-down view) |
| **Height** | 3.86 m | Shorter tail moment than P-51 |
| **Wing area** | 22.4 m² | Elliptical (crucial to silhouette); less swept than P-51 |
| **Fuselage diameter** | 0.82 m | Slender, between Bf-109 and P-51 |
| **Dihedral angle** | 5.5° | Subtly flatter wing than Bf-109 |
| **Engine** | 1× Rolls-Royce Merlin (1650 hp) | Same as late P-51; smaller, tighter mounting |
| **Propeller diameter** | 3.05 m | Slightly smaller arc |
| **Landing gear track** | 3.48 m | Narrower, more compact stance |
| **Gear extension** | 0.95 m | Retracts into wings (not fuselage like P-51) |
| **Fuselage shape** | Elliptical cross-section, not circular | Subtle but key: not a round tube |

**Model Priority:** Elliptical wing from top-view (not circular, distinctly wing-shaped). Slender fuselage with gentle curve. Tight engine bay. Landing gear retracts into wings (geometric detail that affects silhouette). Smallest of the three fighters.

---

### 4. Boeing B-17 Flying Fortress — US Heavy Bomber
**4-engine, complex geometry; most challenging but highest visual detail.**

| Dimension | Measurement | Notes |
|-----------|-------------|-------|
| **Length** | 20.75 m | Fuselage only; much longer than fighters |
| **Wingspan** | 31.62 m | 4× fighter wingspan |
| **Height** | 5.79 m | Tall tail fin for high-altitude stability |
| **Wing area** | 152 m² | Enormous; ~7× fighter wings |
| **Fuselage diameter** | 2.44 m | Pressurized tube; much fatter than fighters |
| **Fuselage length (nose to tail bulk)** | ~17 m | Excluding tail cone |
| **Dihedral angle** | 6.5° | Similar to fighters despite massive size |
| **Engines** | 4× Wright R-1820 (1200 hp each) | Dual nacelles at ~1/3 and ~2/3 span |
| **Propeller diameter** | 3.73 m per engine | Oversized 4-blade props |
| **Tail configuration** | Single vertical stabilizer, dual horizontal | Large cross-tail (not twin-tail) |
| **Landing gear track** | 8.20 m | Massive dual-wheel main gear |
| **Gear extension (deployed)** | 1.95 m | Heavy fuselage sits high off ground |
| **Cockpit glazing** | Extensive greenhouse bubble (nose cone + side blisters) | Bombardier + pilot + navigator + radio operator positions |
| **Bomb bay** | ~3.5 m × 2.4 m × 1.5 m | Simplified: fuselage bulge beneath wings |

**Model Priority:** Long, fat fuselage cylinder. Dual nacelle pairs. Massive wing (flat loading). Tall single tail. Extensive cockpit glazing (complex Plexi bubble). Landing gear pods (fat wheels). Turret positions (top/belly/side — simplified geometric positions, not detailed turret geometry for now).

---

### 5. Heinkel He-111 (H variant) — German Medium Bomber
**Twin-engine, asymmetric nose; unique aerodynamic design.**

| Dimension | Measurement | Notes |
|-----------|-------------|-------|
| **Length** | 16.4 m | Shorter than B-17, longer than fighters |
| **Wingspan** | 22.6 m | Medium bomber span |
| **Height** | 3.85 m | Compact fuselage relative to span |
| **Wing area** | 86.5 m² | Large but less than B-17 |
| **Fuselage diameter** | 1.65 m | Round cross-section; smaller than B-17 |
| **Fuselage shape** | Asymmetric nose offset down-left | Bombardier/nose gunner sits low; cockpit sits high (creates swept-back cockpit line) |
| **Dihedral angle** | 7° | Slightly higher dihedral for altitude stability |
| **Engines** | 2× Daimler-Benz DB601/605 (1175–1340 hp) | Twin in-line V12s at 1/3 and 2/3 span |
| **Propeller diameter** | 3.0 m per engine | 3-blade adjustable |
| **Landing gear track** | 4.5 m | Outrigger tail wheel (not nose-wheel) |
| **Gear extension (deployed)** | 0.95 m | Main wheels below fuselage; tail wheel aft |
| **Tail configuration** | Single vertical fin, single horizontal stab | No tail surfaces aft of fuselage (attached to fuselage sides) |

**Model Priority:** Asymmetric fuselage (nose offset, swept cockpit line, single tail). Twin engine nacelles. Outrigger landing gear configuration. The asymmetric nose is **key to silhouette** — otherwise might be confused with other twin-engine bombers.

---

## Comparative Scale Diagram (Simplified)

```
Fighter tier (single-engine):
  Bf-109:       [====] 8.64m
  Spitfire:    [=====] 9.12m
  P-51:        [====== ] 9.83m

Medium bomber (twin-engine):
  He-111:      [===============] 16.4m

Heavy bomber (4-engine):
  B-17:        [==========================] 20.75m
```

For 3D modeling, the **aspect ratio (length ÷ wingspan)** is more critical than absolute size:
- **Bf-109:** 8.64 ÷ 9.87 = **0.87** (compact, stubby)
- **P-51:** 9.83 ÷ 11.28 = **0.87** (same as Bf-109, longer fuselage feel is cockpit bubble)
- **Spitfire:** 9.12 ÷ 11.23 = **0.81** (longest aspect ratio — sleek)
- **He-111:** 16.4 ÷ 22.6 = **0.73** (elongated bomber)
- **B-17:** 20.75 ÷ 31.62 = **0.66** (massive wing loading)

Maintain these ratios accurately in your models.

---

## Landing Gear Configurations

### Bf-109 (Tail-dragger variant)
- **Main wheels:** Retracts into fuselage (inward, visible cavity when up)
- **Tail wheel:** Non-retractable, trailing below fuselage
- **Ground attitude:** Nose-up angle (~10°) when parked

### P-51 (Tail-dragger)
- **Main wheels:** Retracts outward into wings (wheel doors visible)
- **Tail wheel:** Non-retractable, aft of fuselage
- **Ground attitude:** Level or slight nose-up

### Spitfire (Tail-dragger)
- **Main wheels:** Retract inward into wings (not fuselage)
- **Tail wheel:** Non-retractable
- **Ground attitude:** Level

### He-111 (Tail-dragger)
- **Main wheels:** Retracts downward into fuselage (visible pod)
- **Tail wheel:** Non-retractable, trailing aft
- **Ground attitude:** Nose-up (~12°) when parked

### B-17 (Tail-dragger)
- **Main wheels:** Dual wheels per side, retract into fuselage (large gear doors)
- **Tail wheel:** Non-retractable, trailing aft
- **Ground attitude:** Nose-up (~5–8°), high ride height (heavy fuselage sits ~2m off ground with gear down)

---

## Cockpit Reference (Simplified Geometry)

For canopy/cockpit glazing, use this simplified cross-section:

| Aircraft | Canopy Type | Dimensions | Notes |
|----------|-------------|-----------|-------|
| **Bf-109** | Hinged sidecar | 0.6m wide × 0.5m tall | Small, open-air feel; limited visibility |
| **P-51** | Bubble canopy | 0.85m wide × 0.7m tall | High-rise dome; excellent rearview |
| **Spitfire** | Curved sliding | 0.75m wide × 0.65m tall | Smooth, streamlined bubble |
| **He-111** | Asymmetric greenhouse | 1.2m wide × 0.8m tall | Swept-back, offset down to starboard |
| **B-17** | Nose cone greenhouse | 1.5m wide × 1.0m tall | Bulbous, extensive glazing; bombardier position forward |

**Simplified approach:** Model cockpit as a geometric dome/bubble fitted to fuselage nose section. Don't model detailed instrument panels (cosmetic detail for game, not required).

---

## Summary for Blender MCP

**Start with Bf-109:** Use fuselage diameter (0.76m) and length (8.64m) as anchor proportions. Model fuselage as a tapered cylinder. Apply dihedral angle (6°) to wing. Single engine, retractable gear. This is your **reference template**; once validated, apply the scaling/proportion ratios to P-51, Spitfire, He-111, B-17.

**Key ratios to preserve across all variants:**
- `length ÷ wingspan` (capture the distinctive silhouette)
- `dihedral angle` (wing tilt)
- `fuselage diameter ÷ length` (compact vs. slender)
- `landing gear track ÷ wingspan` (stance proportions)

