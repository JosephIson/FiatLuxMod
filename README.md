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
* `/fiatlux cosmos` opens the Cosmic Expanse screen: the universe, every world with its living people,
  towns and faiths, Heaven, the Underworld, the hierarchy of the fallen and the Chronicle.
  `/fiatlux planet create <type> [name] | planet life <name>`,
  `/fiatlux planet visit <name>` (walk on a created world), `/fiatlux planet approach <name>` (fly to it in the Void),
  `/fiatlux realm heaven|void|underworld|earth`,
  `/fiatlux host` (the nine choirs and how many of each are loaded, loyal and rebel),
  `/fiatlux lineage [name]` (open the Book of Generations), `/fiatlux faiths` (every religion and its followers),
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

### The living Host
From the moment a world is made, Heaven is full: the Heavenly Host of all nine choirs lives about
whoever walks its streets. Angels are the most numerous, then archangels, principalities, powers,
virtues, dominions, cherubim, seraphim and ophanim, the highest fewest (Daniel 7:10). Some keep
stations among the streets and some fly overhead, and on arrival they are all there at once. On the
earth and the living worlds a few ministering angels pass overhead (Heb 1:14). These serve no
Creator in particular, so in the war in Heaven it is from among them that the rebels rise.
Summoning powers still send a being that is yours. Change the numbers with `hostInHeaven` and
`hostOnWorlds`, or set them to 0.

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
* **The Void** (`fiatlux:the_void`): beyond Heaven lies the formless deep where the universe
  begins. Step through any of Heaven's **gates of pearl** (on the four streets out of the central
  crossing), or fall from Heaven's islands. Before the Big Bang it is utter darkness: "darkness was
  upon the face of the deep" (Gen 1:2), with the dark waters below and Heaven's Gate glowing behind
  you. Speak **Let There Be Light** (or the Cosmic Spark) there and the light bursts out ahead. The
  sky fills with stars, nebulae and turning galaxies, the greater light kindles, and every world you
  form circles it as a real globe: lit by the sun, with its own surface, atmosphere and night-side
  city lights, and rings on gas giants. Flight there is weightless. Fly into a world to descend
  onto it, rise above its sky to return, and fly back into Heaven's Gate to go home.
  The starfield sparkles: the brightest stars carry halos and diffraction spikes, a faint band of the
  galaxy crosses the sky, globular clusters gleam and shooting stars streak by. Galaxies show golden
  bulges around white cores and central black holes, arms of stars shading from gold to young blue,
  pink star-forming knots and dark dust lanes, all slowly turning.
* **Black holes, the bottomless pit** (Rev 9:1-2): the first, *the Abyss*, opens with the Big Bang
  off beyond the sun. Each has a horizon of perfect black, a photon ring, a swirling accretion disk
  (white-hot within, red without, brighter on the side turning toward you), the far side of the disk
  bent over and under the shadow, and twin jets. Its pull draws in everything near, and whatever
  crosses the horizon is gone: players fall through into the Underworld. Creators in creative or
  spectator mode can study it safely. Open more with the **Open the Bottomless Pit** power or
  `/fiatlux blackhole create <radius> | remove | list`.
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

## Mortals: the living peoples of the worlds
Speak **Breath of Life** on a created world (or on the earth) and "the LORD God formed man of the
dust of the ground, and breathed into his nostrils the breath of life" (Gen 2:7): the first pair
rises from the dust, and their people live on their own.

### The garden of Eden and the Fall of Man (Genesis 2-3)
The very first man and woman, Adam and Eve, are put into a garden planted for them. It is a round
garden behind a flowering hedge, with a spring at its heart that parts into four rivers: the
Pison (with gold, bdellium and the onyx stone along its banks), Gihon, Hiddekel and Euphrates.
There are fruit trees among the flowers and the beasts of the field, two by two. In the midst
stand the golden **tree of life** and the dark **tree of the knowledge of good and evil**, both
hung with fruit you can pick.
* **Innocence**: Adam and Eve do not age, hunger, sin or die, and no one is born. Adam names the
  beasts (Gen 2:19).
* **The serpent** comes once Satan has fallen in the war in Heaven (`edenTemptationTicks` after the
  garden is planted). He speaks with Eve in the words of Genesis 3:1-5. She takes the fruit, eats,
  and gives it to Adam. Strike the serpent before then to drive him off for a while.
* **The Fall**: "the eyes of them both were opened" (3:7). From then on, people die, children are
  born (Cain, Abel and Seth first), sin enters, and peoples grow. Mortals now know good and evil
  and wonder aloud who they are and why they were made. The pair hide among the trees.
* **Judgment**: come near them, or say "Where art thou?", and the scene of Genesis 3:9-21 plays out.
  Adam and Eve answer, the serpent is cursed and loses his legs, and they are clothed in coats of
  skins. They are then driven out to found the first town east of Eden, and a cherub and a flaming
  sword are set at the east gate to keep the way of the tree of life. No mortal can go back in.
* `/fiatlux eden` shows where the story stands. `/fiatlux eden begin` plants the garden where you
  stand and returns Adam and Eve to innocence (for worlds made before Eden existed).
  `/fiatlux eden tempt` sends the serpent now, and `/fiatlux eden plant` plants a garden only.
* **Lives**: every mortal is born, grows from child to adult to elder, and dies of old age (one
  year is `mortalYearTicks`). Each has a name, two traits from ten (pious, proud, curious, greedy,
  kind, lazy, fearful, joyful, brave, industrious), a trade suited to their people's era (gatherer,
  hunter, farmer, shepherd, builder, priest, scholar, merchant, smith, engineer, pilot), and
  hunger, rest and company that they look after.
* **Days**: they wake at dawn, work their trade, eat at the hearth, gather for evening prayer at
  the shrine and sleep in their houses at night. They talk in speech bubbles about their work, the
  weather and the news of the cosmos: a war in Heaven, angels who fell, a pit in the sky that
  swallows light. Conversations can end in friendship, a quarrel, a conversion or a marriage.
  Builders raise new houses and paths as the village grows, and its buildings follow its era from
  tents to huts to stone.
* **Family trees**: couples marry and have children, and every life is written in the **Book of
  Generations** (Gen 5:1). Use the item or `/fiatlux lineage [name]` to see five generations of a
  family on parchment: each person's age, trade, world and faith, and where the dead went. Click
  anyone to turn to their family.
* **Religions they create, and worship of you**: a people formed by your breath follow *The Way of
  <your name>*. They pray, they kneel when you come among them and call you by name, and they lay
  **petitions** before you (bread, healing, a child, protection, a sign) that arrive in your chat.
  Work a miracle near them to answer: the store fills, the sick are healed and children are given.
  Leave them in silence too long, or ignore their petitions, and doubt grows. A doubter may found a
  faith of their own: the sun, the stars, the ancestors, an idol such as the Golden Calf, or one of
  the fallen. That faith spreads by conversation, and the shrine is crowned by whatever most of the
  village worships. Miracles win the doubting back; wrath only terrifies them. Sneak-use a mortal
  to bless them; use them normally to hear who they are.
* **Peoples that spread**: a world's population grows by real births among the mortals you meet, and
  beyond them at the rates of its age, up to what that age can feed. When a town is full, young
  families go out and found a new town (Enoch, Nod, Shinar, Ur and on through the cities of
  Genesis), up to `maxTownsPerWorld`.
* **Cain and Abel** (Genesis 4): when Adam and Eve's first sons are grown, each brings an offering.
  Abel's is accepted with fire from heaven and Cain's is not, and sin lies at his door. Bless Cain
  then (sneak-use him) and he rules over it, and Abel lives. Otherwise Cain slays Abel in the field.
  Come near him, or say "Where is Abel?", and he answers "Am I my brother's keeper?". He is marked
  so that no one will kill him, and goes out to the land of Nod to build the city of Enoch.
* **Governments and laws**: once a people is numerous, its families gather under their eldest as a
  clan. As the ages pass they become a chiefdom, a people ruled by judges if they keep faith, then a
  kingdom ("make us a king", 1 Sam 8:5), an empire once their towns are many, a republic and at last
  a federation. Crowns pass to the eldest child, and judges and consuls are chosen. Rulers wear
  crowns and give laws over the years: the commandments for a faithful people, while a pagan state
  commands its own gods to be worshipped (Dan 3), which brings persecution. The proud sometimes
  seize the throne. `/fiatlux polity` shows every government, and `/fiatlux anoint <name>` anoints
  a ruler (1 Sam 16:13).
* **Crime and punishment**: theft, violence, murder, idolatry, refusing the state's gods, and
  sedition happen in the mortals' own lives. When someone sees it and there is a law against it,
  the ruler judges, and the punishment fits the age: restitution and exile in clans, the stocks and
  prison in kingdoms, and the cross outside the city under empires, shown without gore. The cross
  and the stocks are models built to fit the condemned's pose, and each bears the charge written on
  it, the name and the crime (John 19:19). The faithful who die for refusing a pagan state's gods
  are martyrs and go up into Heaven. Sneak-use a condemned mortal to **deliver** them (Acts 5:19).
  `/fiatlux judge <name> <crime>` brings someone before their people's law.
* **The generations** (Genesis 4, 5 and 11): the eldest son of each line is given the next name in
  it. Seth begets Enos, then Cainan, Mahalaleel, Jared, Enoch, Methuselah, Lamech and Noah, whose
  sons are Shem, Ham and Japheth. From Shem come Arphaxad and on down to Terah and Abram. Cain begets
  Enoch, Irad, Mehujael, Methusael and Lamech, whose sons are Jabal, Jubal and Tubal-cain. Enoch of
  the line of Seth "walked with God: and he was not; for God took him" (5:24): once he has a son he
  may be taken up into Heaven alive.
* **The Watchers and the Nephilim** (Genesis 6:1-4): once a fallen world's people number
  `watchersMinPeople`, the Watchers come down on it. They are the "sons of God", read here as the Book
  of Enoch tells it: two hundred angels under Shemihazah who swore an oath on Mount Hermon. Their
  chiefs (Shemihazah, Azazel, Armaros, Baraqel and others) take wives in the town, and each teaches
  the arts 1 Enoch 8 gives him, which hastens the age and corrupts it. Their wives bear the
  **Nephilim**, giants who grow up fast to more than twice a man's height. They boast, devour the
  town's bread and strike those near them, no human court dares judge them, and they fill the
  earth with violence (Gen 6:11). When a giant dies, his spirit does not go down to the Underworld
  but wanders the earth as an evil spirit (1 Enoch 15:8-10). Strike a Watcher to bind it in chains
  of darkness (Jude 1:6). `/fiatlux nephilim` brings them down on the town nearest you.
* **The Flood** (Genesis 6:5-9:17): when a world with giants is steeped in sin, its Creator is
  told that "the wickedness of man was great". Say **"Make thee an ark"** (or `/fiatlux flood ark`)
  on that world. Its most righteous man (Noah himself, if the line of Seth has reached him) builds
  the ark beside his town to its true measure: 300 by 50 by 30 cubits, which is 137 by 23 by 14
  blocks of pitch-dark timber. It has three decks of stalls and quarters, a window a cubit high
  under the eaves, and a door in the side. While he builds he preaches, and his neighbours mock.
  * **Boarding**: his house goes in, and the beasts come two and two, the clean by sevens, each kind
    to its stall. After seven days the LORD shuts the door.
  * **The rain**: it rains for forty days, the fountains of the deep break up, and real water rises
    over every town until it stands fifteen cubits above the highest mountain. The ark floats up
    with everyone aboard. All flesh outside dies, and the Watchers are bound.
  * **The waters abate**: they prevail for 150 days while the ark drifts to the highest mountain,
    then go down, and the ark rests "upon the mountains of Ararat". Noah sends out the raven, then
    the dove three times; she brings back an olive leaf.
  * **Going forth**: they come out of the ark, Noah builds an altar, and a rainbow is set in the
    cloud. The world begins again from the eight in a new town, Ararat. The covenant holds: there
    is never a second Flood.
  * **Timing and commands**: the year of the Flood runs at `floodDayTicks` per day. `/fiatlux flood`
    shows where it stands, `/fiatlux flood next` hastens it to its next stage, and
    `/fiatlux flood bow` sets a rainbow in the sky to the south of you.
* **Faiths: leaders, branches and those who leave**: every faith has a leader. It is called an
  elder, then a high priest, a patriarch, an archbishop and a primate as the ages pass. False faiths
  have their own leaders: a sun-priest, a star-seer, a hierophant of the fallen, a druid, a
  pontifex. When a leader dies, the faithful choose another, a priest first. Leaders carry their
  title (use them to see it) and preach their faith best. A corrupt leader brings **scandal**
  (Micah 3:11), and the flock doubts.
  * **Branches**: faiths **split into branches** over doctrine: the keeping of holy days, images in
    the sanctuary, who may lead, the corruption of the leader ("I am of Paul; and I of Apollos",
    1 Cor 1:12). A dissenter founds the *Reformed*, *Orthodox* or *Free* branch, the branch "of
    Enoch", or "the …ites", and most of the faithful in the dissenter's town go with them. Splits are
    likelier when a faith spans many towns or its leader is corrupt.
  * **Leaving**: single mortals **leave** their faith when they doubt, are proud or curious, or are
    shamed by their leader. They may join the faith most of their neighbours hold, or found one of
    their own: the sun, the stars, the ancestors, an idol or one of the fallen, the Great Mother of
    the groves, or, under an empire, the divine emperor himself. Others hold to **no god at all**
    and gather into a school of reason (Psalm 14:1). A faith whose last follower dies is no more.
  * **Commands**: `/fiatlux faiths` shows the faiths of your world as a family tree of branches,
    each with its followers, its leader and what it split over. `/fiatlux faiths split` splits the
    faith of the mortal nearest you, and the Cosmos screen lists leaders and branches.
* **Prophets** (Amos 3:7):
  * **The calling**: when a people goes astray (idols, sin, doubt when heaven is silent, or a
    corrupt priest), the word of its Creator comes to one of the faithful, often a herdsman or a
    farmer (Amos 7:14). You can also call one yourself: say **"Whom shall I send?"** among your
    faithful and the most devout answers "Here am I; send me" (Isaiah 6:8). False faiths raise
    false prophets of their own.
  * **Their work**: prophets leave their trade and go from town to town. They preach against idols,
    against sin, against shepherds who feed themselves, for justice and for the hungry; they bring
    comfort to the doubting and speak of one who is to come ("But thou, Ararat… out of thee shall he
    come forth", Micah 5:2). Hearers turn, or do not. You hear every oracle in full in chat.
  * **Wonders**: where there is need they work wonders. The sick are healed, the bread does not run
    out (2 Kings 4:42-44) and unclean spirits go out.
  * **Disciples**: each prophet takes a disciple, who takes up the mantle when the prophet is gone
    (2 Kings 2:13). An old prophet may be **taken up in a chariot of fire** (2 Kings 2:11).
  * **Persecution**: rulers of other faiths and corrupt priests have prophets seized, and some are
    martyred.
  * **Judgment foretold**: "Yet forty days, and … shall be overthrown". If the town repents it is
    spared (Jonah 3:10). Strike it with a wrathful power and the word is fulfilled. Otherwise the
    word fails (Deut 18:22) and the faithful doubt.
  * **Fire from heaven**: where a town serves another god, the prophet challenges its priest as
    Elijah did on Carmel (1 Kings 18). Work any wonder at the altar within a minute to answer by
    fire, or stay silent and watch the prophet flee. If you are not on that world, the fire falls of
    itself (`prophetsActAlone`). When it falls, the people turn and the altar is crowned for you.
  * **Commands and config**: `/fiatlux prophets` lists every prophet, their oracles and wonders,
    and the judgments foretold. `/fiatlux prophet <name>` calls one. `prophetChance` sets how often
    prophets are raised.
* **The Son** (Galatians 4:4): in the fulness of the time, send your Son into a world you made.
  * **Choosing him**: on that world, `/fiatlux son` opens a chooser for his **name** (with
    suggestions), his **likeness** (shown turning, with a halo), the **town** he is born in, and his
    **mother**, picked from the town's women or left to your providence. `/fiatlux son status`
    shows where his story stands.
  * **The birth**: Gabriel comes to his mother (Luke 1), and a just man takes her to wife. He is
    born under a star seen from far across the land. An angel and then the whole Host appear to
    the shepherds ("Glory to … in the highest"), wise men come from the other towns with gold,
    frankincense and myrrh, and the ruler is troubled. At twelve he teaches the elders in the
    temple.
  * **Baptism and temptation**: he is baptised by the prophet of his day. The heavens open, a dove
    descends, and you are asked to speak from heaven ("This is my beloved Son, in whom I am well
    pleased"); your own words are used. In the wilderness the Devil tempts him three times, and
    angels minister to him.
  * **The ministry**: he calls twelve disciples (one of them loves silver) and goes from town to
    town, giving the Sermon on the Mount and the parables, and hearers believe. He casts out unclean
    spirits (Legion goes into the swine), heals, calms the storm, turns water into wine at a wedding
    feast, feeds the multitude, walks on the water, and raises the dead: someone who really died is
    restored from the Book of Generations with the same name, kin and face. On a mountain he is
    transfigured, and the prophets who went before appear with him.
  * **The Passion**:
    * He rides into the chief city on a donkey to cries of Hosanna, cleanses the temple and eats
      the Last Supper; the betrayer goes out.
    * He prays in the garden while his disciples sleep, and is betrayed with a kiss.
    * He is tried before the priest and the ruler; a prisoner is released in his place, Peter
      denies him, and the ruler washes his hands.
    * He is crucified between two thieves with his charge written over him, and speaks the seven
      words from the cross; the earth quakes and the veil is torn.
    * He is laid in a tomb sealed with a great stone. Meanwhile he preaches to the spirits in
      prison, and souls are led up out of the Underworld.
  * **Glory**: the stone is rolled away and he rises, shining. The women come, he appears to the
    disciples, and Thomas believes. Faith spreads across the world.
  * **The Ascension and Pentecost**: he ascends into the clouds before his disciples. At Pentecost
    tongues of fire rest on them and they speak in many tongues, the **Church of his name** is
    born, and apostles go out to every living world. Four of them write **the Gospels**, with the
    Acts of the Apostles, into the church's scriptures.
  * **Pacing**: at `sonPace = 1` the whole story takes about forty minutes; `sonMinistrySeconds`
    sets the length of the ministry, and `/fiatlux son next` moves on to the next part.
* **Scripture**: every people writes its own holy books out of what really happened on its world.
  * **When**: once it farms and has a priest, a scholar or an elder to write.
  * **The Book of Beginnings**: "In the beginning Dev created the heavens and Eden…", with chapters
    for the Creation, the War in Heaven, the Garden, Cain and Abel, the Generations, the Watchers
    and the Flood, drawn from the annals of the great acts.
  * **The Book of the Law**: the commandments its rulers gave.
  * **Books of Chronicles**: of its towns, rulers, faiths, spirits and prophets, as the years pass.
  * **The Psalms and the Proverbs**: sung and spoken by its faithful and its elders. False faiths
    keep hymns and sayings instead.
  * **A Book of every prophet**: written by their disciple.
  * **A Testament**: written for every branch, which keeps the books of the faith it came from.

  The writings lie open on a **lectern at each shrine**. Priests read verses aloud, with chapter and
  verse. `/fiatlux scripture` gives you each book as a written book, and `/fiatlux scripture write`
  has the scribes write now.
* **Marriage**: the first man and the first woman of a world are husband and wife from the moment
  they are made ("This is now bone of my bones", Gen 2:23), however they came to be: by Breath of
  Life, the garden, or `/fiatlux eden begin`. After the Fall they bear often (Cain, Abel, Seth, and
  sons and daughters, Gen 5:4) and live long. Other mortals who come to love each other are
  **betrothed**, then **wed at the shrine** of their town by day:
  * the couple go up before the altar, the town gathers in rows behind them, and the leader of their
    faith (or a priest, or the eldest) officiates;
  * the vows follow their faith: "What therefore God hath joined together, let not man put
    asunder" (Mark 10:9) and "Whither thou goest, I will go" (Ruth 1:16) for the true faith, with a
    ring in later ages; an oath under the sun, a handfasting under the trees, a blood oath to the
    fallen, a contract under the law for the school of reason, an oath to the divine emperor;
  * then the bell rings, petals fall and the guests cheer, and everyone goes down to the hearth to
    **feast** (John 2:1-10). The Chronicle records who was wed, where and before whom.
  * `/fiatlux wed <man> <woman>` betroths two unwed mortals near you.
* **Buildings of every age**: each age builds in its own manner, and every house turns its door
  toward the hearth:
  * **hunter-gatherer**: hide tents;
  * **agrarian**: mud-brick houses with flat beamed roofs and a parapet (Deut 22:8);
  * **classical**: plastered villas with red-tiled hipped roofs and a columned porch;
  * **medieval**: timber-framed houses on stone footings, with steep roofs and smoking chimneys;
  * **industrial**: brick terraces;
  * **star-faring**: white halls banded with glass.

  The shrine grows with the people: an altar in the open, then a high place of standing stones
  (Ex 20:25), a temple of columns, a stone church with coloured windows and a bell tower, and
  finally a dome of glass. Builders lay out towns with room for every house. When an age turns,
  they rebuild the temple first and then the houses, one at a time. Mortals open and close doors.
  `/fiatlux build` has the town nearest you built anew for its age at once.
* **Unclean spirits and possession** (Eph 6:12): once angels have fallen, **rifts** from the
  Underworld tear open now and then near the towns of fallen worlds. They come more often by night
  and on sinful worlds. Each is a jagged burning tear standing on the ground, and unclean spirits
  come up through it one by one. Some are nameless spirits of the legions, some are of the legions of
  a lord of the Ars Goetia (Paimon, Asmodeus, Bael and the rest), and rarely one is Legion itself
  (Mark 5:9). The spirits of dead giants also rise by night (1 Enoch 15).
  * **Who they enter**: a spirit looks for someone of weak faith (an idolater, a doubter, or one
    with more sin than virtue) and enters them. Finding no one, it "walketh through dry places,
    seeking rest" (Matt 12:43).
  * **A window into the Underworld**: a rift is a real opening. Look into it and you see the
    Underworld beyond: its ash plains, brimstone and rivers, in true perspective as you move, the
    way the Immersive Portals mod shows the far side of a portal. A twin rift opens in the
    Underworld looking back into the world, and **walking through either one takes you to the other
    side**. A guardian angel standing by seals a rift in about eight seconds, and its twin with it.
    The same portal view will be used for the TARDIS door, so that it is bigger on the inside.
  * **What it does**: the possessed writhe in fits, are shadowed, cry out and cannot pray or work.
    Each spirit does what its lord's office gives it: stealing from the store, sowing quarrels,
    striking out (Acts 19:16), infirmity (Luke 13:11), casting its host into fire and water
    (Mark 9:22), soothsaying (Acts 16:16), dumbness (Mark 9:17) or despair. When you come near, the
    spirit knows you: "What have we to do with thee? ... I know thee who thou art" (Mark 1:24).
  * **False exorcists**: a priest of a false god who tries to cast it out is beaten: "Jesus I know,
    and Paul I know; but who are ye?" (Acts 19:15).
* **"Go forth and watch over the planets"**: say it (or `/fiatlux watch`) and a **guardian angel**
  goes down to every town of every living world (Ps 91:11, Heb 1:14). Towns that no one is near get
  theirs when someone comes.
  * **What the guardian does**: no spirit can enter those close to it, it seals rifts within sight,
    and the holy angels cast the spirits they meet into the deep (Luke 8:31).
  * **The anointed**: it **anoints** up to two of the town's faithful to cast out devils (Mark 16:17).
    They go to the possessed and command the spirit to come out in their Creator's name (Acts 16:18).
  * **Casting out**: the spirit tears its host and goes (Mark 1:26). The lords of the Goetia and
    Legion hold on harder: "This kind goeth not out but by prayer and fasting" (Mark 9:29), so the
    anointed one prays and tries again. Legion may beg to go into the swine and drive the herd into
    the water (Mark 5:13).
  * **The empty house**: a spirit cast out of someone whose faith stays weak may come back with seven
    worse (Matt 12:45).
  * **Your part**: sneak-use a possessed mortal, or say "Come out of him", to cast the spirit out
    yourself. Strike a spirit or a rift to cast it into the deep or seal it.
  * **Commands**: `/fiatlux spirits` gives the tally, `/fiatlux rift` opens a rift near you, and
    `/fiatlux possess <name>` lets a spirit enter someone.
* **Judgment and the souls of the dead**: when a mortal dies, its deeds are weighed. The righteous
  rise to Heaven and the wicked go down to the Underworld. Their **souls** then walk those realms in
  their own names and faces: in Heaven made of light, praising and singing; in the Underworld made
  of ash and embers, labouring and weeping. Souls standing for the unnumbered dead of every world
  join them. Use a soul to hear who it was and where its kin are now. Sneak-use a soul in the
  Underworld to **redeem** it into Heaven. Every death is written in the Chronicle and in the Book
  of Generations, and the widowed mourn.
* **Graves** (Genesis 3:19): the dead are buried in their town's burying place, a levelled yard outside
  the town (westward where the ground allows), clear of its houses, temple and field.
  * **Walls**: each yard is walled as its age walls such places: none for hunter-gatherers, an oak fence,
    a stone wall, a mossy wall, iron railings, then glass. The gate faces the town. A town has up to
    four yards of 24 graves; after that the dead lie unmarked.
  * **Markers**: every grave gets a mound and a marker of its age: a heap of stones, a standing stone,
    a carved slab with its lid, a headstone (topped as a cross for the Son's church), a polished stone
    with flowers, then quartz.
  * **Inscriptions**: once men write, a sign bears the name, the years and an epitaph. Rulers,
    prophets and the first parents get taller, raised tombs.
  * **Funerals**: family and neighbours gather at the grave, bow, and the eldest says the words
    ("Dust thou art...", or for the church "I am the resurrection, and the life").
  * **Lazarus**: when the Son raises someone, their grave is opened and they come out of it.
  * **Where they lie**: the Book of Generations says where each person is buried.
    `/fiatlux graves` shows your nearest town's burying place and who was buried there last.
    Turn graves off with `graves` in the common config.

## Films
Cinematics for the great moments, built with a reusable toolkit (`cinema/Film`, `cinema/Cinema`,
`client/cinema/FilmPlayer`, `client/cinema/FilmOverlay`).
* **Shots**: camera moves between points, cranes, pans and orbits. A shot can follow a creature (the
  Son as he rises) or a world along its orbit, with eased motion and its own field of view and roll.
  A camera that would land inside a hill or a wall is pulled out along its line of sight.
* **Cues**: letterbox bars, fades, a colour grade (the darkness at the sixth hour), title cards,
  corner captions, narration and speech as subtitles, camera shake, flashes, sounds and music that
  fades in and out.
* **The world speaks**: whatever the story narrates or says while a film plays becomes its subtitles.
* **Watching**: the HUD, hand and your own body are hidden and you stand still. The server holds you
  near the scene, unseen and unhurt, and returns you to where you stood when the film ends. Hold
  Jump to skip a film.
* **The films**:
  * **The Beginning**: casting Fiat Lux into the formless Void. Darkness on the face of the deep,
    "And God said," then *Let There Be Light*. The Big Bang waits for that moment, and the camera
    rises toward the forming sun as the stars and galaxies come out.
  * **The forming of a world**: Planetary Genesis in the Void. The camera flies to the new world as it
    swells out of nothing, with its name and a verse for its kind.
  * **Golgotha**: a shot for each of the seven last words, the darkness, the earthquake, the veil of
    the temple and the centurion, ending on *It Is Finished*.
  * **The empty tomb**: at dawn, the earthquake and the stone rolled away, then *He Is Risen*.
  * **The ascension**: he is taken up into the cloud, and the screen turns to white.
* **Config**: `cinematics` and `cinematicRange` in the common config, and `cinematics` in the
  client config so each player can switch films off for themselves.
* **Previews**: `/fiatlux film test` plays a short film anywhere, and in the Void
  `/fiatlux film beginning` and `/fiatlux film genesis` replay those films.
* **The tomb**: the Son's tomb is hewn into a hillside near the city where there is one, with its
  door to the east. `/fiatlux son reset` fills it back in, or takes it down, along with the cross.

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
* **Planet skies**: an earthlike world has **a moon** opposite the sun, waxing and waning through
  its phases with a halo of light ("the lesser light to rule the night", Gen 1:16). A gas giant
  has a train of small moons. In the Void, a moon circles every earthlike world.
* **Interface**:
  * **HUD**: a gilded panel with a halo that fills toward the next tier of Ascension, an essence
    bar that eases and glints, a chip for the play mode, and the power at the ready with its cost.
  * **Power wheel**: a true ring of coloured segments that turns and blooms open. The hovered
    power rises from the ring, and its name, cost and words show in the medallion and on a panel
    beneath.
  * **Cosmos screen**: an ornate page under twinkling stars, with its own tabs (arrow keys switch
    them) and smooth scrolling.
  * **Book of Generations**: a parchment page of family cards. Click anyone to open their family;
    use Back (or Backspace) and "The First Parents" to move around. It opens on the first parents
    with the most descendants. The faiths are kept in the margin.
  * **Speech bubbles**: solid rounded bubbles with the speaker's name and title, a tail, a pop
    when something new is said and a fade with distance. Souls, the serpent and the possessed
    each have their own colours.
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
(§8), the rest of the weapon arsenal (§4), demon entities and a Judgment Seat (§6–§7), the management GUIs (§9), and the Tier 4 colossi (§3).
