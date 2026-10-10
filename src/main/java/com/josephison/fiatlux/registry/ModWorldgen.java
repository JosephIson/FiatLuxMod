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

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<com.josephison.fiatlux.realm.UnderworldChunkGenerator>> UNDERWORLD =
            CHUNK_GENERATORS.register("underworld", () -> com.josephison.fiatlux.realm.UnderworldChunkGenerator.CODEC);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<com.josephison.fiatlux.realm.PlanetChunkGenerator>> PLANET =
            CHUNK_GENERATORS.register("planet", () -> com.josephison.fiatlux.realm.PlanetChunkGenerator.CODEC);

    private ModWorldgen() {}
}
