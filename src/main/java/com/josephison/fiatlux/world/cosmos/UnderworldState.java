package com.josephison.fiatlux.world.cosmos;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Abstract state of the Underworld's infernal civilisation (design doc section 6): its population of
 * demons and sinner souls, the soul power that fuels it, accumulated knowledge, era, hierarchy
 * and soul contracts. Mutable; advanced by WorldTicker.
 */
public final class UnderworldState {
    public static final Codec<UnderworldState> CODEC = RecordCodecBuilder.create(i -> i.group(
            UnderworldEra.CODEC.optionalFieldOf("era", UnderworldEra.TRIBAL_PITS).forGetter(u -> u.era),
            Codec.DOUBLE.optionalFieldOf("knowledge", 0.0).forGetter(u -> u.knowledge),
            Codec.DOUBLE.optionalFieldOf("soul_power", 0.0).forGetter(u -> u.soulPower),
            Codec.DOUBLE.optionalFieldOf("demons", 0.0).forGetter(u -> u.demons),
            Codec.DOUBLE.optionalFieldOf("souls", 0.0).forGetter(u -> u.souls),
            Codec.INT.optionalFieldOf("overlords", 0).forGetter(u -> u.overlords),
            Codec.INT.optionalFieldOf("territories", 0).forGetter(u -> u.territories),
            Codec.DOUBLE.optionalFieldOf("contracts", 0.0).forGetter(u -> u.contracts),
            Codec.LONG.optionalFieldOf("last_cleanse_day", 0L).forGetter(u -> u.lastCleanseDay)
    ).apply(i, UnderworldState::new));

    public UnderworldEra era;
    public double knowledge;
    public double soulPower;
    public double demons;
    public double souls;
    public int overlords;
    public int territories;
    public double contracts;
    public long lastCleanseDay;

    public UnderworldState() {
        this(UnderworldEra.TRIBAL_PITS, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public UnderworldState(UnderworldEra era, double knowledge, double soulPower, double demons, double souls, int overlords,
                           int territories, double contracts, long lastCleanseDay) {
        this.era = era;
        this.knowledge = knowledge;
        this.soulPower = soulPower;
        this.demons = demons;
        this.souls = souls;
        this.overlords = overlords;
        this.territories = territories;
        this.contracts = contracts;
        this.lastCleanseDay = lastCleanseDay;
    }
}
