package com.josephison.fiatlux.prophecy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One called to speak for a god: "the word of the LORD came unto" them (Jer 1:4). What they said and
 * did is kept, to be written down as their book when they are gone.
 */
public final class Prophet {
    public final UUID id;
    public final String name;
    public final boolean male;
    public final String world;
    public final UUID faith;
    /** Who called them (a Creator's name, or the god a false faith calls on). */
    public final String calledBy;
    public final long called;
    /** A prophet of a false god: "the prophets prophesied by Baal, and walked after things that do not profit" (Jer 2:8). */
    public final boolean false_;
    /** Their oracles, as they will be written. */
    public final List<String> oracles = new ArrayList<>();
    /** What was done by them, and to them. */
    public final List<String> deeds = new ArrayList<>();
    /** The one who follows them and will take up their mantle (2 Kings 2:9-15). */
    @Nullable public UUID disciple;
    public String discipleName = "";
    /** "" while they live; else "died", "martyred" or "taken up". */
    public String fate = "";
    /** The town they are going to or preaching in now. */
    @Nullable public BlockPos visiting;
    public int preached, visits, wonders;
    public long lastWord, lastWonder, arrived;
    /** Ticks on the road to the town they are going to. */
    public int road;

    public Prophet(UUID id, String name, boolean male, String world, UUID faith, String calledBy, long called, boolean false_) {
        this.id = id;
        this.name = name;
        this.male = male;
        this.world = world;
        this.faith = faith;
        this.calledBy = calledBy;
        this.called = called;
        this.false_ = false_;
    }

    public boolean living() {
        return fate.isEmpty();
    }

    public String he() {
        return male ? "he" : "she";
    }

    public String his() {
        return male ? "his" : "her";
    }

    public String him() {
        return male ? "him" : "her";
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putString("name", name);
        t.putBoolean("male", male);
        t.putString("world", world);
        t.putUUID("faith", faith);
        t.putString("called_by", calledBy);
        t.putLong("called", called);
        t.putBoolean("false", false_);
        ListTag o = new ListTag();
        oracles.forEach(s -> o.add(StringTag.valueOf(s)));
        t.put("oracles", o);
        ListTag d = new ListTag();
        deeds.forEach(s -> d.add(StringTag.valueOf(s)));
        t.put("deeds", d);
        if (disciple != null) t.putUUID("disciple", disciple);
        t.putString("disciple_name", discipleName);
        t.putString("fate", fate);
        if (visiting != null) t.putLong("visiting", visiting.asLong());
        t.putInt("preached", preached);
        t.putInt("visits", visits);
        t.putInt("wonders", wonders);
        t.putLong("last_word", lastWord);
        t.putLong("last_wonder", lastWonder);
        return t;
    }

    public static Prophet load(CompoundTag t) {
        Prophet p = new Prophet(t.getUUID("id"), t.getString("name"), t.getBoolean("male"), t.getString("world"), t.getUUID("faith"),
                t.getString("called_by"), t.getLong("called"), t.getBoolean("false"));
        for (Tag x : t.getList("oracles", Tag.TAG_STRING)) p.oracles.add(x.getAsString());
        for (Tag x : t.getList("deeds", Tag.TAG_STRING)) p.deeds.add(x.getAsString());
        if (t.hasUUID("disciple")) p.disciple = t.getUUID("disciple");
        p.discipleName = t.getString("disciple_name");
        p.fate = t.getString("fate");
        if (t.contains("visiting")) p.visiting = BlockPos.of(t.getLong("visiting"));
        p.preached = t.getInt("preached");
        p.visits = t.getInt("visits");
        p.wonders = t.getInt("wonders");
        p.lastWord = t.getLong("last_word");
        p.lastWonder = t.getLong("last_wonder");
        return p;
    }
}
