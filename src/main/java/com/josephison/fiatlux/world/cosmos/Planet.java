package com.josephison.fiatlux.world.cosmos;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

/**
 * A created world orbiting in the Cosmic Expanse, with the abstract simulation state of its
 * mortal civilisation. Mutable: WorldTicker advances it in place; persisted by codec.
 */
public final class Planet {
    public static final Codec<Planet> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(p -> p.id),
            Codec.STRING.fieldOf("name").forGetter(p -> p.name),
            PlanetType.CODEC.fieldOf("type").forGetter(p -> p.type),
            Codec.INT.fieldOf("galaxy").forGetter(p -> p.galaxy),
            Codec.DOUBLE.fieldOf("orbit_radius").forGetter(p -> p.orbitRadius),
            Codec.DOUBLE.fieldOf("orbit_period_days").forGetter(p -> p.orbitPeriodDays),
            Codec.DOUBLE.fieldOf("phase").forGetter(p -> p.phase),
            Codec.DOUBLE.fieldOf("inclination").forGetter(p -> p.inclination),
            Codec.DOUBLE.fieldOf("gravity").forGetter(p -> p.gravity),
            Codec.INT.fieldOf("day_length").forGetter(p -> p.dayLength),
            Codec.INT.fieldOf("atmosphere").forGetter(p -> p.atmosphere),
            Codec.DOUBLE.optionalFieldOf("population", 0.0).forGetter(p -> p.population),
            Codec.DOUBLE.optionalFieldOf("faith", 0.0).forGetter(p -> p.faith),
            Codec.DOUBLE.optionalFieldOf("sin", 0.0).forGetter(p -> p.sin),
            MortalEra.CODEC.optionalFieldOf("era", MortalEra.HUNTER_GATHERER).forGetter(p -> p.era),
            Codec.DOUBLE.optionalFieldOf("era_progress", 0.0).forGetter(p -> p.eraProgress)
    ).apply(i, Planet::new));

    public final UUID id;
    public final String name;
    public final PlanetType type;
    public final int galaxy;
    public final double orbitRadius;
    public final double orbitPeriodDays;
    public double phase;
    public final double inclination;
    public final double gravity;
    public final int dayLength;
    public final int atmosphere;
    public double population;
    public double faith;
    public double sin;
    public MortalEra era;
    public double eraProgress;

    public Planet(UUID id, String name, PlanetType type, int galaxy, double orbitRadius, double orbitPeriodDays, double phase,
                  double inclination, double gravity, int dayLength, int atmosphere, double population, double faith, double sin,
                  MortalEra era, double eraProgress) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.galaxy = galaxy;
        this.orbitRadius = orbitRadius;
        this.orbitPeriodDays = Math.max(0.05, orbitPeriodDays);
        this.phase = phase;
        this.inclination = inclination;
        this.gravity = gravity;
        this.dayLength = dayLength;
        this.atmosphere = atmosphere;
        this.population = population;
        this.faith = faith;
        this.sin = sin;
        this.era = era;
        this.eraProgress = eraProgress;
    }

    public boolean hasLife() {
        return population >= 1.0;
    }

    /** Carrying capacity for mortal life (0 for uninhabitable worlds). */
    public double capacity() {
        return type.habitability * type.size * 1_000_000.0;
    }
}
