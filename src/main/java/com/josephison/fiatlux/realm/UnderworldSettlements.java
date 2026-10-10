package com.josephison.fiatlux.realm;

import com.josephison.fiatlux.registry.ModBlocks;
import com.josephison.fiatlux.world.cosmos.UnderworldEra;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Infernal settlements of the Underworld, expressed as pure functions of position so they can be
 * generated chunk by chunk (and across chunk borders) without structure pieces.
 *
 * The world is divided into 96-block cells; most cells hold a settlement. A cell takes the
 * Underworld's simulated era at the moment its first chunk is generated and keeps it forever, so
 * exploring the abyss reveals its history: bone camps of the Tribal/Pits era, gothic bastions of the
 * Feudal era, soul-engine foundries and rail lines of the Industrial era, and neon megacity blocks of
 * the Modern/Cybernetic era.
 */
public final class UnderworldSettlements {
    public static final int CELL = 96;
    /** The current simulated era, published by WorldTicker for worldgen threads. */
    public static volatile UnderworldEra currentEra = UnderworldEra.TRIBAL_PITS;
    /** Era fixed for each cell when first generated (persisted in CosmicExpanseSavedData). */
    public static final Map<Long, UnderworldEra> CELL_ERAS = new ConcurrentHashMap<>();
    public static volatile boolean dirty;

    private UnderworldSettlements() {}

    public record Settlement(int cx, int cz, int baseY, UnderworldEra era, long seed) {
        int radius() {
            return switch (era) {
                case TRIBAL_PITS -> 15;
                case FEUDAL_CASTLE -> 19;
                case INDUSTRIAL -> 42;
                case MODERN_CYBERNETIC -> 32;
            };
        }

        int top() {
            return baseY + (era == UnderworldEra.MODERN_CYBERNETIC ? 72 : 28);
        }
    }

    public static long cellKey(int cellX, int cellZ) {
        return ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
    }

    static long hash(long a, long b) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        return h ^ (h >>> 29);
    }

    /** The settlement in a cell, or null. {@code heightAt} gives terrain height for siting. */
    public static Settlement settlementIn(int cellX, int cellZ, java.util.function.IntBinaryOperator heightAt) {
        long h = hash(cellX, cellZ);
        if (Math.floorMod(h, 5) == 0) return null;   // one cell in five is wilderness
        int cx = cellX * CELL + CELL / 2 + (int) (Math.floorMod(h >> 8, 40)) - 20;
        int cz = cellZ * CELL + CELL / 2 + (int) (Math.floorMod(h >> 16, 40)) - 20;
        int y = heightAt.applyAsInt(cx, cz);
        if (y < 0) return null;                      // a river or chasm: no one builds there
        UnderworldEra era = CELL_ERAS.computeIfAbsent(cellKey(cellX, cellZ), k -> {
            dirty = true;
            return currentEra;
        });
        return new Settlement(cx, cz, y, era, h);
    }

    /** The block a settlement places at (x, y, z), or null to leave terrain as is. AIR clears terrain. */
    public static BlockState blockAt(Settlement s, int x, int y, int z) {
        int dx = x - s.cx, dz = z - s.cz, dy = y - s.baseY;
        return switch (s.era) {
            case TRIBAL_PITS -> tribal(s, dx, dy, dz);
            case FEUDAL_CASTLE -> feudal(dx, dy, dz);
            case INDUSTRIAL -> industrial(s, dx, dy, dz);
            case MODERN_CYBERNETIC -> modern(s, dx, dy, dz);
        };
    }

    // --- Tribal/Pits: a soul-fire pit ringed by bone totems, obsidian domes around it
    private static BlockState tribal(Settlement s, int dx, int dy, int dz) {
        double r = Math.sqrt(dx * dx + dz * dz);
        if (dy == 0 && r <= 2.5) return Blocks.SOUL_SOIL.defaultBlockState();
        if (dy == 1 && dx == 0 && dz == 0) return Blocks.SOUL_FIRE.defaultBlockState();
        if (dy > 0 && dy < 8 && r < 13.5 && !(Math.abs(dx) <= 0 && Math.abs(dz) <= 0)) {
            // totems
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                int tx = (int) Math.round(Math.cos(a) * 7), tz = (int) Math.round(Math.sin(a) * 7);
                if (dx == tx && dz == tz) {
                    if (dy <= 4) return Blocks.BONE_BLOCK.defaultBlockState();
                    if (dy == 5) return Blocks.SKELETON_SKULL.defaultBlockState();
                }
            }
            // obsidian domes (hollow, with a doorway facing the pit)
            for (int k = 0; k < 4; k++) {
                double a = Math.PI / 4 + k * Math.PI / 2;
                int hx = (int) Math.round(Math.cos(a) * 11), hz = (int) Math.round(Math.sin(a) * 11);
                double d = Math.sqrt((dx - hx) * (dx - hx) + (dy - 0.5) * (dy - 0.5) + (dz - hz) * (dz - hz));
                if (d <= 3.2) {
                    boolean shell = d > 2.2;
                    boolean door = dy <= 2 && Math.abs((dx - hx) * Math.sin(a) - (dz - hz) * Math.cos(a)) < 0.8
                            && ((dx - hx) * Math.cos(a) + (dz - hz) * Math.sin(a)) < 0;
                    if (shell && !door) return (s.seed >> k & 1) == 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.CRYING_OBSIDIAN.defaultBlockState();
                    return Blocks.AIR.defaultBlockState();
                }
            }
        }
        return null;
    }

    // --- Feudal/Castle: a walled bastion with corner towers and a central keep
    private static BlockState feudal(int dx, int dy, int dz) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        if (dy == 0 && ax <= 16 && az <= 16) return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        if (dy <= 0) return null;
        boolean wall = (ax == 16 && az <= 16) || (az == 16 && ax <= 16);
        boolean tower = ax >= 14 && ax <= 18 && az >= 14 && az <= 18;
        boolean keep = ax <= 4 && az <= 4;
        boolean gate = dz == 16 && ax <= 1 && dy <= 4;
        if (tower) {
            boolean edge = ax == 14 || ax == 18 || az == 14 || az == 18;
            if (dy <= 12 && edge) return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            if (dy == 13 && edge && (ax + az) % 2 == 0) return Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
            if (dy == 12 && !edge) return Blocks.SOUL_LANTERN.defaultBlockState();
            return dy <= 12 ? Blocks.AIR.defaultBlockState() : null;
        }
        if (wall && !gate) {
            if (dy <= 8) return dy == 4 && (ax + az) % 6 == 0 ? Blocks.RED_STAINED_GLASS.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
            if (dy == 9 && (ax + az) % 2 == 0) return Blocks.BLACKSTONE_WALL.defaultBlockState();
            return null;
        }
        if (gate) return Blocks.AIR.defaultBlockState();
        if (keep) {
            boolean edge = ax == 4 || az == 4;
            if (dy <= 18) {
                if (edge) return dy % 5 == 3 && (ax + az) % 3 == 0 ? Blocks.RED_STAINED_GLASS.defaultBlockState() : Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
                return dy % 6 == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState();
            }
            if (dy == 19 && edge && (ax + az) % 2 == 0) return Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
            if (dy == 20 && ax == 0 && az == 0) return Blocks.SOUL_CAMPFIRE.defaultBlockState();
            return null;
        }
        if (ax < 16 && az < 16 && dy <= 9) return Blocks.AIR.defaultBlockState();   // courtyard
        return null;
    }

    // --- Industrial: a soul-engine foundry hall, brick chimneys, rails radiating to other settlements
    private static BlockState industrial(Settlement s, int dx, int dy, int dz) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        // iron tracks
        if ((az <= 1 && ax <= 42 || ax <= 1 && az <= 42) && !(ax <= 11 && az <= 7)) {
            if (dy == 0) return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            if (dy == 1) return (az == 0 || ax == 0) ? Blocks.RAIL.defaultBlockState() : Blocks.AIR.defaultBlockState();
            if (dy > 1 && dy <= 4) return Blocks.AIR.defaultBlockState();
        }
        if (ax <= 11 && az <= 7) {
            boolean edge = ax == 11 || az == 7;
            if (dy == 0) return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            if (dy <= 9) {
                if (edge) {
                    if (dy % 3 == 0 || ax % 4 == 3) return Blocks.IRON_BLOCK.defaultBlockState();
                    return dy == 5 ? Blocks.ORANGE_STAINED_GLASS.defaultBlockState() : Blocks.NETHER_BRICKS.defaultBlockState();
                }
                if (dy <= 2 && ax % 5 == 0 && az % 4 == 0) return ModBlocks.SOUL_ENGINE.get().defaultBlockState();
                if (dy == 1 && (ax + az) % 7 == 3) return ModBlocks.BRIMSTONE.get().defaultBlockState();
                return Blocks.AIR.defaultBlockState();
            }
            if (dy == 10) return Blocks.IRON_BLOCK.defaultBlockState();
        }
        for (int k = -1; k <= 1; k++) {
            int chx = k * 7, chz = -10;
            if (Math.abs(dx - chx) <= 1 && Math.abs(dz - chz) <= 1 && dy >= 0) {
                boolean core = dx == chx && dz == chz;
                if (dy <= 22) return core ? Blocks.AIR.defaultBlockState() : Blocks.BRICKS.defaultBlockState();
                if (dy == 23 && core) return Blocks.MAGMA_BLOCK.defaultBlockState();
            }
        }
        return null;
    }

    // --- Modern/Cybernetic: a block of neon-banded towers on a lit street grid
    private static BlockState modern(Settlement s, int dx, int dy, int dz) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        if (ax > 30 || az > 30) return null;
        int gx = Math.floorDiv(dx + 30, 12), gz = Math.floorDiv(dz + 30, 12);
        int lx = Math.floorMod(dx + 30, 12), lz = Math.floorMod(dz + 30, 12);
        boolean street = lx < 3 || lz < 3;
        if (dy == 0) return street ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState();
        if (dy < 0) return null;
        if (street) {
            if (lx == 1 && lz == 1 && dy <= 4) return dy == 4 ? ModBlocks.NEON_VIOLET.get().defaultBlockState() : Blocks.IRON_BARS.defaultBlockState();
            return dy <= 70 ? Blocks.AIR.defaultBlockState() : null;
        }
        long th = hash(s.seed + gx, gz);
        int height = 18 + (int) Math.floorMod(th, 50);
        int tx = lx - 3, tz = lz - 3;   // 0..8 inside the tower lot
        boolean shell = tx == 0 || tx == 8 || tz == 0 || tz == 8;
        boolean corner = (tx == 0 || tx == 8) && (tz == 0 || tz == 8);
        if (dy > height) return dy <= 70 ? Blocks.AIR.defaultBlockState() : null;
        if (dy == height) return corner ? ModBlocks.NEON_CRIMSON.get().defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
        if (corner) return (th & 1) == 0 ? ModBlocks.NEON_CRIMSON.get().defaultBlockState() : ModBlocks.NEON_VIOLET.get().defaultBlockState();
        if (shell) {
            if (dy % 6 == 0) return ModBlocks.NEON_VIOLET.get().defaultBlockState();
            return (tx + tz + dy) % 3 == 0 ? Blocks.BLACK_CONCRETE.defaultBlockState() : Blocks.GRAY_STAINED_GLASS.defaultBlockState();
        }
        return dy % 6 == 0 ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }
}
