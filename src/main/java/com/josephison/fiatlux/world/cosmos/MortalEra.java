package com.josephison.fiatlux.world.cosmos;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The ages a mortal civilisation passes through (design doc section 7). Each age can feed so many
 * people (on a world of full habitability), grows and dies at its own rate per mortal year, and is
 * left only after enough years in it once the people are numerous enough for the next.
 */
public enum MortalEra implements StringRepresentable {
    //               id                minYears  piety  capacity   growth  mortality  people to enter  per town
    HUNTER_GATHERER("hunter_gatherer", 15, 0.85, 3_000, 0.050, 0.020, 0, 20),
    AGRARIAN("agrarian", 40, 0.8, 120_000, 0.055, 0.022, 40, 40),
    CLASSICAL("classical", 80, 0.7, 2_000_000, 0.045, 0.020, 400, 90),
    MEDIEVAL("medieval", 150, 0.75, 20_000_000, 0.035, 0.022, 4_000, 160),
    INDUSTRIAL("industrial", 60, 0.5, 900_000_000, 0.050, 0.012, 100_000, 500),
    SPACEFARING("spacefaring", Double.MAX_VALUE, 0.45, 9_000_000_000.0, 0.020, 0.008, 1_000_000, 900);

    public static final Codec<MortalEra> CODEC = StringRepresentable.fromEnum(MortalEra::values);
    private final String id;
    /** Mortal years a people must spend in this age before it can leave it. */
    public final double minYears;
    /** Share of the population that prays in this age. */
    public final double piety;
    /** How many people this age can feed on a world of full habitability. */
    public final double capacity;
    /** Births and deaths per person per mortal year. */
    public final double growth, mortality;
    /** How many people a world needs before its people can enter this age. */
    public final double minPopulation;
    /** People to a town: how many it takes before they spread out and found another. */
    public final int perTown;

    MortalEra(String id, double minYears, double piety, double capacity, double growth, double mortality, double minPopulation, int perTown) {
        this.id = id;
        this.minYears = minYears;
        this.piety = piety;
        this.capacity = capacity;
        this.growth = growth;
        this.mortality = mortality;
        this.minPopulation = minPopulation;
        this.perTown = perTown;
    }

    public MortalEra next() {
        return this == SPACEFARING ? this : values()[ordinal() + 1];
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
