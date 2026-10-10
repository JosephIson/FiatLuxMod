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
* **V** opens the radial power wheel. **R** speaks the selected power.
* **Flaming Sword of the Cherubim**: strikes ignite, and corrupt beings fall in one stroke. Use it
  to release a wave of holy fire. Sneak-use on the ground stations it as a guardian "which
  turned every way" (Gen 3:24); right-click the guardian to take it back.
* `/fiatlux heaven | cast <power> | undo | chronicle [n] | mode <unbound|ascension|mortal_pilgrim>
  | glory add <n> | essence <player> <n> | pilgrim <player> <true|false>`

### Play modes
* **Unbound** (default): every power is available. The essence cost multiplier defaults to 0.
* **Ascension**: powers unlock as world Glory reaches each tier (`gloryPerTier`).
* **Mortal Pilgrim**: players marked as pilgrims cannot wield power. Only operators act as God.

### Powers (registry `fiatlux:divine_power`)
| Power | Scripture | Act |
|---|---|---|
| Fiat Lux | Gen 1:3 | Fills dark pockets with light; radiance burst |
| Evening and Morning | Gen 1:5 | Advances to the next dusk or dawn; records the day in the Chronicle |
| Bless | Num 6:24 | Blessed and Regeneration; adds virtue |
| Let the Dry Land Appear | Gen 1:9 | Raises a hill from ground or sea (queued, undoable) |
| Fire from Heaven | 1 Kgs 18:38 | Lightning and a celestial pillar; consumes the corrupt |
| Divide the Waters | Ex 14:21-22 | Dry corridor through water, walled by stilled water |
| Windows of Heaven | Gen 7:11 | Flood that spreads outward ring by ring; storm |
| Send an Angel / Seraph / Cherub / Ophan | Heb 1:14 and below | Summons one of the Host |
| Jacob's Ladder | Gen 28:12 | Travel between Heaven and the earth |

## The Heavenly Host — as Scripture describes it

| Being | Source | What the model shows | Detail (min. tier) |
|---|---|---|---|
| **Angel** | Dan 10:5-6; Mt 28:3 | A man clothed in linen, girded with gold of Uphaz; body like beryl, face like lightning, eyes as lamps of fire, arms and feet of burnished bronze. **No wings, no halo** (the halo comes from later art). | 187 cubes / 70 bones (T2: 120/40) |
| **Seraph** | Isa 6:2-6 | Six burning wings: one pair **veils the face** and one **veils the feet** (verified 0% / 0.5% visible from the front by ray casting), one pair flies. Tongs hold a live coal from the altar. | 354 / 130 (T3: 250/70) |
| **Cherub** | Ezek 1:5-13; 10:12-14 | Four faces (man in front, lion on the right, ox on the left, eagle behind); two wings stretched upward and two covering the body; human hands under the wings on four sides; straight legs and calf's hooves of burnished brass; body like burning coals; eyes throughout. | 467 / 163 (T3) |
| **Ophan** | Ezek 1:15-21; 10:9-13 | A wheel within a wheel, the colour of beryl, rims full of eyes that blink independently, around a burning spirit core. | 480 / 134 (T3) |
| **Flaming Sword** | Gen 3:24 | Gold cherub-wing guard, ivory and gold grip, sapphire pommel, translucent crystal blade wreathed in animated holy fire. | 98 / 10 |

Heaven follows Revelation 21: floating cloud islands, streets of "pure gold, as it were
transparent glass", jasper "clear as crystal", lamp pillars of Lux, and a sky with the light
of the throne and "a rainbow round about the throne, in sight like unto an emerald" (Rev 4:3).

## Visual effects
* **Particles**: additive-blended sparks, an 8-frame animated holy fire, drifting motes,
  falling feathers, embers and radiance, plus a feather pass with normal blending.
* **FxManager**: server events (`FxPayload`) become expanding light rings with a gold contrast
  rim, celestial pillars (core, glow and halo prisms), fire waves, blessing spirals, the
  deluge and screen flashes.
* **Heaven sky**: procedural gradient dome, throne-light, emerald rainbow and rotating god-rays.
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
Phase 1 covers spec §0-§2 foundations, the Heaven hub (§5, first part), the scriptural Host
and the Flaming Sword (§3-§4, first weapon). Planned next:
1. **Realm Forge and the Underworld**: runtime-created dimensions (this also removes vanilla's
   one-time "experimental settings" notice that datapack dimensions trigger), an Underworld
   whose demonic civilisations advance through technological eras.
2. **Autonomous simulation**: mortals and tribes, prayer and faith feeding Glory,
   settlements, eras, covenants and prophets, soul records and judgment.
3. **The rest of the arsenal**: Michael's Lance, the Staff of Wonders, Trumpets of Judgment,
   infernal tech blades and soul firearms.
4. **Cosmic expansion**: the Big Bang, planetary systems and the Chronicle's milestone events.
5. **Colossal beings** (Leviathan, Behemoth) as multipart entities; Blockbench hot-reload and
   a `.geo.json` exporter for species designed in-game.
6. Bespoke audio to replace the vanilla placeholder layers in `sounds.json`.
