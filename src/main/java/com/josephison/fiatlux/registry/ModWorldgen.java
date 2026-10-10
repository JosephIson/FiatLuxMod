package com.josephison.fiatlux.registry;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.world.HeavenChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, FiatLux.MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<HeavenChunkGenerator>> HEAVEN =
            CHUNK_GENERATORS.register("heaven", () -> HeavenChunkGenerator.CODEC);

    private ModWorldgen() {}
}
