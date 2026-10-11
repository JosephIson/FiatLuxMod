package com.josephison.fiatlux.civilization;

/**
 * A mortal's temperament. Each mortal has two; they colour what it says, how often it prays,
 * how it treats its neighbours, and how it meets its Creator.
 */
public enum Trait {
    PIOUS("pious", 2.0, 0.0),
    KIND("kind", 1.0, 0.0),
    CURIOUS("curious", 1.0, 0.0),
    BRAVE("brave", 1.0, 0.0),
    FEARFUL("fearful", 1.2, 0.0),
    JOYFUL("joyful", 1.0, 0.0),
    INDUSTRIOUS("industrious", 0.9, 0.0),
    LAZY("lazy", 0.8, 0.02),
    PROUD("proud", 0.4, 0.05),
    GREEDY("greedy", 0.6, 0.06);

    public final String id;
    /** How readily this temperament turns to prayer. */
    public final double piety;
    /** Chance per day of a petty sin (a quarrel, a theft from the store). */
    public final double vice;

    Trait(String id, double piety, double vice) {
        this.id = id;
        this.piety = piety;
        this.vice = vice;
    }

    public static Trait byId(String id) {
        for (Trait t : values()) if (t.id.equals(id)) return t;
        return KIND;
    }
}
