package com.josephison.fiatlux.realm;

import com.josephison.fiatlux.registry.ModBlocks;
import com.josephison.fiatlux.world.cosmos.PlanetType;
import com.mojang.serialization.Codec;
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
import net.minecraft.world.level.block.LeavesBlock;
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
 * Generates a created world from its pool slot's {@link RealmPool.Settings} (design doc section 5): Earth-like
 * hills, seas, beaches and forests; volcanic basalt crags with crater cones and lava lakes; barren
 * moons of cratered regolith; gas giants of banded, walkable cloud decks over a dense floor; and
 * crystalline plains of calcite crowned with crystal spires. An unbound slot is "without form, and
 * void" (Genesis 1:2).
 */
public class PlanetChunkGenerator extends ChunkGenerator {
    public static final MapCodec<PlanetChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
            Codec.INT.fieldOf("slot").forGetter(g -> g.slot)
    ).apply(i, i.stable(PlanetChunkGenerator::new)));

    public static final int SEA = 62;
    private final int slot;
    private volatile Noise noise;

    private record Noise(long seed, SimplexNoise broad, SimplexNoise detail, SimplexNoise ridge, SimplexNoise extra) {
        static Noise of(long seed) {
            return new Noise(seed, new SimplexNoise(new LegacyRandomSource(seed)), new SimplexNoise(new LegacyRandomSource(seed * 31 + 7)),
                    new SimplexNoise(new LegacyRandomSource(seed * 131 + 3)), new SimplexNoise(new LegacyRandomSource(seed * 1031 + 5)));
        }
    }

    public PlanetChunkGenerator(BiomeSource biomeSource, int slot) {
        super(biomeSource);
        this.slot = slot;
    }

    public int slot() {
        return slot;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private Noise noise(RealmPool.Settings s) {
        Noise n = noise;
        if (n == null || n.seed != s.seed()) noise = n = Noise.of(s.seed());
        return n;
    }

    static long hash(long a, long b) {
        return UnderworldSettlements.hash(a, b);
    }

    // ------------------------------------------------------------------ heights

    /** Solid surface height at (x, z) for the given world, or Integer.MIN_VALUE for none. */
    public int height(RealmPool.Settings s, int x, int z) {
        Noise n = noise(s);
        return switch (s.type()) {
            case EARTHLIKE -> {
                double m = Math.max(0, n.ridge.getValue(x / 260.0, z / 260.0));
                yield 64 + (int) (n.broad.getValue(x / 190.0, z / 190.0) * 16 + n.detail.getValue(x / 42.0, z / 42.0) * 5 + m * m * 55);
            }
            case VOLCANIC -> {
                double r = 1 - Math.abs(n.ridge.getValue(x / 90.0, z / 90.0));
                int h = 58 + (int) (n.broad.getValue(x / 140.0, z / 140.0) * 10 + r * r * r * 26);
                yield Math.max(h, cone(s, x, z));
            }
            case BARREN_MOON -> 64 + (int) (n.broad.getValue(x / 160.0, z / 160.0) * 5 + n.detail.getValue(x / 25.0, z / 25.0) * 1.5) + crater(s, x, z);
            case GAS_GIANT -> 28;
            case CRYSTALLINE -> 64 + (int) (n.broad.getValue(x / 120.0, z / 120.0) * 6 + n.detail.getValue(x / 30.0, z / 30.0) * 2);
        };
    }

    /** Volcanic crater cones on a 160-block lattice. */
    private int cone(RealmPool.Settings s, int x, int z) {
        int cx = Math.floorDiv(x, 160), cz = Math.floorDiv(z, 160);
        long h = hash(cx ^ s.seed(), cz);
        if (Math.floorMod(h, 3) != 0) return Integer.MIN_VALUE;
        int px = cx * 160 + 80 + (int) Math.floorMod(h >> 8, 50) - 25, pz = cz * 160 + 80 + (int) Math.floorMod(h >> 16, 50) - 25;
        double d = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz));
        int peak = 60 + 40 + (int) Math.floorMod(h >> 24, 25);
        if (d > 46) return Integer.MIN_VALUE;
        if (d < 7) return peak - 8 + (int) (d * 0.6);        // the crater bowl
        return (int) (peak - (d - 7) * 1.05);
    }

    /** Impact craters: bowl with a raised rim, several sizes per 48-block cell. */
    private int crater(RealmPool.Settings s, int x, int z) {
        int total = 0;
        int cx = Math.floorDiv(x, 48), cz = Math.floorDiv(z, 48);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                long h = hash((cx + i) ^ s.seed(), cz + j);
                if (Math.floorMod(h, 2) != 0) continue;
                double r = 6 + Math.floorMod(h >> 8, 16);
                double px = (cx + i) * 48 + 24 + Math.floorMod(h >> 16, 20) - 10, pz = (cz + j) * 48 + 24 + Math.floorMod(h >> 24, 20) - 10;
                double d = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz)) / r;
                if (d < 1) total -= (int) ((1 - d * d) * r * 0.45);
                else if (d < 1.3) total += (int) ((1.3 - d) / 0.3 * 2.5);
            }
        }
        return total;
    }

    // ------------------------------------------------------------------ blocks

    public BlockState stateAt(int x, int y, int z) {
        RealmPool.Settings s = RealmPool.get(slot);
        if (s == null) return null;                                   // without form, and void
        if (y <= -63 && s.type() != PlanetType.GAS_GIANT) return Blocks.BEDROCK.defaultBlockState();
        Noise n = noise(s);
        int top = height(s, x, z);
        return switch (s.type()) {
            case EARTHLIKE -> {
                if (y > top) yield y <= SEA ? Blocks.WATER.defaultBlockState() : null;
                if (y == top) yield top <= SEA + 1 ? (top < SEA - 3 ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState())
                        : top > 118 ? Blocks.SNOW_BLOCK.defaultBlockState() : top > 100 ? Blocks.STONE.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
                if (y > top - 4) yield top <= SEA + 1 ? Blocks.SAND.defaultBlockState() : Blocks.DIRT.defaultBlockState();
                yield y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
            }
            case VOLCANIC -> {
                int lavaLevel = 54;
                if (y > top) {
                    int c = cone(s, x, z);
                    if (c != Integer.MIN_VALUE && y <= c + 2 && y > top && isCraterCore(s, x, z)) yield Blocks.LAVA.defaultBlockState();
                    yield y <= lavaLevel ? Blocks.LAVA.defaultBlockState() : null;
                }
                double d = n.extra.getValue(x / 12.0, y / 9.0, z / 12.0);
                if (y == top) yield d > 0.45 ? Blocks.MAGMA_BLOCK.defaultBlockState() : d < -0.4 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState();
                yield d > 0.6 ? Blocks.MAGMA_BLOCK.defaultBlockState() : y < 20 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.SMOOTH_BASALT.defaultBlockState();
            }
            case BARREN_MOON -> {
                if (y > top) yield null;
                if (y > top - 4) yield ModBlocks.REGOLITH.get().defaultBlockState();
                double d = n.extra.getValue(x / 16.0, y / 12.0, z / 16.0);
                yield d > 0.5 ? Blocks.TUFF.defaultBlockState() : Blocks.ANDESITE.defaultBlockState();
            }
            case GAS_GIANT -> gasGiant(s, n, x, y, z);
            case CRYSTALLINE -> {
                BlockState spire = spire(s, x, y, z, top);
                if (spire != null) yield spire;
                if (y > top) yield y <= top && y <= SEA - 4 ? Blocks.WATER.defaultBlockState() : null;
                if (y == top) yield n.extra.getValue(x / 9.0, z / 9.0) > 0.55 ? Blocks.QUARTZ_BLOCK.defaultBlockState() : Blocks.CALCITE.defaultBlockState();
                yield y > top - 5 ? Blocks.CALCITE.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState();
            }
        };
    }

    private boolean isCraterCore(RealmPool.Settings s, int x, int z) {
        int cx = Math.floorDiv(x, 160), cz = Math.floorDiv(z, 160);
        long h = hash(cx ^ s.seed(), cz);
        int px = cx * 160 + 80 + (int) Math.floorMod(h >> 8, 50) - 25, pz = cz * 160 + 80 + (int) Math.floorMod(h >> 16, 50) - 25;
        return (x - px) * (x - px) + (z - pz) * (z - pz) < 36;
    }

    /** Banded cloud decks at three altitudes over a dense floor; bands follow latitude (z). */
    private BlockState gasGiant(RealmPool.Settings s, Noise n, int x, int y, int z) {
        if (y < 20) return null;
        int band = Math.floorMod((int) Math.floor((z + n.broad.getValue(x / 80.0, z / 80.0) * 18) / 22.0), 3);
        BlockState color = band == 0 ? ModBlocks.CLOUD_BAND_AMBER.get().defaultBlockState()
                : band == 1 ? ModBlocks.CLOUD_BAND_CREAM.get().defaultBlockState() : ModBlocks.CLOUD_BAND_RUST.get().defaultBlockState();
        if (y <= 28) return ModBlocks.CLOUD_BAND_RUST.get().defaultBlockState();          // the dense deep
        for (int deck : new int[]{88, 108, 128}) {
            double patch = n.detail.getValue(x / 34.0 + deck, z / 34.0) + 0.35 * n.extra.getValue(x / 11.0, z / 11.0);
            boolean origin = deck == 88 && x * x + z * z < 100;                              // a landing deck at the origin
            if ((patch > 0.05 || origin) && y >= deck && y <= deck + 1 + (patch > 0.4 ? 1 : 0)) return color;
        }
        return null;
    }

    /** Crystal spires on a 26-block lattice. */
    private BlockState spire(RealmPool.Settings s, int x, int y, int z, int ground) {
        int cx = Math.floorDiv(x, 26), cz = Math.floorDiv(z, 26);
        long h = hash(cx ^ s.seed(), cz);
        if (Math.floorMod(h, 3) == 0) return null;
        int px = cx * 26 + 13 + (int) Math.floorMod(h >> 8, 10) - 5, pz = cz * 26 + 13 + (int) Math.floorMod(h >> 16, 10) - 5;
        int height = 8 + (int) Math.floorMod(h >> 24, 26);
        int dy = y - ground;
        if (dy < 0 || dy > height + 1) return null;
        double radius = 3.2 * (1 - dy / (double) (height + 2));
        double d = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz));
        if (dy == height + 1 && d < 0.5) return ModBlocks.CELESTIAL_CRYSTAL.get().defaultBlockState();
        if (d > radius) return null;
        return d < radius * 0.45 ? Blocks.AMETHYST_BLOCK.defaultBlockState() : (Math.floorMod(h >> 40, 2) == 0 ? Blocks.QUARTZ_BLOCK.defaultBlockState() : Blocks.WHITE_STAINED_GLASS.defaultBlockState());
    }

    // ------------------------------------------------------------------ chunk filling

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        RealmPool.Settings s = RealmPool.get(slot);
        if (s == null) return CompletableFuture.completedFuture(chunk);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        int min = chunk.getMinBuildHeight();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = minX + lx, z = minZ + lz;
                int top = s.type() == PlanetType.GAS_GIANT ? 132 : Math.max(height(s, x, z), SEA) + 40;
                for (int y = min; y <= Math.min(top, chunk.getMaxBuildHeight() - 1); y++) {
                    BlockState b = stateAt(x, y, z);
                    if (b == null) continue;
                    chunk.setBlockState(pos.set(lx, y, lz), b, false);
                    ocean.update(lx, y, lz, b);
                    surface.update(lx, y, lz, b);
                }
            }
        }
        if (s.type() == PlanetType.EARTHLIKE) decorateEarth(s, chunk, minX, minZ);
        return CompletableFuture.completedFuture(chunk);
    }

    /** Trees, grass and flowers placed wholly inside the chunk (no cross-chunk writes). */
    private void decorateEarth(RealmPool.Settings s, ChunkAccess chunk, int minX, int minZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = minX + lx, z = minZ + lz;
                int top = height(s, x, z);
                if (top <= SEA + 1 || top > 100) continue;
                long h = hash(x ^ s.seed(), z);
                boolean treeSite = lx >= 2 && lx <= 13 && lz >= 2 && lz <= 13 && Math.floorMod(h, 61) == 0;
                if (treeSite) {
                    int trunk = 4 + (int) Math.floorMod(h >> 8, 3);
                    for (int dy = 1; dy <= trunk; dy++) chunk.setBlockState(pos.set(lx, top + dy, lz), Blocks.OAK_LOG.defaultBlockState(), false);
                    for (int ox = -2; ox <= 2; ox++) for (int oz = -2; oz <= 2; oz++) for (int oy = trunk - 1; oy <= trunk + 2; oy++) {
                        if (ox * ox + oz * oz + (oy - trunk) * (oy - trunk) > 6 || (ox == 0 && oz == 0 && oy <= trunk)) continue;
                        pos.set(lx + ox, top + oy, lz + oz);
                        if (chunk.getBlockState(pos).isAir()) chunk.setBlockState(pos, leaves, false);
                    }
                } else if (Math.floorMod(h >> 12, 7) == 0) {
                    pos.set(lx, top + 1, lz);
                    if (chunk.getBlockState(pos).isAir()) {
                        BlockState plant = Math.floorMod(h >> 20, 9) == 0 ? Blocks.POPPY.defaultBlockState()
                                : Math.floorMod(h >> 20, 9) == 1 ? Blocks.DANDELION.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState();
                        chunk.setBlockState(pos, plant, false);
                    }
                }
            }
        }
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        RealmPool.Settings s = RealmPool.get(slot);
        if (s == null) return level.getMinBuildHeight();
        if (s.type() == PlanetType.GAS_GIANT) {
            for (int y = 132; y > 20; y--) if (stateAt(x, y, z) != null) return y + 1;
            return 29;
        }
        int h = height(s, x, z);
        if (s.type() == PlanetType.EARTHLIKE && h < SEA && type != Heightmap.Types.OCEAN_FLOOR && type != Heightmap.Types.OCEAN_FLOOR_WG) return SEA + 1;
        return h + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        BlockState[] states = new BlockState[height.getHeight()];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        int min = height.getMinBuildHeight();
        for (int y = min; y < Math.min(height.getMaxBuildHeight(), 200); y++) {
            BlockState s = stateAt(x, y, z);
            if (s != null) states[y - min] = s;
        }
        return new NoiseColumn(min, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        RealmPool.Settings s = RealmPool.get(slot);
        info.add("Created world slot " + slot + ": " + (s == null ? "unformed" : s.name() + " (" + s.type().getSerializedName() + ")"));
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
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return SEA;
    }

    @Override
    public int getMinY() {
        return -64;
    }
}
