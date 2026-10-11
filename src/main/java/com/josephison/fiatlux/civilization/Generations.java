package com.josephison.fiatlux.civilization;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "This is the book of the generations of Adam" (Genesis 5:1): every mortal that has lived, and
 * every faith they have held, persisted for the whole world (SavedData).
 */
public final class Generations extends SavedData {
    private static final String ID = "fiatlux_generations";
    public final Map<UUID, Person> people = new LinkedHashMap<>();
    public final Map<UUID, Religion> religions = new LinkedHashMap<>();

    public static Generations get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(Generations::new, Generations::load, null), ID);
    }

    @Nullable
    public Person person(@Nullable UUID id) {
        return id == null ? null : people.get(id);
    }

    @Nullable
    public Religion religion(@Nullable UUID id) {
        return id == null ? null : religions.get(id);
    }

    public List<Person> childrenOf(UUID parent) {
        List<Person> out = new ArrayList<>();
        for (Person p : people.values()) if (parent.equals(p.father) || parent.equals(p.mother)) out.add(p);
        out.sort((a, b) -> Long.compare(a.born, b.born));
        return out;
    }

    public void add(Person p) {
        people.put(p.id, p);
        setDirty();
    }

    public void add(Religion r) {
        religions.put(r.id, r);
        setDirty();
    }

    /** The true faith of one Creator on one world, made the first time it is needed. */
    public Religion faithOf(UUID creator, String creatorName, String world, long now) {
        for (Religion r : religions.values()) {
            if (r.deity == Religion.Deity.CREATOR && creator.equals(r.creator) && r.world.equals(world)) return r;
        }
        Religion r = new Religion(UUID.randomUUID(), "The Way of " + creatorName, Religion.Deity.CREATOR, creatorName, creator,
                creatorName, world, now);
        add(r);
        return r;
    }

    /** Followers among the living, by religion. */
    public Map<UUID, Integer> followers() {
        Map<UUID, Integer> out = new LinkedHashMap<>();
        for (Person p : people.values()) if (p.living() && p.religion != null) out.merge(p.religion, 1, Integer::sum);
        return out;
    }

    private static Generations load(CompoundTag tag, HolderLookup.Provider registries) {
        Generations g = new Generations();
        for (Tag t : tag.getList("people", Tag.TAG_COMPOUND)) {
            Person p = Person.load((CompoundTag) t);
            g.people.put(p.id, p);
        }
        for (Tag t : tag.getList("religions", Tag.TAG_COMPOUND)) {
            Religion r = Religion.load((CompoundTag) t);
            g.religions.put(r.id, r);
        }
        return g;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag people = new ListTag();
        this.people.values().forEach(p -> people.add(p.save()));
        tag.put("people", people);
        ListTag religions = new ListTag();
        this.religions.values().forEach(r -> religions.add(r.save()));
        tag.put("religions", religions);
        return tag;
    }
}
