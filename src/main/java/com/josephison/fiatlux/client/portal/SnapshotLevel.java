package com.josephison.fiatlux.client.portal;

import com.josephison.fiatlux.portal.RegionSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/**
 * A snapshot of the far side of a portal, standing in for a level while its blocks are turned into
 * geometry: block states and block light from the snapshot (positions are in the snapshot's own
 * coordinates, 0..size), solid beyond its edges so that their faces are not drawn.
 */
final class SnapshotLevel implements BlockAndTintGetter {
    private static final BlockState BEYOND = Blocks.STONE.defaultBlockState();
    private final RegionSnapshot s;
    private final LevelLightEngine light;
    @Nullable private final Biome biome;
    private final int minLight;
    private final boolean daylight;

    /** `daylight`: the far side lies open under the sun (a living world), not in the gloom of the deep. */
    SnapshotLevel(RegionSnapshot s, @Nullable String biomeId, int minLight, boolean daylight) {
        this.s = s;
        this.minLight = minLight;
        this.daylight = daylight;
        this.light = new LevelLightEngine(new LightChunkGetter() {
            @Override
            public LightChunk getChunkForLighting(int x, int z) {
                return null;
            }

            @Override
            public BlockGetter getLevel() {
                return SnapshotLevel.this;
            }
        }, false, false);
        Biome b = null;
        var level = Minecraft.getInstance().level;
        if (level != null && biomeId != null) {
            b = level.registryAccess().registryOrThrow(Registries.BIOME).get(net.minecraft.resources.ResourceLocation.parse(biomeId));
        }
        this.biome = b;
    }

    int stateIndex(BlockPos p) {
        return s.inside(p.getX(), p.getY(), p.getZ()) ? s.states[s.index(p.getX(), p.getY(), p.getZ())] : RegionSnapshot.HIDDEN;
    }

    @Override
    public BlockState getBlockState(BlockPos p) {
        if (!s.inside(p.getX(), p.getY(), p.getZ())) return BEYOND;
        return s.palette[s.states[s.index(p.getX(), p.getY(), p.getZ())]];
    }

    @Override
    public FluidState getFluidState(BlockPos p) {
        return getBlockState(p).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos p) {
        return null;
    }

    @Override
    public int getHeight() {
        return s.sy;
    }

    @Override
    public int getMinBuildHeight() {
        return 0;
    }

    @Override
    public float getShade(Direction d, boolean shade) {
        if (!shade) return 1f;
        return switch (d) {
            case DOWN -> 0.5f;
            case UP -> 1f;
            case NORTH, SOUTH -> 0.8f;
            default -> 0.6f;
        };
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return light;
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos p) {
        if (layer == LightLayer.SKY) return daylight ? 15 : 0;
        int l = s.inside(p.getX(), p.getY(), p.getZ()) ? s.light[s.index(p.getX(), p.getY(), p.getZ())] : 0;
        return Math.max(minLight, l);
    }

    @Override
    public int getRawBrightness(BlockPos p, int amount) {
        return Math.max(0, getBrightness(LightLayer.BLOCK, p) - amount);
    }

    @Override
    public int getBlockTint(BlockPos p, ColorResolver resolver) {
        return biome == null ? 0xFFFFFF : resolver.getColor(biome, p.getX(), p.getZ());
    }
}
