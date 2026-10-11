package com.josephison.fiatlux.eden;

import com.josephison.fiatlux.registry.ModBlocks;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.DoublePlantBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * "And the LORD God planted a garden eastward in Eden; and there he put the man whom he had formed.
 * And out of the ground made the LORD God to grow every tree that is pleasant to the sight, and good
 * for food; the tree of life also in the midst of the garden, and the tree of knowledge of good and
 * evil. And a river went out of Eden to water the garden; and from thence it was parted, and became
 * into four heads" (Genesis 2:8-10).
 *
 * <p>A round garden behind a flowering hedge: a spring at its heart parting into four rivers that run
 * out through the hedge - Pison, which compasses the land of gold, bdellium and the onyx stone,
 * Gihon, Hiddekel and Euphrates (2:11-14) - an orchard of fruit trees among flowers, and in the midst
 * two great trees: the tree of life, gold-leaved and hung with light, and the tree of knowledge, dark
 * and hung with red-gold fruit. The beasts of the field roam it in pairs.
 */
public final class EdenGarden {
    public static final int RADIUS = 30;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private EdenGarden() {}

    /** Plants the garden around centre (the ground at its heart) and records it in the cosmos. */
    public static void plant(ServerLevel level, BlockPos centre, @Nullable UUID world) {
        RandomSource r = RandomSource.create(centre.asLong());
        int y0 = centre.getY();
        int cx = centre.getX(), cz = centre.getZ();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState(), dirt = Blocks.DIRT.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        // the ground: levelled, green and watered
        for (int dx = -RADIUS - 3; dx <= RADIUS + 3; dx++) {
            for (int dz = -RADIUS - 3; dz <= RADIUS + 3; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > RADIUS + 3) continue;
                int x = cx + dx, z = cz + dz;
                for (int y = y0 - 5; y < y0 - 1; y++) if (level.getBlockState(new BlockPos(x, y, z)).isAir()) set(level, x, y, z, dirt);
                set(level, x, y0 - 1, z, d < 3.5 ? Blocks.MOSS_BLOCK.defaultBlockState() : grass);
                for (int y = y0; y < y0 + 16; y++) set(level, x, y, z, air);
                if (d >= RADIUS - 1 && d < RADIUS + 0.5 && !gate(dx, dz)) {   // the hedge
                    set(level, x, y0, z, Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                    set(level, x, y0 + 1, z, (dx + dz) % 3 == 0 ? Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true)
                            : Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                }
            }
        }
        // the land around it falls away in gentle terraces down to the garden, so nothing walls it in
        BlockState air2 = Blocks.AIR.defaultBlockState();
        for (int dx = -RADIUS - 14; dx <= RADIUS + 14; dx++) {
            for (int dz = -RADIUS - 14; dz <= RADIUS + 14; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= RADIUS + 3 || d > RADIUS + 14) continue;
                int x = cx + dx, z = cz + dz;
                int top = y0 + (int) ((d - RADIUS - 3) * 1.3);
                boolean cut = false;
                for (int y = y0 + 48; y >= top; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, air2, FLAGS);
                        cut = true;
                    }
                }
                if (cut) set(level, x, top - 1, z, grass);
            }
        }
        for (int dx = -RADIUS - 3; dx <= RADIUS + 3; dx++) {   // and nothing hangs over it
            for (int dz = -RADIUS - 3; dz <= RADIUS + 3; dz++) {
                if (dx * dx + dz * dz > (RADIUS + 3) * (RADIUS + 3)) continue;
                for (int y = y0 + 16; y <= y0 + 48; y++) {
                    BlockPos p = new BlockPos(cx + dx, y, cz + dz);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, air2, FLAGS);
                }
            }
        }
        // the spring at its heart, and the four heads of the river
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (dx * dx + dz * dz <= 5) {
                set(level, cx + dx, y0 - 2, cz + dz, Blocks.CLAY.defaultBlockState());
                set(level, cx + dx, y0 - 1, cz + dz, Blocks.WATER.defaultBlockState());
            }
        }
        for (Direction dir : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            for (int t = 2; t <= RADIUS + 6; t++) {
                for (int w = 0; w <= 1; w++) {
                    int x = cx + dir.getStepX() * t + (dir.getStepX() == 0 ? w : 0);
                    int z = cz + dir.getStepZ() * t + (dir.getStepZ() == 0 ? w : 0);
                    set(level, x, y0 - 2, z, Blocks.CLAY.defaultBlockState());
                    set(level, x, y0 - 1, z, Blocks.WATER.defaultBlockState());
                    set(level, x, y0, z, air);
                    set(level, x, y0 + 1, z, air);
                }
            }
            // its name, where it leaves the spring
            String name = switch (dir) {
                case NORTH -> "Pison";
                case EAST -> "Hiddekel";
                case SOUTH -> "Gihon";
                default -> "Euphrates";
            };
            int sx = cx + dir.getStepX() * 5 + (dir.getStepX() == 0 ? 3 : 0), sz = cz + dir.getStepZ() * 5 + (dir.getStepZ() == 0 ? 3 : 0);
            sign(level, new BlockPos(sx, y0, sz), dir, name);
        }
        // "the gold of that land is good: there is bdellium and the onyx stone" (2:12): along the Pison
        for (int t = 8; t <= RADIUS - 3; t += 4) {
            int x = cx + (r.nextBoolean() ? -1 : 2), z = cz - t;
            set(level, x, y0 - 1, z, r.nextInt(3) == 0 ? Blocks.RAW_GOLD_BLOCK.defaultBlockState()
                    : r.nextBoolean() ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.AMETHYST_BLOCK.defaultBlockState());
        }
        // the two trees in the midst of the garden
        BlockPos life = new BlockPos(cx - 9, y0, cz - 9), knowledge = new BlockPos(cx + 9, y0, cz + 9);
        greatTree(level, life, Blocks.STRIPPED_BIRCH_LOG.defaultBlockState(), ModBlocks.LIFE_LEAVES.get().defaultBlockState(),
                ModBlocks.LIFE_FRUIT.get().defaultBlockState(), r);
        greatTree(level, knowledge, Blocks.DARK_OAK_LOG.defaultBlockState(), ModBlocks.KNOWLEDGE_LEAVES.get().defaultBlockState(),
                ModBlocks.KNOWLEDGE_FRUIT.get().defaultBlockState(), r);
        // "every tree that is pleasant to the sight, and good for food" (2:9), among flowers
        for (int gx = -RADIUS + 4; gx <= RADIUS - 4; gx += 6) {
            for (int gz = -RADIUS + 4; gz <= RADIUS - 4; gz += 6) {
                int dx = gx + r.nextInt(3) - 1, dz = gz + r.nextInt(3) - 1;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > RADIUS - 4 || d < 6 || Math.abs(dx) < 4 || Math.abs(dz) < 4) continue;
                if (near(dx, dz, -9, -9, 7) || near(dx, dz, 9, 9, 7)) continue;
                fruitTree(level, new BlockPos(cx + dx, y0, cz + dz), r);
            }
        }
        BlockState[] flowers = {Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(),
                Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.ALLIUM.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(),
                Blocks.LILY_OF_THE_VALLEY.defaultBlockState(), Blocks.BLUE_ORCHID.defaultBlockState(), Blocks.PINK_TULIP.defaultBlockState(),
                Blocks.SHORT_GRASS.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState(), Blocks.FERN.defaultBlockState()};
        for (int i = 0; i < 520; i++) {
            int dx = r.nextInt(RADIUS * 2) - RADIUS, dz = r.nextInt(RADIUS * 2) - RADIUS;
            if (dx * dx + dz * dz > (RADIUS - 2) * (RADIUS - 2)) continue;
            BlockPos p = new BlockPos(cx + dx, y0, cz + dz);
            if (!level.getBlockState(p).isAir() || !level.getBlockState(p.below()).is(Blocks.GRASS_BLOCK)) continue;
            if (r.nextInt(14) == 0) {
                BlockState tall = (r.nextBoolean() ? Blocks.ROSE_BUSH : r.nextBoolean() ? Blocks.PEONY : Blocks.LILAC).defaultBlockState();
                set(level, p.getX(), p.getY(), p.getZ(), tall.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
                set(level, p.getX(), p.getY() + 1, p.getZ(), tall.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
            } else {
                set(level, p.getX(), p.getY(), p.getZ(), flowers[r.nextInt(flowers.length)]);
            }
        }
        // the beasts of the field, two by two (2:19)
        for (EntityType<? extends Mob> type : List.of(EntityType.SHEEP, EntityType.RABBIT, EntityType.FOX, EntityType.HORSE, EntityType.PARROT,
                EntityType.OCELOT, EntityType.COW, EntityType.GOAT)) {
            for (int k = 0; k < 2; k++) {
                Mob m = type.create(level);
                if (m == null) continue;
                double a = r.nextDouble() * Math.PI * 2, d = 8 + r.nextDouble() * (RADIUS - 12);
                int x = cx + (int) (Math.cos(a) * d), z = cz + (int) (Math.sin(a) * d);
                m.moveTo(x + 0.5, y0 + 1, z + 0.5, r.nextFloat() * 360f, 0);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(centre), MobSpawnType.STRUCTURE, null);
                m.setPersistenceRequired();
                level.addFreshEntity(m);
            }
        }
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(level.getServer());
        cosmos.edenDim = level.dimension().location().toString();
        cosmos.edenCentre = centre;
        cosmos.lifeTree = life;
        cosmos.knowledgeTree = knowledge;
        cosmos.edenWorld = world;
        cosmos.edenSince = level.getGameTime();
        cosmos.edenStep = 0;
        cosmos.eden = cosmos.fallOfMan ? EdenState.EXILED : EdenState.INNOCENCE;
        cosmos.setDirty();
        if (cosmos.eden == EdenState.EXILED) EdenEvents.station(level, cosmos, eastGate(centre));
    }

    /** The gaps in the hedge: where the four rivers run out, and the gate on the east (3:24). */
    private static boolean gate(int dx, int dz) {
        return (Math.abs(dx) <= 2 && Math.abs(dz) > RADIUS - 3) || (Math.abs(dz) <= 2 && Math.abs(dx) > RADIUS - 3) || (dx > 0 && Math.abs(dz) <= 3);
    }

    /** Where the east gate stands, at which the cherubim are set. */
    public static BlockPos eastGate(BlockPos centre) {
        return centre.offset(RADIUS + 1, 0, 3);
    }

    /** Whether a place lies within the garden. */
    public static boolean inGarden(BlockPos centre, net.minecraft.world.phys.Vec3 p) {
        double dx = p.x - centre.getX() - 0.5, dz = p.z - centre.getZ() - 0.5;
        return dx * dx + dz * dz < RADIUS * RADIUS && Math.abs(p.y - centre.getY()) < 24;
    }

    private static boolean near(int x, int z, int px, int pz, int r) {
        return (x - px) * (x - px) + (z - pz) * (z - pz) < r * r;
    }

    /** A great tree in the midst of the garden: a broad trunk, a wide crown, and fruit hanging beneath it. */
    private static void greatTree(ServerLevel level, BlockPos base, BlockState log, BlockState leaves, BlockState fruit, RandomSource r) {
        int x = base.getX(), y = base.getY(), z = base.getZ();
        int trunk = 9;
        for (int dy = 0; dy < trunk; dy++) {
            for (int ox = 0; ox <= 1; ox++) for (int oz = 0; oz <= 1; oz++) set(level, x + ox, y + dy, z + oz, log);
        }
        for (int i = 0; i < 4; i++) {   // boughs
            Direction d = Direction.from2DDataValue(i);
            for (int t = 1; t <= 3; t++) set(level, x + d.getStepX() * t + (d.getStepX() > 0 ? 1 : 0), y + trunk - 3 + t / 2,
                    z + d.getStepZ() * t + (d.getStepZ() > 0 ? 1 : 0), log);
        }
        BlockState persistent = leaves.setValue(LeavesBlock.PERSISTENT, true);
        double cx = x + 1, cz = z + 1, cy = y + trunk + 0.5;
        for (int dx = -6; dx <= 6; dx++) for (int dy = -3; dy <= 4; dy++) for (int dz = -6; dz <= 6; dz++) {
            double e = (dx * dx + dz * dz) / 36.0 + (dy * dy) / (dy < 0 ? 9.0 : 16.0);
            if (e > 1 || r.nextInt(12) == 0) continue;
            BlockPos p = BlockPos.containing(cx + dx, cy + dy, cz + dz);
            if (level.getBlockState(p).isAir()) level.setBlock(p, persistent, FLAGS);
        }
        // fruit hangs from the underside of the crown
        int hung = 0;
        for (int tries = 0; tries < 200 && hung < 12; tries++) {
            int dx = r.nextInt(11) - 5, dz = r.nextInt(11) - 5;
            for (int dy = -4; dy <= 0; dy++) {
                BlockPos p = BlockPos.containing(cx + dx, cy + dy, cz + dz);
                if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).is(leaves.getBlock())) {
                    level.setBlock(p, fruit, FLAGS);
                    hung++;
                    break;
                }
            }
        }
    }

    /** One of the trees "pleasant to the sight, and good for food": blossoming cherry, flowering azalea or oak. */
    private static void fruitTree(ServerLevel level, BlockPos base, RandomSource r) {
        int kind = r.nextInt(3);
        BlockState log = kind == 0 ? Blocks.CHERRY_LOG.defaultBlockState() : Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = (kind == 0 ? Blocks.CHERRY_LEAVES : kind == 1 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.OAK_LEAVES)
                .defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        int h = 4 + r.nextInt(2);
        for (int dy = 0; dy < h; dy++) set(level, base.getX(), base.getY() + dy, base.getZ(), log);
        for (int dx = -2; dx <= 2; dx++) for (int dy = h - 2; dy <= h + 1; dy++) for (int dz = -2; dz <= 2; dz++) {
            int dd = dx * dx + dz * dz + (dy - h) * (dy - h);
            if (dd > 6 || (dx == 0 && dz == 0 && dy < h)) continue;
            BlockPos p = base.offset(dx, dy, dz);
            if (level.getBlockState(p).isAir()) level.setBlock(p, leaves, FLAGS);
        }
        BlockPos under = base.offset(r.nextBoolean() ? 1 : -1, 0, r.nextBoolean() ? 2 : -2);
        if (level.getBlockState(under).isAir()) {   // fruit for the gathering
            level.setBlock(under, r.nextBoolean() ? Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE, 3)
                    : Blocks.MELON.defaultBlockState(), FLAGS);
        }
    }

    private static void sign(ServerLevel level, BlockPos pos, Direction facing, String name) {
        int rot = switch (facing) {
            case NORTH -> 8;
            case EAST -> 12;
            case SOUTH -> 0;
            default -> 4;
        };
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rot), FLAGS);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = new SignText().setMessage(1, Component.literal(name)).setMessage(2, Component.literal("~"));
            sign.setText(text, true);
            sign.setText(text, false);
            sign.setChanged();
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState s) {
        level.setBlock(new BlockPos(x, y, z), s, FLAGS);
    }
}
