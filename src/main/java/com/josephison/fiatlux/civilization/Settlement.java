package com.josephison.fiatlux.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A mortal settlement on a living world: its hearth, shrine and field, the houses its builders have
 * raised and the beds in them, and the store of food its farmers, gatherers and hunters bring in and
 * everyone eats from at the hearth.
 */
public final class Settlement {
    public final BlockPos centre;
    /** The town's name (the first town of a world is named after it until its people name it). */
    public String name = "";
    /** When it was founded (game time), and by whom ("" for the first). */
    public long founded;
    public String founders = "";
    public BlockPos shrine;
    @Nullable
    public BlockPos field;
    public final List<BlockPos> houses = new ArrayList<>();
    public final List<BlockPos> beds = new ArrayList<>();
    public int food = 30;
    /** How many paths and lights the builders have laid so far. */
    public int works;
    /** When the people last saw a sign from heaven (a miracle near them). */
    public long lastSign;
    /** Open petitions laid before their Creator, by kind, with the words of the prayer. */
    public final java.util.EnumMap<Faiths.Petition, String> petitions = new java.util.EnumMap<>(Faiths.Petition.class);
    public long petitionsSince, lastPetition;
    /** A prayer for a child was answered: the next couple to hope for one will have it. */
    public boolean blessedWithChild;
    /** The age each house was built in (its style), parallel to {@link #houses}; -1 for the old huts and tents. */
    public final List<Integer> houseStyles = new ArrayList<>();
    /** The style of the house of their god (see {@link Architecture#templeStyle}). */
    public int templeStyle;
    /** 1 once the town is laid out with room for houses of every age. */
    public int layout;
    /** The burying place outside the town (the centre of its first yard), once someone has died there. */
    @org.jetbrains.annotations.Nullable public BlockPos graveyard;
    /** Graves dug in it so far. */
    public int graves;

    public Settlement(BlockPos centre) {
        this.centre = centre;
        this.shrine = centre.offset(0, 0, -8);
    }

    /** Room for this many sleepers in the houses (two to a bed's house). */
    public int capacity() {
        return Math.max(4, beds.size() * 2);
    }

    /**
     * Where the n-th house would stand: on rings around the hearth with room between them for the
     * largest houses, clear of the shrine and its temple to the north and the field to the south.
     */
    public BlockPos houseSpot(int n) {
        int found = 0;
        for (int ring = 0; ring < 12; ring++) {
            double r = 15 + ring * 12;
            int count = (int) (2 * Math.PI * r / 13.5);
            for (int k = 0; k < count; k++) {
                double a = 2 * Math.PI * k / count + ring * 0.37 + 0.2;
                int dx = (int) Math.round(Math.cos(a) * r), dz = (int) Math.round(Math.sin(a) * r);
                if (clashes(dx, dz)) continue;
                if (found++ == n) return centre.offset(dx, 0, dz);
            }
        }
        return centre.offset(n * 12, 0, 200);
    }

    private boolean clashes(int dx, int dz) {
        int sx = shrine.getX() - centre.getX(), sz = shrine.getZ() - centre.getZ();
        if (Math.abs(dx - sx) <= 13 && dz - sz >= -15 && dz - sz <= 13) return true;   // the temple, whatever its size
        if (graveyard != null) {   // the burying place and the yards that may be added beyond it
            int gx = graveyard.getX() - centre.getX(), gz = graveyard.getZ() - centre.getZ();
            int ux = Math.abs(gx) >= Math.abs(gz) ? Integer.signum(gx) : 0, uz = ux == 0 ? Integer.signum(gz) : 0;
            for (int k = 0; k < 4; k++) if (Math.abs(dx - gx - ux * 15 * k) <= 14 && Math.abs(dz - gz - uz * 15 * k) <= 14) return true;
        }
        int fx = field != null ? field.getX() - centre.getX() : 0, fz = field != null ? field.getZ() - centre.getZ() : 18;
        return Math.abs(dx - fx) <= 10 && Math.abs(dz - fz) <= 10;   // the field
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putLong("centre", centre.asLong());
        t.putLong("shrine", shrine.asLong());
        if (field != null) t.putLong("field", field.asLong());
        t.put("houses", new LongArrayTag(houses.stream().mapToLong(BlockPos::asLong).toArray()));
        t.put("beds", new LongArrayTag(beds.stream().mapToLong(BlockPos::asLong).toArray()));
        t.putInt("food", food);
        t.putInt("works", works);
        t.putLong("last_sign", lastSign);
        t.putBoolean("blessed_child", blessedWithChild);
        CompoundTag pt = new CompoundTag();
        petitions.forEach((k, v) -> pt.putString(k.name(), v));
        t.put("petitions", pt);
        t.putLong("petitions_since", petitionsSince);
        t.putString("name", name);
        t.putLong("founded", founded);
        t.putString("founders", founders);
        t.putIntArray("house_styles", houseStyles.stream().mapToInt(Integer::intValue).toArray());
        t.putInt("temple_style", templeStyle);
        t.putInt("layout", layout);
        if (graveyard != null) t.putLong("graveyard", graveyard.asLong());
        t.putInt("graves", graves);
        return t;
    }

    public static Settlement load(CompoundTag t) {
        Settlement s = new Settlement(BlockPos.of(t.getLong("centre")));
        if (t.contains("shrine")) s.shrine = BlockPos.of(t.getLong("shrine"));
        if (t.contains("field")) s.field = BlockPos.of(t.getLong("field"));
        if (t.contains("graveyard")) s.graveyard = BlockPos.of(t.getLong("graveyard"));
        s.graves = t.getInt("graves");
        for (long l : t.getLongArray("houses")) s.houses.add(BlockPos.of(l));
        for (long l : t.getLongArray("beds")) s.beds.add(BlockPos.of(l));
        s.food = t.contains("food") ? t.getInt("food") : 30;
        s.works = t.getInt("works");
        s.lastSign = t.getLong("last_sign");
        s.blessedWithChild = t.getBoolean("blessed_child");
        CompoundTag pt = t.getCompound("petitions");
        for (String k : pt.getAllKeys()) {
            try {
                s.petitions.put(Faiths.Petition.valueOf(k), pt.getString(k));
            } catch (IllegalArgumentException ignored) {
            }
        }
        s.petitionsSince = t.getLong("petitions_since");
        s.name = t.getString("name");
        s.founded = t.getLong("founded");
        s.founders = t.getString("founders");
        int[] styles = t.getIntArray("house_styles");
        for (int i = 0; i < s.houses.size(); i++) s.houseStyles.add(i < styles.length ? styles[i] : -1);
        s.templeStyle = t.getInt("temple_style");
        s.layout = t.getInt("layout");
        return s;
    }
}
