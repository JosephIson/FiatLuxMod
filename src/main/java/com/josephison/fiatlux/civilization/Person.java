package com.josephison.fiatlux.civilization;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One mortal's entry in the Book of Generations. It outlives the mortal: births, marriages,
 * trades, faith and death are written here, so a family can be traced back to Adam and Eve.
 */
public final class Person {
    public final UUID id;
    public final String name;
    public final boolean male;
    public final String world;
    public final long born;
    @Nullable public final UUID father, mother;
    @Nullable public UUID spouse;
    public long died = -1;
    public String profession = "";
    @Nullable public UUID religion;
    /** "heaven", "underworld", or "" while living. */
    public String soul = "";
    public int ageAtDeath;
    /** The soul walks its realm as a SoulEntity (so it is not brought forth twice). */
    public boolean embodied;
    /** Raised out of the Underworld by a Creator's mercy. */
    public boolean redeemed;
    /** The look the mortal had in life, kept by its soul. */
    public int variant = -1;
    /** The line this one carries on, if any (see {@link Genealogy}): "seth", "shem", "cain". */
    public String line = "";
    /** One of the Nephilim, son of a Watcher (Gen 6:4): at death its spirit wanders the earth. */
    public boolean nephil;
    /** Taken up living, without death (Enoch, Gen 5:24). */
    public boolean translated;

    public Person(UUID id, String name, boolean male, String world, long born, @Nullable UUID father, @Nullable UUID mother) {
        this.id = id;
        this.name = name;
        this.male = male;
        this.world = world;
        this.born = born;
        this.father = father;
        this.mother = mother;
    }

    public boolean living() {
        return died < 0;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putString("name", name);
        t.putBoolean("male", male);
        t.putString("world", world);
        t.putLong("born", born);
        if (father != null) t.putUUID("father", father);
        if (mother != null) t.putUUID("mother", mother);
        if (spouse != null) t.putUUID("spouse", spouse);
        t.putLong("died", died);
        t.putString("profession", profession);
        if (religion != null) t.putUUID("religion", religion);
        t.putString("soul", soul);
        t.putInt("age_at_death", ageAtDeath);
        t.putBoolean("embodied", embodied);
        t.putBoolean("redeemed", redeemed);
        t.putInt("variant", variant);
        if (!line.isEmpty()) t.putString("line", line);
        if (nephil) t.putBoolean("nephil", true);
        if (translated) t.putBoolean("translated", true);
        return t;
    }

    public static Person load(CompoundTag t) {
        Person p = new Person(t.getUUID("id"), t.getString("name"), t.getBoolean("male"), t.getString("world"), t.getLong("born"),
                t.hasUUID("father") ? t.getUUID("father") : null, t.hasUUID("mother") ? t.getUUID("mother") : null);
        if (t.hasUUID("spouse")) p.spouse = t.getUUID("spouse");
        p.died = t.getLong("died");
        p.profession = t.getString("profession");
        if (t.hasUUID("religion")) p.religion = t.getUUID("religion");
        p.soul = t.getString("soul");
        p.ageAtDeath = t.getInt("age_at_death");
        p.embodied = t.getBoolean("embodied");
        p.redeemed = t.getBoolean("redeemed");
        p.variant = t.contains("variant") ? t.getInt("variant") : -1;
        p.line = t.getString("line");
        p.nephil = t.getBoolean("nephil");
        p.translated = t.getBoolean("translated");
        return p;
    }
}
