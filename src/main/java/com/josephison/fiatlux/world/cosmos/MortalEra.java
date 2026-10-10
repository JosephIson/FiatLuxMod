package com.josephison.fiatlux.world.cosmos;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** The ages a mortal civilisation passes through (design doc section 7). Threshold = era progress needed to leave it. */
public enum MortalEra implements StringRepresentable {
    HUNTER_GATHERER("hunter_gatherer", 100, 0.85),
    AGRARIAN("agrarian", 400, 0.8),
    CLASSICAL("classical", 1500, 0.7),
    MEDIEVAL("medieval", 5000, 0.75),
    INDUSTRIAL("industrial", 15000, 0.5),
    SPACEFARING("spacefaring", Double.MAX_VALUE, 0.45);

    public static final Codec<MortalEra> CODEC = StringRepresentable.fromEnum(MortalEra::values);
    private final String id;
    public final double threshold;
    /** Share of the population that prays in this age. */
    public final double piety;

    MortalEra(String id, double threshold, double piety) {
        this.id = id;
        this.threshold = threshold;
        this.piety = piety;
    }

    public MortalEra next() {
        return this == SPACEFARING ? this : values()[ordinal() + 1];
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
