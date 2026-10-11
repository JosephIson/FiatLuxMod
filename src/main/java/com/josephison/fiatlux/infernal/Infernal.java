package com.josephison.fiatlux.infernal;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.MortalPopulation;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.infernal.InfernalHierarchy.Affliction;
import com.josephison.fiatlux.infernal.InfernalHierarchy.Lord;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.realm.RealmPool;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The war for the living, on every world where men have fallen: "For we wrestle not against flesh and
 * blood, but against principalities, against powers, against the rulers of the darkness of this
 * world, against spiritual wickedness in high places" (Eph 6:12).
 *
 * Once there are fallen angels, rifts open now and then near the towns of sinful worlds, and unclean
 * spirits come up through them - nameless ones of the legions, or the spirits of a lord of the Ars
 * Goetia, or Legion itself - and go about seeking those of weak faith to enter. Where giants have
 * died, their spirits rise of themselves at night (1 Enoch 15:8-11). When the Creator says "Go forth
 * and watch over the planets", a guardian angel goes down to every town of every living world: it
 * keeps the faithful, seals rifts, casts spirits into the deep, and anoints the faithful of the town
 * to cast out devils.
 */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class Infernal {
    private Infernal() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 == 17) tick(server, false);
    }

    /** Every second: rifts open and the dead giants' spirits rise; guardians go down to towns that lack one. */
    public static void tick(MinecraftServer server, boolean force) {
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(server);
        if (!c.fallOfMan) return;
        InfernalData d = InfernalData.get(server);
        var flood = com.josephison.fiatlux.flood.FloodData.get(server);
        for (int i = 0; i < Math.min(RealmPool.SIZE, c.planets.size()); i++) {
            Planet p = c.planets.get(i);
            if (!p.hasLife() || flood.upon(p.id)) continue;
            ServerLevel level = server.getLevel(RealmPool.dimension(i));
            if (level == null || level.players().isEmpty()) continue;
            if (d.watch && server.getTickCount() % 100 == 17) for (Settlement t : c.townsOf(p.id)) if (level.isLoaded(t.centre)) guard(level, t);
            if (c.fallenAngels <= 0) continue;
            Settlement town = c.townsOf(p.id).isEmpty() ? null : c.townsOf(p.id).get(level.random.nextInt(c.townsOf(p.id).size()));
            if (town == null || !level.isLoaded(town.centre)) continue;
            long time = level.getDayTime() % 24000L;
            boolean night = time > 13000 && time < 23000;
            double chance = FiatLuxConfig.COMMON.riftChance.get() * (0.4 + p.sin * 1.6) * (night ? 2 : 0.5) * (d.watch ? 0.5 : 1);
            if (force || level.random.nextDouble() < chance) {
                if (level.getEntitiesOfClass(RiftEntity.class, new AABB(town.centre).inflate(160)).size() < 2) openRift(level, town.centre, null);
            }
            // "the spirits of the giants ... shall rise up against the children of men" (1 Enoch 15:12)
            if (night && flood.giantSpirits > 0 && level.random.nextDouble() < chance * 0.5) risenGiant(level, town.centre);
        }
    }

    /**
     * A rift opens on the ground a little way from `near`, and spirits come up through it: of the
     * legions of a lord risen in the Underworld, or nameless, or - rarely - Legion (Mark 5:9).
     */
    @Nullable
    public static RiftEntity openRift(ServerLevel level, BlockPos near, @Nullable Lord lord) {
        RandomSource r = level.random;
        BlockPos g = null;
        for (int tries = 0; tries < 8 && g == null; tries++) {   // it tears the earth itself, not a roof or a floor
            double a = r.nextDouble() * Math.PI * 2;
            int dist = 28 + r.nextInt(20);
            g = MortalPopulation.safeGround(level, near.getX() + (int) (Math.cos(a) * dist), near.getZ() + (int) (Math.sin(a) * dist), 12);
            if (g != null && !level.getBlockState(g.below()).is(net.minecraft.tags.BlockTags.DIRT)
                    && !level.getBlockState(g.below()).is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
                    && !level.getBlockState(g.below()).is(net.minecraft.tags.BlockTags.SAND)) g = null;
        }
        if (g == null) return null;
        RiftEntity rift = ModEntities.RIFT.get().create(level);
        if (rift == null) return null;
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(level.getServer());
        if (lord == null && r.nextDouble() < 0.4) lord = InfernalHierarchy.sender(Math.max(2, c.underworld.overlords), r);
        boolean legion = lord == null && r.nextDouble() < 0.08;
        Affliction affliction = lord != null ? lord.affliction() : Affliction.values()[r.nextInt(Affliction.values().length)];
        rift.carry(legion ? 1 : 2 + r.nextInt(3), lord == null ? "" : lord.title(), affliction, legion);
        rift.moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, r.nextFloat() * 360f, 0);
        level.addFreshEntity(rift);
        InfernalData d = InfernalData.get(level.getServer());
        d.rifts++;
        d.setDirty();
        String from = legion ? "Legion" : lord != null ? "the legions of " + lord.title() : "unclean spirits";
        Component warning = Component.translatable("message.fiatlux.rift.opened", from).withStyle(s -> s.withColor(0xD05040));
        for (ServerPlayer p : level.players()) if (p.blockPosition().distSqr(g) < 200 * 200) p.sendSystemMessage(warning);
        CosmosData.get(level.getServer()).record(level.getServer(), "The Underworld", "rift",
                "tore a rift near " + nameOf(level, near) + "; " + from + " came up out of the pit.");
        return rift;
    }

    /** A dead giant's spirit rises out of the earth at night. */
    private static void risenGiant(ServerLevel level, BlockPos near) {
        BlockPos g = MortalPopulation.safeGround(level, near.getX() + level.random.nextInt(61) - 30, near.getZ() + level.random.nextInt(61) - 30, 10);
        if (g == null) return;
        SpiritEntity s = ModEntities.SPIRIT.get().create(level);
        if (s == null) return;
        s.moveTo(g.getX() + 0.5, g.getY() + 0.5, g.getZ() + 0.5, 0, 0);
        s.become("a spirit of the giants", "", level.random.nextBoolean() ? Affliction.VIOLENCE : Affliction.THEFT);
        level.addFreshEntity(s);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, s.getX(), s.getY(), s.getZ(), 20, 0.3, 0.3, 0.3, 0.02);
    }

    // ================================================================== the guardians

    /**
     * "Go forth and watch over the planets": the Creator sends the angels. Every town of every living
     * world is given a guardian (those whose worlds are not near anyone receive theirs when they are).
     */
    public static void decree(MinecraftServer server, String by) {
        InfernalData d = InfernalData.get(server);
        boolean first = !d.watch;
        d.watch = true;
        d.watchedBy = by;
        d.setDirty();
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(server);
        int sent = 0;
        for (int i = 0; i < Math.min(RealmPool.SIZE, c.planets.size()); i++) {
            Planet p = c.planets.get(i);
            ServerLevel level = server.getLevel(RealmPool.dimension(i));
            if (!p.hasLife() || level == null) continue;
            for (Settlement t : c.townsOf(p.id)) if (level.isLoaded(t.centre) && guard(level, t)) sent++;
        }
        Component psalm = Component.literal("For he shall give his angels charge over thee, to keep thee in all thy ways. They shall bear thee up "
                + "in their hands, lest thou dash thy foot against a stone.").withStyle(s -> s.withColor(0xF5D77A).withItalic(true))
                .append(Component.literal("  (Psalm 91:11-12)").withStyle(s -> s.withColor(0x9A8A6A).withItalic(false)));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(psalm);
            p.sendSystemMessage(Component.translatable("message.fiatlux.watch.sent", c.planets.stream().filter(Planet::hasLife).count(), sent)
                    .withStyle(s -> s.withColor(0x9AB8E8)));
        }
        if (first) {
            CosmosData.get(server).record(server, by, "watch", "said, Go forth and watch over the planets: and the angels went down, "
                    + "a guardian to every town of every living world, ministering spirits sent forth to minister (Heb 1:14).");
        }
    }

    /** Sends a guardian down to a town that has none. Returns whether one was sent. */
    public static boolean guard(ServerLevel level, Settlement town) {
        boolean kept = !level.getEntitiesOfClass(GuardianEntity.class, new AABB(town.centre).inflate(96),
                g -> town.centre.equals(g.town())).isEmpty();
        if (kept) return false;
        GuardianEntity g = ModEntities.GUARDIAN.get().create(level);
        if (g == null) return false;
        g.moveTo(town.centre.getX() + 0.5, town.centre.getY() + 24, town.centre.getZ() + 0.5, level.random.nextFloat() * 360f, 0);
        String name = town.name.isEmpty() ? "the town" : town.name;
        g.keep(town.centre, name);
        g.setCustomName(Component.literal("Guardian of " + name));
        level.addFreshEntity(g);
        Fx.send(level, FxKind.HOST_ARRIVAL, g.position(), 3f, 0xFFFFF4D6, 40);
        Fx.send(level, FxKind.CELESTIAL_PILLAR, town.centre.getCenter(), 26f, 0xFFFFF0C0, 30);
        return true;
    }

    /** "Go forth and watch over the planets", spoken by a Creator. */
    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        String said = event.getRawText().toLowerCase().replaceAll("[^a-z ]", " ").replaceAll("\\s+", " ").trim();
        ServerPlayer player = event.getPlayer();
        boolean watch = said.contains("watch over the planets") || said.contains("watch over the worlds") || said.contains("watch over the earth")
                || said.contains("go forth and watch");
        boolean comeOut = said.contains("come out of him") || said.contains("come out of her") || said.contains("hold thy peace");
        if (!watch && !comeOut) return;
        player.server.execute(() -> {
            if (Divinity.get(player).pilgrim()) return;
            if (watch) decree(player.server, player.getGameProfile().getName());
            if (comeOut) {   // "Hold thy peace, and come out of him" (Mark 1:25)
                for (var m : player.serverLevel().getEntitiesOfClass(com.josephison.fiatlux.entity.MortalEntity.class, player.getBoundingBox().inflate(12),
                        com.josephison.fiatlux.entity.MortalEntity::isPossessed)) {
                    m.exorcise(player.getGameProfile().getName());
                }
            }
        });
    }

    // ================================================================== reports

    /** A short account of the war for the living, for reports. */
    public static String describe(MinecraftServer server) {
        InfernalData d = InfernalData.get(server);
        int possessed = 0, exorcists = 0;
        for (Person p : Generations.get(server).people.values()) {
            if (!p.living()) continue;
            if (!p.possessed.isEmpty()) possessed++;
            if (p.exorcist) exorcists++;
        }
        String watch = d.watch ? "Guardian angels watch over every town (sent by " + d.watchedBy + ")." : "No guardians have been sent; say \"Go forth and watch over the planets\".";
        return watch + " Rifts opened: " + d.rifts + " (" + d.sealed + " sealed). Possessed now: " + possessed + "; set free: " + d.exorcisms
                + "; cast into the deep: " + d.castIntoTheDeep + (d.returned > 0 ? "; returned sevenfold: " + d.returned : "")
                + (d.swine > 0 ? "; swine drowned: " + d.swine : "") + ". Anointed to cast out devils: " + exorcists + " living.";
    }

    private static String nameOf(ServerLevel level, BlockPos near) {
        int slot = RealmPool.slotOf(level.dimension());
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(level.getServer());
        if (slot < 0 || slot >= c.planets.size()) return "the earth";
        Planet p = c.planets.get(slot);
        Settlement t = c.townNear(p.id, near);
        return t != null && !t.name.isEmpty() ? t.name + " on " + p.name : p.name;
    }

    public static void narrate(ServerLevel level, BlockPos near, String text, String ref) {
        Component line = Component.literal(text).withStyle(s -> s.withColor(0xF5D77A).withItalic(true))
                .append(Component.literal("  (" + ref + ")").withStyle(s -> s.withColor(0x9A8A6A).withItalic(false)));
        for (ServerPlayer p : level.players()) if (p.blockPosition().distSqr(near) < 160 * 160) p.sendSystemMessage(line);
    }
}
