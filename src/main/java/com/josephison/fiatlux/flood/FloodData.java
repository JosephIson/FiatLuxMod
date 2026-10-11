package com.josephison.fiatlux.flood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The days of Noah, kept for the whole world: where the Flood stands, the ark and those in it, the
 * waters, and the covenant afterwards; and the Watchers - which worlds they came down to, how many
 * were bound, and how many spirits of their giant sons wander the earth (1 Enoch 15:8-10).
 */
public final class FloodData extends SavedData {
    private static final String ID = "fiatlux_flood";

    public enum Stage { NONE, BUILDING, BOARDING, RAIN, PREVAIL, ABATE, DRY }

    public Stage stage = Stage.NONE;
    /** When the present stage began, and when the rain began (Gen 7:11). */
    public long since, floodStart;
    /** The dimension and world being judged (world null for a place that is not a created world). */
    public String dim = "";
    @Nullable public UUID world;
    public String worldName = "";
    /** The righteous man and all his house (7:1). */
    @Nullable public UUID noah;
    public String noahName = "";
    public final List<UUID> family = new ArrayList<>();
    /** The ark: where its corner stands now, where it was built, how much of it is built, and whether the door is shut. */
    public BlockPos ark = BlockPos.ZERO, arkHome = BlockPos.ZERO;
    public int arkProgress;
    public boolean doorShut;
    /** Pairs of beasts sent in so far; birds sent out from the window; one-time beats of the story already told. */
    public int pairs, birds, beats;
    /** The waters: the circles they cover, the lowest water laid in each column, and the levels. */
    public final List<BlockPos> centres = new ArrayList<>();
    public final List<Integer> radii = new ArrayList<>();
    @Nullable public int[] firstWater;
    public int startY, peakY, waterY, cursor;
    /** The highest ground under the waters, where the ark comes to rest ("the mountains of Ararat", 8:4). */
    public BlockPos ararat = BlockPos.ZERO;
    public boolean rested;
    public final Set<Long> forced = new HashSet<>();
    /** "I do set my bow in the cloud" (9:13): after the first Flood there is never another. */
    public boolean covenant;
    public long rainbowUntil;
    public BlockPos rainbow = BlockPos.ZERO;
    public int floods;
    // --- the Watchers and their sons
    public final Set<UUID> watchedWorlds = new HashSet<>();
    public final Set<UUID> grieved = new HashSet<>();
    public int watchersBound, giantsBorn, giantSpirits;

    private transient FloodWaters waters;

    public static FloodData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(FloodData::new, FloodData::load, null), ID);
    }

    public boolean active() {
        return stage != Stage.NONE;
    }

    /** Whether the Flood is upon this world now. */
    public boolean upon(@Nullable UUID planet) {
        return active() && planet != null && planet.equals(world);
    }

    public FloodWaters waters() {
        if (waters == null) waters = new FloodWaters(centres, radii, firstWater);
        return waters;
    }

    public void resetWaters() {
        waters = null;
        firstWater = null;
    }

    private static FloodData load(CompoundTag t, HolderLookup.Provider registries) {
        FloodData d = new FloodData();
        try {
            d.stage = Stage.valueOf(t.getString("stage"));
        } catch (IllegalArgumentException e) {
            d.stage = Stage.NONE;
        }
        d.since = t.getLong("since");
        d.floodStart = t.getLong("flood_start");
        d.dim = t.getString("dim");
        d.world = t.hasUUID("world") ? t.getUUID("world") : null;
        d.worldName = t.getString("world_name");
        d.noah = t.hasUUID("noah") ? t.getUUID("noah") : null;
        d.noahName = t.getString("noah_name");
        for (Tag x : t.getList("family", Tag.TAG_INT_ARRAY)) d.family.add(NbtUtils.loadUUID(x));
        d.ark = BlockPos.of(t.getLong("ark"));
        d.arkHome = BlockPos.of(t.getLong("ark_home"));
        d.arkProgress = t.getInt("ark_progress");
        d.doorShut = t.getBoolean("door_shut");
        d.pairs = t.getInt("pairs");
        d.birds = t.getInt("birds");
        d.beats = t.getInt("beats");
        for (long c : t.getLongArray("centres")) d.centres.add(BlockPos.of(c));
        for (int r : t.getIntArray("radii")) d.radii.add(r);
        if (t.contains("first_water")) d.firstWater = t.getIntArray("first_water");
        d.startY = t.getInt("start_y");
        d.peakY = t.getInt("peak_y");
        d.waterY = t.getInt("water_y");
        d.cursor = t.getInt("cursor");
        d.ararat = BlockPos.of(t.getLong("ararat"));
        d.rested = t.getBoolean("rested");
        for (long c : t.getLongArray("forced")) d.forced.add(c);
        d.covenant = t.getBoolean("covenant");
        d.rainbowUntil = t.getLong("rainbow_until");
        d.rainbow = BlockPos.of(t.getLong("rainbow"));
        d.floods = t.getInt("floods");
        for (Tag x : t.getList("watched", Tag.TAG_INT_ARRAY)) d.watchedWorlds.add(NbtUtils.loadUUID(x));
        for (Tag x : t.getList("grieved", Tag.TAG_INT_ARRAY)) d.grieved.add(NbtUtils.loadUUID(x));
        d.watchersBound = t.getInt("watchers_bound");
        d.giantsBorn = t.getInt("giants_born");
        d.giantSpirits = t.getInt("giant_spirits");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider registries) {
        t.putString("stage", stage.name());
        t.putLong("since", since);
        t.putLong("flood_start", floodStart);
        t.putString("dim", dim);
        if (world != null) t.putUUID("world", world);
        t.putString("world_name", worldName);
        if (noah != null) t.putUUID("noah", noah);
        t.putString("noah_name", noahName);
        ListTag fam = new ListTag();
        family.forEach(u -> fam.add(NbtUtils.createUUID(u)));
        t.put("family", fam);
        t.putLong("ark", ark.asLong());
        t.putLong("ark_home", arkHome.asLong());
        t.putInt("ark_progress", arkProgress);
        t.putBoolean("door_shut", doorShut);
        t.putInt("pairs", pairs);
        t.putInt("birds", birds);
        t.putInt("beats", beats);
        t.put("centres", new LongArrayTag(centres.stream().mapToLong(BlockPos::asLong).toArray()));
        t.putIntArray("radii", radii.stream().mapToInt(Integer::intValue).toArray());
        if (waters != null) firstWater = waters.save();
        if (firstWater != null) t.putIntArray("first_water", firstWater);
        t.putInt("start_y", startY);
        t.putInt("peak_y", peakY);
        t.putInt("water_y", waterY);
        t.putInt("cursor", cursor);
        t.putLong("ararat", ararat.asLong());
        t.putBoolean("rested", rested);
        t.put("forced", new LongArrayTag(forced.stream().mapToLong(Long::longValue).toArray()));
        t.putBoolean("covenant", covenant);
        t.putLong("rainbow_until", rainbowUntil);
        t.putLong("rainbow", rainbow.asLong());
        t.putInt("floods", floods);
        ListTag w = new ListTag();
        watchedWorlds.forEach(u -> w.add(NbtUtils.createUUID(u)));
        t.put("watched", w);
        ListTag g = new ListTag();
        grieved.forEach(u -> g.add(NbtUtils.createUUID(u)));
        t.put("grieved", g);
        t.putInt("watchers_bound", watchersBound);
        t.putInt("giants_born", giantsBorn);
        t.putInt("giant_spirits", giantSpirits);
        return t;
    }
}
