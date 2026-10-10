package com.josephison.fiatlux.world.cosmos;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Kinds of world the Creator can form (design doc section 5). Habitability bounds mortal populations. */
public enum PlanetType implements StringRepresentable {
    EARTHLIKE("earthlike", 0xFF4C8CE0, 0xFF6FC26A, 1.0, 24000, 1.0, 1.0),
    VOLCANIC("volcanic", 0xFF7A2A14, 0xFFFF7A2A, 1.3, 20000, 0.12, 0.8),
    BARREN_MOON("barren_moon", 0xFF9A968E, 0xFFCFCAC0, 0.17, 48000, 0.02, 0.5),
    GAS_GIANT("gas_giant", 0xFFD8A866, 0xFFF2DDB0, 2.4, 10000, 0.0, 2.2),
    CRYSTALLINE("crystalline", 0xFF9AD8F0, 0xFFE6F8FF, 0.8, 30000, 0.6, 0.9);

    public static final Codec<PlanetType> CODEC = StringRepresentable.fromEnum(PlanetType::values);

    private final String id;
    public final int baseColor;
    public final int accentColor;
    public final double gravity;
    public final int dayLength;
    public final double habitability;
    public final double size;

    PlanetType(String id, int baseColor, int accentColor, double gravity, int dayLength, double habitability, double size) {
        this.id = id;
        this.baseColor = baseColor;
        this.accentColor = accentColor;
        this.gravity = gravity;
        this.dayLength = dayLength;
        this.habitability = habitability;
        this.size = size;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static PlanetType byName(String name) {
        for (PlanetType t : values()) if (t.id.equals(name)) return t;
        return null;
    }
}
