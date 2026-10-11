package com.josephison.fiatlux.world;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.world.cosmos.Galaxy;
import com.josephison.fiatlux.world.cosmos.Planet;
import com.josephison.fiatlux.world.cosmos.PlanetType;
import com.josephison.fiatlux.world.cosmos.UnderworldState;
import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The simulated universe (design doc sections 0, 5, 6, 7): whether the Big Bang has happened, the
 * galaxies it formed, every created planet with its orbit and mortal civilisation, the moral
 * climate of creation (corruption, angelic pride/loyalty, rebellion) and the Underworld's
 * infernal civilisation. Advanced autonomously by {@link WorldTicker}; persisted on the overworld.
 */
public class CosmicExpanseSavedData extends SavedData {
    private static final String NAME = FiatLux.MODID + "_cosmic_expanse";
    private static final Codec<List<Planet>> PLANETS = Planet.CODEC.listOf();
    private static final Codec<List<Galaxy>> GALAXIES = Galaxy.CODEC.listOf();
    private static final Codec<List<com.josephison.fiatlux.world.cosmos.BlackHole>> BLACK_HOLES =
            com.josephison.fiatlux.world.cosmos.BlackHole.CODEC.listOf();
    private static final String[] GALAXY_NAMES = {"Aurora", "Selah", "Hosanna", "Shekinah", "Elohim's Veil", "Bethel", "Zion",
            "Peniel", "Ophir", "Tarshish", "Havilah", "Gihon", "Pishon", "Hiddekel", "Euphrates", "Ararat"};

    // --- the cosmos
    public long bigBangTime = -1;          // overworld game time of the Big Bang, -1 = not yet
    public double cosmicAge;               // simulated days since the Big Bang
    public final List<Galaxy> galaxies = new ArrayList<>();
    /** The towns of each living world, by planet id: the first is where its people began. */
    public final java.util.Map<java.util.UUID, List<com.josephison.fiatlux.civilization.Settlement>> towns = new java.util.HashMap<>();
    /** Version of the simulation's units; older universes are rescaled to believable numbers on load. */
    public static final int SIM_VERSION = 2;
    /** Who breathed life into each world (planet id -> player id and name): its people worship them. */
    public final java.util.Map<java.util.UUID, com.mojang.datafixers.util.Pair<java.util.UUID, String>> lifeGivers = new java.util.HashMap<>();
    /** Whether the first man and woman (Adam and Eve) have been brought forth. */
    public boolean firstPairBorn;
    // --- the garden of Eden and the Fall of Man (Genesis 2-3)
    public com.josephison.fiatlux.eden.EdenState eden = com.josephison.fiatlux.eden.EdenState.NONE;
    /** The dimension and place of the garden. */
    public String edenDim = "";
    public net.minecraft.core.BlockPos edenCentre = net.minecraft.core.BlockPos.ZERO;
    public net.minecraft.core.BlockPos lifeTree = net.minecraft.core.BlockPos.ZERO, knowledgeTree = net.minecraft.core.BlockPos.ZERO;
    /** The world Eden lies on (null for the earth). */
    @org.jetbrains.annotations.Nullable public java.util.UUID edenWorld;
    /** When the garden's present state began, and how far its scene has gone. */
    public long edenSince;
    public int edenStep;
    /** Man has eaten of the tree of knowledge: death, labour, birth in sorrow, and sin are in the world (Rom 5:12). */
    public boolean fallOfMan;
    /** Black holes in the Void; the first, the Abyss, forms with the Big Bang. */
    public final List<com.josephison.fiatlux.world.cosmos.BlackHole> blackHoles = new ArrayList<>();
    public final List<Planet> planets = new ArrayList<>();
    // --- the moral climate of creation
    public double corruption;              // 0..1
    public double angelicPride;            // 0..1
    public boolean rebellionActive;
    public double rebellionProgress;       // days of war elapsed
    public double fallenAngels;
    /** Embodied rebels still at war in the world (the Angelic Rebellion made visible). */
    public final java.util.Set<java.util.UUID> rebelIds = new java.util.HashSet<>();
    public double heavenSouls;
    // --- the Underworld
    public UnderworldState underworld = new UnderworldState();
    // --- bookkeeping
    public int planetCursor;
    public double pendingGlory;            // fractional Glory carried between steps

    public static CosmicExpanseSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CosmicExpanseSavedData::new, CosmicExpanseSavedData::load), NAME);
    }

    /** The towns of one world (empty if none). */
    public List<com.josephison.fiatlux.civilization.Settlement> townsOf(java.util.UUID planet) {
        return towns.getOrDefault(planet, List.of());
    }

    /** The first town of a world: where its people began. */
    @org.jetbrains.annotations.Nullable
    public com.josephison.fiatlux.civilization.Settlement firstTown(java.util.UUID planet) {
        var l = towns.get(planet);
        return l == null || l.isEmpty() ? null : l.getFirst();
    }

    /** The town of a world nearest a place. */
    @org.jetbrains.annotations.Nullable
    public com.josephison.fiatlux.civilization.Settlement townNear(java.util.UUID planet, net.minecraft.core.BlockPos pos) {
        com.josephison.fiatlux.civilization.Settlement best = null;
        double bd = Double.MAX_VALUE;
        for (var t : townsOf(planet)) {
            double d = t.centre.distSqr(pos);
            if (d < bd) {
                bd = d;
                best = t;
            }
        }
        return best;
    }

    public void addTown(java.util.UUID planet, com.josephison.fiatlux.civilization.Settlement town) {
        towns.computeIfAbsent(planet, k -> new ArrayList<>()).add(town);
        setDirty();
    }

    /** Every town of every world. */
    public List<com.josephison.fiatlux.civilization.Settlement> allTowns() {
        List<com.josephison.fiatlux.civilization.Settlement> all = new ArrayList<>();
        towns.values().forEach(all::addAll);
        return all;
    }

    public boolean hasBigBang() {
        return bigBangTime >= 0;
    }

    public double angelicLoyalty() {
        return 1.0 - angelicPride;
    }

    // ------------------------------------------------------------------ creation acts

    /** "In the beginning God created the heaven and the earth." Forms galaxies; idempotent after the first. */
    public void bigBang(long gameTime, int galaxyCount, RandomSource random) {
        if (!hasBigBang()) {
            bigBangTime = gameTime;
            cosmicAge = 0;
            // the bottomless pit opens in the deep with the first light (Rev 9:1-2)
            if (blackHoles.isEmpty()) {
                net.minecraft.world.phys.Vec3 a = com.josephison.fiatlux.world.VoidSpace.ABYSS;
                blackHoles.add(new com.josephison.fiatlux.world.cosmos.BlackHole("The Abyss", a.x, a.y, a.z,
                        (float) com.josephison.fiatlux.world.VoidSpace.ABYSS_RADIUS));
            }
        }
        int start = galaxies.size();
        for (int i = 0; i < galaxyCount; i++) {
            int n = start + i;
            float hueShift = random.nextFloat();
            int r = 160 + (int) (95 * Mth.sin(hueShift * Mth.TWO_PI));
            int g = 140 + (int) (80 * Mth.sin(hueShift * Mth.TWO_PI + 2.1f));
            int b = 180 + (int) (75 * Mth.sin(hueShift * Mth.TWO_PI + 4.2f));
            galaxies.add(new Galaxy(GALAXY_NAMES[n % GALAXY_NAMES.length] + (n >= GALAXY_NAMES.length ? " " + (n / GALAXY_NAMES.length + 1) : ""),
                    random.nextFloat() * 360f, 15f + random.nextFloat() * 70f, 4f + random.nextFloat() * 9f,
                    0xFF000000 | Mth.clamp(r, 0, 255) << 16 | Mth.clamp(g, 0, 255) << 8 | Mth.clamp(b, 0, 255),
                    random.nextInt(5), 1.5f + random.nextFloat() * 3f));
        }
        setDirty();
    }

    /** Forms a new world in orbit. Orbital period follows Kepler's third law (T^2 ~ a^3). */
    public Planet createPlanet(String name, PlanetType type, RandomSource random) {
        double a = 0.4 + planets.size() * 0.55 + random.nextDouble() * 0.3;
        Planet p = new Planet(UUID.randomUUID(), name, type, galaxies.isEmpty() ? 0 : random.nextInt(galaxies.size()),
                a, 3.0 * Math.pow(a, 1.5), random.nextDouble(), (random.nextDouble() - 0.5) * 30.0, type.gravity, type.dayLength,
                type.accentColor, 0, 0, 0, com.josephison.fiatlux.world.cosmos.MortalEra.HUNTER_GATHERER, 0);
        planets.add(p);
        com.josephison.fiatlux.realm.RealmPool.publish(planets);
        setDirty();
        return p;
    }

    public Optional<Planet> planet(String name) {
        return planets.stream().filter(p -> p.name.equalsIgnoreCase(name)).findFirst();
    }

    @org.jetbrains.annotations.Nullable
    public com.mojang.datafixers.util.Pair<java.util.UUID, String> lifeGiverOf(@org.jetbrains.annotations.Nullable Planet p) {
        return p == null ? null : lifeGivers.get(p.id);
    }

    /** "the LORD God ... breathed into his nostrils the breath of life" (Gen 2:7): seeds mortal life. */
    public boolean breatheLife(Planet p) {
        if (p.capacity() <= 0 || p.hasLife()) return false;
        p.population = 2.0;
        setDirty();
        return true;
    }

    /**
     * Universes from before the simulation counted in mortal years ran away into the billions: bring
     * them back to believable numbers, keeping the ages they reached.
     */
    private void rescale() {
        for (Planet p : planets) {
            double hab = p.type.habitability * p.type.size;
            if (p.population >= 1) p.population = Math.max(2, Math.min(p.population, p.era.capacity * hab * 0.05));
            p.faith = p.era.piety;
            p.sin = 0.15;
            p.eraProgress = 0;
        }
        heavenSouls = Math.min(heavenSouls, 2_000);
        UnderworldState u = underworld;
        u.souls = Math.min(u.souls, 1_000);
        u.soulPower = u.souls;
        u.demons = fallenAngels;
        u.contracts = Math.min(u.contracts, 300);
        var c = com.josephison.fiatlux.config.FiatLuxConfig.COMMON;
        u.knowledge = switch (u.era) {
            case TRIBAL_PITS -> Math.min(u.knowledge, c.feudalThreshold.get() * 0.5);
            case FEUDAL_CASTLE -> c.feudalThreshold.get();
            case INDUSTRIAL -> c.industrialThreshold.get();
            case MODERN_CYBERNETIC -> c.modernThreshold.get();
        };
        setDirty();
    }

    // ------------------------------------------------------------------ persistence

    public static CosmicExpanseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CosmicExpanseSavedData d = new CosmicExpanseSavedData();
        d.bigBangTime = tag.contains("big_bang_time") ? tag.getLong("big_bang_time") : -1;
        d.cosmicAge = tag.getDouble("cosmic_age");
        GALAXIES.parse(NbtOps.INSTANCE, tag.get("galaxies")).result().ifPresent(d.galaxies::addAll);
        BLACK_HOLES.parse(NbtOps.INSTANCE, tag.get("black_holes")).result().ifPresent(d.blackHoles::addAll);
        d.firstPairBorn = tag.getBoolean("first_pair_born");
        if (tag.contains("eden_state")) {
            try {
                d.eden = com.josephison.fiatlux.eden.EdenState.valueOf(tag.getString("eden_state"));
            } catch (IllegalArgumentException ignored) {
            }
            d.edenDim = tag.getString("eden_dim");
            d.edenCentre = net.minecraft.core.BlockPos.of(tag.getLong("eden_centre"));
            d.lifeTree = net.minecraft.core.BlockPos.of(tag.getLong("life_tree"));
            d.knowledgeTree = net.minecraft.core.BlockPos.of(tag.getLong("knowledge_tree"));
            if (tag.hasUUID("eden_world")) d.edenWorld = tag.getUUID("eden_world");
            d.edenSince = tag.getLong("eden_since");
            d.edenStep = tag.getInt("eden_step");
            d.fallOfMan = tag.getBoolean("fall_of_man");
        } else {
            d.fallOfMan = d.firstPairBorn;   // peoples given life before Eden was planted are fallen already
        }
        net.minecraft.nbt.CompoundTag lg = tag.getCompound("life_givers");
        for (String k : lg.getAllKeys()) {
            net.minecraft.nbt.CompoundTag e = lg.getCompound(k);
            try {
                d.lifeGivers.put(java.util.UUID.fromString(k), com.mojang.datafixers.util.Pair.of(e.getUUID("id"), e.getString("name")));
            } catch (IllegalArgumentException ignored) {
            }
        }
        net.minecraft.nbt.CompoundTag tw = tag.getCompound("towns");
        for (String k : tw.getAllKeys()) {
            try {
                var id = java.util.UUID.fromString(k);
                for (net.minecraft.nbt.Tag t : tw.getList(k, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                    d.towns.computeIfAbsent(id, x -> new ArrayList<>()).add(com.josephison.fiatlux.civilization.Settlement.load((CompoundTag) t));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        net.minecraft.nbt.CompoundTag st = tag.getCompound("settlements");   // one settlement per world, before towns
        for (String k : st.getAllKeys()) {
            try {
                var id = java.util.UUID.fromString(k);
                if (d.towns.containsKey(id)) continue;
                d.towns.computeIfAbsent(id, x -> new ArrayList<>()).add(st.contains(k, net.minecraft.nbt.Tag.TAG_COMPOUND)
                        ? com.josephison.fiatlux.civilization.Settlement.load(st.getCompound(k))
                        : new com.josephison.fiatlux.civilization.Settlement(net.minecraft.core.BlockPos.of(st.getLong(k))));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (d.hasBigBang() && !tag.contains("black_holes")) {   // universes born before black holes existed gain their Abyss
            net.minecraft.world.phys.Vec3 a = com.josephison.fiatlux.world.VoidSpace.ABYSS;
            d.blackHoles.add(new com.josephison.fiatlux.world.cosmos.BlackHole("The Abyss", a.x, a.y, a.z,
                    (float) com.josephison.fiatlux.world.VoidSpace.ABYSS_RADIUS));
        }
        PLANETS.parse(NbtOps.INSTANCE, tag.get("planets")).result().ifPresent(d.planets::addAll);
        d.corruption = tag.getDouble("corruption");
        d.angelicPride = tag.getDouble("angelic_pride");
        d.rebellionActive = tag.getBoolean("rebellion_active");
        d.rebellionProgress = tag.getDouble("rebellion_progress");
        d.fallenAngels = tag.getDouble("fallen_angels");
        for (net.minecraft.nbt.Tag t : tag.getList("rebels", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) {
            d.rebelIds.add(net.minecraft.nbt.NbtUtils.loadUUID(t));
        }
        d.heavenSouls = tag.getDouble("heaven_souls");
        UnderworldState.CODEC.parse(NbtOps.INSTANCE, tag.get("underworld")).result().ifPresent(u -> d.underworld = u);
        d.planetCursor = tag.getInt("planet_cursor");
        d.pendingGlory = tag.getDouble("pending_glory");
        // the era each explored Underworld region was founded in
        com.josephison.fiatlux.realm.UnderworldSettlements.CELL_ERAS.clear();
        long[] cells = tag.getLongArray("underworld_cells");
        byte[] eras = tag.getByteArray("underworld_cell_eras");
        for (int i = 0; i < Math.min(cells.length, eras.length); i++) {
            com.josephison.fiatlux.realm.UnderworldSettlements.CELL_ERAS.put(cells[i],
                    com.josephison.fiatlux.world.cosmos.UnderworldEra.values()[Math.max(0, Math.min(3, eras[i]))]);
        }
        if (tag.getInt("sim_version") < SIM_VERSION) d.rescale();
        com.josephison.fiatlux.realm.UnderworldSettlements.currentEra = d.underworld.era;
        com.josephison.fiatlux.realm.RealmPool.publish(d.planets);
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("big_bang_time", bigBangTime);
        tag.putDouble("cosmic_age", cosmicAge);
        GALAXIES.encodeStart(NbtOps.INSTANCE, galaxies).result().ifPresent(t -> tag.put("galaxies", t));
        BLACK_HOLES.encodeStart(NbtOps.INSTANCE, blackHoles).result().ifPresent(t -> tag.put("black_holes", t));
        tag.putBoolean("first_pair_born", firstPairBorn);
        tag.putString("eden_state", eden.name());
        tag.putString("eden_dim", edenDim);
        tag.putLong("eden_centre", edenCentre.asLong());
        tag.putLong("life_tree", lifeTree.asLong());
        tag.putLong("knowledge_tree", knowledgeTree.asLong());
        if (edenWorld != null) tag.putUUID("eden_world", edenWorld);
        tag.putLong("eden_since", edenSince);
        tag.putInt("eden_step", edenStep);
        tag.putBoolean("fall_of_man", fallOfMan);
        net.minecraft.nbt.CompoundTag lg = new net.minecraft.nbt.CompoundTag();
        lifeGivers.forEach((k, v) -> {
            net.minecraft.nbt.CompoundTag e = new net.minecraft.nbt.CompoundTag();
            e.putUUID("id", v.getFirst());
            e.putString("name", v.getSecond());
            lg.put(k.toString(), e);
        });
        tag.put("life_givers", lg);
        net.minecraft.nbt.CompoundTag tw = new net.minecraft.nbt.CompoundTag();
        towns.forEach((k, v) -> {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (var t : v) list.add(t.save());
            tw.put(k.toString(), list);
        });
        tag.put("towns", tw);
        tag.putInt("sim_version", SIM_VERSION);
        PLANETS.encodeStart(NbtOps.INSTANCE, planets).result().ifPresent(t -> tag.put("planets", t));
        tag.putDouble("corruption", corruption);
        tag.putDouble("angelic_pride", angelicPride);
        tag.putBoolean("rebellion_active", rebellionActive);
        tag.putDouble("rebellion_progress", rebellionProgress);
        tag.putDouble("fallen_angels", fallenAngels);
        net.minecraft.nbt.ListTag rebels = new net.minecraft.nbt.ListTag();
        for (java.util.UUID id : rebelIds) rebels.add(net.minecraft.nbt.NbtUtils.createUUID(id));
        tag.put("rebels", rebels);
        tag.putDouble("heaven_souls", heavenSouls);
        UnderworldState.CODEC.encodeStart(NbtOps.INSTANCE, underworld).result().ifPresent(t -> tag.put("underworld", t));
        tag.putInt("planet_cursor", planetCursor);
        tag.putDouble("pending_glory", pendingGlory);
        var cellMap = com.josephison.fiatlux.realm.UnderworldSettlements.CELL_ERAS;
        long[] cells = new long[cellMap.size()];
        byte[] eras = new byte[cellMap.size()];
        int i = 0;
        for (var e : cellMap.entrySet()) {
            if (i >= cells.length) break;
            cells[i] = e.getKey();
            eras[i++] = (byte) e.getValue().ordinal();
        }
        tag.putLongArray("underworld_cells", java.util.Arrays.copyOf(cells, i));
        tag.putByteArray("underworld_cell_eras", java.util.Arrays.copyOf(eras, i));
        return tag;
    }
}
