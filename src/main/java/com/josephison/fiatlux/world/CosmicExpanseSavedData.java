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
    private static final String[] GALAXY_NAMES = {"Aurora", "Selah", "Hosanna", "Shekinah", "Elohim's Veil", "Bethel", "Zion",
            "Peniel", "Ophir", "Tarshish", "Havilah", "Gihon", "Pishon", "Hiddekel", "Euphrates", "Ararat"};

    // --- the cosmos
    public long bigBangTime = -1;          // overworld game time of the Big Bang, -1 = not yet
    public double cosmicAge;               // simulated days since the Big Bang
    public final List<Galaxy> galaxies = new ArrayList<>();
    public final List<Planet> planets = new ArrayList<>();
    // --- the moral climate of creation
    public double corruption;              // 0..1
    public double angelicPride;            // 0..1
    public boolean rebellionActive;
    public double rebellionProgress;       // days of war elapsed
    public double fallenAngels;
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
        setDirty();
        return p;
    }

    public Optional<Planet> planet(String name) {
        return planets.stream().filter(p -> p.name.equalsIgnoreCase(name)).findFirst();
    }

    /** "the LORD God ... breathed into his nostrils the breath of life" (Gen 2:7): seeds mortal life. */
    public boolean breatheLife(Planet p) {
        if (p.capacity() <= 0 || p.hasLife()) return false;
        p.population = 2.0;
        setDirty();
        return true;
    }

    // ------------------------------------------------------------------ persistence

    public static CosmicExpanseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CosmicExpanseSavedData d = new CosmicExpanseSavedData();
        d.bigBangTime = tag.contains("big_bang_time") ? tag.getLong("big_bang_time") : -1;
        d.cosmicAge = tag.getDouble("cosmic_age");
        GALAXIES.parse(NbtOps.INSTANCE, tag.get("galaxies")).result().ifPresent(d.galaxies::addAll);
        PLANETS.parse(NbtOps.INSTANCE, tag.get("planets")).result().ifPresent(d.planets::addAll);
        d.corruption = tag.getDouble("corruption");
        d.angelicPride = tag.getDouble("angelic_pride");
        d.rebellionActive = tag.getBoolean("rebellion_active");
        d.rebellionProgress = tag.getDouble("rebellion_progress");
        d.fallenAngels = tag.getDouble("fallen_angels");
        d.heavenSouls = tag.getDouble("heaven_souls");
        UnderworldState.CODEC.parse(NbtOps.INSTANCE, tag.get("underworld")).result().ifPresent(u -> d.underworld = u);
        d.planetCursor = tag.getInt("planet_cursor");
        d.pendingGlory = tag.getDouble("pending_glory");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("big_bang_time", bigBangTime);
        tag.putDouble("cosmic_age", cosmicAge);
        GALAXIES.encodeStart(NbtOps.INSTANCE, galaxies).result().ifPresent(t -> tag.put("galaxies", t));
        PLANETS.encodeStart(NbtOps.INSTANCE, planets).result().ifPresent(t -> tag.put("planets", t));
        tag.putDouble("corruption", corruption);
        tag.putDouble("angelic_pride", angelicPride);
        tag.putBoolean("rebellion_active", rebellionActive);
        tag.putDouble("rebellion_progress", rebellionProgress);
        tag.putDouble("fallen_angels", fallenAngels);
        tag.putDouble("heaven_souls", heavenSouls);
        UnderworldState.CODEC.encodeStart(NbtOps.INSTANCE, underworld).result().ifPresent(t -> tag.put("underworld", t));
        tag.putInt("planet_cursor", planetCursor);
        tag.putDouble("pending_glory", pendingGlory);
        return tag;
    }
}
