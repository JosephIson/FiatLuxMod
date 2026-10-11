package com.josephison.fiatlux.civilization;

import com.josephison.fiatlux.world.cosmos.MortalEra;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * What a mortal does with its days, chosen from the trades of its people's age. A trade decides
 * where it works and what that work does to the settlement: farmers tend and harvest the field,
 * gatherers and hunters bring in food, builders lay paths, set lights and raise new houses as the
 * people grow, and priests keep the shrine and lead the evening prayer.
 */
public enum Profession {
    GATHERER("gatherer", MortalEra.HUNTER_GATHERER, MortalEra.AGRARIAN),
    HUNTER("hunter", MortalEra.HUNTER_GATHERER, MortalEra.CLASSICAL),
    FARMER("farmer", MortalEra.AGRARIAN, MortalEra.SPACEFARING),
    SHEPHERD("shepherd", MortalEra.AGRARIAN, MortalEra.MEDIEVAL),
    BUILDER("builder", MortalEra.HUNTER_GATHERER, MortalEra.SPACEFARING),
    PRIEST("priest", MortalEra.HUNTER_GATHERER, MortalEra.SPACEFARING),
    SCHOLAR("scholar", MortalEra.CLASSICAL, MortalEra.SPACEFARING),
    MERCHANT("merchant", MortalEra.CLASSICAL, MortalEra.INDUSTRIAL),
    SMITH("smith", MortalEra.MEDIEVAL, MortalEra.INDUSTRIAL),
    ENGINEER("engineer", MortalEra.INDUSTRIAL, MortalEra.SPACEFARING),
    PILOT("pilot", MortalEra.SPACEFARING, MortalEra.SPACEFARING);

    public final String id;
    private final MortalEra from, to;

    Profession(String id, MortalEra from, MortalEra to) {
        this.id = id;
        this.from = from;
        this.to = to;
    }

    public boolean availableIn(MortalEra era) {
        return era.ordinal() >= from.ordinal() && era.ordinal() <= to.ordinal();
    }

    /** Does this trade bring food to the settlement's store? */
    public boolean feeds() {
        return this == GATHERER || this == HUNTER || this == FARMER || this == SHEPHERD;
    }

    public static Profession pick(MortalEra era, RandomSource random) {
        List<Profession> options = new ArrayList<>();
        for (Profession p : values()) {
            if (!p.availableIn(era)) continue;
            int weight = p.feeds() ? 3 : p == PRIEST ? 1 : 2;   // most hands bring in food
            for (int i = 0; i < weight; i++) options.add(p);
        }
        return options.get(random.nextInt(options.size()));
    }

    /** The trade a mortal moves to when its people enter a new age and its old trade is gone. */
    public Profession adapt(MortalEra era, RandomSource random) {
        if (availableIn(era)) return this;
        if (this == GATHERER || this == HUNTER || this == SHEPHERD) return availableIn(era) ? this : FARMER.availableIn(era) ? FARMER : pick(era, random);
        return pick(era, random);
    }

    public static Profession byId(String id) {
        for (Profession p : values()) if (p.id.equals(id)) return p;
        return GATHERER;
    }
}
