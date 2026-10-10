package com.josephison.fiatlux.world.cosmos;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** The Underworld's technological eras (design doc section 6). */
public enum UnderworldEra implements StringRepresentable {
    /** Primitive bone and obsidian camps. */
    TRIBAL_PITS("tribal_pits"),
    /** Dark gothic fortresses, walled bastions and bladed weaponry. */
    FEUDAL_CASTLE("feudal_castle"),
    /** Steam-driven soul engines, brimstone foundries, iron tracks and workshops. */
    INDUSTRIAL("industrial"),
    /** Neon abyssal megacities, electrical grids, automated foundries and digital soul-contract networks. */
    MODERN_CYBERNETIC("modern_cybernetic");

    public static final Codec<UnderworldEra> CODEC = StringRepresentable.fromEnum(UnderworldEra::values);
    private final String id;

    UnderworldEra(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
