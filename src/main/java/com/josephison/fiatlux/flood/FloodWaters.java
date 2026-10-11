package com.josephison.fiatlux.flood;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The waters of the Flood over one stretch of a world: every column within reach of its towns and
 * the ark. They are raised a layer at a time - "the waters prevailed, and were increased greatly upon
 * the earth" (Gen 7:18) - filling every open place and washing away all that grows, and later lowered
 * a layer at a time; for each column the lowest water the Flood laid there is remembered, so only the
 * Flood's own water is taken away and the seas and springs that were there before remain.
 */
public final class FloodWaters {
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final short NONE = Short.MAX_VALUE;
    final int[] xs, zs;
    /** The lowest y the Flood filled in each column (NONE where it laid nothing). */
    final short[] first;
    private final Map<Long, Integer> index = new HashMap<>();

    /** The columns within `radii` of `centres`, in a fixed order (so they can be rebuilt on load). */
    public FloodWaters(List<BlockPos> centres, List<Integer> radii, @Nullable int[] saved) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < centres.size(); i++) {
            BlockPos c = centres.get(i);
            int r = radii.get(i);
            minX = Math.min(minX, c.getX() - r);
            maxX = Math.max(maxX, c.getX() + r);
            minZ = Math.min(minZ, c.getZ() - r);
            maxZ = Math.max(maxZ, c.getZ() + r);
        }
        java.util.ArrayList<int[]> cols = new java.util.ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int i = 0; i < centres.size(); i++) {
                    BlockPos c = centres.get(i);
                    long dx = x - c.getX(), dz = z - c.getZ();
                    if (dx * dx + dz * dz <= (long) radii.get(i) * radii.get(i)) {
                        cols.add(new int[]{x, z});
                        break;
                    }
                }
            }
        }
        xs = new int[cols.size()];
        zs = new int[cols.size()];
        first = new short[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            xs[i] = cols.get(i)[0];
            zs[i] = cols.get(i)[1];
            first[i] = saved != null && i < saved.length ? (short) saved[i] : NONE;
            index.put(BlockPos.asLong(xs[i], 0, zs[i]), i);
        }
    }

    public int size() {
        return xs.length;
    }

    public boolean covers(long column) {
        return index.containsKey(column);
    }

    public boolean covers(int x, int z) {
        return index.containsKey(BlockPos.asLong(x, 0, z));
    }

    public int[] save() {
        int[] out = new int[first.length];
        for (int i = 0; i < first.length; i++) out[i] = first[i];
        return out;
    }

    /** Notes water laid at a place by other means (where the ark has floated off). */
    public void wetted(BlockPos p) {
        Integer i = index.get(BlockPos.asLong(p.getX(), 0, p.getZ()));
        if (i != null && p.getY() < first[i]) first[i] = (short) p.getY();
    }

    /** The top of the solid ground in a column (water, and the ark at `ark`, are not ground). */
    public static int ground(ServerLevel level, int x, int z, @Nullable BlockPos ark) {
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        while (y > level.getMinBuildHeight()) {
            p.set(x, y, z);
            BlockState s = level.getBlockState(p);
            boolean hull = ark != null && Ark.contains(ark, x, y, z);
            if (!hull && s.blocksMotion() && !s.is(net.minecraft.tags.BlockTags.LEAVES) && !s.is(net.minecraft.tags.BlockTags.LOGS)) return y;
            y--;
        }
        return y;
    }

    /** The lowest and highest ground under the waters (the ark aside), and the place of the highest - the mountain. */
    public int[] relief(ServerLevel level, @Nullable BlockPos ark) {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, hx = 0, hz = 0;
        for (int i = 0; i < xs.length; i++) {
            if (ark != null && Ark.inside(xs[i] - ark.getX(), zs[i] - ark.getZ())) continue;
            int g = ground(level, xs[i], zs[i], ark);
            if (g < lo) lo = g;
            if (g > hi) {
                hi = g;
                hx = xs[i];
                hz = zs[i];
            }
        }
        return new int[]{lo, hi, hx, hz};
    }

    /**
     * Raises the waters into layer y over columns [from, from + budget): every open place, and every
     * plant, torch and tent-peg in the way, is covered. Returns where it got to (size() when the layer is full).
     */
    public int raise(ServerLevel level, int y, int from, int budget, @Nullable BlockPos ark) {
        int end = Math.min(xs.length, from + budget);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (int i = from; i < end; i++) {
            minX = Math.min(minX, xs[i]);
            maxX = Math.max(maxX, xs[i]);
            minZ = Math.min(minZ, zs[i]);
            maxZ = Math.max(maxZ, zs[i]);
        }
        for (int i = from; i < end; i++) {
            int x = xs[i], z = zs[i];
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            if (ark != null && Ark.contains(ark, x, y, z)) continue;
            p.set(x, y, z);
            BlockState s = level.getBlockState(p);
            if (s.isAir() || s.getFluidState().isEmpty() && !s.blocksMotion() && !(s.getBlock() instanceof BedBlock)) {
                level.setBlock(p, WATER, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            } else if (s.hasProperty(BlockStateProperties.WATERLOGGED) && !s.getValue(BlockStateProperties.WATERLOGGED)) {
                level.setBlock(p, s.setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            } else {
                continue;
            }
            if (y < first[i]) first[i] = (short) y;
        }
        // the waters stand where they are laid: none spills over the edge to spring up where they will not be taken away
        if (end > from) level.getFluidTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(minX, y, minZ, maxX, y, maxZ));
        return end;
    }

    /** Lowers the waters out of layer y over columns [from, from + budget), taking only what the Flood laid. */
    public int lower(ServerLevel level, int y, int from, int budget, @Nullable BlockPos ark) {
        int end = Math.min(xs.length, from + budget);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = from; i < end; i++) {
            if (first[i] == NONE || y < first[i]) continue;
            int x = xs[i], z = zs[i];
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            if (ark != null && Ark.contains(ark, x, y, z)) continue;
            p.set(x, y, z);
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.WATER)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            } else if (s.hasProperty(BlockStateProperties.WATERLOGGED) && s.getValue(BlockStateProperties.WATERLOGGED)) {
                level.setBlock(p, s.setValue(BlockStateProperties.WATERLOGGED, false), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            if (y == first[i]) first[i] = NONE;
        }
        return end;
    }

    /** Whether the water stands in a column at y (for those caught in it). */
    public boolean flooded(ServerLevel level, BlockPos p) {
        return covers(p.getX(), p.getZ()) && level.getFluidState(p).is(FluidTags.WATER);
    }

    /** Every chunk the waters touch. */
    public java.util.Set<Long> chunks() {
        java.util.Set<Long> out = new java.util.HashSet<>();
        for (int i = 0; i < xs.length; i++) out.add(net.minecraft.world.level.ChunkPos.asLong(xs[i] >> 4, zs[i] >> 4));
        return out;
    }

    public int x(int i) {
        return xs[i];
    }

    public int z(int i) {
        return zs[i];
    }
}
