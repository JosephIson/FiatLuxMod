package com.josephison.fiatlux.world.cosmos;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

/**
 * A black hole in the Void: "the bottomless pit ... and there arose a smoke out of the pit, as the
 * smoke of a great furnace; and the sun and the air were darkened" (Revelation 9:1-2). It draws
 * everything near it inward, and whatever crosses its horizon falls through into the Underworld.
 *
 * @param radius event-horizon radius in blocks; its pull and its disk scale with it
 */
public record BlackHole(String name, double x, double y, double z, float radius) {
    public static final Codec<BlackHole> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(BlackHole::name),
            Codec.DOUBLE.fieldOf("x").forGetter(BlackHole::x),
            Codec.DOUBLE.fieldOf("y").forGetter(BlackHole::y),
            Codec.DOUBLE.fieldOf("z").forGetter(BlackHole::z),
            Codec.FLOAT.fieldOf("radius").forGetter(BlackHole::radius)
    ).apply(i, BlackHole::new));

    public Vec3 center() {
        return new Vec3(x, y, z);
    }
}
