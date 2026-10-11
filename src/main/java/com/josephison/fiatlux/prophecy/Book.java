package com.josephison.fiatlux.prophecy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * One book of a people's scriptures: its title, who wrote it down, when, what kind of writing it is,
 * and its verses in order. Chapters are kept as verses beginning with {@link #CHAPTER}.
 */
public final class Book {
    /** A verse that opens a new chapter (its text, if any, is the chapter's heading). */
    public static final String CHAPTER = "§chapter ";

    public enum Kind {
        /** The account of the beginning, the garden, the fall, the generations and the flood (Genesis). */
        BEGINNINGS,
        /** The commandments and statutes of a people (Exodus 20, Deuteronomy). */
        LAW,
        /** The deeds of a people and its rulers (Judges, Kings, Chronicles). */
        CHRONICLES,
        /** The songs of the faithful (Psalms); for other faiths, their hymns. */
        PSALMS,
        /** The sayings of the wise (Proverbs); for other faiths, the sayings of their founder. */
        PROVERBS,
        /** The words and deeds of a prophet (Isaiah, Jeremiah, Jonah). */
        PROPHET,
        /** The doctrine over which a branch parted from the faith it came from. */
        TESTAMENT
    }

    public final String title;
    public final Kind kind;
    /** Who wrote it down (a scribe, or the prophet's disciple). */
    public String scribe;
    public final long written;
    /** The faith it was written in (a branch keeps the books of the faith it split from, with their own). */
    public final java.util.UUID faith;
    public final List<String> verses = new ArrayList<>();

    public Book(String title, Kind kind, String scribe, long written, java.util.UUID faith) {
        this.title = title;
        this.kind = kind;
        this.scribe = scribe;
        this.written = written;
        this.faith = faith;
    }

    public Book chapter(String heading) {
        verses.add(CHAPTER + heading);
        return this;
    }

    public Book verse(String text) {
        verses.add(text);
        return this;
    }

    /** How many verses there are (not counting the chapter headings). */
    public int length() {
        return (int) verses.stream().filter(v -> !v.startsWith(CHAPTER)).count();
    }

    /** The verse at `index` with its chapter and verse number ("3:16"), or null if it is a heading. */
    public String reference(int index) {
        int chapter = 1, verse = 0;
        boolean opened = false;
        for (int i = 0; i <= index && i < verses.size(); i++) {
            if (verses.get(i).startsWith(CHAPTER)) {
                if (opened) chapter++;
                opened = true;
                verse = 0;
            } else {
                verse++;
                opened = true;
            }
        }
        return verses.get(index).startsWith(CHAPTER) ? null : chapter + ":" + verse;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("title", title);
        t.putString("kind", kind.name());
        t.putString("scribe", scribe);
        t.putLong("written", written);
        t.putUUID("faith", faith);
        ListTag v = new ListTag();
        for (String s : verses) v.add(StringTag.valueOf(s));
        t.put("verses", v);
        return t;
    }

    public static Book load(CompoundTag t) {
        Kind kind;
        try {
            kind = Kind.valueOf(t.getString("kind"));
        } catch (IllegalArgumentException e) {
            kind = Kind.CHRONICLES;
        }
        Book b = new Book(t.getString("title"), kind, t.getString("scribe"), t.getLong("written"), t.getUUID("faith"));
        for (Tag v : t.getList("verses", Tag.TAG_STRING)) b.verses.add(v.getAsString());
        return b;
    }
}
