package com.josephison.fiatlux.realm;

import com.josephison.fiatlux.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The Underworld (design doc section 6) - a realm of its own, not the vanilla Nether: yellow sulfur plains,
 * ridged obsidian mountains streaked with crying obsidian, abyssal rivers of black water in deep
 * channels, brimstone veins and lava pools over abyssal stone - and the infernal settlements of
 * {@link UnderworldSettlements}, built in the architecture of the era in which each region was
 * first explored.
 */
public class UnderworldChunkGenerator extends ChunkGenerator {
    public static final MapCodec<UnderworldChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
    ).apply(i, i.stable(UnderworldChunkGenerator::new)));

    public static final int BASE = 64;
    public static final int RIVER_FLOOR = 44;
    public static final int RIVER_LEVEL = 54;

    private final SimplexNoise broad = new SimplexNoise(new LegacyRandomSource(0x666L));
    private final SimplexNoise ridge = new SimplexNoise(new LegacyRandomSource(0xAB55L));
    private final SimplexNoise river = new SimplexNoise(new LegacyRandomSource(0x5717L));
    private final SimplexNoise detail = new SimplexNoise(new LegacyRandomSource(0xB12EL));

    public UnderworldChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ------------------------------------------------------------------ terrain

    private double riverValue(int x, int z) {
        return Math.abs(river.getValue(x / 220.0, z / 220.0) + 0.3 * river.getValue(x / 60.0 + 77, z / 60.0));
    }

    private double mountain(int x, int z) {
        double r = 1.0 - Math.abs(ridge.getValue(x / 150.0, z / 150.0));
        double mask = Math.max(0, broad.getValue(x / 400.0 + 31, z / 400.0) + 0.1);
        return Math.pow(r, 4) * 80 * Math.min(1, mask * 2.5);
    }

    /** Surface height, ignoring rivers. */
    public int landHeight(int x, int z) {
        return BASE + (int) (broad.getValue(x / 120.0, z / 120.0) * 8 + detail.getValue(x / 30.0, z / 30.0) * 3 + mountain(x, z));
    }

    /** Surface height including river channels; -1 for river/chasm columns (used to site settlements). */
    public int siteHeight(int x, int z) {
        if (riverValue(x, z) < 0.07) return -1;
        int h = landHeight(x, z);
        return mountain(x, z) > 6 ? -1 : h;
    }

    public BlockState terrainAt(int x, int y, int z) {
        if (y <= 1) return Blocks.BEDROCK.defaultBlockState();
        double rv = riverValue(x, z);
        int land = landHeight(x, z);
        int top = land;
        boolean riverbed = rv < 0.05;
        if (riverbed) {
            // banks slope down into the abyssal channel
            top = Math.min(land, RIVER_FLOOR + (int) (rv / 0.05 * (land - RIVER_FLOOR)));
        }
        if (y > top) {
            if (riverbed && y <= RIVER_LEVEL) return Blocks.WATER.defaultBlockState();
            return null;
        }
        double m = mountain(x, z);
        double d = detail.getValue(x / 14.0, y / 10.0, z / 14.0);
        if (y == top) {
            if (riverbed) return Blocks.BLACKSTONE.defaultBlockState();
            if (m > 12) return d > 0.55 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState();
            if (m > 4) return Blocks.BLACKSTONE.defaultBlockState();
            if (d > 0.62) return Blocks.MAGMA_BLOCK.defaultBlockState();
            return ModBlocks.SULFUR.get().defaultBlockState();
        }
        if (m > 12 && y > top - 6) return d > 0.5 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState();
        if (y > top - 3) return m > 4 ? Blocks.BLACKSTONE.defaultBlockState() : ModBlocks.SULFUR.get().defaultBlockState();
        if (d > 0.68) return ModBlocks.BRIMSTONE.get().defaultBlockState();
        if (y < 20 && d < -0.7) return Blocks.LAVA.defaultBlockState();
        return ModBlocks.ABYSSAL_STONE.get().defaultBlockState();
    }

    private List<UnderworldSettlements.Settlement> settlementsNear(int minX, int minZ) {
        List<UnderworldSettlements.Settlement> out = new ArrayList<>();
        int cx = Math.floorDiv(minX, UnderworldSettlements.CELL), cz = Math.floorDiv(minZ, UnderworldSettlements.CELL);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                var s = UnderworldSettlements.settlementIn(cx + i, cz + j, this::siteHeight);
                if (s == null) continue;
                int r = s.radius();
                if (s.cx() + r < minX || s.cx() - r > minX + 15 || s.cz() + r < minZ || s.cz() - r > minZ + 15) continue;
                out.add(s);
            }
        }
        return out;
    }

    public BlockState blockAt(int x, int y, int z, List<UnderworldSettlements.Settlement> near) {
        for (var s : near) {
            if (Math.abs(x - s.cx()) <= s.radius() && Math.abs(z - s.cz()) <= s.radius() && y >= s.baseY() - 1 && y <= s.top()) {
                BlockState b = UnderworldSettlements.blockAt(s, x, y, z);
                if (b != null) return b.isAir() ? null : b;
            }
        }
        return terrainAt(x, y, z);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        var near = settlementsNear(minX, minZ);
        int maxY = Math.min(chunk.getMaxBuildHeight() - 1, 240);
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = minX + lx, z = minZ + lz;
                int top = Math.max(landHeight(x, z), RIVER_LEVEL) + 1;
                int colTop = top;
                for (var s : near) colTop = Math.max(colTop, s.top());
                for (int y = chunk.getMinBuildHeight(); y <= Math.min(maxY, colTop); y++) {
                    BlockState b = blockAt(x, y, z, near);
                    if (b == null) continue;
                    chunk.setBlockState(pos.set(lx, y, lz), b, false);
                    ocean.update(lx, y, lz, b);
                    surface.update(lx, y, lz, b);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        int land = landHeight(x, z);
        if (riverValue(x, z) < 0.05) return type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG ? RIVER_FLOOR + 1 : RIVER_LEVEL + 1;
        return land + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        BlockState[] states = new BlockState[height.getHeight()];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        int min = height.getMinBuildHeight();
        for (int y = min; y < Math.min(height.getMaxBuildHeight(), 200); y++) {
            BlockState s = terrainAt(x, y, z);
            if (s != null) states[y - min] = s;
        }
        return new NoiseColumn(min, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Underworld: era now " + UnderworldSettlements.currentEra.getSerializedName());
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    @Override
    public int getSeaLevel() {
        return RIVER_LEVEL;
    }

    @Override
    public int getMinY() {
        return 0;
    }
}
