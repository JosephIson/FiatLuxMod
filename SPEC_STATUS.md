# FIAT LUX: spec coverage

This file checks every section of the FIAT LUX design spec (§0–§10) against the code in this repository.
It is meant to be honest: an item is **Done** only when it exists in the mod and has been seen working,
either in a GameTest or in the game client.

| Mark | Meaning |
|------|---------|
| ✅ Done | Implemented, tested or seen in game |
| 🟡 Partial | Works, but simpler than the spec describes (the gap is stated) |
| ❌ Not yet | Not implemented |

The 10 GameTests in `gametest/FiatLuxGameTests.java` run in CI on every push (`./gradlew runGameTestServer`).

---

## §0 Design philosophy

| Spec item | Status | Where / notes |
|---|---|---|
| Self-sustaining simulation (SavedData + throttled ticks) | ✅ | `world/WorldTicker.java` and `CosmicExpanseSavedData`: orbits, mortal growth, prayer → faith → Glory, sin → corruption, deaths → souls, eras. The simulation advances without the player (GameTest `creationSustainsItself`). |
| Mortals form tribes, pray, build, advance eras, sin, die | 🟡 | Simulated as numbers per planet (`Planet`: population, faith, sin, `MortalEra`). There are no mortal *entities* walking around yet (see §7). |
| Angels track alignment and pride | ✅ | `divinity/AngelState` (pride, loyalty) and `Alignment` attachments. Pride drifts with the cosmic climate (`RebellionWar.tickBeing`). |
| Demons form hierarchies, advance tech eras, trade contracts | 🟡 | `UnderworldState`: demons, overlords, territories, contracts, knowledge and eras. Territory *wars* between overlords are not simulated. |
| No mandatory gates; milestones are events, not cutscenes | ✅ | Big Bang, Genesis, Rebellion, Fall, Flood (Deluge) and Underworld revolutions all emerge from the simulation or are cast/triggered (`/fiatlux event …`). |
| Babel, Exodus, Incarnation, Crucifixion, Resurrection events | ❌ | Not implemented. (Part Waters exists as a power, but not as an Exodus event.) |
| Everything configurable via ModConfigSpec | ✅ | `config/FiatLuxConfig.java`: COMMON/CLIENT/SERVER. |
| Live in-game editing of config | 🟡 | NeoForge's built-in `ConfigurationScreen` from the Mods list. There is no custom management GUI. |
| Play modes: Unbound / Ascension / Mortal Pilgrim | ✅ | `config/PlayMode.java`, `/fiatlux mode`, `/fiatlux pilgrim`. Ascension tiers gate powers by Glory. |

## §1 Technical stack & mandates

| Spec item | Status | Where / notes |
|---|---|---|
| NeoForge 1.21.1, Java 21, Parchment, ModDevGradle | ✅ | `build.gradle`, `gradle.properties` |
| DeferredRegister / DeferredHolder everywhere | ✅ | `registry/*`, `PowerRegistry`, `DivineWeaponRegistry` |
| Custom registry | ✅ | `fiatlux:divine_power` (`PowerRegistry`) |
| Data Components only on items (no raw NBT) | ✅ | `registry/ModDataComponents` (weapon traits, bound power) |
| Data Attachments for per-entity data | ✅ | `ModAttachments`: divine state, alignment, angel state. Soul records are not built yet (see §7). |
| SavedData for world systems | ✅ | `CosmicExpanseSavedData`, `CosmosData` (mode, Glory, Chronicle). Covenants and the Son's state are not built. |
| CustomPacketPayload + StreamCodec, split by direction | ✅ | `network/*`: clientbound Fx/SyncCosmos/SyncDivinity; serverbound Cast/SelectPower |
| PartEntity for colossal models | ✅ | `OphanEntity` + `OphanPart`. Leviathan and Behemoth don't exist yet. |
| Heavy edits queued over tick budgets | ✅ | `creation/EditQueue` (with undo), GameTest `creationQueueAppliesAndUndoes` |

## §2 Package structure

| Package | Status |
|---|---|
| registry, config, network, divinity, power, entity, realm, weapon, world, effect, client, command, datagen, creation | ✅ present |
| faith, civilization, son, soul, artifact, achievement | ❌ not created yet (their systems are listed as missing below) |

## §3 Blockbench workflow & model quality

| Spec item | Status | Where / notes |
|---|---|---|
| GeckoLib .geo.json / .animation.json / textures | ✅ | `tools/modelgen` generates Blockbench-compatible GeckoLib files; every model opens in Blockbench. |
| Hot-reload of edited models without restarting | 🟡 | GeckoLib reloads models on F3+T (resource reload). There is no file-watcher for live reloading. |
| In-game exporter to .geo.json | ❌ | |
| Detail tiers enforced | ✅ | `build_models.py` fails the build when a model is below its tier. Current counts (cubes/bones): Angel 187/70 (T2) · Fallen 246/88 (T2) · Principality 302/100 · Archangel 334/100 · Virtue 318/99 (T3) · Dominion 350/99 (T3) · Power 389/104 (T3) · Seraph 354/130 (T3) · Cherub 467/163 (T3) · Ophan 480/134 (T3) · Flaming Sword 98/10 |
| Ophanim: interlocking rotating rings with blinking eyes | ✅ | `ophan.py`; the rims are PartEntities |
| Cherubim: 4 sculpted faces | ✅ | `cherub.py`: man, lion, ox, eagle (Ezek 1:10) |
| Tier 4 colossi (Leviathan, Behemoth, High Sins) | ❌ | |
| AutoGlowingGeoLayer + _glowmask | ✅ | All Host renderers, `FallenRenderer`, `FlamingSwordRenderer` |
| Translucent passes for ethereal wings/energy | ✅ | `entityTranslucent` for the Host and the Fallen (membranes) |

## §4 Weaponry

| Weapon | Status | Where / notes |
|---|---|---|
| Flaming Sword of the Cherubim | ✅ | `weapon/FlamingSwordItem`: 3D GeckoLib model with emissive fire. Ignites on hit, fire wave on swing, slays the corrupt in one stroke (GameTest), can be stationed as a guardian. |
| Spear of the Archangel (Michael's Lance) | ❌ | The Archangel *carries* a spear in its model, but there is no player weapon. |
| Exorcist Scythe & Harpoons | ❌ | |
| Staff of Wonders | ❌ | Part Waters exists as a power, not a staff. |
| Infernal Tech Blades & Soul Firearms | ❌ | |
| Trumpets of Judgment | ❌ | The Archangel's trumpet blast is an entity ability, not an item. |
| Divine Scepter (casting focus) | ✅ | `weapon/DivineScepterItem` (an extra item, not in the spec) |

## §5 Hub & cosmic expansion

| Spec item | Status | Where / notes |
|---|---|---|
| Heaven: crystalline realm, golden streets, pearl gates, clouds, thrones | 🟡 | `world/HeavenChunkGenerator`: floating cloud islands, golden streets on jasper kerbs, lamps of Lux and celestial crystal (GameTest `heavenHasGoldenStreets`). **Gates of pearl** (Rev 21:21) span the four streets out of the central crossing, each holding a veil into the Void (GameTest `theVoidLiesBeyondHeaven`). There are four gates, not Scripture's twelve. The throne is drawn in the sky; **built thrones are missing** from the terrain. |
| **The Void beyond Heaven** (the dark Cosmic Expanse) | ✅ | Its own dimension, `fiatlux:the_void`, reached through Heaven's gates of pearl (or by falling from Heaven's islands). Before the Big Bang it is utter darkness over the face of the deep (Gen 1:2), with only Heaven's Gate glowing behind you. Speaking Let There Be Light or the Cosmic Spark there begins the universe. The sky fills with stars, nebulae and galaxies, the greater light kindles ahead, and every created world circles it as a real globe you can fly to (`client/fx/VoidSkyEffects`, `VoidBodies`, `world/VoidSpace`). Flight is weightless. Seen in game. |
| Big Bang visuals: nebulae, star field, galaxy formation | ✅ | `VoidSkyEffects`, `GalaxyRenderer`: sparkling stars with diffraction spikes, the galaxy's band, clusters, shooting stars; spiral galaxies with bulges, central black holes, coloured arms, star-forming knots and dust lanes. |
| Black holes (beyond the spec) | ✅ | `cosmos/BlackHole`, `realm/AbyssPhysics`, `VoidBodies`: the Abyss forms with the Big Bang; horizon, photon ring, accretion disk, lensed image and jets; pull and horizon → Underworld. GameTest `bottomlessPitsPull`. Seen in game. |
| Flying into a world / rising out of it | ✅ | `RealmTravel.tickPassage`: flying into a world's body in the Void sets you down on it; rising above its sky (or Jacob's Ladder) returns you to the Void beside it. `/fiatlux planet approach <name>` jumps to a world in the Void. Seen in game. |
| Big Bang: light beams, nebulae, star field, galaxy formation | ✅ | `CosmicSparkPower` + `HeavenSkyEffects`; seen in game |
| Planet types: Earth-like, Volcanic, Barren Moon, Gas Giant, Crystalline | ✅ | `world/cosmos/PlanetType`, `PlanetaryGenesisPower` |
| Orbiting planets that tick autonomously | ✅ | Kepler orbits in `WorldTicker`; an orrery in Heaven's sky |
| **Enterable planetary dimensions** | ✅ | `realm/PlanetChunkGenerator` + a pool of 16 dimensions (`planet_0..15`), one terrain per type. `/fiatlux planet visit <name>` or the Visit World power. Lands on safe ground. GameTest `createdWorldsAreEnterable`; all five types seen in game. |
| Custom gravity | ✅ | `RealmTravel.applyGravity` (gravity + safe-fall attributes) |
| Atmospheric haze | ✅ | `PlanetSkyEffects`: fog and sky tinted by each planet's air |
| Day/night clock per planet | 🟡 | The sky follows a sun with day and night, but every planet shares the same clock. Each planet's own day length is not applied yet. |
| Planet biome configuration | 🟡 | One biome per world (`created_world`); the terrain varies by type. |

## §6 Rebellion, the Fall & the Underworld

| Spec item | Status | Where / notes |
|---|---|---|
| Pride / Alignment / Loyalty attachments | ✅ | `AngelState`, `Alignment` |
| Civil war breaks out autonomously when corruption crosses the threshold | ✅ | `WorldTicker.stepRebellion` (config `rebellionCorruptionThreshold`). The war is **embodied**: `RebellionWar.begin` musters rebels (led the first time by Lucifer, son of the morning) from every choir against Michael the Archangel and the Powers. The proud among existing angels may turn. |
| "An Archangel rejects divine laws" as a trigger | ❌ | Only corruption triggers it (or `/fiatlux event rebellion`). |
| The Fall: lightning, halos shatter, wings tear/mutate, plunge down a sky fracture | ✅ | `HeavenlyBeing.castOut` → `FallenEntity`. A rebel can't die: when defeated or cast out it is struck by lightning, its halo shatters into gold shards and torn feathers, the sky splits in a crimson fracture, and it becomes a Fallen Angel (bat wings, broken halo) that plunges into the Underworld and lands in a crater-wave. GameTest `rebelAngelsAreCastOut`; seen in game. |
| Cast Out power | ✅ | `CastOutPower` (Rev 12:9), `/fiatlux event castout` |
| The war ends when no rebel remains | ✅ | `RebellionWar.onCastOut`; GameTest `rebellionEndsInTheFall` |
| Custom Underworld dimension (not the Nether) | ✅ | `realm/UnderworldChunkGenerator`: sulfur plains, obsidian mountains, abyssal rivers, a dark red sky (`UnderworldSkyEffects`). GameTest `underworldHasItsEras`. |
| Tech eras: Tribal → Feudal → Industrial → Modern/Cybernetic | ✅ | Simulated in `UnderworldState`. Settlements built in the world keep the era they were founded in (`UnderworldSettlements`): bone camps, bastions, soul-engine foundries and rails, neon megacity blocks. |
| Demons / sinner souls as living entities | 🟡 | Fallen Angels live and spawn there. Demon and sinner-soul *entities* don't exist; they are simulated counts. |
| Hierarchy & soul economy (overlords, territories, contracts) | 🟡 | Simulated as numbers (overlords, territories, contracts). No overlord entities and no territory buildings. |
| Trade networks between infernal cities | ❌ | |
| Periodic divine cleanses | ✅ | `WorldTicker.divineCleanse` (configurable interval and strength). Abstract; no Exorcist Angel entities descend yet. |

## The celestial hierarchy (requested after the original spec)

| Item | Status | Where / notes |
|---|---|---|
| Nine choirs in three spheres | ✅ | `entity/Choir`. Seraphim, Cherubim, Thrones (Ophanim) / Dominions, Virtues, Powers / Principalities, Archangels, Angels. Each has its own model and an act of office. Dominions strengthen lower choirs, Virtues heal, Powers bind the corrupt, Principalities guard mortals, Archangels sound the trumpet. GameTests `heavenlyHostSpawns` and `virtuesHeal`. `/fiatlux host` lists the hierarchy. |

## §7 Mortals, civilization & redemption

| Spec item | Status | Where / notes |
|---|---|---|
| Settlements, families, temples, eras up to spacefaring | 🟡 | Simulated per planet (population, eras from hunter-gatherer to spacefaring). No mortal entities or built settlements on planets yet. |
| Soul Record & deeds per entity | ❌ | Only the `Alignment` attachment (virtue/sin). |
| Judgment Seat / automated realm rules | 🟡 | The simulation sends the dead to Heaven or the Underworld by their sin; there is no Judgment Seat. |
| Redemption Havens in the Underworld | ❌ | |

## §8 The Son

| Spec item | Status |
|---|---|
| Send the Son, ministry AI, disciples, miracles, Passion, Resurrection, New Covenant | ❌ Not started |

## §9 Achievement Forge & GUIs

| Spec item | Status | Where / notes |
|---|---|---|
| Radial Power Wheel (default key R) | ✅ | `client/screen/PowerWheelScreen`; G casts the selected power |
| Creation Codex (Chronicle timeline) | 🟡 | `/fiatlux chronicle` command; no GUI |
| Achievement Forge GUI + datapack export | ❌ | |
| Species Designer, Realm Forge, Judgment Seat, Commandment Editor screens | ❌ | (`/fiatlux planet create` stands in for the Realm Forge) |

## §10 Starting implementation task

| File | Status |
|---|---|
| `FiatLux.java` | ✅ |
| `WorldTicker.java` & `CosmicExpanseSavedData.java` | ✅ |
| `FlamingSwordItem.java` & `DivineWeaponRegistry.java` | ✅ |
| `FiatLuxConfig.java` | ✅ |
| `AbstractDivinePower.java` & `PowerRegistry.java` (+ casting packets) | ✅ |

---

## Biggest gaps, in suggested order

1. **The Son (§8):** incarnation NPC, ministry, disciples, miracles, Passion and Resurrection.
2. **Weapon arsenal (§4):** Michael's Lance, Exorcist Scythe, Staff of Wonders, Infernal blades, Trumpets.
3. **Mortal and demon entities (§7, §6):** settlements on planets, overlords and territories in the Underworld, Soul Records, Judgment Seat, Redemption Havens.
4. **GUIs (§9):** Creation Codex, Achievement Forge, Realm Forge, Judgment Seat, Commandment Editor.
5. **Tier 4 colossi (§3):** Leviathan and Behemoth as PartEntities.
6. **Remaining biblical events (§0):** Babel, Exodus, Incarnation, and per-planet day length.
