package com.josephison.fiatlux.world;

import com.josephison.fiatlux.FiatLux;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public final class FiatLuxDimensions {
    /** Heaven: the radiant hub realm where every Creator begins. Defined in data/fiatlux/dimension/heaven.json. */
    public static final ResourceKey<Level> HEAVEN = ResourceKey.create(Registries.DIMENSION, FiatLux.id("heaven"));
    public static final ResourceKey<DimensionType> HEAVEN_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, FiatLux.id("heaven"));

    /** The Underworld: a realm of its own, separate from the vanilla Nether (design doc section 6). */
    public static final ResourceKey<Level> UNDERWORLD = ResourceKey.create(Registries.DIMENSION, FiatLux.id("underworld"));

    /** The Void: the formless deep beyond Heaven where the universe begins (Genesis 1:2). */
    public static final ResourceKey<Level> VOID = ResourceKey.create(Registries.DIMENSION, FiatLux.id("the_void"));

    private FiatLuxDimensions() {}
}
