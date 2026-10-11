package com.josephison.fiatlux.prophecy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Surely the Lord GOD will do nothing, but he revealeth his secret unto his servants the prophets"
 * (Amos 3:7). Every prophet who has been called, what they foretold and whether it came to pass,
 * the annals of the great acts from which each people writes its scriptures, and the books written.
 */
public final class ProphecyData extends SavedData {
    private static final String ID = "fiatlux_prophecy";

    /** One great act, kept for the scribes (the Chronicle forgets; the annals do not). */
    public record Annal(long time, String actor, String kind, String text) {
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putLong("t", time);
            t.putString("a", actor);
            t.putString("k", kind);
            t.putString("x", text);
            return t;
        }

        static Annal load(CompoundTag t) {
            return new Annal(t.getLong("t"), t.getString("a"), t.getString("k"), t.getString("x"));
        }
    }

    /** "Yet forty days, and Nineveh shall be overthrown" (Jonah 3:4): a judgment foretold on a town. */
    public static final class Foretelling {
        public final UUID prophet;
        public final String prophetName, world, town, faithName;
        public final UUID faith;
        public final BlockPos centre;
        public final long deadline;
        /** How many in the town turned from their ways after it was spoken. */
        public int repented;
        /** "" while it waits; then "fulfilled", "spared" or "failed". */
        public String outcome = "";

        public Foretelling(UUID prophet, String prophetName, String world, String town, UUID faith, String faithName, BlockPos centre, long deadline) {
            this.prophet = prophet;
            this.prophetName = prophetName;
            this.world = world;
            this.town = town;
            this.faith = faith;
            this.faithName = faithName;
            this.centre = centre;
            this.deadline = deadline;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("prophet", prophet);
            t.putString("prophet_name", prophetName);
            t.putString("world", world);
            t.putString("town", town);
            t.putUUID("faith", faith);
            t.putString("faith_name", faithName);
            t.putLong("centre", centre.asLong());
            t.putLong("deadline", deadline);
            t.putInt("repented", repented);
            t.putString("outcome", outcome);
            return t;
        }

        static Foretelling load(CompoundTag t) {
            Foretelling f = new Foretelling(t.getUUID("prophet"), t.getString("prophet_name"), t.getString("world"), t.getString("town"),
                    t.getUUID("faith"), t.getString("faith_name"), BlockPos.of(t.getLong("centre")), t.getLong("deadline"));
            f.repented = t.getInt("repented");
            f.outcome = t.getString("outcome");
            return f;
        }
    }

    public final Map<UUID, Prophet> prophets = new LinkedHashMap<>();
    public final List<Foretelling> foretellings = new ArrayList<>();
    public final List<Annal> annals = new ArrayList<>();
    public final List<Book> books = new ArrayList<>();
    /** The words spoken of one who is to come (Isaiah 7:14, 9:6; Micah 5:2), as "prophet: words". */
    public final List<String> ofTheSon = new ArrayList<>();
    /** When a prophet was last raised on each world. */
    public final Map<String, Long> lastCalled = new HashMap<>();
    /** Whether the annals have been gathered from the Chronicle as it stood before they were kept. */
    public boolean seeded;
    public int wonders, contestsWon, contestsLost;

    public static ProphecyData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ProphecyData::new, ProphecyData::load, null), ID);
    }

    @Nullable
    public Prophet prophet(@Nullable UUID id) {
        return id == null ? null : prophets.get(id);
    }

    /** The living prophet this one follows as a disciple, if any. */
    @Nullable
    public Prophet masterOf(UUID disciple) {
        for (Prophet p : prophets.values()) if (p.living() && disciple.equals(p.disciple)) return p;
        return null;
    }

    public List<Book> booksOf(UUID faith) {
        List<Book> out = new ArrayList<>();
        for (Book b : books) if (b.faith.equals(faith)) out.add(b);
        return out;
    }

    @Nullable
    public Book book(UUID faith, Book.Kind kind) {
        for (Book b : books) if (b.faith.equals(faith) && b.kind == kind) return b;
        return null;
    }

    private static ProphecyData load(CompoundTag tag, HolderLookup.Provider registries) {
        ProphecyData d = new ProphecyData();
        for (Tag t : tag.getList("prophets", Tag.TAG_COMPOUND)) {
            Prophet p = Prophet.load((CompoundTag) t);
            d.prophets.put(p.id, p);
        }
        for (Tag t : tag.getList("foretellings", Tag.TAG_COMPOUND)) d.foretellings.add(Foretelling.load((CompoundTag) t));
        for (Tag t : tag.getList("annals", Tag.TAG_COMPOUND)) d.annals.add(Annal.load((CompoundTag) t));
        for (Tag t : tag.getList("books", Tag.TAG_COMPOUND)) d.books.add(Book.load((CompoundTag) t));
        for (Tag t : tag.getList("of_the_son", Tag.TAG_STRING)) d.ofTheSon.add(t.getAsString());
        CompoundTag lc = tag.getCompound("last_called");
        for (String k : lc.getAllKeys()) d.lastCalled.put(k, lc.getLong(k));
        d.seeded = tag.getBoolean("seeded");
        d.wonders = tag.getInt("wonders");
        d.contestsWon = tag.getInt("contests_won");
        d.contestsLost = tag.getInt("contests_lost");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag p = new ListTag();
        prophets.values().forEach(x -> p.add(x.save()));
        tag.put("prophets", p);
        ListTag f = new ListTag();
        foretellings.forEach(x -> f.add(x.save()));
        tag.put("foretellings", f);
        ListTag a = new ListTag();
        annals.forEach(x -> a.add(x.save()));
        tag.put("annals", a);
        ListTag b = new ListTag();
        books.forEach(x -> b.add(x.save()));
        tag.put("books", b);
        ListTag s = new ListTag();
        ofTheSon.forEach(x -> s.add(StringTag.valueOf(x)));
        tag.put("of_the_son", s);
        CompoundTag lc = new CompoundTag();
        lastCalled.forEach(lc::putLong);
        tag.put("last_called", lc);
        tag.putBoolean("seeded", seeded);
        tag.putInt("wonders", wonders);
        tag.putInt("contests_won", contestsWon);
        tag.putInt("contests_lost", contestsLost);
        return tag;
    }
}
