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
                ModEntities.CHERUB.get(), ModEntities.OPHAN.get());
        for (EntityType<? extends HeavenlyBeing> type : host) {
            HeavenlyBeing being = helper.spawn(type, new BlockPos(2, 2, 2));
            helper.assertTrue(being.isAlive(), type + " did not survive spawning");
            helper.assertTrue(being.isNoGravity(), type + " should fly");
            helper.assertTrue(being.getType().is(ModTags.HEAVENLY_HOST), type + " is missing from #fiatlux:heavenly_host");
            helper.assertTrue(being.getAnimatableInstanceCache() != null, type + " has no GeckoLib cache");
            being.discard();
        }
        helper.succeed();
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
        Planet eden = PlanetaryGenesisPower.create(server, "GameTest", PlanetType.EARTHLIKE, "GameTestEden");
        helper.assertTrue(eden != null && cosmos.breatheLife(eden), "could not breathe life on an Earth-like world");
        double phase0 = eden.phase;
        long glory0 = CosmosData.get(server).glory();
        cosmos.underworld = new UnderworldState();   // the GameTest world persists between runs
        cosmos.underworld.souls = 50_000;
        for (int i = 0; i < 400; i++) WorldTicker.step(server, 60.0);   // 400 simulated minutes
        helper.assertTrue(eden.population > 1000, "population did not grow: " + eden.population);
        helper.assertTrue(eden.era.ordinal() > 0, "the mortals of Eden never left the hunter-gatherer age");
        helper.assertTrue(CosmosData.get(server).glory() > glory0, "prayer added no Glory");
        helper.assertTrue(eden.phase != phase0, "the planet did not move along its orbit");
        helper.assertTrue(cosmos.underworld.era.ordinal() > 0, "the Underworld never left the Tribal/Pits era: knowledge " + cosmos.underworld.knowledge);
        cosmos.planets.remove(eden);
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
        WorldTicker.step(server, 1300.0);   // more than one simulated day of war
        helper.assertTrue(!cosmos.rebellionActive, "the rebellion never resolved");
        helper.assertTrue(cosmos.fallenAngels >= fallen0 + 100, "no angels fell: " + (cosmos.fallenAngels - fallen0));
        helper.assertTrue(cosmos.underworld.demons >= demons0 + 100, "the fallen did not reach the Underworld: demons " + demons0 + " -> " + cosmos.underworld.demons + ", cap " + com.josephison.fiatlux.config.FiatLuxConfig.COMMON.maxInfernalPopulation.get());
        helper.succeed();
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
}
