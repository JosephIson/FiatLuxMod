package com.josephison.fiatlux.world;

import com.josephison.fiatlux.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Heaven, after Revelation 21: an endless radiant sky of floating cloud islands threaded by
 * "the street of the city ... pure gold, as it were transparent glass" (21:21) - a grid of
 * golden streets on jasper kerbs (21:18), lit by lamp pillars, crossing the void as bridges.
 * Celestial crystal clusters crown the islands. Deterministic and seed-independent: Heaven is
 * the same for every world.
 */
public class HeavenChunkGenerator extends ChunkGenerator {
    public static final MapCodec<HeavenChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
            com.mojang.serialization.Codec.INT.optionalFieldOf("street_level", 120).forGetter(g -> g.level),
            com.mojang.serialization.Codec.INT.optionalFieldOf("street_spacing", 48).forGetter(g -> g.spacing)
    ).apply(i, i.stable(HeavenChunkGenerator::new)));

    private static final int STREET_WIDTH = 7;   // kerb + 5 gold + kerb
    private final int level;
    private final int spacing;
    private final SimplexNoise broad = new SimplexNoise(new LegacyRandomSource(0x5A1C7L));
    private final SimplexNoise detail = new SimplexNoise(new LegacyRandomSource(0xF1A7L));

    public HeavenChunkGenerator(BiomeSource biomeSource, int level, int spacing) {
        super(biomeSource);
        this.level = level;
        this.spacing = Math.max(16, spacing);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public int streetLevel() {
        return level;
    }

    // ------------------------------------------------------------------ the shape of Heaven

    private double islandNoise(int x, int z) {
        return broad.getValue(x / 110.0, z / 110.0) * 0.7 + detail.getValue(x / 28.0, z / 28.0) * 0.3;
    }

    /** Street lane offset across the street (0..STREET_WIDTH-1) or -1 if not on a street. */
    private int lane(int x, int z) {
        int ox = Math.floorMod(x + STREET_WIDTH / 2, spacing);
        int oz = Math.floorMod(z + STREET_WIDTH / 2, spacing);
        if (ox < STREET_WIDTH) return ox;
        if (oz < STREET_WIDTH) return oz;
        return -1;
    }

    private static long hash(int x, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        return h * 0x94D049BB133111EBL;
    }

    /** The block at (x, y, z) in Heaven. */
    public BlockState stateAt(int x, int y, int z) {
        int lane = lane(x, z);
        if (lane >= 0) {
            if (y == level) {
                return lane == 0 || lane == STREET_WIDTH - 1 ? ModBlocks.JASPER.get().defaultBlockState()
                        : ModBlocks.GOLDEN_STREET.get().defaultBlockState();
            }
            if (y == level - 1) {
                return ModBlocks.JASPER.get().defaultBlockState();               // foundation beam
            }
            boolean kerb = lane == 0 || lane == STREET_WIDTH - 1;
            int along = Math.floorMod(x + z, 12);
            if (kerb && along == 0 && y > level && y <= level + 4) {            // lamp pillars
                return y == level + 4 ? ModBlocks.LUX_BLOCK.get().defaultBlockState() : ModBlocks.JASPER.get().defaultBlockState();
            }
        }
        double n = islandNoise(x, z) - 0.12;
        if (n <= 0) return null;
        int top = level - 1 + (int) (n * 10);
        int bottom = level - 2 - (int) (Math.sqrt(n) * 46);
        if (y > top) {
            if (y == top + 1 && lane < 0 && (hash(x, z) & 127) == 0) {
                return ModBlocks.CELESTIAL_CRYSTAL.get().defaultBlockState();    // crystal clusters
            }
            return null;
        }
        if (y < bottom) return null;
        if (y >= top - 1) return ModBlocks.CLOUDSTONE.get().defaultBlockState();
        return (hash(x * 31 + y, z) & 31) == 0 ? ModBlocks.CELESTIAL_CRYSTAL.get().defaultBlockState()
                : ModBlocks.CLOUDSTONE.get().defaultBlockState();
    }

    private int columnTop(int x, int z, int minY, int maxY) {
        for (int y = Math.min(maxY, level + 12); y >= Math.max(minY, level - 60); y--) {
            if (stateAt(x, y, z) != null) return y;
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int minY = Math.max(chunk.getMinBuildHeight(), level - 60);
        int maxY = Math.min(chunk.getMaxBuildHeight() - 1, level + 12);
        int cx = chunk.getPos().getMinBlockX(), cz = chunk.getPos().getMinBlockZ();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockState s = stateAt(cx + lx, y, cz + lz);
                    if (s == null) continue;
                    chunk.setBlockState(pos.set(lx, y, lz), s, false);
                    ocean.update(lx, y, lz, s);
                    surface.update(lx, y, lz, s);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState random) {
        int top = columnTop(x, z, heightAccessor.getMinBuildHeight(), heightAccessor.getMaxBuildHeight() - 1);
        return top == Integer.MIN_VALUE ? heightAccessor.getMinBuildHeight() : top + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState random) {
        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        int min = heightAccessor.getMinBuildHeight();
        for (int y = Math.max(min, level - 60); y <= Math.min(heightAccessor.getMaxBuildHeight() - 1, level + 12); y++) {
            BlockState s = stateAt(x, y, z);
            if (s != null) states[y - min] = s;
        }
        return new NoiseColumn(min, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Heaven: street level " + level + ", lane " + lane(pos.getX(), pos.getZ())
                + ", island " + Mth.floor(islandNoise(pos.getX(), pos.getZ()) * 100) / 100.0);
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return 320;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }
}
