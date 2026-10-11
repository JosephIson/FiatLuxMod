package com.josephison.fiatlux.gametest;

import com.josephison.fiatlux.weapon.DivineWeaponRegistry;
import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.creation.EditQueue;
import com.josephison.fiatlux.entity.HeavenlyBeing;
import com.josephison.fiatlux.registry.ModBlocks;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.registry.ModItems;
import com.josephison.fiatlux.registry.ModTags;
import com.josephison.fiatlux.power.powers.CosmicSparkPower;
import com.josephison.fiatlux.power.powers.PlanetaryGenesisPower;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.FiatLuxDimensions;
import com.josephison.fiatlux.world.WorldTicker;
import com.josephison.fiatlux.world.cosmos.Planet;
import com.josephison.fiatlux.world.cosmos.PlanetType;
import com.josephison.fiatlux.world.cosmos.UnderworldState;
import com.josephison.fiatlux.world.HeavenChunkGenerator;
import com.josephison.fiatlux.world.cosmos.UnderworldEra;
import com.josephison.fiatlux.realm.PlanetChunkGenerator;
import com.josephison.fiatlux.realm.RealmPool;
import com.josephison.fiatlux.realm.UnderworldChunkGenerator;
import com.josephison.fiatlux.realm.UnderworldSettlements;
import net.minecraft.world.level.block.Block;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** In-game tests, run headless by `./gradlew runGameTestServer`. */
@GameTestHolder(FiatLux.MODID)
@PrefixGameTestTemplate(false)
public final class FiatLuxGameTests {
    private FiatLuxGameTests() {}

    /**
     * Heaven's dimension, dimension type, biome and generator decode through Minecraft's own codecs,
     * and the crossing of golden streets lies at the origin (Rev 21:21). The GameTest server loads
     * no datapack dimensions (it bakes an empty LevelStem registry), so when Heaven is not live we
     * decode data/fiatlux/dimension/heaven.json ourselves.
     */
    @GameTest(template = "empty")
    public static void heavenHasGoldenStreets(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel heaven = server.getLevel(FiatLuxDimensions.HEAVEN);
        ChunkGenerator generator;
        if (heaven != null) {
            generator = heaven.getChunkSource().getGenerator();
        } else {
            try (var reader = server.getResourceManager().getResourceOrThrow(FiatLux.id("dimension/heaven.json")).openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
                LevelStem stem = LevelStem.CODEC.parse(ops, json).getOrThrow(msg -> new IllegalStateException("heaven.json: " + msg));
                helper.assertTrue(stem.type().is(FiatLuxDimensions.HEAVEN_TYPE), "Heaven has the wrong dimension type");
                generator = stem.generator();
            } catch (java.io.IOException e) {
                throw new IllegalStateException("cannot read heaven.json", e);
            }
        }
        helper.assertTrue(generator instanceof HeavenChunkGenerator, "Heaven does not use the Heaven generator: " + generator);
        HeavenChunkGenerator gen = (HeavenChunkGenerator) generator;
        int y = gen.streetLevel();
        BlockState street = gen.stateAt(2, y, 0);
        BlockState kerb = gen.stateAt(-3, y, 10);
        BlockState lamp = gen.stateAt(-3, y + 4, 15);
        helper.assertTrue(street != null && street.is(ModBlocks.GOLDEN_STREET.get()), "expected golden street at (2," + y + ",0) but found " + street);
        helper.assertTrue(kerb != null && kerb.is(ModBlocks.JASPER.get()), "expected a jasper kerb at (-3," + y + ",10) but found " + kerb);
        helper.assertTrue(lamp != null && lamp.is(ModBlocks.LUX_BLOCK.get()), "expected a lamp of Lux at (-3," + (y + 4) + ",15) but found " + lamp);
        helper.assertTrue(gen.stateAt(24, y + 30, 24) == null, "the sky above Heaven should be open");
        helper.succeed();
    }

    /** Decodes one of the mod's datapack dimensions with Minecraft's own codecs and returns its generator. */
    private static ChunkGenerator decodeGenerator(MinecraftServer server, String dimension) {
        try (var reader = server.getResourceManager().getResourceOrThrow(FiatLux.id("dimension/" + dimension + ".json")).openAsReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            return LevelStem.CODEC.parse(ops, json).getOrThrow(msg -> new IllegalStateException(dimension + ".json: " + msg)).generator();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + dimension + ".json", e);
        }
    }

    /** Topmost generated block in each column of a square, as a set of blocks. */
    private static java.util.Set<Block> surfaces(java.util.function.IntFunction<java.util.function.IntFunction<java.util.function.IntFunction<BlockState>>> at,
                                                 int from, int to, int step, int top) {
        java.util.Set<Block> out = new java.util.HashSet<>();
        for (int x = from; x <= to; x += step) {
            for (int z = from; z <= to; z += step) {
                for (int y = top; y > -64; y--) {
                    BlockState b = at.apply(x).apply(y).apply(z);
                    if (b != null && !b.isAir()) {
                        out.add(b.getBlock());
                        break;
                    }
                }
            }
        }
        return out;
    }

    /**
     * Every created world is an enterable dimension: planet_0.json decodes to the planet generator,
     * and binding its slot to each world type yields that type's ground - meadows and seas, basalt and
     * lava, regolith, banded cloud decks, or calcite with crystal spires. An unbound slot is void.
     */
    @GameTest(template = "empty")
    public static void createdWorldsAreEnterable(GameTestHelper helper) {
        ChunkGenerator generator = decodeGenerator(helper.getLevel().getServer(), "planet_0");
        helper.assertTrue(generator instanceof PlanetChunkGenerator, "planet_0 does not use the planet generator: " + generator);
        PlanetChunkGenerator gen = (PlanetChunkGenerator) generator;
        RealmPool.Settings saved = RealmPool.get(gen.slot());
        try {
            RealmPool.bind(gen.slot(), null);
            helper.assertTrue(gen.stateAt(0, 64, 0) == null && gen.stateAt(0, -64, 0) == null, "an unformed world should be void");
            java.util.Map<PlanetType, List<Block>> expect = java.util.Map.of(
                    PlanetType.EARTHLIKE, List.of(Blocks.GRASS_BLOCK, Blocks.WATER),
                    PlanetType.VOLCANIC, List.of(Blocks.BASALT, Blocks.LAVA),
                    PlanetType.BARREN_MOON, List.of(ModBlocks.REGOLITH.get()),
                    PlanetType.GAS_GIANT, List.of(ModBlocks.CLOUD_BAND_AMBER.get(), ModBlocks.CLOUD_BAND_CREAM.get(), ModBlocks.CLOUD_BAND_RUST.get()),
                    PlanetType.CRYSTALLINE, List.of(Blocks.CALCITE, ModBlocks.CELESTIAL_CRYSTAL.get()));
            for (PlanetType type : PlanetType.values()) {
                RealmPool.bind(gen.slot(), new RealmPool.Settings("Test" + type, type, 1234L + type.ordinal(), type.gravity));
                var found = surfaces(x -> y -> z -> gen.stateAt(x, y, z), -400, 400, 8, 200);
                for (Block b : expect.get(type)) {
                    helper.assertTrue(found.contains(b), type + " world has no " + b + " at its surface; found " + found);
                }
                helper.assertTrue(gen.stateAt(0, -63, 0) != null || type == PlanetType.GAS_GIANT, type + " world has no floor");
            }
        } finally {
            RealmPool.bind(gen.slot(), saved);
        }
        helper.succeed();
    }

    /**
     * The Underworld decodes to its own generator: sulfur plains, obsidian mountains and abyssal
     * rivers, and its settlements take the era of the Underworld when first generated - bone totems
     * in the Tribal/Pits age, bastions in the Feudal age, soul-engine foundries, then neon towers.
     */
    @GameTest(template = "empty")
    public static void underworldHasItsEras(GameTestHelper helper) {
        ChunkGenerator generator = decodeGenerator(helper.getLevel().getServer(), "underworld");
        helper.assertTrue(generator instanceof UnderworldChunkGenerator, "the Underworld does not use its generator: " + generator);
        UnderworldChunkGenerator gen = (UnderworldChunkGenerator) generator;
        var terrain = surfaces(x -> y -> z -> gen.terrainAt(x, y, z), -1500, 1500, 12, 200);
        for (Block b : List.of(ModBlocks.SULFUR.get(), Blocks.OBSIDIAN, Blocks.WATER)) {
            helper.assertTrue(terrain.contains(b), "Underworld terrain lacks " + b + "; found " + terrain);
        }
        java.util.Map<UnderworldEra, Block> landmark = java.util.Map.of(
                UnderworldEra.TRIBAL_PITS, Blocks.BONE_BLOCK,
                UnderworldEra.FEUDAL_CASTLE, Blocks.POLISHED_BLACKSTONE_BRICKS,
                UnderworldEra.INDUSTRIAL, ModBlocks.SOUL_ENGINE.get(),
                UnderworldEra.MODERN_CYBERNETIC, ModBlocks.NEON_CRIMSON.get());
        UnderworldEra savedEra = UnderworldSettlements.currentEra;
        try {
            int cell = 50_000;   // far from anything a player explored, and forgotten afterwards
            for (UnderworldEra era : UnderworldEra.values()) {
                UnderworldSettlements.currentEra = era;
                UnderworldSettlements.Settlement s = null;
                int cx = cell;
                while (s == null) s = UnderworldSettlements.settlementIn(++cx, cell, (x, z) -> 64);
                helper.assertTrue(s.era() == era, "a new settlement took era " + s.era() + " instead of " + era);
                boolean seen = false;
                for (int dx = -45; dx <= 45 && !seen; dx++)
                    for (int dz = -45; dz <= 45 && !seen; dz++)
                        for (int dy = 0; dy <= 30 && !seen; dy++) {
                            BlockState b = UnderworldSettlements.blockAt(s, s.cx() + dx, s.baseY() + dy, s.cz() + dz);
                            seen = b != null && b.is(landmark.get(era));
                        }
                helper.assertTrue(seen, era + " settlement has no " + landmark.get(era));
                UnderworldSettlements.currentEra = UnderworldEra.MODERN_CYBERNETIC;
                helper.assertTrue(UnderworldSettlements.settlementIn(cx, cell, (x, z) -> 64).era() == era, "a settlement forgot its era");
                UnderworldSettlements.CELL_ERAS.remove(UnderworldSettlements.cellKey(cx, cell));
                cell += 10;
            }
        } finally {
            UnderworldSettlements.currentEra = savedEra;
        }
        helper.succeed();
    }

    /**
     * The Void lies beyond Heaven: its dimension decodes to an empty, formless expanse; Heaven's
     * gates of pearl hold the veil that leads there; and the bodies of the created worlds sit on
     * their orbits around the greater light, so flying into one is entering it.
     */
    @GameTest(template = "empty")
    public static void theVoidLiesBeyondHeaven(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ChunkGenerator voidGen = decodeGenerator(server, "the_void");
        helper.assertTrue(voidGen instanceof net.minecraft.world.level.levelgen.FlatLevelSource flat && flat.settings().getLayers().isEmpty(),
                "the Void should be without form: " + voidGen);
        HeavenChunkGenerator heaven = (HeavenChunkGenerator) decodeGenerator(server, "heaven");
        int y = heaven.streetLevel() + 3, d = HeavenChunkGenerator.GATE_DISTANCE;
        for (int[] g : new int[][]{{0, d}, {0, -d}, {d, 0}, {-d, 0}}) {
            BlockState veil = heaven.stateAt(g[0], y, g[1]);
            helper.assertTrue(veil != null && veil.is(ModBlocks.VOID_GATE.get()), "no veil in the gate at " + g[0] + "," + g[1] + ": " + veil);
        }
        BlockState pillar = heaven.stateAt(4, y, d);
        helper.assertTrue(pillar != null && pillar.is(ModBlocks.PEARL.get()), "the gate has no pillar of pearl: " + pillar);
        helper.assertTrue(heaven.stateAt(0, heaven.streetLevel(), 0).is(ModBlocks.GOLDEN_STREET.get()), "the gates cut the golden street");

        var gate = com.josephison.fiatlux.world.VoidSpace.GATE;
        helper.assertTrue(com.josephison.fiatlux.world.VoidSpace.inGate(gate), "the gate's own centre is not in its veil");
        helper.assertTrue(!com.josephison.fiatlux.world.VoidSpace.inGate(com.josephison.fiatlux.world.VoidSpace.arrival()), "arrivals land inside the gate");
        Planet world = new Planet(UUID.randomUUID(), "VoidTest", PlanetType.GAS_GIANT, 0, 1.5, 4.0, 0.25, 5.0, 2.4, 10000, 0xFFAA88, 0, 0, 0,
                com.josephison.fiatlux.world.cosmos.MortalEra.HUNTER_GATHERER, 0);
        var at = com.josephison.fiatlux.world.VoidSpace.planetPos(world);
        helper.assertTrue(Math.abs(at.distanceTo(com.josephison.fiatlux.world.VoidSpace.SUN) - 1.5 * com.josephison.fiatlux.world.VoidSpace.AU) < 1.0,
                "the world is not on its orbit: " + at);
        List<Planet> worlds = List.of(world);
        helper.assertTrue(com.josephison.fiatlux.world.VoidSpace.planetAt(worlds, at, 0) == 0, "flying into the world's body does not reach it");
        helper.assertTrue(com.josephison.fiatlux.world.VoidSpace.planetAt(worlds, gate, 0) == -1, "Heaven's Gate is inside a world");
        var out = com.josephison.fiatlux.world.VoidSpace.leavingWorld(world);
        helper.assertTrue(com.josephison.fiatlux.world.VoidSpace.planetAt(worlds, out, 1.5) == -1, "one leaving a world lands back inside it");
        helper.succeed();
    }

    /**
     * A black hole draws things toward it, harder the nearer they are, not at all beyond its reach,
     * and whatever crosses the horizon is past saving.
     */
    @GameTest(template = "empty")
    public static void bottomlessPitsPull(GameTestHelper helper) {
        var c = com.josephison.fiatlux.world.VoidSpace.ABYSS;
        double r = com.josephison.fiatlux.world.VoidSpace.ABYSS_RADIUS;
        var near = c.add(r * 3, 0, 0);
        var far = c.add(r * 10, 0, 0);
        var beyond = c.add(r * 20, 0, 0);
        var pNear = com.josephison.fiatlux.world.VoidSpace.pull(near, c, r);
        var pFar = com.josephison.fiatlux.world.VoidSpace.pull(far, c, r);
        helper.assertTrue(pNear.x < 0 && Math.abs(pNear.y) < 1e-9, "the pull does not point at the pit: " + pNear);
        helper.assertTrue(pNear.length() > pFar.length() * 5, "the pull does not grow nearer the pit: " + pNear.length() + " vs " + pFar.length());
        helper.assertTrue(com.josephison.fiatlux.world.VoidSpace.pull(beyond, c, r).lengthSqr() == 0, "the pit pulls beyond its reach");
        helper.assertTrue(com.josephison.fiatlux.world.VoidSpace.beyondHorizon(c.add(r * 0.5, 0, 0), c, r), "inside the horizon is not beyond it");
        helper.assertTrue(!com.josephison.fiatlux.world.VoidSpace.beyondHorizon(near, c, r), "outside the horizon counts as beyond it");
        helper.assertTrue(c.distanceTo(com.josephison.fiatlux.world.VoidSpace.GATE) > r * com.josephison.fiatlux.world.VoidSpace.PULL_REACH,
                "Heaven's Gate lies within the Abyss's pull");
        helper.succeed();
    }

    /**
     * The breath of life and the life that follows: mortals rise from the dust with names, temperaments
     * and homes, are written in the Book of Generations, bear children whose parents are recorded,
     * speak, pray and rise again, may found a faith of their own, and when they die are judged.
     */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void mortalsComeAlive(GameTestHelper helper) {
        var level = helper.getLevel();
        var gens = com.josephison.fiatlux.civilization.Generations.get(level.getServer());
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
        var era = com.josephison.fiatlux.world.cosmos.MortalEra.CLASSICAL;
        var dad = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, at, at, era, null, true, 30, null, null, null);
        var mum = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, at.east(), at, era, null, false, 28, null, null, null);
        helper.assertTrue(dad != null && mum != null && dad.isMale() && !mum.isMale(), "the first pair did not rise from the dust");
        helper.assertTrue(dad.hasCustomName() && dad.era() == era && at.equals(dad.home()), "a mortal has no name, age or home");
        helper.assertTrue(gens.person(dad.getUUID()) != null, "the mortal is not written in the Book of Generations");
        var child = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, at, at, era, null, null, 0, dad, mum, null);
        helper.assertTrue(child != null && child.isChild(), "the newborn is not a child");
        var record = gens.person(child.getUUID());
        helper.assertTrue(record != null && dad.getUUID().equals(record.father) && mum.getUUID().equals(record.mother), "the child's parents are not recorded");
        helper.assertTrue(gens.childrenOf(dad.getUUID()).stream().anyMatch(p -> p.id.equals(child.getUUID())), "the family tree does not reach the child");
        helper.assertTrue(!ModTags.isCorrupt(dad), "a newborn soul is counted among the corrupt");
        dad.say(com.josephison.fiatlux.civilization.MortalSpeech.Topic.CHAT);
        helper.assertTrue(!dad.speech().isEmpty(), "the mortal said nothing");
        var faith = com.josephison.fiatlux.civilization.Faiths.schism(level, mum, "TestWorld");
        helper.assertTrue(faith.deity != com.josephison.fiatlux.civilization.Religion.Deity.CREATOR && faith.id.equals(mum.religion())
                && gens.religion(faith.id) != null, "the doubter founded no faith");
        mum.beginAct(com.josephison.fiatlux.entity.MortalEntity.PRAY, 10);
        helper.assertTrue(mum.isPraying(), "the mortal would not kneel to pray");
        helper.assertTrue(com.josephison.fiatlux.civilization.MortalPopulation.embodied(0) == 0
                && com.josephison.fiatlux.civilization.MortalPopulation.embodied(1_000_000) > 10, "the settlement does not follow the population");
        helper.assertTrue(!com.josephison.fiatlux.civilization.MortalPopulation.fauna(PlanetType.EARTHLIKE).isEmpty()
                && com.josephison.fiatlux.civilization.MortalPopulation.fauna(PlanetType.GAS_GIANT).isEmpty(), "creatures were brought forth where none could live");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!mum.isPraying(), "the prayer never ended");
            dad.hurt(level.damageSources().generic(), Float.MAX_VALUE);
            var dead = gens.person(dad.getUUID());
            helper.assertTrue(dead != null && !dead.living() && dead.soul.equals("heaven"), "the righteous dead were not judged: " + (dead == null ? "none" : dead.soul));
            mum.discard();
            child.discard();
            gens.religions.remove(faith.id);
            helper.succeed();
        });
    }

    /** The creation queue applies a batched edit over ticks, then undoes it exactly. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void creationQueueAppliesAndUndoes(GameTestHelper helper) {
        UUID creator = UUID.randomUUID();
        List<BlockPos> cells = List.of(new BlockPos(1, 1, 1), new BlockPos(2, 1, 1), new BlockPos(3, 1, 1));
        List<EditQueue.Change> changes = cells.stream()
                .map(p -> new EditQueue.Change(helper.absolutePos(p), s -> ModBlocks.LUX_BLOCK.get().defaultBlockState())).toList();
        EditQueue.submit(creator, helper.getLevel(), changes.iterator(), true, () -> {
        });
        helper.runAfterDelay(5, () -> {
            cells.forEach(p -> helper.assertBlockPresent(ModBlocks.LUX_BLOCK.get(), p));
            helper.assertTrue(EditQueue.undo(creator, helper.getLevel().getServer()), "undo history was empty");
            helper.runAfterDelay(5, () -> {
                cells.forEach(p -> helper.assertBlockPresent(Blocks.AIR, p));
                helper.succeed();
            });
        });
    }

    /** All four orders of the Host spawn, fly, and carry their GeckoLib animators. */
    @GameTest(template = "empty")
    public static void heavenlyHostSpawns(GameTestHelper helper) {
        List<EntityType<? extends HeavenlyBeing>> host = List.of(ModEntities.ANGEL.get(), ModEntities.SERAPH.get(),
                ModEntities.CHERUB.get(), ModEntities.OPHAN.get(), ModEntities.DOMINION.get(), ModEntities.VIRTUE.get(),
                ModEntities.POWER.get(), ModEntities.PRINCIPALITY.get(), ModEntities.ARCHANGEL.get());
        java.util.Set<com.josephison.fiatlux.entity.Choir> choirs = java.util.EnumSet.noneOf(com.josephison.fiatlux.entity.Choir.class);
        for (EntityType<? extends HeavenlyBeing> type : host) {
            HeavenlyBeing being = helper.spawn(type, new BlockPos(2, 2, 2));
            helper.assertTrue(being.isAlive(), type + " did not survive spawning");
            helper.assertTrue(being.isNoGravity(), type + " should fly");
            helper.assertTrue(being.getType().is(ModTags.HEAVENLY_HOST), type + " is missing from #fiatlux:heavenly_host");
            helper.assertTrue(being.getAnimatableInstanceCache() != null, type + " has no GeckoLib cache");
            helper.assertTrue(choirs.add(being.choir()), type + " shares its choir with another order");
            being.discard();
        }
        helper.assertTrue(choirs.size() == 9, "the hierarchy is incomplete: " + choirs);
        helper.succeed();
    }

    /** A Virtue's office is the miracle of healing: a wounded angel beside it is restored. */
    @GameTest(template = "empty", timeoutTicks = 260)
    public static void virtuesHeal(GameTestHelper helper) {
        HeavenlyBeing wounded = helper.spawn(ModEntities.ANGEL.get(), new BlockPos(2, 2, 2));
        wounded.setAngelState(new com.josephison.fiatlux.divinity.AngelState(0f, 1f));
        wounded.setHealth(10f);
        HeavenlyBeing virtue = helper.spawn(ModEntities.VIRTUE.get(), new BlockPos(4, 2, 2));
        virtue.setAngelState(new com.josephison.fiatlux.divinity.AngelState(0f, 1f));
        helper.succeedWhen(() -> {
            helper.assertTrue(wounded.getHealth() > 14f, "the Virtue did not heal the wounded angel: " + wounded.getHealth());
            wounded.discard();
            virtue.discard();
        });
    }

    /**
     * The self-sustaining simulation: after the Big Bang a world is formed, receives life, and - with no
     * further intervention - its mortals multiply, pray (raising world Glory), advance an age, and the
     * planet moves along its orbit. The Underworld, fed sinner souls, crosses a technological revolution.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void creationSustainsItself(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        CosmicSparkPower.ignite(server, "GameTest");
        helper.assertTrue(cosmos.hasBigBang() && !cosmos.galaxies.isEmpty(), "the Big Bang formed no galaxies");
        helper.assertTrue(!cosmos.blackHoles.isEmpty(), "the bottomless pit did not open with the first light");
        Planet eden = PlanetaryGenesisPower.create(server, "GameTest", PlanetType.EARTHLIKE, "GameTestEden");
        helper.assertTrue(eden != null && cosmos.breatheLife(eden), "could not breathe life on an Earth-like world");
        double phase0 = eden.phase;
        long glory0 = CosmosData.get(server).glory();
        cosmos.underworld = new UnderworldState();   // the GameTest world persists between runs
        cosmos.underworld.souls = 50_000;
        boolean fallen = cosmos.fallOfMan;
        double heaven0 = cosmos.heavenSouls;
        cosmos.fallOfMan = true;   // peoples multiply only after the Fall of Man (Gen 4:1)
        for (int i = 0; i < 1000; i++) WorldTicker.step(server, 60.0);   // 1000 simulated minutes: some five hundred mortal years
        helper.assertTrue(eden.population > 1000 && eden.population < eden.era.capacity * 2, "population did not grow believably: " + eden.population);
        double people = cosmos.planets.stream().mapToDouble(p -> p.population).sum();
        helper.assertTrue(cosmos.heavenSouls - heaven0 < people * 20 + 1000 && cosmos.underworld.knowledge < 1e10,
                "the numbers ran away: heaven +" + (cosmos.heavenSouls - heaven0) + " for " + people + " people"
                + ", knowledge " + cosmos.underworld.knowledge);
        helper.assertTrue(eden.era.ordinal() > 0, "the mortals of Eden never left the hunter-gatherer age");
        helper.assertTrue(CosmosData.get(server).glory() > glory0, "prayer added no Glory");
        helper.assertTrue(eden.phase != phase0, "the planet did not move along its orbit");
        helper.assertTrue(cosmos.underworld.era.ordinal() > 0, "the Underworld never left the Tribal/Pits era: knowledge " + cosmos.underworld.knowledge);
        cosmos.planets.remove(eden);
        cosmos.fallOfMan = fallen;
        helper.succeed();
    }

    /**
     * The dead walk their realms: a righteous soul is brought forth in its own name and face, a soul in
     * the Underworld can be redeemed into Heaven, and souls cannot die again.
     */
    @GameTest(template = "empty")
    public static void soulsWalkTheirRealms(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        var gens = com.josephison.fiatlux.civilization.Generations.get(server);
        var abel = new com.josephison.fiatlux.civilization.Person(java.util.UUID.randomUUID(), "Abel", true, "GameTestEden", 0, null, null);
        abel.died = 100;
        abel.soul = "underworld";
        abel.profession = "shepherd";
        abel.variant = 3;
        gens.add(abel);
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
        var soul = com.josephison.fiatlux.civilization.SoulRealms.spawn(helper.getLevel(), abel, abel.name, true, abel.world, false, at, true);
        helper.assertTrue(soul != null && abel.embodied && soul.variant() == 3 && "Abel".equals(soul.getName().getString()),
                "the soul was not brought forth as the person it was");
        soul.hurt(helper.getLevel().damageSources().generic(), 1000f);
        helper.assertTrue(soul.isAlive(), "a soul died a second death");
        com.josephison.fiatlux.civilization.SoulRealms.redeem(server, "GameTest", soul);
        helper.assertTrue(soul.isRemoved() && "heaven".equals(abel.soul) && abel.redeemed, "the soul was not redeemed into Heaven");
        gens.people.remove(abel.id);
        helper.succeed();
    }

    /**
     * Genesis 2-3 played through: the garden is planted with its two trees; the serpent comes once Satan
     * has fallen; Eve takes of the fruit and gives to Adam; their eyes are opened and mankind falls;
     * judged, they are clothed and sent out, and cherubim and a flaming sword keep the way.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void edenAndTheFall(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        var saved = new Object[]{cosmos.eden, cosmos.fallOfMan, cosmos.fallenAngels, cosmos.edenDim, cosmos.edenCentre, cosmos.edenWorld};
        cosmos.fallOfMan = false;
        cosmos.fallenAngels = Math.max(1, cosmos.fallenAngels);
        BlockPos centre = helper.absolutePos(new BlockPos(1, 2, 1)).offset(400, 0, 400);
        helper.getLevel().getChunk(centre);
        com.josephison.fiatlux.eden.EdenGarden.plant(helper.getLevel(), centre, null);
        helper.assertTrue(cosmos.eden == com.josephison.fiatlux.eden.EdenState.INNOCENCE, "the garden was not planted in innocence");
        int fruit0 = countFruit(helper, cosmos.knowledgeTree);
        helper.assertTrue(fruit0 > 0 && helper.getLevel().getBlockState(centre.below()).is(net.minecraft.world.level.block.Blocks.WATER),
                "the tree of knowledge bore no fruit, or no river went out of Eden");
        var adam = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(helper.getLevel(), centre.offset(4, 0, 0), centre,
                com.josephison.fiatlux.world.cosmos.MortalEra.HUNTER_GATHERER, "Adam", true, 24, null, null, null);
        var eve = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(helper.getLevel(), centre.offset(4, 0, 2), centre,
                com.josephison.fiatlux.world.cosmos.MortalEra.HUNTER_GATHERER, "Eve", false, 22, null, null, null);
        helper.assertTrue(adam != null && eve != null, "the first pair were not formed");
        for (int i = 0; i < 40 && cosmos.eden != com.josephison.fiatlux.eden.EdenState.FALLEN; i++) com.josephison.fiatlux.eden.EdenEvents.tick(server, true);
        helper.assertTrue(cosmos.eden == com.josephison.fiatlux.eden.EdenState.FALLEN && cosmos.fallOfMan, "they did not eat: " + cosmos.eden + " step " + cosmos.edenStep);
        helper.assertTrue(countFruit(helper, cosmos.knowledgeTree) == fruit0 - 1, "no fruit was taken from the tree of knowledge");
        for (int i = 0; i < 40 && cosmos.eden != com.josephison.fiatlux.eden.EdenState.EXILED; i++) com.josephison.fiatlux.eden.EdenEvents.tick(server, true);
        helper.assertTrue(cosmos.eden == com.josephison.fiatlux.eden.EdenState.EXILED, "they were never sent out: " + cosmos.eden);
        BlockPos gate = com.josephison.fiatlux.eden.EdenGarden.eastGate(centre);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(com.josephison.fiatlux.entity.CherubEntity.class, new net.minecraft.world.phys.AABB(gate).inflate(10)).isEmpty()
                && !helper.getLevel().getEntitiesOfClass(com.josephison.fiatlux.entity.StationedSwordEntity.class, new net.minecraft.world.phys.AABB(gate).inflate(10)).isEmpty(),
                "no cherubim and flaming sword keep the way");
        helper.assertTrue(adam.isLeaving() && eve.isLeaving(), "Adam and Eve did not go out east of Eden");
        // put the cosmos back as it was
        for (var e : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(centre).inflate(80),
                e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
        cosmos.eden = (com.josephison.fiatlux.eden.EdenState) saved[0];
        cosmos.fallOfMan = (boolean) saved[1];
        cosmos.fallenAngels = (double) saved[2];
        cosmos.edenDim = (String) saved[3];
        cosmos.edenCentre = (BlockPos) saved[4];
        cosmos.edenWorld = (java.util.UUID) saved[5];
        helper.succeed();
    }

    private static int countFruit(GameTestHelper helper, BlockPos tree) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(tree.offset(-6, 4, -6), tree.offset(7, 14, 7))) {
            if (helper.getLevel().getBlockState(p).is(com.josephison.fiatlux.registry.ModBlocks.KNOWLEDGE_FRUIT.get())) n++;
        }
        return n;
    }

    /**
     * Peoples govern themselves and keep their laws: a classical people with many towns forms an
     * empire with an emperor and laws; a seen crime is judged by the age's punishment - under an
     * empire, sedition is punished by the cross - and a Creator can deliver the condemned.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void governmentsAndTheirLaws(GameTestHelper helper) {
        helper.assertTrue(com.josephison.fiatlux.civilization.Polity.Form.of(com.josephison.fiatlux.world.cosmos.MortalEra.CLASSICAL, 5, 0.5, true)
                == com.josephison.fiatlux.civilization.Polity.Form.EMPIRE, "five towns of the classical age did not make an empire");
        helper.assertTrue(com.josephison.fiatlux.civilization.Justice.punishment(com.josephison.fiatlux.civilization.Polity.Form.EMPIRE,
                com.josephison.fiatlux.civilization.Polity.Crime.SEDITION) == com.josephison.fiatlux.civilization.Justice.Sentence.CROSS,
                "an empire did not crucify sedition");
        helper.assertTrue(com.josephison.fiatlux.civilization.Justice.punishment(com.josephison.fiatlux.civilization.Polity.Form.CLAN,
                com.josephison.fiatlux.civilization.Polity.Crime.MURDER) == com.josephison.fiatlux.civilization.Justice.Sentence.EXILE,
                "a clan did not drive out a murderer");
        MinecraftServer server = helper.getLevel().getServer();
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        Planet world = PlanetaryGenesisPower.create(server, "GameTest", PlanetType.EARTHLIKE, "GameTestRome");
        helper.assertTrue(world != null, "no world");
        BlockPos centre = helper.absolutePos(new BlockPos(1, 2, 1));
        var town = new com.josephison.fiatlux.civilization.Settlement(centre);
        town.name = "GameTestRome";
        cosmos.addTown(world.id, town);
        var g = new com.josephison.fiatlux.civilization.Polity(java.util.UUID.randomUUID(), world.id, "Empire of GameTestRome",
                com.josephison.fiatlux.civilization.Polity.Form.EMPIRE, "GameTestRome", 0);
        g.laws.add(com.josephison.fiatlux.civilization.Polity.Law.SEDITION);
        com.josephison.fiatlux.civilization.Polities.get(server).byWorld.put(world.id, g);
        var rebel = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(helper.getLevel(), centre.offset(2, 0, 0), centre,
                com.josephison.fiatlux.world.cosmos.MortalEra.CLASSICAL, "Barabbas", true, 30, null, null, null);
        helper.assertTrue(rebel != null, "no rebel");
        com.josephison.fiatlux.civilization.Justice.judge(helper.getLevel(), g, world, rebel, com.josephison.fiatlux.civilization.Polity.Crime.SEDITION);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(rebel.sentence() == com.josephison.fiatlux.civilization.Justice.Sentence.CROSS && rebel.act() == com.josephison.fiatlux.entity.MortalEntity.CRUCIFIED,
                    "the rebel was not crucified: " + rebel.sentence());
            var crosses = helper.getLevel().getEntitiesOfClass(com.josephison.fiatlux.entity.PunishmentPropEntity.class, rebel.getBoundingBox().inflate(2, 4, 2));
            helper.assertTrue(!crosses.isEmpty() && crosses.getFirst().inscription().equals("BARABBAS\nREBEL"),
                    "no cross bears the charge: " + (crosses.isEmpty() ? "no cross" : crosses.getFirst().inscription()));
            crosses.forEach(net.minecraft.world.entity.Entity::discard);
            rebel.release();
            rebel.setNoGravity(false);
            rebel.discard();
            com.josephison.fiatlux.civilization.Polities.get(server).byWorld.remove(world.id);
            cosmos.towns.remove(world.id);
            cosmos.planets.remove(world);
            helper.succeed();
        });
    }

    /** Every /fiatlux report runs without error on a living cosmos. */
    @GameTest(template = "empty")
    public static void reportsRun(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        java.util.List<String> out = new java.util.ArrayList<>();
        java.util.List<String> errors = new java.util.ArrayList<>();
        net.minecraft.commands.CommandSource sink = new net.minecraft.commands.CommandSource() {
            @Override public void sendSystemMessage(net.minecraft.network.chat.Component c) { out.add(c.getString()); }
            @Override public boolean acceptsSuccess() { return true; }
            @Override public boolean acceptsFailure() { return true; }
            @Override public boolean shouldInformAdmins() { return false; }
        };
        var src = new net.minecraft.commands.CommandSourceStack(sink, net.minecraft.world.phys.Vec3.ZERO, net.minecraft.world.phys.Vec2.ZERO,
                helper.getLevel(), 4, "GameTest", net.minecraft.network.chat.Component.literal("GameTest"), server, null)
                .withCallback((ok, result) -> { if (!ok) errors.add("failed"); });
        for (String cmd : new String[]{"fiatlux cosmos", "fiatlux host", "fiatlux faiths", "fiatlux chronicle 5"}) {
            out.clear();
            server.getCommands().performPrefixedCommand(src, cmd);
            FiatLux.LOGGER.info("[reportsRun] {} -> {}", cmd, out);
            helper.assertTrue(out.stream().noneMatch(l -> l.toLowerCase().contains("unexpected error") || l.contains("Unknown")),
                    cmd + " failed: " + out);
        }
        helper.succeed();
    }

    /** The Angelic Rebellion resolves into the Fall: the cast-out swell the Underworld. */
    @GameTest(template = "empty")
    public static void rebellionEndsInTheFall(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        cosmos.underworld = new UnderworldState();   // the GameTest world persists between runs
        double demons0 = cosmos.underworld.demons;
        double fallen0 = cosmos.fallenAngels;
        // isolate the Fall from a scheduled divine cleanse landing on the same step
        cosmos.underworld.lastCleanseDay = server.overworld().getDayTime() / 24000L;
        WorldTicker.startRebellion(server, CosmosData.get(server), cosmos);
        helper.assertTrue(cosmos.rebellionActive, "war in heaven did not begin");
        helper.assertTrue(!cosmos.rebelIds.isEmpty(), "no rebel host gathered when war broke out");
        WorldTicker.step(server, 1300.0);   // more than one simulated day of war
        helper.assertTrue(!cosmos.rebellionActive, "the rebellion never resolved");
        helper.assertTrue(cosmos.fallenAngels >= fallen0 + 100, "no angels fell: " + (cosmos.fallenAngels - fallen0));
        helper.assertTrue(cosmos.underworld.demons >= demons0 + 100, "the fallen did not reach the Underworld: demons " + demons0 + " -> " + cosmos.underworld.demons + ", cap " + com.josephison.fiatlux.config.FiatLuxConfig.COMMON.maxInfernalPopulation.get());
        helper.assertTrue(cosmos.rebelIds.isEmpty(), "rebels remain after the Fall");
        var rebels = com.josephison.fiatlux.world.RebellionWar.loadedRebels(server);
        helper.assertTrue(rebels.isEmpty(), rebels.size() + " embodied rebels were never cast out");
        // the war was mustered at world spawn, far from the test plots: clear the field for the next run
        BlockPos here = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(net.minecraft.world.entity.LivingEntity.class),
                        e -> (e instanceof HeavenlyBeing || e instanceof com.josephison.fiatlux.entity.FallenEntity) && e.distanceToSqr(here.getCenter()) > 200 * 200)
                .forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    /**
     * A rebel angel cannot die, only fall (Luke 10:18): a killing blow leaves it alive and being cast
     * out; lightning strikes, it is lifted up, and it is replaced by a fallen one - scorched, bat-winged,
     * hostile - which plunges and lands (here, with no Underworld loaded, in the same world).
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void rebelAngelsAreCastOut(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        double fallen0 = cosmos.fallenAngels;
        HeavenlyBeing loyal = helper.spawn(ModEntities.ANGEL.get(), new BlockPos(1, 2, 1));
        HeavenlyBeing rebel = helper.spawn(ModEntities.ANGEL.get(), new BlockPos(3, 2, 3));
        loyal.setAngelState(new com.josephison.fiatlux.divinity.AngelState(0f, 1f));
        rebel.setRebel(true);
        helper.assertTrue(ModTags.isCorrupt(rebel), "a rebel should be counted among the corrupt");
        helper.assertTrue(!loyal.isAlliedTo(rebel) && !rebel.isAlliedTo(loyal), "the loyal and the rebel should be at war");
        helper.assertTrue(!ModTags.isCorrupt(loyal), "a loyal angel is not corrupt");
        rebel.hurt(helper.getLevel().damageSources().magic(), 10_000f);
        helper.assertTrue(rebel.isAlive() && rebel.isBeingCastOut(), "a defeated rebel should be cast out, not killed");
        helper.assertTrue(cosmos.fallenAngels >= fallen0 + 1, "the cast-out rebel was not counted among the fallen");
        loyal.discard();
        helper.runAfterDelay(HeavenlyBeing.STRIKE_TICKS + 10, () -> {
            helper.assertTrue(rebel.isRemoved(), "the rebel angel was never transformed");
            var fallen = helper.getLevel().getEntitiesOfClass(com.josephison.fiatlux.entity.FallenEntity.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(3, 2, 3))).inflate(16));
            helper.assertTrue(!fallen.isEmpty(), "no fallen one appeared where the rebel was cast out");
            com.josephison.fiatlux.entity.FallenEntity one = fallen.getFirst();
            helper.assertTrue(ModTags.isCorrupt(one), "the fallen should be in #fiatlux:corrupt");
            helper.succeedWhen(() -> {
                helper.assertTrue(!one.isPlunging(), "the fallen one is still plunging");
                fallen.forEach(net.minecraft.world.entity.Entity::discard);
            });
        });
    }

    /** The Flaming Sword slays the corrupt in a single stroke. */
    @GameTest(template = "empty")
    public static void flamingSwordSlaysTheCorrupt(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        HeavenlyBeing wielder = helper.spawn(ModEntities.ANGEL.get(), new BlockPos(1, 1, 1));
        helper.assertTrue(ModTags.isCorrupt(zombie), "zombies should be in #fiatlux:corrupt");
        ItemStack sword = new ItemStack(DivineWeaponRegistry.FLAMING_SWORD.get());
        DivineWeaponRegistry.FLAMING_SWORD.get().hurtEnemy(sword, zombie, wielder);
        helper.assertTrue(zombie.isDeadOrDying(), "the zombie survived the Flaming Sword");
        wielder.discard();
        helper.succeed();
    }

    /** The Watchers take wives, a giant is born whose spirit wanders when he dies, and the lines of Seth and Cain are named. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void watchersAndGiants(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        var gens = com.josephison.fiatlux.civilization.Generations.get(server);
        var flood = com.josephison.fiatlux.flood.FloodData.get(server);
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2)).offset(-600, 0, 600);
        forceAround(level, at, true);
        helper.runAfterDelay(10, () -> {
            var era = com.josephison.fiatlux.world.cosmos.MortalEra.AGRARIAN;
            var woman = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, at, at, era, "Naamah", false, 20, null, null, null);
            helper.assertTrue(woman != null, "no daughter of men was born");
            int came = com.josephison.fiatlux.flood.Nephilim.descend(level, flood, null, "TestWorld", at);
            var watchers = level.getEntitiesOfClass(com.josephison.fiatlux.flood.WatcherEntity.class, new net.minecraft.world.phys.AABB(at).inflate(60));
            helper.assertTrue(came >= 1 && !watchers.isEmpty() && woman.watcherHusband() != null, "the sons of God took no wives");
            int born = flood.giantsBorn, spirits = flood.giantSpirits, bound = flood.watchersBound;
            woman.bearGiant(level);
            var giant = level.getEntitiesOfClass(com.josephison.fiatlux.entity.MortalEntity.class, new net.minecraft.world.phys.AABB(at).inflate(6),
                    com.josephison.fiatlux.entity.MortalEntity::isNephil).stream().findFirst().orElse(null);
            helper.assertTrue(giant != null && flood.giantsBorn == born + 1 && giant.fatherName().startsWith("Shemihazah")
                    && gens.person(giant.getUUID()) != null && gens.person(giant.getUUID()).nephil, "no giant was born of the Watcher");
            giant.hurt(level.damageSources().generic(), Float.MAX_VALUE);
            helper.assertTrue("spirit".equals(gens.person(giant.getUUID()).soul) && flood.giantSpirits == spirits + 1,
                    "the giant's spirit did not go out to wander the earth");
            for (var w : watchers) com.josephison.fiatlux.flood.Nephilim.bind(level, w, "");
            helper.assertTrue(watchers.stream().allMatch(net.minecraft.world.entity.Entity::isRemoved) && flood.watchersBound == bound + watchers.size(),
                    "the Watchers were not bound");
            // the generations: Jared begets Enoch; Noah begets Shem, Ham and Japheth; Cain begets Enoch
            var jared = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, at.east(3), at, era, "Jared", true, 60, null, null, null);
            var noah = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, at.east(4), at, era, "Noah", true, 60, null, null, null);
            var cain = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, at.east(5), at, era, "Cain", true, 60, null, null, null);
            gens.person(jared.getUUID()).line = "seth";
            gens.person(noah.getUUID()).line = "seth";
            gens.person(cain.getUUID()).line = "cain";
            var a = com.josephison.fiatlux.civilization.Genealogy.nextSon(server, jared);
            var b = com.josephison.fiatlux.civilization.Genealogy.nextSon(server, noah);
            var c = com.josephison.fiatlux.civilization.Genealogy.nextSon(server, cain);
            helper.assertTrue(a != null && a.name().equals("Enoch") && b != null && b.name().equals("Shem") && b.line().equals("shem")
                    && c != null && c.name().equals("Enoch") && c.line().equals("cain"), "the generations were not named: " + a + " " + b + " " + c);
            for (var m : new net.minecraft.world.entity.Entity[]{woman, jared, noah, cain}) m.discard();
            forceAround(level, at, false);
            helper.succeed();
        });
    }

    /**
     * The Flood from the word to the bow: the righteous man builds the ark to its measure, his house and
     * the beasts go in, the waters rise over the land and bear the ark up, all flesh outside dies, the
     * waters abate, they go forth, and the covenant forbids a second Flood.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void theFloodAndTheArk(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        var flood = com.josephison.fiatlux.flood.FloodData.get(server);
        boolean covenant = flood.covenant;
        flood.covenant = false;
        flood.stage = com.josephison.fiatlux.flood.FloodData.Stage.NONE;   // the test world outlives a failed run
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2)).offset(1500, 0, 1500);
        forceAround(level, at, true);
        helper.runAfterDelay(10, () -> {
            BlockPos here = com.josephison.fiatlux.civilization.MortalPopulation.safeGround(level, at.getX(), at.getZ(), 4);
            var era = com.josephison.fiatlux.world.cosmos.MortalEra.AGRARIAN;
            var noah = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, here, here, era, "Noah", true, 50, null, null, null);
            var wife = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, here.east(), here, era, "Naamah", false, 48, null, null, null);
            var shem = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, here.south(), here, era, "Shem", true, 20, noah, wife, null);
            var wicked = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, here.north(2), here, era, "Lamech", true, 30, null, null, null);
            noah.addVirtue(20);
            var gens = com.josephison.fiatlux.civilization.Generations.get(server);
            // Noah and his wife are married
            gens.person(noah.getUUID()).spouse = wife.getUUID();
            var said = com.josephison.fiatlux.flood.Deluge.begin(level, here);
            helper.assertTrue(flood.stage == com.josephison.fiatlux.flood.FloodData.Stage.BUILDING && noah.getUUID().equals(flood.noah)
                    && flood.family.contains(shem.getUUID()) && !flood.family.contains(wicked.getUUID()), "no one was set to build the ark: " + said.getString());
            BlockPos home = flood.ark;
            com.josephison.fiatlux.flood.Deluge.tick(server, true);
            helper.assertTrue(flood.stage == com.josephison.fiatlux.flood.FloodData.Stage.BOARDING
                    && level.getBlockState(home.offset(30, 0, 5)).is(net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS)
                    && level.getBlockState(home.offset(30, 5, 5)).is(net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS), "the ark was not built to its measure");
            com.josephison.fiatlux.flood.Deluge.tick(server, true);
            var ark = com.josephison.fiatlux.flood.Ark.bounds(flood.ark);
            int beasts = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, ark).size();
            helper.assertTrue(flood.stage == com.josephison.fiatlux.flood.FloodData.Stage.RAIN && flood.doorShut && ark.contains(noah.position())
                    && ark.contains(shem.position()) && beasts >= 70, "the house and the beasts did not go in, or the door was not shut: " + beasts);
            com.josephison.fiatlux.flood.Deluge.tick(server, true);
            BlockPos field = here.north(12);
            helper.assertTrue(flood.stage == com.josephison.fiatlux.flood.FloodData.Stage.PREVAIL && flood.waterY == flood.peakY
                    && level.getFluidState(field.atY(flood.waterY)).is(net.minecraft.tags.FluidTags.WATER), "the waters did not prevail over the land");
            helper.assertTrue(flood.ark.getY() > home.getY() && noah.isAlive() && !wicked.isAlive(),
                    "the ark was not borne up, or all flesh did not die: ark " + flood.ark.getY() + " home " + home.getY() + " wicked " + wicked.isAlive());
            com.josephison.fiatlux.flood.Deluge.tick(server, true);
            helper.assertTrue(flood.stage == com.josephison.fiatlux.flood.FloodData.Stage.ABATE, "the waters did not begin to abate");
            com.josephison.fiatlux.flood.Deluge.tick(server, true);
            helper.assertTrue(flood.stage == com.josephison.fiatlux.flood.FloodData.Stage.DRY && flood.rested
                    && level.getBlockState(field.atY(flood.peakY)).isAir() && !flood.doorShut, "the waters were not dried up, or the ark did not rest");
            com.josephison.fiatlux.flood.Deluge.tick(server, true);
            helper.assertTrue(flood.stage == com.josephison.fiatlux.flood.FloodData.Stage.NONE && flood.covenant && noah.isAlive()
                    && !com.josephison.fiatlux.flood.Ark.bounds(flood.ark).contains(noah.position()) && flood.forced.isEmpty(), "they did not go forth of the ark");
            var again = com.josephison.fiatlux.flood.Deluge.begin(level, here);
            helper.assertTrue(flood.stage == com.josephison.fiatlux.flood.FloodData.Stage.NONE && again.getString().contains("bow"), "the covenant did not hold");
            // put things back
            for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(here).inflate(200),
                    e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
            flood.covenant = covenant;
            flood.rainbowUntil = 0;
            forceAround(level, at, false);
            helper.succeed();
        });
    }

    /**
     * A rift opens and an unclean spirit comes up and enters one of weak faith; a guardian angel comes
     * down to the town and anoints one of the faithful; the spirit is cast out, and cast into the deep.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void possessionAndExorcism(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2)).offset(-1200, 0, -1200);
        forceAround(level, at, true);
        helper.runAfterDelay(10, () -> {
            var d = com.josephison.fiatlux.infernal.InfernalData.get(server);
            int rifts = d.rifts, freed = d.exorcisms, deep = d.castIntoTheDeep;
            BlockPos here = com.josephison.fiatlux.civilization.MortalPopulation.safeGround(level, at.getX(), at.getZ(), 4);
            var era = com.josephison.fiatlux.world.cosmos.MortalEra.CLASSICAL;
            var host = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, here, here, era, "Mary", false, 30, null, null, null);
            var peter = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, here.east(3), here, era, "Peter", true, 40, null, null, null);
            var gens = com.josephison.fiatlux.civilization.Generations.get(server);
            peter.setReligion(gens.faithOf(java.util.UUID.randomUUID(), "Tester", peter.worldName(), level.getGameTime()).id);
            peter.addVirtue(5);
            helper.assertTrue(com.josephison.fiatlux.infernal.SpiritEntity.open(host) && !com.josephison.fiatlux.infernal.SpiritEntity.open(peter),
                    "faith did not decide who is open to a spirit");
            var rift = com.josephison.fiatlux.infernal.Infernal.openRift(level, here, com.josephison.fiatlux.infernal.InfernalHierarchy.byName("Paimon"));
            helper.assertTrue(rift != null && d.rifts == rifts + 1, "no rift opened");
            var spirit = rift.release(level);
            helper.assertTrue(spirit != null && spirit.lord().contains("Paimon"), "nothing came up out of the pit: " + (spirit == null ? "none" : spirit.lord()));
            spirit.moveTo(host.getX() + 2, host.getY() + 1, host.getZ(), 0, 0);
            for (int i = 0; i < 40 && !host.isPossessed(); i++) spirit.tick();
            helper.assertTrue(host.isPossessed() && !gens.person(host.getUUID()).possessed.isEmpty(), "the spirit did not enter the one of weak faith");
            var town = new com.josephison.fiatlux.civilization.Settlement(here);
            town.name = "Capernaum";
            helper.assertTrue(com.josephison.fiatlux.infernal.Infernal.guard(level, town), "no guardian came down to the town");
            var guardian = level.getEntitiesOfClass(com.josephison.fiatlux.infernal.GuardianEntity.class, new net.minecraft.world.phys.AABB(here).inflate(40))
                    .stream().findFirst().orElse(null);
            helper.assertTrue(guardian != null && com.josephison.fiatlux.infernal.GuardianEntity.worthy(peter), "the faithful one is not fit to be anointed");
            peter.anoint(guardian);
            helper.assertTrue(peter.isExorcist() && gens.person(peter.getUUID()).exorcist && !com.josephison.fiatlux.infernal.Infernal.guard(level, town),
                    "the angel did not anoint the faithful, or a second guardian came");
            host.exorcise("Peter");
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(!host.isPossessed() && d.exorcisms == freed + 1, "the spirit was not cast out");
                var out = level.getEntitiesOfClass(com.josephison.fiatlux.infernal.SpiritEntity.class, host.getBoundingBox().inflate(20)).stream().findFirst().orElse(null);
                helper.assertTrue(out != null, "nothing came out of her");
                out.castIntoTheDeep(level);
                helper.assertTrue(out.isRemoved() && d.castIntoTheDeep > deep, "the spirit was not cast into the deep");
                for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(here).inflate(80),
                        e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
                forceAround(level, at, false);
                helper.succeed();
            });
        });
    }

    /** A faith gets a leader and a successor when the leader dies; a branch splits off it; one of its people leaves it for another faith. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void faithsLeadersAndSchisms(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2)).offset(900, 0, -1500);
        forceAround(level, at, true);
        helper.runAfterDelay(10, () -> {
            var gens = com.josephison.fiatlux.civilization.Generations.get(server);
            var cosmos = CosmicExpanseSavedData.get(server);
            boolean fell = cosmos.fallOfMan;
            cosmos.fallOfMan = true;
            String world = "FaithTest";
            Planet planet = new Planet(UUID.randomUUID(), world, PlanetType.EARTHLIKE, 0, 1.5, 4.0, 0.25, 5.0, 1.0, 10000, 0xFFAA88, 10, 0.5, 0.2,
                    com.josephison.fiatlux.world.cosmos.MortalEra.CLASSICAL, 0);
            var faith = new com.josephison.fiatlux.civilization.Religion(UUID.randomUUID(), "The Way of Tester", com.josephison.fiatlux.civilization.Religion.Deity.CREATOR,
                    "Tester", UUID.randomUUID(), "Tester", world, level.getGameTime());
            gens.add(faith);
            BlockPos here = com.josephison.fiatlux.civilization.MortalPopulation.safeGround(level, at.getX(), at.getZ(), 4);
            java.util.List<com.josephison.fiatlux.entity.MortalEntity> flock = new java.util.ArrayList<>();
            for (int k = 0; k < 9; k++) {
                var m = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, here.offset(k % 3 * 2, 0, k / 3 * 2), here,
                        com.josephison.fiatlux.world.cosmos.MortalEra.CLASSICAL, null, k % 2 == 0, 30 + k * 4, null, null, faith.id);
                m.setReligion(faith.id);
                var person = gens.person(m.getUUID());
                flock.add(m);
                gens.people.put(m.getUUID(), new com.josephison.fiatlux.civilization.Person(m.getUUID(), person.name, person.male, world, person.born, null, null));
                gens.person(m.getUUID()).religion = faith.id;
            }
            com.josephison.fiatlux.civilization.Churches.tick(level, cosmos, planet);
            UUID first = faith.leader;
            helper.assertTrue(first != null && flock.stream().anyMatch(m -> m.getUUID().equals(first)) && faith.leaders == 1,
                    "the faith chose no leader from its people");
            var branch = com.josephison.fiatlux.civilization.Churches.branch(level, faith, planet, level.random);
            helper.assertTrue(branch != null && faith.id.equals(branch.parent) && !branch.doctrine.isEmpty() && branch.deity == faith.deity
                    && branch.leader != null && !branch.leader.equals(first), "no branch split from the faith");
            var leaver = flock.stream().filter(m -> faith.id.equals(m.religion()) && !m.getUUID().equals(first)).findFirst().orElse(null);
            if (leaver != null) {
                var now = com.josephison.fiatlux.civilization.Churches.apostasy(level, leaver, faith);
                helper.assertTrue(now != null && !faith.id.equals(leaver.religion()), "the one who left did not leave");
            }
            var reason = com.josephison.fiatlux.civilization.Churches.reason(level, flock.getFirst(), world);
            helper.assertTrue(reason.deity == com.josephison.fiatlux.civilization.Religion.Deity.REASON
                    && com.josephison.fiatlux.civilization.Churches.reason(level, flock.getFirst(), world) == reason, "those without a god have no gathering");
            // the leader dies, and another is chosen
            var leader = flock.stream().filter(m -> m.getUUID().equals(first)).findFirst().orElseThrow();
            gens.person(first).died = level.getGameTime();
            leader.discard();
            com.josephison.fiatlux.civilization.Churches.tick(level, cosmos, planet);
            helper.assertTrue(faith.leader != null && !faith.leader.equals(first) && faith.leaders == 2 || faith.extinct >= 0,
                    "no one succeeded the dead leader");
            // put things back
            flock.forEach(net.minecraft.world.entity.Entity::discard);
            flock.forEach(m -> gens.people.remove(m.getUUID()));
            gens.religions.values().removeIf(r -> world.equals(r.world));
            cosmos.fallOfMan = fell;
            forceAround(level, at, false);
            helper.succeed();
        });
    }

    /**
     * The houses of every age each stand with a bed where the builders say, and (but for the tents)
     * a door; a church rises about an altar without moving it, with a bell in its tower; and the
     * first man and the first woman are husband and wife within moments of being made (Gen 2:22-24).
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void housesTemplesAndTheFirstMarriage(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1)).offset(-640, 0, 640);
        forceAround(level, at, true);
        level.getChunk(at);
        BlockPos g = com.josephison.fiatlux.civilization.MortalPopulation.safeGround(level, at.getX(), at.getZ(), 8);
        helper.assertTrue(g != null, "no ground to build on");
        for (var era : com.josephison.fiatlux.world.cosmos.MortalEra.values()) {
            BlockPos h = g.offset(era.ordinal() * 16, 0, 0);
            BlockPos bed = com.josephison.fiatlux.civilization.Architecture.house(level, h, h.offset(0, 0, -30), era);
            helper.assertTrue(level.getBlockState(bed).getBlock() instanceof net.minecraft.world.level.block.BedBlock,
                    "the " + era.getSerializedName() + " house has no bed where it should");
            if (era == com.josephison.fiatlux.world.cosmos.MortalEra.HUNTER_GATHERER) continue;
            boolean door = false;
            for (BlockPos p : BlockPos.betweenClosed(h.offset(-5, 0, -5), h.offset(5, 1, 5))) {
                if (level.getBlockState(p).getBlock() instanceof net.minecraft.world.level.block.DoorBlock) door = true;
            }
            helper.assertTrue(door, "the " + era.getSerializedName() + " house has no door");
        }
        BlockPos shrine = g.offset(0, 0, 40);
        level.setBlock(shrine, net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        com.josephison.fiatlux.civilization.Architecture.temple(level, shrine, shrine.offset(0, 0, 8), 3);
        helper.assertTrue(level.getBlockState(shrine).is(net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS), "the church took the altar away");
        boolean bell = false;
        for (BlockPos p : BlockPos.betweenClosed(shrine.offset(-9, 0, -9), shrine.offset(9, 14, 9))) {
            if (level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.BELL)) bell = true;
        }
        helper.assertTrue(bell, "the church has no bell");
        var adam = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, g.offset(0, 0, -20), g, com.josephison.fiatlux.world.cosmos.MortalEra.AGRARIAN,
                "Adam", true, 24, null, null, null);
        var eve = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, g.offset(2, 0, -20), g, com.josephison.fiatlux.world.cosmos.MortalEra.AGRARIAN,
                "Eve", false, 22, null, null, null);
        helper.assertTrue(adam != null && eve != null, "the first pair were not formed");
        helper.runAfterDelay(140, () -> {
            boolean wed = eve.getUUID().equals(adam.spouseId()) && adam.getUUID().equals(eve.spouseId());
            adam.discard();
            eve.discard();
            forceAround(level, at, false);
            helper.assertTrue(wed, "Adam and Eve were not husband and wife");
            helper.succeed();
        });
    }

    /**
     * A prophet is called and speaks the word; challenges the priest of an idol at an altar, and with no
     * Creator by, the fire falls of itself and the altar is crowned for the true God (1 Kings 18). When
     * the prophet's words are written down they make a written book of several pages.
     */
    @GameTest(template = "empty", timeoutTicks = 1100)
    public static void prophetsFireAndScripture(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1)).offset(700, 0, -700);
        forceAround(level, at, true);
        level.getChunk(at);
        BlockPos g = com.josephison.fiatlux.civilization.MortalPopulation.safeGround(level, at.getX(), at.getZ(), 8);
        helper.assertTrue(g != null, "no ground");
        var gens = com.josephison.fiatlux.civilization.Generations.get(server);
        var faith = gens.faithOf(java.util.UUID.nameUUIDFromBytes("testgod".getBytes()), "Testgod", "the earth", level.getGameTime());
        var idol = new com.josephison.fiatlux.civilization.Religion(java.util.UUID.randomUUID(), "Cult of the Golden Calf",
                com.josephison.fiatlux.civilization.Religion.Deity.IDOL, "the Golden Calf", null, "Aaron", "the earth", level.getGameTime());
        gens.add(idol);
        var era = com.josephison.fiatlux.world.cosmos.MortalEra.AGRARIAN;
        var prophetM = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, g.offset(2, 0, 4), g, era, "Micaiah", true, 30, null, null, faith.id);
        var priest = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, g.offset(-2, 0, 4), g, era, "Zedekiah", true, 40, null, null, idol.id);
        var follower = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, g.offset(0, 0, 6), g, era, "Ahab", true, 35, null, null, idol.id);
        helper.assertTrue(prophetM != null && priest != null && follower != null, "the people were not formed");
        var p = com.josephison.fiatlux.prophecy.Prophets.call(level, faith, prophetM, "Testgod");
        helper.assertTrue(p != null && "Prophet".equals(com.josephison.fiatlux.prophecy.Prophets.titleOf(level, prophetM)), "the prophet was not called");
        for (int i = 0; i < 3; i++) com.josephison.fiatlux.prophecy.Prophets.preach(level, prophetM, p, faith, null);
        helper.assertTrue(!p.oracles.isEmpty(), "the prophet spoke no word");
        BlockPos altar = g.offset(0, 0, 0);
        level.setBlock(altar, net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(altar.above(), net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        var d = com.josephison.fiatlux.prophecy.ProphecyData.get(server);
        int won = d.contestsWon;
        helper.assertTrue(com.josephison.fiatlux.prophecy.Prophets.challenge(level, prophetM, p, faith, priest, idol, altar, "Samaria"), "no contest began");
        helper.runAfterDelay(1000, () -> {
            boolean fire = d.contestsWon > won && level.getBlockState(altar.above()).is(com.josephison.fiatlux.registry.ModBlocks.LUX_BLOCK.get());
            var book = com.josephison.fiatlux.prophecy.Scripture.prophetBook(level, p, "Tester");
            var stack = com.josephison.fiatlux.prophecy.Scripture.item(book, faith.name);
            var content = stack.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
            int pages = content == null ? 0 : content.pages().size();
            for (var e : java.util.List.of(prophetM, priest, follower)) e.discard();
            forceAround(level, at, false);
            helper.assertTrue(fire, "the fire did not fall on the altar: won " + d.contestsWon + "/" + won + ", crown " + level.getBlockState(altar.above())
                    + ", prophet alive " + prophetM.isAlive() + " removed " + prophetM.getRemovalReason() + " pos " + prophetM.blockPosition() + " altar " + altar
                    + " fate " + p.fate);
            helper.assertTrue(pages >= 2, "the prophet's book has " + pages + " pages");
            helper.succeed();
        });
    }

    /**
     * The story of the Son, hastened from the annunciation to Pentecost: he is born of the chosen
     * mother, grows, calls disciples, is crucified with his charge written over him, laid in the tomb,
     * rises shining, is taken up, and the church is born with its Gospels.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void theSonFromBirthToPentecost(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1)).offset(-900, 0, -900);
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) level.setChunkForced((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz, true);
        level.getChunk(at);
        BlockPos g = com.josephison.fiatlux.civilization.MortalPopulation.safeGround(level, at.getX(), at.getZ(), 8);
        helper.assertTrue(g != null, "no ground");
        var gens = com.josephison.fiatlux.civilization.Generations.get(server);
        var faith = gens.faithOf(java.util.UUID.nameUUIDFromBytes("sontest".getBytes()), "Testgod", "the earth", level.getGameTime());
        var era = com.josephison.fiatlux.world.cosmos.MortalEra.CLASSICAL;
        var mother = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, g.offset(2, 0, 2), g, era, "Miriam", false, 20, null, null, faith.id);
        var joseph = com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, g.offset(4, 0, 2), g, era, "Joseph", true, 26, null, null, faith.id);
        java.util.List<com.josephison.fiatlux.entity.MortalEntity> folk = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) folk.add(com.josephison.fiatlux.civilization.MortalPopulation.bringForth(level, g.offset(-4 + i, 0, 6), g, era, null, true, 24, null, null, faith.id));
        helper.assertTrue(mother != null && joseph != null, "the people were not formed");
        var d = com.josephison.fiatlux.son.SonData.get(server);
        helper.runAfterDelay(20, () -> {
        com.josephison.fiatlux.son.TheSon.start(level, g, "Nazareth", "Galilee", faith.creator, "Testgod", mother, faith.id, "Immanuel", 3);
        com.josephison.fiatlux.son.TheSon.hasten(level, d, com.josephison.fiatlux.son.SonData.Stage.MINISTRY);
        var son = d.son == null ? null : level.getEntity(d.son);
        helper.assertTrue(son instanceof com.josephison.fiatlux.entity.MortalEntity m && m.variant() == 3 && m.ageYears() >= 30,
                "the Son was not born and grown as chosen: stage " + d.stage + " step " + d.step + ", son " + son + ", mother " + level.getEntity(d.mother)
                        + " dim " + d.dim + " town " + d.town + " loaded " + level.isLoaded(d.town) + " secs2 " + com.josephison.fiatlux.son.TheSon.describe(server)
                        + (son instanceof com.josephison.fiatlux.entity.MortalEntity m2 ? " variant " + m2.variant() + " age " + m2.ageYears() : ""));
        com.josephison.fiatlux.son.TheSon.hasten(level, d, com.josephison.fiatlux.son.SonData.Stage.TOMB);
        helper.assertTrue(!d.disciples.isEmpty(), "no disciples were called: stage " + d.stage + " step " + d.step + " son " + level.getEntity(d.son));
        helper.assertTrue(d.cross != null && d.tomb != null, "he was not crucified and laid in a tomb");
        com.josephison.fiatlux.son.TheSon.hasten(level, d, com.josephison.fiatlux.son.SonData.Stage.ASCENSION);
        boolean shone = son instanceof com.josephison.fiatlux.entity.MortalEntity m && m.glory() == 2;
        com.josephison.fiatlux.son.TheSon.hasten(level, d, com.josephison.fiatlux.son.SonData.Stage.DONE);
        var church = gens.religion(d.church);
        int gospels = (int) com.josephison.fiatlux.prophecy.ProphecyData.get(server).books.stream()
                .filter(b -> b.kind == com.josephison.fiatlux.prophecy.Book.Kind.GOSPEL && church != null && b.faith.equals(church.id)).count();
        boolean ascended = son == null || son.isRemoved();
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(g).inflate(120),
                e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) level.setChunkForced((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz, false);
        helper.assertTrue(shone, "he did not rise shining");
        helper.assertTrue(ascended, "he was not taken up");
        helper.assertTrue(d.stage == com.josephison.fiatlux.son.SonData.Stage.DONE && church != null, "the church was not born: " + d.stage);
        helper.assertTrue(gospels >= 2, "the Gospels were not written (" + gospels + ")");
        helper.succeed();
        });
    }

    /** Keeps the chunks around a place loaded with their entities, as they are around a watching player. */
    private static void forceAround(net.minecraft.server.level.ServerLevel level, BlockPos at, boolean on) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) level.setChunkForced((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz, on);
    }
}
