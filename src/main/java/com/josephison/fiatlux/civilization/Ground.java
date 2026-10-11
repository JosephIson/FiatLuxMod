package com.josephison.fiatlux.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * The lie of the land: where the ground is the earth itself (not a roof, a wall, the Ark, or a tomb
 * already raised), and where a hillside rises steeply enough for a chamber to be hewn into it.
 */
public final class Ground {
    private Ground() {}

    /** Earth, sand, gravel or the rock of the world: ground no one built. */
    public static boolean natural(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.GRAVEL)
                || s.is(Blocks.SNOW_BLOCK) || s.is(BlockTags.TERRACOTTA);
    }

    /** The y of the first open block above the ground at (x, z), ignoring leaves (the bottom of the world where it is not loaded). */
    public static int surface(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return level.getMinBuildHeight();
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    /** Is the top of the ground at (x, z) natural, dry and firm? */
    public static boolean naturalAt(ServerLevel level, int x, int z) {
        int y = surface(level, x, z);
        BlockPos top = new BlockPos(x, y - 1, z);
        BlockState s = level.getBlockState(top);
        return y > level.getMinBuildHeight() + 1 && s.getFluidState().isEmpty() && natural(s);
    }

    /** The open block above natural ground nearest (x, z) within `radius`, searching outward in rings. */
    @Nullable
    public static BlockPos naturalGround(ServerLevel level, int x, int z, int radius) {
        for (int r = 0; r <= radius; r += 2) {
            int steps = r == 0 ? 1 : Math.max(8, r * 2);
            for (int k = 0; k < steps; k++) {
                double a = Math.PI * 2 * k / steps;
                int px = x + (int) Math.round(Math.cos(a) * r), pz = z + (int) Math.round(Math.sin(a) * r);
                if (naturalAt(level, px, pz)) return new BlockPos(px, surface(level, px, pz), pz);
            }
        }
        return null;
    }

    /**
     * A hillside facing east (+x) between `minR` and `maxR` blocks from `around`, where a chamber can be
     * hewn: open natural ground before it, and the hill standing at least `cover` blocks over the
     * chamber's roof behind. Returns the floor-level block just inside the doorway's line (the chamber
     * runs west of it, the door opens east), or null if there is no such hill near.
     */
    @Nullable
    public static BlockPos hillside(ServerLevel level, BlockPos around, int minR, int maxR, int depth, int height, int cover) {
        for (int r = minR; r <= maxR; r += 3) {
            int steps = Math.max(12, r);
            for (int k = 0; k < steps; k++) {
                double a = Math.PI * 2 * k / steps;
                int x = around.getX() + (int) Math.round(Math.cos(a) * r), z = around.getZ() + (int) Math.round(Math.sin(a) * r);
                // the open ground before the door
                if (!naturalAt(level, x + 1, z) || !naturalAt(level, x + 2, z)) continue;
                int floor = surface(level, x + 1, z);
                if (Math.abs(surface(level, x + 2, z) - floor) > 1) continue;
                // the hill behind, over the whole chamber
                boolean hill = true;
                for (int dx = -depth; dx <= -1 && hill; dx++) {
                    for (int dz = -2; dz <= 2 && hill; dz += 2) {
                        int top = surface(level, x + dx, z + dz);
                        if (top < floor + height + cover || !naturalAt(level, x + dx, z + dz)) hill = false;
                    }
                }
                if (hill) return new BlockPos(x, floor, z);
            }
        }
        return null;
    }
}
