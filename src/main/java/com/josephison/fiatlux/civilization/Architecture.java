package com.josephison.fiatlux.civilization;

import com.josephison.fiatlux.world.cosmos.MortalEra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;

/**
 * How each age builds. Its people raise houses in the manner of their time - hide tents for those
 * who follow the herds; houses of mud brick with flat roofs and a parapet (Deut 22:8) once they farm;
 * plastered villas roofed with red tile and a columned porch in the classical age; timber-framed
 * houses with steep roofs and a chimney in the medieval age; brick terraces in the industrial; white
 * halls of glass among the stars - and the shrine grows with them: from an altar in the open to a
 * high place of standing stones, a temple of columns, a church with a bell tower, and a dome of glass.
 * Every house is turned so that its door faces the hearth.
 */
public final class Architecture {
    private Architecture() {}

    /** Blocks the builders laid in the old ways, taken down when a house is rebuilt. */
    private static final List<Block> OLD = List.of(Blocks.BROWN_WOOL, Blocks.LIGHT_GRAY_WOOL, Blocks.OAK_FENCE, Blocks.OAK_PLANKS, Blocks.OAK_LOG,
            Blocks.GLASS_PANE, Blocks.OAK_SLAB, Blocks.CRAFTING_TABLE, Blocks.RED_BED);

    // ------------------------------------------------------------------ a plan, turned to face the hearth

    /** Places blocks in a frame whose front (-z) faces a given way; shaped blocks are joined up at the end. */
    static final class Plan {
        final ServerLevel level;
        final BlockPos origin;
        final Rotation rot;
        private final List<BlockPos> shaped = new ArrayList<>();
        /** A place nothing is built over or cleared (the hearth of the town). */
        private BlockPos guarded;
        private int guardRadius;

        Plan guard(BlockPos centre, int radius) {
            this.guarded = centre;
            this.guardRadius = radius;
            return this;
        }

        private boolean guarded(BlockPos p) {
            if (guarded == null) return false;
            int dx = p.getX() - guarded.getX(), dz = p.getZ() - guarded.getZ();
            return dx * dx + dz * dz <= guardRadius * guardRadius && Math.abs(p.getY() - guarded.getY()) <= 4;
        }

        Plan(ServerLevel level, BlockPos origin, Direction front) {
            this.level = level;
            this.origin = origin;
            this.rot = switch (front) {
                case EAST -> Rotation.CLOCKWISE_90;
                case SOUTH -> Rotation.CLOCKWISE_180;
                case WEST -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
        }

        BlockPos at(int x, int y, int z) {
            return origin.offset(new BlockPos(x, y, z).rotate(rot));
        }

        void set(int x, int y, int z, BlockState s) {
            BlockPos p = at(x, y, z);
            if (guarded(p)) return;
            BlockState r = s.rotate(rot);
            level.setBlock(p, r, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            Block b = r.getBlock();
            if (b instanceof StairBlock || b instanceof FenceBlock || b instanceof IronBarsBlock || b instanceof WallBlock) shaped.add(p);
        }

        void set(int x, int y, int z, Block b) {
            set(x, y, z, b.defaultBlockState());
        }

        void box(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                    for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
        }

        void box(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
            box(x0, y0, z0, x1, y1, z1, b.defaultBlockState());
        }

        /** The four walls of a rectangle, from y0 to y1. */
        void walls(int x0, int z0, int x1, int z1, int y0, int y1, BlockState s) {
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    set(x, y, z0, s);
                    set(x, y, z1, s);
                }
                for (int z = z0; z <= z1; z++) {
                    set(x0, y, z, s);
                    set(x1, y, z, s);
                }
            }
        }

        /** Levels the site: solid ground below the floor, nothing above it, but `keep` stays. */
        void site(int x0, int z0, int x1, int z1, int height, BlockState floor, BlockPos... keep) {
            List<BlockPos> kept = List.of(keep);
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                if (guarded(at(x, 0, z))) continue;
                for (int y = -2; y >= -6; y--) {
                    BlockPos p = at(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced()) break;
                    level.setBlock(p, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                set(x, -1, z, floor);
                for (int y = 0; y <= height; y++) {
                    BlockPos p = at(x, y, z);
                    if (kept.contains(p)) continue;
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }

        void door(int x, int z, Block door) {
            BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            set(x, 0, z, lower);
            set(x, 1, z, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }

        /** A bed with its head toward +z; returns where its foot is. */
        BlockPos bed(int x, int z, Block bed) {
            BlockState foot = bed.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT);
            set(x, 0, z, foot);
            set(x, 0, z + 1, foot.setValue(BedBlock.PART, BedPart.HEAD));
            return at(x, 0, z);
        }

        void stair(int x, int y, int z, Block stair, Direction facing, boolean upsideDown) {
            set(x, y, z, stair.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM));
        }

        void slab(int x, int y, int z, Block slab, boolean top) {
            set(x, y, z, slab.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM));
        }

        void log(int x, int y, int z, Block log, Direction.Axis axis) {
            set(x, y, z, log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
        }

        void hangingLantern(int x, int y, int z) {
            set(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }

        /** A hipped roof: rings of stairs climbing inward from the eaves, closed with slabs. */
        void hippedRoof(int x0, int z0, int x1, int z1, int y, Block stair, Block slab, Block fill) {
            while (x0 <= x1 && z0 <= z1) {
                if (x1 - x0 < 1 || z1 - z0 < 1) {
                    box(x0, y, z0, x1, y, z1, slab.defaultBlockState());
                    break;
                }
                for (int x = x0; x <= x1; x++) {
                    stair(x, y, z0, stair, Direction.SOUTH, false);
                    stair(x, y, z1, stair, Direction.NORTH, false);
                }
                for (int z = z0 + 1; z < z1; z++) {
                    stair(x0, y, z, stair, Direction.EAST, false);
                    stair(x1, y, z, stair, Direction.WEST, false);
                }
                if (x1 - x0 >= 2 && z1 - z0 >= 2) box(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, fill.defaultBlockState());
                x0++;
                z0++;
                x1--;
                z1--;
                y++;
            }
        }

        /** A gable roof whose ridge runs along z, with its ends closed by `gable`. */
        void gableRoof(int x0, int x1, int z0, int z1, int y, Block stair, Block slab, BlockState gable, int wallZ0, int wallZ1) {
            int half = (x1 - x0) / 2;
            int mid = (x0 + x1) / 2;
            for (int i = 0; i <= half; i++) {
                int yy = y + i;
                for (int z = z0; z <= z1; z++) {
                    if (x0 + i == x1 - i) {
                        slab(mid, yy, z, slab, false);
                    } else {
                        stair(x0 + i, yy, z, stair, Direction.EAST, false);
                        stair(x1 - i, yy, z, stair, Direction.WEST, false);
                    }
                }
                // close the gables
                for (int x = x0 + i + 1; x <= x1 - i - 1; x++) {
                    set(x, yy, wallZ0, gable);
                    set(x, yy, wallZ1, gable);
                }
            }
        }

        /** Joins panes, fences, walls and stairs to what is beside them. */
        void finish() {
            for (BlockPos p : shaped) {
                BlockState s = level.getBlockState(p);
                BlockState joined = Block.updateFromNeighbourShapes(s, level, p);
                if (joined != s) level.setBlock(p, joined, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    // ------------------------------------------------------------------ houses

    /** The way a house at `g` turns to face the hearth at `centre`. */
    public static Direction facing(BlockPos g, BlockPos centre) {
        int dx = centre.getX() - g.getX(), dz = centre.getZ() - g.getZ();
        return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    /** Raises a house of the age at `g`, its door toward `centre`. Returns where its bed is. */
    public static BlockPos house(ServerLevel level, BlockPos g, BlockPos centre, MortalEra era) {
        Plan p = new Plan(level, g, facing(g, centre)).guard(centre, 4);
        BlockPos bed = switch (era) {
            case HUNTER_GATHERER -> tent(p);
            case AGRARIAN -> mudBrick(p);
            case CLASSICAL -> villa(p);
            case MEDIEVAL -> timber(p);
            case INDUSTRIAL -> brick(p);
            case SPACEFARING -> white(p);
        };
        p.finish();
        return bed;
    }

    /** Takes down what was built at `g` in the old ways (only the builders' own blocks). */
    public static void clearOld(ServerLevel level, BlockPos g) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int y = 0; y <= 5; y++) {
            BlockPos p = g.offset(dx, y, dz);
            if (OLD.contains(level.getBlockState(p).getBlock())) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** A hide tent around its pole, open toward the hearth, with a bed of skins. */
    private static BlockPos tent(Plan p) {
        p.site(-2, -2, 2, 2, 5, Blocks.COARSE_DIRT.defaultBlockState());
        for (int y = 0; y < 4; y++) {
            int r = 2 - y / 2;
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                boolean edge = Math.abs(x) == r || Math.abs(z) == r;
                if (!edge || (z == -r && x == 0 && y < 2)) continue;
                p.set(x, y, z, y % 2 == 0 ? Blocks.BROWN_WOOL : Blocks.LIGHT_GRAY_WOOL);
            }
        }
        for (int y = 0; y < 5; y++) p.set(0, y, 0, Blocks.OAK_FENCE);
        p.set(1, 0, -1, Blocks.BROWN_CARPET);
        return p.bed(-1, -1, Blocks.BROWN_BED);
    }

    /** A house of mud brick with a flat roof of beams and a parapet about it (Deut 22:8). */
    private static BlockPos mudBrick(Plan p) {
        p.site(-3, -3, 3, 3, 6, Blocks.PACKED_MUD.defaultBlockState());
        p.walls(-3, -3, 3, 3, 0, 2, Blocks.MUD_BRICKS.defaultBlockState());
        for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) for (int y = 0; y <= 2; y++) p.log(c[0], y, c[1], Blocks.STRIPPED_OAK_LOG, Direction.Axis.Y);
        // the roof: beams laid across, a parapet about it
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) p.log(x, 3, z, Blocks.STRIPPED_OAK_LOG, Direction.Axis.X);
        for (int x = -3; x <= 3; x++) {
            p.set(x, 4, -3, Blocks.MUD_BRICK_WALL);
            p.set(x, 4, 3, Blocks.MUD_BRICK_WALL);
        }
        for (int z = -2; z <= 2; z++) {
            p.set(-3, 4, z, Blocks.MUD_BRICK_WALL);
            p.set(3, 4, z, Blocks.MUD_BRICK_WALL);
        }
        p.door(0, -3, Blocks.OAK_DOOR);
        // small windows of lattice
        p.set(-3, 1, 0, Blocks.OAK_FENCE);
        p.set(3, 1, 0, Blocks.OAK_FENCE);
        p.set(0, 1, 3, Blocks.OAK_FENCE);
        // within: a clay oven, a jar of oil, a table, a lamp
        p.set(2, 0, 2, Blocks.SMOKER);
        p.set(2, 0, 1, Blocks.DECORATED_POT);
        p.set(2, 0, -2, Blocks.CRAFTING_TABLE);
        p.hangingLantern(0, 2, 0);
        return p.bed(-2, 1, Blocks.WHITE_BED);
    }

    /** A plastered house roofed with red tile, a porch of two columns before its door. */
    private static BlockPos villa(Plan p) {
        p.site(-4, -4, 4, 3, 8, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
        p.walls(-4, -2, 4, 3, 0, 0, Blocks.CUT_SANDSTONE.defaultBlockState());
        p.walls(-4, -2, 4, 3, 1, 3, Blocks.WHITE_TERRACOTTA.defaultBlockState());
        // the porch
        for (int x : new int[]{-3, 3}) for (int y = 0; y <= 3; y++) p.set(x, y, -4, Blocks.QUARTZ_PILLAR.defaultBlockState());
        p.box(-4, -1, -4, 4, -1, -3, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        p.box(-4, 4, -4, 4, 4, 3, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
        p.hippedRoof(-5, -5, 5, 4, 5, Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB, Blocks.BRICKS);
        p.door(0, -2, Blocks.BIRCH_DOOR);
        // windows
        for (int z : new int[]{0, 1}) for (int y = 1; y <= 2; y++) {
            p.set(-4, y, z, Blocks.GLASS_PANE);
            p.set(4, y, z, Blocks.GLASS_PANE);
        }
        for (int x : new int[]{-2, 2}) for (int y = 1; y <= 2; y++) p.set(x, y, 3, Blocks.GLASS_PANE);
        // within
        p.set(3, 0, 2, Blocks.BARREL);
        p.set(3, 0, -1, Blocks.FLOWER_POT);
        p.set(-3, 0, -1, Blocks.CRAFTING_TABLE);
        p.hangingLantern(0, 3, 1);
        p.set(-1, 0, -3, Blocks.POTTED_RED_TULIP);
        p.set(1, 0, -3, Blocks.POTTED_RED_TULIP);
        return p.bed(-3, 1, Blocks.RED_BED);
    }

    /** A timber-framed house on a stone footing, under a steep roof, its chimney smoking. */
    private static BlockPos timber(Plan p) {
        p.site(-3, -4, 3, 4, 10, Blocks.COBBLESTONE.defaultBlockState());
        p.walls(-3, -4, 3, 4, 0, 0, Blocks.COBBLESTONE.defaultBlockState());
        p.walls(-3, -4, 3, 4, 1, 3, Blocks.WHITE_TERRACOTTA.defaultBlockState());
        // the frame
        for (int[] c : new int[][]{{-3, -4}, {3, -4}, {-3, 4}, {3, 4}, {-3, 0}, {3, 0}, {-1, -4}, {1, -4}, {-1, 4}, {1, 4}}) {
            for (int y = 1; y <= 3; y++) p.log(c[0], y, c[1], Blocks.DARK_OAK_LOG, Direction.Axis.Y);
        }
        for (int x = -3; x <= 3; x++) {
            p.log(x, 3, -4, Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X);
            p.log(x, 3, 4, Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X);
        }
        for (int z = -4; z <= 4; z++) {
            p.log(-3, 3, z, Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Z);
            p.log(3, 3, z, Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Z);
        }
        p.box(-2, 3, -3, 2, 3, 3, Blocks.SPRUCE_PLANKS.defaultBlockState());   // the ceiling
        p.gableRoof(-4, 4, -5, 5, 4, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.SPRUCE_PLANKS.defaultBlockState(), -4, 4);
        p.door(0, -4, Blocks.SPRUCE_DOOR);
        for (int y = 1; y <= 2; y++) {
            p.set(-3, y, -2, Blocks.GLASS_PANE);
            p.set(-3, y, 2, Blocks.GLASS_PANE);
            p.set(3, y, -2, Blocks.GLASS_PANE);
            p.set(3, y, 2, Blocks.GLASS_PANE);
            p.set(-2, y, -4, Blocks.GLASS_PANE);
            p.set(2, y, -4, Blocks.GLASS_PANE);
        }
        // the hearth and its chimney
        p.set(2, 0, 3, Blocks.FURNACE);
        for (int y = 1; y <= 8; y++) p.set(2, y, 3, Blocks.BRICKS);
        p.set(2, 9, 3, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false));
        p.set(-2, 0, 3, Blocks.CHEST);
        p.set(2, 0, -2, Blocks.CRAFTING_TABLE);
        p.set(-2, 0, -3, Blocks.BARREL);
        p.hangingLantern(0, 2, 0);
        return p.bed(-2, 0, Blocks.BLUE_BED);
    }

    /** A brick house of the mills, with tall windows and a chimney. */
    private static BlockPos brick(Plan p) {
        p.site(-3, -4, 3, 4, 7, Blocks.STONE_BRICKS.defaultBlockState());
        p.walls(-3, -4, 3, 4, 0, 4, Blocks.BRICKS.defaultBlockState());
        p.walls(-3, -4, 3, 4, 0, 0, Blocks.STONE_BRICKS.defaultBlockState());
        p.walls(-3, -4, 3, 4, 4, 4, Blocks.STONE_BRICKS.defaultBlockState());
        p.box(-3, 5, -4, 3, 5, 4, Blocks.STONE_BRICK_SLAB.defaultBlockState());
        p.box(-2, 4, -3, 2, 4, 3, Blocks.SPRUCE_PLANKS.defaultBlockState());
        p.door(0, -4, Blocks.DARK_OAK_DOOR);
        for (int y = 1; y <= 3; y++) {
            for (int z : new int[]{-2, 0, 2}) {
                p.set(-3, y, z, Blocks.GLASS_PANE);
                p.set(3, y, z, Blocks.GLASS_PANE);
            }
            p.set(-2, y, -4, Blocks.GLASS_PANE);
            p.set(2, y, -4, Blocks.GLASS_PANE);
        }
        p.set(2, 0, 3, Blocks.BLAST_FURNACE);
        for (int y = 1; y <= 7; y++) p.set(2, y, 3, Blocks.BRICKS);
        p.set(2, 8, 3, Blocks.CAMPFIRE.defaultBlockState());
        p.set(-2, 0, 3, Blocks.BOOKSHELF);
        p.set(2, 0, -2, Blocks.LOOM);
        p.set(0, 3, 0, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true));
        return p.bed(-2, 0, Blocks.GRAY_BED);
    }

    /** A white house of the star-faring age, with bands of glass and its own light. */
    private static BlockPos white(Plan p) {
        p.site(-3, -4, 3, 4, 7, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        p.walls(-3, -4, 3, 4, 0, 3, Blocks.WHITE_CONCRETE.defaultBlockState());
        for (int x = -2; x <= 2; x++) for (int y = 1; y <= 2; y++) {
            p.set(x, y, 4, Blocks.LIGHT_BLUE_STAINED_GLASS);
        }
        for (int z = -3; z <= 3; z++) for (int y = 1; y <= 2; y++) {
            p.set(-3, y, z, Blocks.LIGHT_BLUE_STAINED_GLASS);
            p.set(3, y, z, Blocks.LIGHT_BLUE_STAINED_GLASS);
        }
        p.box(-3, 4, -4, 3, 4, 4, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
        p.box(-1, 4, -1, 1, 4, 1, Blocks.SEA_LANTERN.defaultBlockState());
        p.set(2, 5, 3, Blocks.LIGHTNING_ROD);
        p.door(0, -4, Blocks.BIRCH_DOOR);
        p.set(2, 0, 3, Blocks.ENCHANTING_TABLE);
        p.set(-2, 0, -3, Blocks.CHISELED_BOOKSHELF);
        return p.bed(-2, 1, Blocks.LIGHT_GRAY_BED);
    }

    // ------------------------------------------------------------------ the house of their god

    /** The style of shrine an age raises (0 an altar in the open, 1 a high place, 2 a temple, 3 a church, 4 a dome). */
    public static int templeStyle(MortalEra era) {
        return switch (era) {
            case HUNTER_GATHERER -> 0;
            case AGRARIAN -> 1;
            case CLASSICAL -> 2;
            case MEDIEVAL, INDUSTRIAL -> 3;
            case SPACEFARING -> 4;
        };
    }

    /**
     * Raises the house of their god about the altar at `shrine`, its front toward the hearth (south).
     * The altar and what crowns it stay where they are.
     */
    public static void temple(ServerLevel level, BlockPos shrine, BlockPos centre, int style) {
        Plan p = new Plan(level, shrine, Direction.SOUTH).guard(centre, 4);
        BlockPos altar = shrine, crown = shrine.above(), fire = shrine.above(2);
        switch (style) {
            case 1 -> highPlace(p, altar, crown, fire);
            case 2 -> classicalTemple(p, altar, crown, fire);
            case 3 -> church(p, altar, crown, fire);
            case 4 -> dome(p, altar, crown, fire);
            default -> {
            }
        }
        p.finish();
    }

    /** "An altar of earth thou shalt make unto me ... And if thou wilt make me an altar of stone, thou shalt not build it of hewn stone" (Ex 20:24-25). */
    private static void highPlace(Plan p, BlockPos... keep) {
        p.site(-4, -4, 4, 4, 6, Blocks.COBBLESTONE.defaultBlockState(), keep);
        p.box(-3, -1, -3, 3, -1, 3, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        for (int[] c : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}, {0, 4}}) {
            for (int y = 0; y <= 2; y++) p.set(c[0], y, c[1], y == 2 ? Blocks.MOSSY_STONE_BRICK_WALL : Blocks.MOSSY_STONE_BRICKS);
        }
        p.set(-4, 3, -4, Blocks.LANTERN);
        p.set(4, 3, -4, Blocks.LANTERN);
        p.set(2, 0, 1, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        p.set(-2, 0, 1, Blocks.CAMPFIRE);
        for (int x = -1; x <= 1; x++) p.stair(x, -1, -5, Blocks.COBBLESTONE_STAIRS, Direction.SOUTH, false);
    }

    /** A temple of columns on a stepped base, its altar within the sanctuary. */
    private static void classicalTemple(Plan p, BlockPos... keep) {
        p.site(-5, -4, 5, 6, 9, Blocks.SMOOTH_QUARTZ.defaultBlockState(), keep);
        for (int x = -5; x <= 5; x++) p.stair(x, -1, -5, Blocks.QUARTZ_STAIRS, Direction.SOUTH, false);
        // the colonnade
        for (int x = -5; x <= 5; x += 2) for (int y = 0; y <= 4; y++) {
            p.set(x, y, -4, Blocks.QUARTZ_PILLAR);
            p.set(x, y, 6, Blocks.QUARTZ_PILLAR);
        }
        for (int z = -2; z <= 4; z += 2) for (int y = 0; y <= 4; y++) {
            p.set(-5, y, z, Blocks.QUARTZ_PILLAR);
            p.set(5, y, z, Blocks.QUARTZ_PILLAR);
        }
        // the sanctuary
        p.walls(-3, -2, 3, 5, 0, 4, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
        p.box(-1, 0, -2, 1, 3, -2, Blocks.AIR.defaultBlockState());
        p.box(-5, 5, -4, 5, 5, 6, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        p.gableRoof(-6, 6, -5, 7, 6, Blocks.SMOOTH_QUARTZ_STAIRS, Blocks.SMOOTH_QUARTZ_SLAB, Blocks.SMOOTH_QUARTZ.defaultBlockState(), -5, 7);
        p.box(-2, 4, -1, 2, 4, 4, Blocks.AIR.defaultBlockState());
        p.set(-2, 0, 4, Blocks.LANTERN);
        p.set(2, 0, 4, Blocks.LANTERN);
        p.hangingLantern(0, 4, 2);
        p.set(-2, 0, 0, Blocks.POTTED_OAK_SAPLING);
        p.set(2, 0, 0, Blocks.POTTED_OAK_SAPLING);
    }

    /** A church of stone with windows of coloured glass, a steep roof and a bell tower. */
    private static void church(Plan p, BlockPos... keep) {
        p.site(-5, -5, 5, 8, 14, Blocks.POLISHED_ANDESITE.defaultBlockState(), keep);
        p.site(-7, -7, -5, -5, 14, Blocks.POLISHED_ANDESITE.defaultBlockState(), keep);
        p.walls(-4, -5, 4, 8, 0, 5, Blocks.STONE_BRICKS.defaultBlockState());
        // buttresses
        for (int z = -3; z <= 7; z += 3) for (int y = 0; y <= 3; y++) {
            p.set(-5, y, z, Blocks.STONE_BRICKS);
            p.set(5, y, z, Blocks.STONE_BRICKS);
        }
        // windows of coloured glass
        Block[] glass = {Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.WHITE_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE};
        for (int z = -2; z <= 7; z += 3) for (int y = 2; y <= 4; y++) {
            p.set(-4, y, z, glass[(y + z) % 3 < 0 ? 0 : (y + z) % 3]);
            p.set(4, y, z, glass[(y + z) % 3 < 0 ? 0 : (y + z) % 3]);
        }
        // the great door and a round window above it
        p.box(-1, 0, -5, 1, 3, -5, Blocks.AIR.defaultBlockState());
        for (int x = -1; x <= 1; x++) p.stair(x, 4, -5, Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true);
        p.set(0, 6, -5, Blocks.YELLOW_STAINED_GLASS_PANE);
        p.set(-1, 7, -5, Blocks.WHITE_STAINED_GLASS_PANE);
        p.set(1, 7, -5, Blocks.WHITE_STAINED_GLASS_PANE);
        p.set(0, 8, -5, Blocks.YELLOW_STAINED_GLASS_PANE);
        p.gableRoof(-5, 5, -6, 9, 6, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.STONE_BRICKS.defaultBlockState(), -5, 8);
        // the bell tower, at the front corner
        p.walls(-7, -7, -5, -5, 0, 10, Blocks.STONE_BRICKS.defaultBlockState());
        p.box(-6, 0, -6, -6, 9, -6, Blocks.AIR.defaultBlockState());
        p.box(-7, 8, -6, -5, 9, -6, Blocks.AIR.defaultBlockState());
        p.box(-6, 8, -7, -6, 9, -5, Blocks.AIR.defaultBlockState());
        p.set(-6, 9, -6, Blocks.BELL.defaultBlockState().setValue(net.minecraft.world.level.block.BellBlock.ATTACHMENT,
                net.minecraft.world.level.block.state.properties.BellAttachType.CEILING));
        p.set(-6, 10, -6, Blocks.STONE_BRICKS);
        p.hippedRoof(-7, -7, -5, -5, 11, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILES);
        p.set(-6, 13, -6, Blocks.LIGHTNING_ROD);
        // within: pews, lights
        for (int z = -3; z <= -1; z++) for (int x : new int[]{-3, -2, 2, 3}) p.stair(x, 0, z, Blocks.SPRUCE_STAIRS, Direction.NORTH, false);
        for (int z : new int[]{2, 6}) p.hangingLantern(0, 5, z);
        p.set(-3, 0, 7, Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        p.set(3, 0, 7, Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
    }

    /** A dome of glass upon a white ring. */
    private static void dome(Plan p, BlockPos... keep) {
        p.site(-5, -5, 5, 5, 7, Blocks.SMOOTH_QUARTZ.defaultBlockState(), keep);
        int r = 5;
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) for (int y = 0; y <= r; y++) {
            double d = Math.sqrt(x * x + z * z + y * y);
            if (d > r || d < r - 1) continue;
            boolean door = z <= -3 && Math.abs(x) <= 1 && y <= 2;
            if (door) continue;
            p.set(x, y, z, y <= 1 ? Blocks.QUARTZ_BRICKS : Blocks.GLASS);
        }
        p.set(0, r, 0, Blocks.SEA_LANTERN);
        for (int[] c : new int[][]{{-3, 0}, {3, 0}, {0, 3}}) p.set(c[0], 0, c[1], Blocks.END_ROD);
    }
}
