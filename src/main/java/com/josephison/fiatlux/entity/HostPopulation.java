package com.josephison.fiatlux.entity;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.civilization.MortalPopulation;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.realm.RealmPool;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.FiatLuxDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * "Thousand thousands ministered unto him, and ten thousand times ten thousand stood before him"
 * (Daniel 7:10). Heaven is never empty: from the first moment of a world the Heavenly Host lives
 * there - all nine choirs, angels most of all, the highest fewest - about whoever walks its streets;
 * and on the living worlds, now and then, an angel passes, "sent forth to minister" (Heb 1:14). These
 * serve no one Creator in particular: they keep their own stations and their own counsel, and, when
 * pride grows in Heaven, it is from among them that the rebels rise. Those summoned by a Creator are
 * that Creator's and are not counted here. The Host drawn here leaves when no one is near.
 */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class HostPopulation {
    /** Marks the beings of the living Host, as distinct from those a Creator summons. */
    public static final String NATIVE = "fiatlux_host";
    private static final int INTERVAL = 60;
    /** The choirs and how many of each, from the lowest and most numerous to the highest. */
    private record Choir(Supplier<? extends EntityType<? extends HeavenlyBeing>> type, int weight, boolean heavenOnly) {}

    private static final List<Choir> CHOIRS = List.of(
            new Choir(ModEntities.ANGEL, 34, false), new Choir(ModEntities.ARCHANGEL, 10, false), new Choir(ModEntities.PRINCIPALITY, 9, false),
            new Choir(ModEntities.POWER, 8, true), new Choir(ModEntities.VIRTUE, 8, false), new Choir(ModEntities.DOMINION, 7, true),
            new Choir(ModEntities.CHERUB, 6, true), new Choir(ModEntities.SERAPH, 5, true), new Choir(ModEntities.OPHAN, 4, true));

    private HostPopulation() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL != 23) return;
        ServerLevel heaven = server.getLevel(FiatLuxDimensions.HEAVEN);
        if (heaven != null) populate(heaven, FiatLuxConfig.COMMON.hostInHeaven.get(), true);
        int passing = FiatLuxConfig.COMMON.hostOnWorlds.get();
        if (passing <= 0) return;
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(server);
        for (int i = 0; i < Math.min(RealmPool.SIZE, c.planets.size()); i++) {
            if (!c.planets.get(i).hasLife()) continue;
            ServerLevel world = server.getLevel(RealmPool.dimension(i));
            if (world != null) populate(world, passing, false);
        }
        populate(server.overworld(), passing, false);
    }

    /** Keeps `want` of the Host about each player in `level`, and sends away those no one is near. */
    public static void populate(ServerLevel level, int want, boolean heaven) {
        if (level.players().isEmpty() || want <= 0) {
            return;
        }
        // those no one is near go back to their stations
        for (HeavenlyBeing b : level.getEntities(EntityTypeTest.forClass(HeavenlyBeing.class), b -> b.getTags().contains(NATIVE))) {
            if (b.isRebel() || b.isBeingCastOut()) continue;
            if (level.getNearestPlayer(b, 160) == null) b.discard();
        }
        RandomSource r = level.random;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() && !heaven) continue;
            int near = level.getEntitiesOfClass(HeavenlyBeing.class, p.getBoundingBox().inflate(64), b -> b.getTags().contains(NATIVE)).size();
            // on arrival they are all there at once; afterwards they come a few at a time
            int add = near == 0 ? want : Math.min(2, want - near);
            for (int n = 0; n < add; n++) {
                HeavenlyBeing b = spawn(level, p.blockPosition(), r, heaven);
                if (b == null) break;
            }
        }
    }

    @Nullable
    public static HeavenlyBeing spawn(ServerLevel level, BlockPos near, RandomSource r, boolean heaven) {
        Choir choir = pick(r, heaven);
        HeavenlyBeing b = choir.type().get().create(level);
        if (b == null) return null;
        BlockPos at = null;
        for (int tries = 0; tries < 6 && at == null; tries++) {
            double a = r.nextDouble() * Math.PI * 2, d = 12 + r.nextDouble() * 36;
            BlockPos g = MortalPopulation.safeGround(level, near.getX() + (int) (Math.cos(a) * d), near.getZ() + (int) (Math.sin(a) * d), 6);
            if (g != null && Math.abs(g.getY() - near.getY()) < 40) at = g;
        }
        if (at == null) return null;
        // they stand among the streets of Heaven, or pass high over the living worlds
        double lift = heaven ? r.nextInt(3) == 0 ? 6 + r.nextInt(14) : 0.5 : 18 + r.nextInt(16);
        b.moveTo(at.getX() + 0.5, at.getY() + lift, at.getZ() + 0.5, r.nextFloat() * 360f, 0);
        b.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null);
        b.addTag(NATIVE);
        if (heaven && r.nextInt(3) == 0) b.setStation(at.above((int) lift));   // some keep a station
        level.addFreshEntity(b);
        return b;
    }

    private static Choir pick(RandomSource r, boolean heaven) {
        int total = 0;
        for (Choir c : CHOIRS) if (heaven || !c.heavenOnly()) total += c.weight();
        int roll = r.nextInt(total);
        for (Choir c : CHOIRS) {
            if (!heaven && c.heavenOnly()) continue;
            roll -= c.weight();
            if (roll < 0) return c;
        }
        return CHOIRS.getFirst();
    }
}
