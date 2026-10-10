package com.josephison.fiatlux.world;

import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.divinity.AngelState;
import com.josephison.fiatlux.entity.HeavenlyBeing;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Angelic Rebellion made flesh (Revelation 12:7-9). When war breaks out in heaven, the proud
 * among the living Host turn, a rebel host gathers over the golden streets - led, the first time,
 * by "Lucifer, son of the morning" (Isaiah 14:12) - and Michael and his angels muster against
 * them. Rebels cannot be killed, only cast out: when they are defeated, or the Creator casts them
 * out, they fall. The war ends when no rebel remains, or after a simulated day, when the Host
 * prevails and every rebel still standing is cast down.
 */
public final class RebellionWar {
    private static final List<java.util.function.Supplier<? extends EntityType<? extends HeavenlyBeing>>> REBEL_CHOIRS = List.of(
            ModEntities.ANGEL, ModEntities.PRINCIPALITY, ModEntities.POWER, ModEntities.ANGEL, ModEntities.VIRTUE,
            ModEntities.ARCHANGEL, ModEntities.DOMINION, ModEntities.ANGEL, ModEntities.SERAPH);

    private RebellionWar() {}

    /** Where the war is fought: Heaven if it exists, otherwise the overworld. */
    public static ServerLevel battlefield(MinecraftServer server) {
        ServerLevel heaven = server.getLevel(FiatLuxDimensions.HEAVEN);
        return heaven != null ? heaven : server.overworld();
    }

    private static Vec3 musterPoint(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator()) return p.position().add(p.getLookAngle().multiply(14, 0, 14)).add(0, 5, 0);
        }
        if (level.getChunkSource().getGenerator() instanceof HeavenChunkGenerator heaven) {
            return new Vec3(0.5, heaven.streetLevel() + 8, 0.5);
        }
        BlockPos spawn = level.getSharedSpawnPos();
        return Vec3.atBottomCenterOf(spawn).add(0, 8, 0);
    }

    /** Musters both hosts at the start of the war. Returns the rebels that took up arms. */
    public static List<HeavenlyBeing> begin(MinecraftServer server, CosmicExpanseSavedData cosmos) {
        List<HeavenlyBeing> rebels = new ArrayList<>();
        // the proud among the living Host turn first
        for (ServerLevel level : server.getAllLevels()) {
            for (HeavenlyBeing being : level.getEntities(EntityTypeTest.forClass(HeavenlyBeing.class), b -> !b.isRebel() && !b.hasCreator())) {
                if (being.angelState().wouldRebel(cosmos.angelicPride)) {
                    turn(being, cosmos);
                    rebels.add(being);
                }
            }
        }
        if (FiatLuxConfig.COMMON.maxEmbodiedRebels.get() <= 0) return rebels;
        // then a rebel host gathers, and Michael and his angels muster against it
        ServerLevel level = battlefield(server);
        Vec3 c = musterPoint(level);
        level.getChunk(BlockPos.containing(c));
        int max = FiatLuxConfig.COMMON.maxEmbodiedRebels.get();
        int count = Math.min(max, 3 + (int) Math.round(cosmos.angelicPride * 6));
        boolean first = cosmos.fallenAngels < 1;
        for (int i = 0; i < count; i++) {
            // "the anointed cherub that covereth" (Ezek 28:14) leads; the rest come from every choir
            EntityType<? extends HeavenlyBeing> type = i == 0 ? ModEntities.CHERUB.get() : REBEL_CHOIRS.get(i % REBEL_CHOIRS.size()).get();
            Vec3 at = c.add(7 + (i % 3) * 3, (i / 3) * 2.5, (i % 2 == 0 ? 1 : -1) * (i * 1.6));
            HeavenlyBeing rebel = spawn(level, type, at, 0xFFFF3A2A);
            if (rebel == null) continue;
            if (i == 0) {
                rebel.setCustomName(Component.translatable(first ? "entity.fiatlux.lucifer" : "entity.fiatlux.rebel_captain"));
            }
            turn(rebel, cosmos);
            rebels.add(rebel);
        }
        int loyal = 1 + count / 4;   // Michael and a few of his angels: the war is the Creator's to end
        for (int i = 0; i < loyal; i++) {
            Vec3 at = c.add(-7 - (i % 3) * 3, (i / 3) * 2.5, (i % 2 == 0 ? 1 : -1) * (i * 1.6));
            // "Michael and his angels fought against the dragon" (Rev 12:7): the archangel leads the Powers
            HeavenlyBeing defender = spawn(level, i == 0 ? ModEntities.ARCHANGEL.get() : ModEntities.POWER.get(), at, 0xFFFFE6A0);
            if (defender == null) continue;
            defender.setAngelState(new AngelState(0f, 1f));
            if (i == 0) defender.setCustomName(Component.translatable("entity.fiatlux.michael"));
        }
        server.getPlayerList().broadcastSystemMessage(Component.translatable("chat.fiatlux.war_in_heaven",
                rebels.size(), level.dimension().location().toString(), (int) c.x, (int) c.y, (int) c.z)
                .withStyle(s -> s.withColor(0xFF6A4A)), false);
        cosmos.setDirty();
        return rebels;
    }

    private static HeavenlyBeing spawn(ServerLevel level, EntityType<? extends HeavenlyBeing> type, Vec3 at, int color) {
        HeavenlyBeing being = type.create(level);
        if (being == null) return null;
        being.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360f, 0);
        level.addFreshEntity(being);
        Fx.send(level, FxKind.HOST_ARRIVAL, at, 1.2f, color, 26);
        return being;
    }

    /** One of the Host takes up arms against the throne. */
    public static void turn(HeavenlyBeing being, CosmicExpanseSavedData cosmos) {
        being.setRebel(true);
        cosmos.rebelIds.add(being.getUUID());
        cosmos.setDirty();
    }

    /** Called as a rebel is struck down: it is counted among the fallen, and the war may end. */
    public static void onCastOut(MinecraftServer server, HeavenlyBeing being) {
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        cosmos.rebelIds.remove(being.getUUID());
        cosmos.fallenAngels += 1;
        cosmos.underworld.demons = Math.min(FiatLuxConfig.COMMON.maxInfernalPopulation.get(), cosmos.underworld.demons + 1);
        cosmos.setDirty();
        if (cosmos.rebellionActive && cosmos.rebelIds.isEmpty()) {
            // the last rebel has fallen: the war in heaven is over, and the unseen third of the stars falls with them
            WorldTicker.theFall(server, CosmosData.get(server), cosmos);
        }
    }

    /** All rebels currently loaded, in every dimension. */
    public static List<HeavenlyBeing> loadedRebels(MinecraftServer server) {
        List<HeavenlyBeing> out = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            out.addAll(level.getEntities(EntityTypeTest.forClass(HeavenlyBeing.class), b -> b.isRebel() && !b.isBeingCastOut()));
        }
        return out;
    }

    /** Casts out every loaded rebel, one after another. Rebels in unloaded regions fall when next seen. */
    public static int castOutAll(MinecraftServer server, int stagger) {
        List<HeavenlyBeing> rebels = loadedRebels(server);
        int i = 0;
        for (HeavenlyBeing rebel : rebels) {
            if (rebel.castOut(i * stagger)) i++;
        }
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        cosmos.rebelIds.clear();
        cosmos.setDirty();
        return i;
    }

    /**
     * Per-being upkeep (every second, server side): pride drifts with the climate of creation; while
     * war rages the proud join it; a rebel left over once the war is over is cast down.
     */
    public static void tickBeing(ServerLevel level, HeavenlyBeing being) {
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(level.getServer());
        if (being.isRebel()) {
            if (!cosmos.rebellionActive) being.castOut(level.random.nextInt(40));
            return;
        }
        AngelState s = being.angelState().drift(cosmos.angelicPride, 0.01);
        being.setAngelState(s);
        if (cosmos.rebellionActive && !being.hasCreator() && s.wouldRebel(cosmos.angelicPride)) {
            turn(being, cosmos);
        }
    }
}
