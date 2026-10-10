package com.josephison.fiatlux.world.cosmos;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A galaxy formed by the Big Bang, positioned on the celestial sphere.
 *
 * @param yaw,pitch  direction on the sky (degrees)
 * @param size       apparent size
 * @param hue        ARGB core colour
 * @param arms       spiral arm count (0 = elliptical)
 * @param twist      spiral tightness
 */
public record Galaxy(String name, float yaw, float pitch, float size, int hue, int arms, float twist) {
    public static final Codec<Galaxy> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(Galaxy::name),
            Codec.FLOAT.fieldOf("yaw").forGetter(Galaxy::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(Galaxy::pitch),
            Codec.FLOAT.fieldOf("size").forGetter(Galaxy::size),
            Codec.INT.fieldOf("hue").forGetter(Galaxy::hue),
            Codec.INT.fieldOf("arms").forGetter(Galaxy::arms),
            Codec.FLOAT.fieldOf("twist").forGetter(Galaxy::twist)
    ).apply(i, Galaxy::new));
}
