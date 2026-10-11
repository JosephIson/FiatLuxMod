package com.josephison.fiatlux.flood;

import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.MortalPopulation;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.realm.RealmPool;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "And it came to pass, when men began to multiply on the face of the earth, and daughters were born
 * unto them, that the sons of God saw the daughters of men that they were fair; and they took them
 * wives of all which they chose. ... There were giants in the earth in those days; and also after
 * that, when the sons of God came in unto the daughters of men, and they bare children to them, the
 * same became mighty men which were of old, men of renown" (Genesis 6:1-4).
 *
 * Once a fallen world's people are many, the Watchers come down - in the reading of the Book of Enoch,
 * two hundred under Shemihazah on the summit of Hermon (1 Enoch 6) - and their leaders take wives in
 * the towns and teach what men were not meant to know (1 Enoch 8). Their wives bear the Nephilim:
 * giants who grow fast and huge, devour the people's bread and do violence (1 Enoch 7; Gen 6:11), and
 * whose spirits, when they die, wander the earth as evil spirits (1 Enoch 15:8-10). When the
 * wickedness is great, the Creator grieves (Gen 6:5-7): the time for the Flood has come.
 */
public final class Nephilim {
    /** The chiefs of the Watchers and what each taught (1 Enoch 6:7, 8:1-3). */
    private static final String[][] CHIEFS = {
            {"Shemihazah", "enchantments, and the cutting of roots"},
            {"Azazel", "the making of swords and knives, shields and breastplates, and the working of metals, and bracelets and ornaments, "
                    + "and the beautifying of the eyelids"},
            {"Armaros", "the resolving of enchantments"},
            {"Baraqel", "astrology"},
            {"Kokabiel", "the signs of the constellations"},
            {"Ezeqeel", "the knowledge of the clouds"},
            {"Araqiel", "the signs of the earth"},
            {"Shamsiel", "the signs of the sun"},
            {"Sariel", "the course of the moon"},
    };
    /** Giants named in the Book of Giants (4Q530-532), and the Anakim of Numbers 13:33. */
    private static final String[] GIANTS = {"Ohyah", "Hahyah", "Mahaway", "Gilgamesh", "Hobabish", "Ahiram", "Anak", "Arba"};

    private Nephilim() {}

    /** Every few seconds: the Watchers look down on worlds whose people have multiplied, and the Creator sees what the earth has become. */
    public static void tick(MinecraftServer server, boolean force) {
        FloodData d = FloodData.get(server);
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(server);
        if (!c.fallOfMan || d.covenant) return;
        java.util.Map<String, Integer> census = Generations.get(server).census();
        for (int i = 0; i < Math.min(RealmPool.SIZE, c.planets.size()); i++) {
            Planet p = c.planets.get(i);
            if (!p.hasLife()) continue;
            ServerLevel level = server.getLevel(RealmPool.dimension(i));
            if (level == null || level.players().isEmpty()) continue;
            if (!d.watchedWorlds.contains(p.id) && FiatLuxConfig.COMMON.watchersDescend.get() && !d.active()
                    && census.getOrDefault(p.name, 0) >= FiatLuxConfig.COMMON.watchersMinPeople.get()
                    && (force || level.random.nextInt(900) == 0)) {
                Settlement town = c.firstTown(p.id);
                if (town != null && level.isLoaded(town.centre)) descend(level, d, p.id, p.name, town.centre);
            }
            if (!d.grieved.contains(p.id) && !d.active() && p.sin >= 0.4 && giants(server, p.name) > 0) grieve(level, d, p);
        }
    }

    /** The living giants of a world. */
    public static int giants(MinecraftServer server, String world) {
        int n = 0;
        for (Person x : Generations.get(server).people.values()) if (x.nephil && x.living() && world.equals(x.world)) n++;
        return n;
    }

    /**
     * "They were in all two hundred; who descended in the days of Jared on the summit of Mount Hermon"
     * (1 Enoch 6:6): the chiefs come down over a town and each takes a wife of the daughters of men.
     */
    public static int descend(ServerLevel level, FloodData d, @Nullable java.util.UUID world, String worldName, BlockPos town) {
        if (world != null) d.watchedWorlds.add(world);
        d.setDirty();
        List<MortalEntity> women = new ArrayList<>(level.getEntitiesOfClass(MortalEntity.class, new AABB(town).inflate(80),
                m -> !m.isMale() && !m.isChild() && m.isAlive() && m.watcherHusband() == null && !m.isNephil()));
        women.sort(Comparator.comparing((MortalEntity m) -> m.spouseEntity() != null).thenComparingDouble(MortalEntity::ageYears));
        int n = Math.min(Math.min(5, CHIEFS.length), Math.max(1, women.size()));
        RandomSource r = level.random;
        int came = 0;
        for (int k = 0; k < n; k++) {
            WatcherEntity w = ModEntities.WATCHER.get().create(level);
            if (w == null) continue;
            String name = CHIEFS[k][0];
            double a = r.nextDouble() * Math.PI * 2;
            BlockPos at = town.offset((int) (Math.cos(a) * 6), 0, (int) (Math.sin(a) * 6));
            w.moveTo(at.getX() + 0.5, at.getY() + 26 + k * 3, at.getZ() + 0.5, r.nextFloat() * 360f, 0);
            w.setCustomName(Component.literal(name + ", the Watcher"));
            w.setTeaching(CHIEFS[k][1]);
            w.setStation(at.above());
            if (k < women.size()) {
                MortalEntity wife = women.get(k);
                w.takeWife(wife.getUUID(), wife.getName().getString());
                wife.takenBy(w.getUUID(), name);
                w.setStation(wife.home() != null ? wife.home().above() : at.above());
            }
            level.addFreshEntity(w);
            Fx.send(level, FxKind.CELESTIAL_PILLAR, w.position(), 3f, 0xFFB8B0D8, 40);
            came++;
            CosmosData.get(level.getServer()).record(level.getServer(), name, "watchers",
                    "came down to " + worldName + (w.wife() != null ? " and took " + w.wifeName() + " to wife" : "")
                            + ", and taught them " + CHIEFS[k][1] + ".");
        }
        narrate(level, town, "And it came to pass, when men began to multiply on the face of the earth, and daughters were born unto them, "
                + "that the sons of God saw the daughters of men that they were fair; and they took them wives of all which they chose.", "Genesis 6:1-2");
        narrate(level, town, "And they were in all two hundred; who descended on the summit of Mount Hermon, and they called it Mount Hermon, "
                + "because they had sworn and bound themselves by mutual imprecations upon it.", "1 Enoch 6:6");
        level.playSound(null, town, SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 3f, 0.5f);
        // what they taught hastens the age, and corrupts it
        Planet p = planet(level, world);
        if (p != null) {
            p.eraProgress += 3;
            p.sin = Math.min(1, p.sin + 0.12);
            CosmicExpanseSavedData.get(level.getServer()).setDirty();
        }
        CosmosData.get(level.getServer()).record(level.getServer(), "The Watchers", "descent",
                "Two hundred of the sons of God came down to " + worldName + " and took them wives of the daughters of men.");
        return came;
    }

    /** "And GOD saw that the wickedness of man was great in the earth" (Gen 6:5): the Creator is shown what the world has become. */
    private static void grieve(ServerLevel level, FloodData d, Planet p) {
        d.grieved.add(p.id);
        d.setDirty();
        Settlement town = CosmicExpanseSavedData.get(level.getServer()).firstTown(p.id);
        BlockPos at = town != null ? town.centre : level.getSharedSpawnPos();
        narrate(level, at, "And GOD saw that the wickedness of man was great in the earth, and that every imagination of the thoughts of his heart "
                + "was only evil continually. And it repented the LORD that he had made man on the earth, and it grieved him at his heart.", "Genesis 6:5-6");
        narrate(level, at, "The earth also was corrupt before God, and the earth was filled with violence.", "Genesis 6:11");
        for (ServerPlayer pl : level.players()) pl.sendSystemMessage(Component.translatable("message.fiatlux.flood.grieved").withStyle(s -> s.withColor(0x9AB8E8)));
        CosmosData.get(level.getServer()).record(level.getServer(), p.name, "wickedness",
                "was filled with violence: giants walked the earth, and every imagination of the heart of man was only evil continually.");
    }

    /** The name of a Watcher's son (the sons of Shemihazah were Ohyah and Hahyah, Baraqel's was Mahaway). */
    public static String giantName(String father, int sons, RandomSource r) {
        if (father.startsWith("Shemihazah") && sons < 2) return GIANTS[sons];
        if (father.startsWith("Baraqel") && sons == 0) return "Mahaway";
        return GIANTS[3 + r.nextInt(GIANTS.length - 3)];
    }

    /** "Reserved in everlasting chains under darkness unto the judgment of the great day" (Jude 1:6). */
    public static void bind(ServerLevel level, WatcherEntity w, String by) {
        if (w.isRemoved()) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(w.position());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        Fx.send(level, FxKind.HALO_SHATTER, w.position().add(0, 1.6, 0), 2f, 0xFF6A5A8A, 30);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, w.getX(), w.getY() + 1, w.getZ(), 40, 0.4, 1.0, 0.4, 0.02);
        level.sendParticles(ParticleTypes.CRIT, w.getX(), w.getY() + 1, w.getZ(), 60, 0.5, 1.2, 0.5, 0.3);
        level.playSound(null, w.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 3f, 0.5f);
        level.playSound(null, w.blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.HOSTILE, 2f, 0.4f);
        String name = w.getCustomName() != null ? w.getCustomName().getString().replace(", the Watcher", "") : "A Watcher";
        FloodData d = FloodData.get(level.getServer());
        d.watchersBound++;
        d.setDirty();
        CosmosData.get(level.getServer()).record(level.getServer(), name, "bound",
                "was bound hand and foot and cast into the darkness" + (by.isEmpty() ? "" : " by " + by) + ", reserved unto the judgment of the great day.");
        w.discard();
    }

    /** At the Flood every Watcher on that world is bound, and all the rest of the two hundred with them. */
    public static void bindAll(ServerLevel level) {
        for (WatcherEntity w : level.getEntities(ModEntities.WATCHER.get(), x -> true)) bind(level, w, "");
        FloodData d = FloodData.get(level.getServer());
        d.watchersBound = Math.max(d.watchersBound, 200 * Math.max(1, d.watchedWorlds.size()));
        d.setDirty();
    }

    @Nullable
    private static Planet planet(ServerLevel level, @Nullable java.util.UUID world) {
        if (world == null) return null;
        for (Planet p : CosmicExpanseSavedData.get(level.getServer()).planets) if (p.id.equals(world)) return p;
        return null;
    }

    static void narrate(ServerLevel level, BlockPos near, String text, String ref) {
        Component line = Component.literal(text).withStyle(s -> s.withColor(0xF5D77A).withItalic(true))
                .append(Component.literal("  (" + ref + ")").withStyle(s -> s.withColor(0x9A8A6A).withItalic(false)));
        for (ServerPlayer p : level.players()) if (p.blockPosition().distSqr(near) < 400 * 400) p.sendSystemMessage(line);
    }

    /** Where a giant comes to be born: by its mother. */
    static BlockPos near(ServerLevel level, BlockPos p) {
        BlockPos g = MortalPopulation.safeGround(level, p.getX(), p.getZ(), 4);
        return g != null ? g : p;
    }
}
