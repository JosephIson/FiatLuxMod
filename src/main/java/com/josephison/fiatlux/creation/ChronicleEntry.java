package com.josephison.fiatlux.creation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** One line of the world's Chronicle: when, who, what kind of act, and its description. */
public record ChronicleEntry(long gameTime, String actor, String kind, String text) {
    public static final Codec<ChronicleEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("t").forGetter(ChronicleEntry::gameTime),
            Codec.STRING.fieldOf("actor").forGetter(ChronicleEntry::actor),
            Codec.STRING.fieldOf("kind").forGetter(ChronicleEntry::kind),
            Codec.STRING.fieldOf("text").forGetter(ChronicleEntry::text)
    ).apply(i, ChronicleEntry::new));
}
