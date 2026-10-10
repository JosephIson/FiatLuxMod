# FIAT LUX — *Let there be light*

A NeoForge 1.21.1 sandbox god-mod where the player is the Creator. You begin in Heaven, speak
light into the darkness, raise land from the sea, part the waters, open the windows of heaven,
and send the Heavenly Host. The Host is modelled on how Scripture describes it, not on later
art.

> Status: **Phase 1, the foundation.** It compiles, all GameTests pass, and it has been verified
> in-game on a dedicated server and on a real client (screenshots taken under Xvfb). See the
> [roadmap](#roadmap) for what comes next.

## Stack

| | |
|---|---|
| Minecraft / Loader | 1.21.1 / NeoForge 21.1.209 (Java 21) |
| Build | ModDevGradle 2.0.148, Parchment 2024.11.17 |
| Animation | GeckoLib 4.9.3 (`.geo.json`, `.animation.json`, `_glowmask` emissive layers) |

## Build, test, run

```bash
./gradlew build                 # compile + jar  -> build/libs/fiatlux-<version>.jar
./gradlew runGameTestServer     # headless server runs every fiatlux GameTest, then exits
./gradlew runData               # regenerate models/lang/loot/tags/sounds into src/generated
./gradlew runClient             # dev client  (-PquickPlayWorld=<save> boots straight into a world)
python3 tools/modelgen/build_models.py --preview /tmp/preview   # regenerate 3D models + turnarounds
python3 tools/texgen/build_textures.py                           # regenerate 2D textures
```

Model work needs only Python 3 with Pillow.

## Playing

* New Creators open their eyes in **Heaven** (config `startInHeaven`).
* **Scepter of the Creator**: use it to speak the selected power. Sneak-use cycles powers.
* **R** opens the radial power wheel (§9). **G** speaks the selected power.
* **Flaming Sword of the Cherubim**: strikes ignite, and corrupt beings fall in one stroke. Use it
  to release a wave of holy fire. Sneak-use on the ground stations it as a guardian "which
  turned every way" (Gen 3:24); right-click the guardian to take it back.
* `/fiatlux heaven | cast <power> | undo | chronicle [n] | mode <unbound|ascension|mortal_pilgrim>
  | glory add <n> | essence <player> <n> | pilgrim <player> <true|false>`
* `/fiatlux cosmos` shows the universe, every world, the moral climate and the Underworld.
  `/fiatlux planet create <type> [name] | planet life <name>`,
  `/fiatlux planet visit <name>` (walk on a created world), `/fiatlux realm heaven|underworld|earth`,
  `/fiatlux host` (the nine choirs and how many of each are loaded, loyal and rebel),
  `/fiatlux sim step <seconds>`, `/fiatlux event bigbang|rebellion|fall|castout|cleanse`

### Play modes
* **Unbound** (default): every power is available. The essence cost multiplier defaults to 0.
* **Ascension**: powers unlock as world Glory reaches each tier (`gloryPerTier`).
* **Mortal Pilgrim**: players marked as pilgrims cannot wield power. Only operators act as God.

### Powers (`PowerRegistry`, registry `fiatlux:divine_power`, base class `AbstractDivinePower`)
| Power | Scripture | Act |
|---|---|---|
| Cosmic Spark | Gen 1:1 | **The Big Bang**: white-out, prismatic shells and beams fill the sky, galaxies unfurl |
| Planetary Genesis | Gen 1:14-16 | Forms a world (Earth-like, volcanic, barren moon, gas giant, crystalline) on a Kepler orbit |
| Breath of Life | Gen 2:7 | Seeds mortal life on a habitable world; it then lives on its own |
| Fiat Lux | Gen 1:3 | Fills dark pockets with light; radiance burst |
| Evening and Morning | Gen 1:5 | Advances to the next dusk or dawn; records the day in the Chronicle |
| Bless | Num 6:24 | Blessed and Regeneration; adds virtue |
| Let the Dry Land Appear | Gen 1:9 | Raises a hill from ground or sea (queued, undoable) |
| Fire from Heaven | 1 Kgs 18:38 | Lightning and a celestial pillar; consumes the corrupt |
| Divide the Waters | Ex 14:21-22 | Dry corridor through water, walled by stilled water |
| Windows of Heaven | Gen 7:11 | Flood that spreads outward ring by ring; storm |
| Visit World | | Walk on the next created world, under its own sky and gravity |
| Cast Out | Rev 12:9; Luke 10:18 | Strikes down the rebel angels in your gaze and around it: they fall into the Underworld |
| Send an Angel / Archangel / Principality, Summon a Power / Virtue / Dominion / Seraph / Cherub / Ophan | Heb 1:14 and below | Summons one of the nine choirs |
| Jacob's Ladder | Gen 28:12 | Travel between Heaven and the earth |

## The self-sustaining creation (§0, §5-§7, §10.2)
`world/WorldTicker` advances `world/CosmicExpanseSavedData` on the server thread every
`cosmicStepInterval` ticks, scaled by `simulationSpeed`. Per-planet work is round-robin and
capped by `planetsPerStep`, so the cost per tick stays bounded however large the universe grows.
* **Orbits**: every planet moves along its orbit (period ∝ a^1.5). Clients extrapolate between syncs,
  so the orrery in Heaven's sky moves smoothly.
* **Mortals**: logistic population growth up to the world's habitability. The pious share prays,
  and their faith becomes world Glory, which feeds Ascension tiers. Sin and death follow: the
  righteous ascend to Heaven and the unrepentant descend to the Underworld. Peoples advance
  Hunter-Gatherer → Agrarian → Classical → Medieval → Industrial → Spacefaring.
* **Moral climate**: sin, faith and infernal pressure set world corruption, and corruption breeds
  angelic pride. Above `rebellionCorruptionThreshold`, war in Heaven may break out on its own
  (Rev 12:7). It resolves into **the Fall**: the cast-out swell the Underworld (Rev 12:9).
* **The Underworld**: sinner souls yield soul power, the fallen beget an infernal hierarchy of
  overlords and territories, soul contracts are signed, and knowledge accumulates.
  **Technological revolutions** carry it from Tribal/Pits to Feudal/Castle, then Industrial, then
  Modern/Cybernetic, with every threshold configurable. Periodic **divine cleanses** by Exorcist
  Angels hold infernal empires in check.
* Every milestone is written to the Chronicle and shown as an effect to all players. Nothing
  is scripted: events happen when the simulated conditions are met, or when a Creator forces them.

### Spec §10 file map
| §10 deliverable | Implementation |
|---|---|
| `FiatLux.java` | `FiatLux` (+ `client/FiatLuxClient`) |
| `WorldTicker.java`, `CosmicExpanseSavedData.java` | `world/WorldTicker`, `world/CosmicExpanseSavedData`, `world/cosmos/*` |
| `FlamingSwordItem.java`, `DivineWeaponRegistry.java` | `weapon/FlamingSwordItem` (fire wave on swing and use; GeckoLib renderer `client/render/FlamingSwordRenderer` with `AutoGlowingGeoLayer`), `weapon/DivineWeaponRegistry` |
| `FiatLuxConfig.java` | `config/FiatLuxConfig` (simulation speed, essence rates, rebellion probabilities, Underworld progression rates, and more) |
| `AbstractDivinePower.java`, `PowerRegistry.java` | `power/AbstractDivinePower`, `power/PowerRegistry`, `power/PowerCasting`; packets `network/CastPowerPayload` / `SelectPowerPayload` |

## The Heavenly Host — as Scripture describes it

| Being | Source | What the model shows | Detail (min. tier) |
|---|---|---|---|
| **Angel** | Dan 10:5-6; Mt 28:3 | A man clothed in linen, girded with gold of Uphaz; body like beryl, face like lightning, eyes as lamps of fire, arms and feet of burnished bronze. **No wings, no halo** (the halo comes from later art). | 187 cubes / 70 bones (T2: 120/40) |
| **Seraph** | Isa 6:2-6 | Six burning wings: one pair **veils the face** and one **veils the feet** (verified 0% / 0.5% visible from the front by ray casting), one pair flies. Tongs hold a live coal from the altar. | 354 / 130 (T3: 250/70) |
| **Cherub** | Ezek 1:5-13; 10:12-14 | Four faces (man in front, lion on the right, ox on the left, eagle behind); two wings stretched upward and two covering the body; human hands under the wings on four sides; straight legs and calf's hooves of burnished brass; body like burning coals; eyes throughout. | 467 / 163 (T3) |
| **Ophan** | Ezek 1:15-21; 10:9-13 | A wheel within a wheel, the colour of beryl, rims full of eyes that blink independently, around a burning spirit core. Multipart: four rim `PartEntity` hit-boxes. | 480 / 134 (T3) |
| **Dominion** | Col 1:16 | The man in linen vested for rule: sapphire stole embroidered with gold, a crown, the sceptre in one hand and the orb with a cross in the other, great wings. **Office:** strengthens the lower choirs, burdens enemies. | 350 / 99 (T3) |
| **Virtue** | Eph 1:21; 1 Pet 3:22 | Robed in light, a sunburst behind the head, the lily of grace, one hand raised in blessing. **Office:** heals the wounded and lifts afflictions. | 318 / 99 (T3) |
| **Power** | Eph 6:12-17; Jude 1:6; Rev 20:1 | Armed for war: breastplate with a cross, helmet with a plume, pauldrons, the sword of the Spirit with an edge of holy fire, and the great chain. **Office:** binds the corrupt; doubly fierce to the fallen. | 389 / 104 (T3) |
| **Principality** | Col 1:16; Dan 10:13, 21 | A prince over a people: circlet with a cross, crimson cloak, the swallow-tailed standard of its people, a cross-headed sceptre. **Office:** guards mortals from every hostile creature and shields them. | 302 / 100 |
| **Archangel** | 1 Thess 4:16; Jude 1:9; Rev 12:7 | Captain of the Host: gold breastplate over linen, crimson cloak, great wings, the spear that thrusts down the dragon, and the trumpet of God. **Office:** the trumpet blast strikes and hurls back enemies. Michael leads the war in heaven. | 334 / 100 |
| **Fallen Angel** | Isa 14:12; Luke 10:18; Jude 1:6 | Scorched ash-grey flesh cracked with smouldering veins, a blackened robe torn to ribbons, a halo broken into drifting tarnished shards, red eyes, and torn skeletal bat wings with a few burnt feathers left. | 246 / 88 (T2) |
| **Flaming Sword** | Gen 3:24 | Gold cherub-wing guard, ivory and gold grip, sapphire pommel, translucent crystal blade wreathed in animated holy fire. | 98 / 10 |

Scripture names every choir of the hierarchy (`entity/Choir`, highest first): Seraphim, Cherubim,
Thrones / Dominions, Virtues, Powers / Principalities, Archangels, Angels. It *describes* only some
of them. The second and third spheres are built on the man in linen of Daniel 10 and set apart by the
regalia tradition gives each office.

### War in heaven and the Fall
When corruption crosses `rebellionCorruptionThreshold` (or on `/fiatlux event rebellion`), the
proud among the living Host turn. A rebel host musters in Heaven from every choir, led the
first time by *Lucifer, son of the morning*. Michael the Archangel and the Powers stand against it.
Rebels are tinted toward blood and ash and cannot die, only fall. When one is defeated, or the
Creator speaks **Cast Out**, lightning strikes it and its radiance shatters into gold shards and
torn feathers. The sky splits in a crimson fracture, and it mutates into a Fallen Angel that
plunges through the sky into the Underworld and lands in a crater-wave of fire. The war ends when
no rebel remains.

### Realms
* **Created worlds**: each planet you form is bound to one of 16 dimensions (`planet_0..15`) with
  its own terrain: Earth-like meadows, seas and snowy peaks; volcanic basalt, lava lakes and crater
  cones; cratered regolith moons; banded gas-giant cloud decks; crystalline plains with crystal
  spires. Each has its own gravity and an atmosphere tinted by its air, and you land on safe ground.
* **The Underworld** (not the Nether): sulfur plains, obsidian mountains, abyssal rivers and a
  dark red sky. Its settlements take the infernal era in which they were first generated: bone
  camps with soul-fire pits, gothic bastions, soul-engine foundries with rail lines, and
  neon-banded megacity blocks. Fallen angels dwell there.

Heaven follows Revelation 21: floating cloud islands, streets of "pure gold, as it were
transparent glass", jasper "clear as crystal", lamp pillars of Lux, and a sky with the light
of the throne and "a rainbow round about the throne, in sight like unto an emerald" (Rev 4:3).

## Visual effects
* **Particles**: additive-blended sparks, an 8-frame animated holy fire, drifting motes,
  falling feathers, embers and radiance, plus a feather pass with normal blending.
* **FxManager**: server events (`FxPayload`) become expanding light rings with a gold contrast
  rim, celestial pillars (core, glow and halo prisms), fire waves, blessing spirals, the
  deluge and screen flashes.
* **Heaven sky**: procedural azure-to-gold dome, the throne-light in a golden corona, the emerald
  rainbow (Rev 4:3) and rotating rays of glory. After the Big Bang, pastel nebula galaxies
  unfurl and every created world circles the throne as a live orrery (lit discs, atmospheres,
  the lights of inhabited worlds). Elements over the bright sky use alpha blending, because
  additive light washes out against it.
* **GeckoLib**: `AutoGlowingGeoLayer` lights every `_glowmask` texel (holy fire, coal veins,
  lightning faces, eyes); translucent passes for crystal and beryl.
* All of it is configurable in the CLIENT config: density, flashes, god rays, vignette, auras.

## Architecture
```
com.josephison.fiatlux
 ├─ FiatLux / client.FiatLuxClient   entry points
 ├─ config/        COMMON · CLIENT · SERVER ModConfigSpec, PlayMode
 ├─ registry/      DeferredRegisters: blocks, items, data components, attachments,
 │                 entities, effects, particles, sounds, tabs, chunk generators, tags
 ├─ divinity/      DivineState + Alignment attachments, essence regen, login → Heaven
 ├─ power/         DivinePower registry, server-authoritative casting pipeline, powers/
 ├─ creation/      EditQueue (tick-budgeted, undoable world edits), Chronicle entries
 ├─ world/         CosmosData (SavedData), HeavenChunkGenerator, travel, Stilled Water
 ├─ entity/        HeavenlyBeing base + Angel, Seraph, Cherub, Ophan, stationed sword
 ├─ weapon/        Flaming Sword (GeoItem), Scepter, WeaponTraits data component
 ├─ effect/        Blessed, Sanctified, Cursed, Corrupted, Spirit-Filled
 ├─ network/       CustomPacketPayloads: Cast/SelectPower (serverbound), SyncDivinity/Fx (clientbound)
 ├─ client/        fx/ (particles, FxManager, sky, overlay), render/, screen/, HUD, keybinds
 ├─ command/       /fiatlux
 ├─ datagen/       models, blockstates, lang, loot, tags, sounds, particles
 └─ gametest/      in-world tests
tools/modelgen     Python → Blockbench-compatible GeckoLib models, painted atlases, previews
tools/texgen       Python → block/item/particle/effect textures
```
Rules followed: all item data uses Data Components (no raw item NBT); per-entity data uses
Attachments; world state uses SavedData; heavy edits are queued under a per-tick block
budget; the client sends intent and the server decides.

### Model pipeline notes
`tools/modelgen/geo.py` reproduces GeckoLib's exact transform conventions (mirrored x,
negated x/y rotations, X-then-Y-then-Z order). Previews and the ray-cast coverage check therefore
match the game, and every model is checked against its tier minimum at generation time.
Outputs open directly in Blockbench for hand polishing.

## Roadmap
See [SPEC_STATUS.md](SPEC_STATUS.md) for every item of the design spec (§0–§10) marked Done,
Partial or Not yet, with where it lives in the code. The largest remaining pieces are the Son
(§8), the rest of the weapon arsenal (§4), mortal and demon entities with Soul Records and
Judgment (§6–§7), the management GUIs (§9), and the Tier 4 colossi (§3).
