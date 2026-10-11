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

    public Settlement(BlockPos centre) {
        this.centre = centre;
        this.shrine = centre.offset(0, 0, -8);
    }

    /** Room for this many sleepers in the houses (two to a bed's house). */
    public int capacity() {
        return Math.max(4, beds.size() * 2);
    }

    /** Where the n-th house would stand: on rings around the hearth, clear of shrine and field. */
    public BlockPos houseSpot(int n) {
        int ring = n / 6;
        double r = 10 + ring * 7;
        double a = Math.PI * 2 * (n % 6) / 6 + ring * 0.5 + 0.3;
        return centre.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
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
        return t;
    }

    public static Settlement load(CompoundTag t) {
        Settlement s = new Settlement(BlockPos.of(t.getLong("centre")));
        if (t.contains("shrine")) s.shrine = BlockPos.of(t.getLong("shrine"));
        if (t.contains("field")) s.field = BlockPos.of(t.getLong("field"));
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
        return s;
    }
}
