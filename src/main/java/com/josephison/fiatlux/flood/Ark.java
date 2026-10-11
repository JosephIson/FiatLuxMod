package com.josephison.fiatlux.flood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.LongPredicate;

/**
 * The ark, built to the measure the LORD gave Noah (Genesis 6:14-16): "Make thee an ark of gopher
 * wood; rooms shalt thou make in the ark, and shalt pitch it within and without with pitch. ... The
 * length of the ark shall be three hundred cubits, the breadth of it fifty cubits, and the height of
 * it thirty cubits. A window shalt thou make to the ark, and in a cubit shalt thou finish it above;
 * and the door of the ark shalt thou set in the side thereof; with lower, second, and third stories
 * shalt thou make it."
 *
 * At a cubit of about 0.46 m that is 137 by 23 by 14 blocks: a box-like barge of pitch-dark timber,
 * its ends chamfered, with three decks joined by ladders. The lower two are lined with stalls
 * ("rooms", literally "nests") with hay for the beasts, the upper holds beds for eight souls and
 * stores of food; a one-cubit window runs under the eaves all round, and the door is in the side.
 *
 * The ark is pure geometry: {@link #design} gives the block at any place relative to its corner, so
 * it can be built a little at a time, and floated - moved up, down or along by any offset - by
 * changing only the blocks that differ, carrying everything aboard with it.
 */
public final class Ark {
    public static final int LENGTH = 137, BREADTH = 23, HEIGHT = 16;
    /** Its keel sits this far below the waterline once afloat. */
    public static final int DRAFT = 4;
    public static final int MID = BREADTH / 2;
    /** The door, in the side (z = 0), opening onto the lower deck. */
    public static final int DOOR_X0 = 67, DOOR_X1 = 69;
    public static final int[] LADDERS = {24, 112};
    private static final int TOTAL = LENGTH * BREADTH * HEIGHT;

    private static final BlockState HULL = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    private static final BlockState DECK = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private Ark() {}

    // ------------------------------------------------------------------ the plan

    /** Half the breadth at a point along the length: the ends are chamfered. */
    static int halfWidth(int x) {
        int end = Math.min(x, LENGTH - 1 - x);
        return end >= 4 ? MID : 7 + end;
    }

    static boolean inside(int x, int z) {
        return x >= 0 && x < LENGTH && Math.abs(z - MID) <= halfWidth(x);
    }

    private static boolean wall(int x, int z) {
        return inside(x, z) && (!inside(x - 1, z) || !inside(x + 1, z) || !inside(x, z - 1) || !inside(x, z + 1));
    }

    private static boolean ridge(int x, int z) {
        return Math.abs(z - MID) <= 3 && x >= 6 && x <= LENGTH - 7;
    }

    /** True where the ark's hull or its air stands (its envelope), for the waters to leave alone. */
    public static boolean occupies(int x, int y, int z) {
        if (!inside(x, z) || y < 0 || y >= HEIGHT) return false;
        return y < 15 || ridge(x, z);
    }

    /** The block at (x, y, z) from the ark's corner, or null outside it. Fences are joined to their neighbours. */
    @Nullable
    public static BlockState design(int x, int y, int z, boolean doorShut) {
        BlockState s = base(x, y, z, doorShut);
        if (s != null && s.getBlock() instanceof FenceBlock) {
            s = s.setValue(CrossCollisionBlock.NORTH, joins(base(x, y, z - 1, doorShut)))
                    .setValue(CrossCollisionBlock.SOUTH, joins(base(x, y, z + 1, doorShut)))
                    .setValue(CrossCollisionBlock.WEST, joins(base(x - 1, y, z, doorShut)))
                    .setValue(CrossCollisionBlock.EAST, joins(base(x + 1, y, z, doorShut)));
        }
        return s;
    }

    private static boolean joins(@Nullable BlockState n) {
        return n != null && (n.getBlock() instanceof FenceBlock || n.getBlock() instanceof FenceGateBlock || n.is(BlockTags.PLANKS)
                || n.is(BlockTags.LOGS) || n.is(Blocks.HAY_BLOCK));
    }

    @Nullable
    private static BlockState base(int x, int y, int z, boolean doorShut) {
        if (!occupies(x, y, z)) return null;
        if (y == 15) return Blocks.DARK_OAK_SLAB.defaultBlockState();                         // the ridge of the roof
        if (wall(x, z)) {
            if (z == 0 && x >= DOOR_X0 && x <= DOOR_X1 && y >= 1 && y <= 4) return doorShut ? HULL : AIR;   // "the door ... in the side"
            if (y == 13) return Blocks.DARK_OAK_FENCE.defaultBlockState();                     // "a window ... in a cubit ... above"
            if (y == 2 || y == 7 || y == 11) {                                                 // strakes of the hull
                boolean alongX = wall(x - 1, z) && wall(x + 1, z);
                return Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                        alongX ? Direction.Axis.X : Direction.Axis.Z);
            }
            return HULL;   // "pitch it within and without with pitch"
        }
        if (y == 0) return HULL;
        if (y == 14) return DECK;
        for (int lx : LADDERS) {
            if (z == MID && x == lx + 1 && y <= 13) return Blocks.SPRUCE_LOG.defaultBlockState();
            if (z == MID && x == lx && y <= 11) return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
        }
        if (y == 5 || y == 10) return DECK;                                                    // "lower, second, and third stories"
        if (z == MID && x % 12 == 6 && (y == 4 || y == 9 || y == 13)) {
            return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        }
        int floor = y < 5 ? 0 : y < 10 ? 5 : 10;
        if (floor < 10) return stall(x, y, z, floor);
        return quarters(x, y, z);
    }

    /** "Rooms shalt thou make in the ark": stalls along both sides of the lower and second decks, a corridor between. */
    @Nullable
    private static BlockState stall(int x, int y, int z, int floor) {
        boolean sideA = z >= 1 && z <= 8, sideB = z >= 14 && z <= 21;
        if (!(sideA || sideB) || x < 8 || x > 128) return AIR;
        boolean hall = floor == 0 && sideA && x > 64 && x < 72;   // the way in from the door
        if (y == floor + 1) {
            if (x % 8 == 0) return Blocks.SPRUCE_FENCE.defaultBlockState();
            if (!hall && (z == 8 || z == 14)) {
                return x % 8 == 4 ? Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH)
                        : Blocks.SPRUCE_FENCE.defaultBlockState();
            }
            if (!hall && x % 8 == 2 && (z == 2 || z == 20)) return Blocks.HAY_BLOCK.defaultBlockState();   // "of all food that is eaten" (6:21)
        }
        return AIR;
    }

    /** The upper deck: beds for Noah, his wife, his sons and their wives, and the stores. */
    private static BlockState quarters(int x, int y, int z) {
        if (y == 11 && x >= 61 && x <= 75 && x % 2 == 1) {
            if (z == 2) return Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD);
            if (z == 3) return Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT);
        }
        if (y == 11 && z == 20 && x >= 40 && x <= 96 && x % 3 == 0) return x % 2 == 0 ? Blocks.BARREL.defaultBlockState() : Blocks.HAY_BLOCK.defaultBlockState();
        if (y == 11 && z == 16 && x == 68) return Blocks.CRAFTING_TABLE.defaultBlockState();
        return AIR;
    }

    // ------------------------------------------------------------------ places in it

    /** The middle of the stall `index` (0..), for the beasts that go in "two and two" (7:9). */
    public static BlockPos stall(BlockPos origin, int index) {
        List<int[]> stalls = new ArrayList<>();
        for (int floor : new int[]{0, 5}) {
            for (int k = 1; k <= 15; k++) {
                for (int side = 0; side < 2; side++) {
                    if (floor == 0 && side == 0 && k == 8) continue;   // the hall
                    stalls.add(new int[]{8 * k + 4, floor + 1, side == 0 ? 4 : 18});
                }
            }
        }
        int[] s = stalls.get(Math.floorMod(index, stalls.size()));
        return origin.offset(s[0], s[1], s[2]);
    }

    /** Where those aboard live: the upper deck, near the beds. */
    public static BlockPos quarters(BlockPos origin, int index) {
        return origin.offset(62 + (index % 8) * 2, 11, 7 + (index / 8) % 6);
    }

    /** Just outside the door, on the ground. */
    public static BlockPos door(BlockPos origin) {
        return origin.offset(DOOR_X0 + 1, 0, -2);
    }

    public static BlockPos centre(BlockPos origin) {
        return origin.offset(LENGTH / 2, 0, MID);
    }

    public static AABB bounds(BlockPos origin) {
        return new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + LENGTH, origin.getY() + HEIGHT, origin.getZ() + BREADTH);
    }

    public static boolean contains(BlockPos origin, int wx, int wy, int wz) {
        return occupies(wx - origin.getX(), wy - origin.getY(), wz - origin.getZ());
    }

    // ------------------------------------------------------------------ building

    /**
     * Levels the ground for the ark: clears what stands where it will be and a little around it,
     * and fills hollows beneath it with earth.
     */
    public static void prepareSite(ServerLevel level, BlockPos origin) {
        for (int x = -3; x < LENGTH + 3; x++) {
            for (int z = -3; z < BREADTH + 3; z++) {
                int wx = origin.getX() + x, wz = origin.getZ() + z;
                for (int y = 0; y < HEIGHT + 6; y++) {
                    BlockPos p = new BlockPos(wx, origin.getY() + y, wz);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, AIR, Block.UPDATE_CLIENTS);
                }
                for (int y = 1; y <= 10; y++) {
                    BlockPos p = new BlockPos(wx, origin.getY() - y, wz);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced()) break;
                    level.setBlock(p, y == 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /** Builds the next `count` places of the ark (from the keel up). Returns the new progress; done at {@link #total()}. */
    public static int build(ServerLevel level, BlockPos origin, int progress, int count) {
        int end = Math.min(TOTAL, progress + count);
        for (int i = progress; i < end; i++) {
            int z = i % BREADTH, x = (i / BREADTH) % LENGTH, y = i / (BREADTH * LENGTH);
            BlockState s = design(x, y, z, false);
            if (s == null || s.isAir()) continue;
            level.setBlock(origin.offset(x, y, z), s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        return end;
    }

    public static int total() {
        return TOTAL;
    }

    /** "And the LORD shut him in" (7:16) - or, when the earth is dry, the door is opened again. */
    public static void setDoor(ServerLevel level, BlockPos origin, boolean shut) {
        for (int x = DOOR_X0; x <= DOOR_X1; x++) {
            for (int y = 1; y <= 4; y++) level.setBlock(origin.offset(x, y, 0), shut ? HULL : AIR, Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ floating

    /**
     * Moves the ark from `from` to `to`, changing only the blocks that differ, and carries every
     * creature in or on it along. Places it leaves become `outside.apply(y)` (water below the
     * waterline, air above) where `flooded` holds for the column, and air elsewhere.
     */
    public static void move(ServerLevel level, BlockPos from, BlockPos to, boolean doorShut, IntFunction<BlockState> outside, LongPredicate flooded,
                            @Nullable java.util.function.Consumer<BlockPos> wetted) {
        if (from.equals(to)) return;
        int dx = to.getX() - from.getX(), dy = to.getY() - from.getY(), dz = to.getZ() - from.getZ();
        List<Entity> aboard = level.getEntitiesOfClass(Entity.class, bounds(from).inflate(0.5, 0, 0.5).expandTowards(0, 2, 0),
                e -> !e.isPassenger() && !(e instanceof net.minecraft.world.entity.LightningBolt));
        int x0 = Math.min(from.getX(), to.getX()), y0 = Math.min(from.getY(), to.getY()), z0 = Math.min(from.getZ(), to.getZ());
        int x1 = Math.max(from.getX(), to.getX()) + LENGTH, y1 = Math.max(from.getY(), to.getY()) + HEIGHT, z1 = Math.max(from.getZ(), to.getZ()) + BREADTH;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                for (int z = z0; z < z1; z++) {
                    BlockState was = design(x - from.getX(), y - from.getY(), z - from.getZ(), doorShut);
                    BlockState now = design(x - to.getX(), y - to.getY(), z - to.getZ(), doorShut);
                    if (now == null && was == null) continue;
                    BlockState put;
                    if (now != null) {
                        if (now.equals(was)) continue;
                        put = now;
                    } else {
                        boolean wet = flooded.test(BlockPos.asLong(x, 0, z));
                        put = wet ? outside.apply(y) : AIR;
                        if (wet && !put.isAir() && wetted != null) wetted.accept(new BlockPos(x, y, z));
                    }
                    p.set(x, y, z);
                    level.setBlock(p, put, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
        for (Entity e : aboard) {
            if (e instanceof ServerPlayer sp) sp.teleportTo(level, sp.getX() + dx, sp.getY() + dy, sp.getZ() + dz, sp.getYRot(), sp.getXRot());
            else e.teleportTo(e.getX() + dx, e.getY() + dy, e.getZ() + dz);
            e.resetFallDistance();
        }
    }

    /** The lowest the ark's keel may lie with its corner over `at`: just above the highest ground beneath it (the ark itself at `now` aside). */
    public static int restingY(ServerLevel level, BlockPos at, BlockPos now) {
        int best = level.getMinBuildHeight();
        for (int x = 0; x < LENGTH; x += 2) {
            for (int z = 0; z < BREADTH; z += 2) {
                if (!inside(x, z)) continue;
                best = Math.max(best, FloodWaters.ground(level, at.getX() + x, at.getZ() + z, now) + 1);
            }
        }
        return best;
    }
}
