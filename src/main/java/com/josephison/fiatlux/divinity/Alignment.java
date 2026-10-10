package com.josephison.fiatlux.divinity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Moral record carried by any living being as a data attachment. Divine weapons and the Host
 * read it: high sin marks a being as corrupt; virtue shields it from holy fire.
 */
public record Alignment(int virtue, int sin) {
    public static final Alignment NEUTRAL = new Alignment(0, 0);
    public static final Codec<Alignment> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("virtue", 0).forGetter(Alignment::virtue),
            Codec.INT.optionalFieldOf("sin", 0).forGetter(Alignment::sin)
    ).apply(i, Alignment::new));

    public boolean isCorrupt() {
        return sin > virtue + 10;
    }

    public Alignment addSin(int n) {
        return new Alignment(virtue, sin + n);
    }

    public Alignment addVirtue(int n) {
        return new Alignment(virtue + n, sin);
    }
}
