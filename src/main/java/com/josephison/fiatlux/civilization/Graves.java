package com.josephison.fiatlux.civilization;

import com.josephison.fiatlux.cinema.Cinema;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.cosmos.MortalEra;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "Dust thou art, and unto dust shalt thou return" (Genesis 3:19). The dead are buried in their town's
 * burying place: a yard outside the town, walled as its age walls such places, with a marker of that
 * age at each head (a heap of stones, a standing stone, a carved slab, a headstone or a cross) and, once
 * men write, their name, their years and a word about them. Those near come to the grave and mourn.
 * Rulers, prophets and the first parents are laid in a raised tomb. One who dies far from any town is
 * buried where they fell.
 */
public final class Graves {
    /** Graves in each yard: six across, four deep. */
    private static final int PER_YARD = 24;
    /** Yards a town may have before its burying place is full (the rest lie unmarked). */
    private static final int YARDS = 4;
    private static final int HALF = 7;

    private Graves() {}

    /** Lays the dead to rest (called as a mortal dies). */
    public static void bury(ServerLevel level, MortalEntity dead, @Nullable Person p, boolean saved) {
        if (!FiatLuxConfig.COMMON.graves.get()) return;
        Planet world = dead.planet();
        if (world == null) return;
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(level.getServer());
        Settlement town = cosmos.townNear(world.id, dead.blockPosition());
        if (town != null && town.centre.distSqr(dead.blockPosition()) > 160 * 160) town = null;
        layToRest(level, dead, p, saved, world.era, town, world.name);
        if (town != null) cosmos.setDirty();
    }

    /**
     * Digs the grave and sets up its marker: in the town's burying place (choosing one if it has none),
     * or where they fell if there is no town or no room. Returns the head of the grave, or null.
     */
    @Nullable
    public static BlockPos layToRest(ServerLevel level, MortalEntity dead, @Nullable Person p, boolean saved, MortalEra era,
                                     @Nullable Settlement town, String worldName) {
        BlockPos head = null;
        String where = "where they fell, on " + worldName;
        if (town != null) {
            if (town.graveyard == null) town.graveyard = chooseYard(level, town);
            if (town.graveyard != null) {
                head = nextPlot(level, town, era);
                if (head == null) return null;   // the burying place is full: they lie there unmarked
                where = "in the burying place of " + (town.name.isEmpty() ? worldName : town.name);
            }
        }
        if (head == null) head = Ground.naturalGround(level, dead.getBlockX(), dead.getBlockZ(), 8);
        if (head == null) return null;
        boolean great = dead.isFirstParent() || prophet(level, dead) || ruler(level, dead);
        boolean church = church(level, dead);
        marker(level, head, era, great, church, inscription(level, dead, saved, church, town));
        if (p != null) {
            p.grave = head;
            p.buried = where;
            Generations.get(level.getServer()).setDirty();
        }
        funeral(level, dead, head, church);
        return head;
    }

    // ------------------------------------------------------------------ where

    /** A yard outside the town on open natural ground, clear of its houses, temple and field; westward if it can be. */
    @Nullable
    private static BlockPos chooseYard(ServerLevel level, Settlement town) {
        for (int r = 30; r <= 72; r += 6) {
            for (int k = 0; k < 12; k++) {
                double a = Math.PI + (k % 2 == 0 ? 1 : -1) * ((k + 1) / 2) * Math.PI / 6;   // west first, then either side of it
                int x = town.centre.getX() + (int) Math.round(Math.cos(a) * r), z = town.centre.getZ() + (int) Math.round(Math.sin(a) * r);
                BlockPos c = new BlockPos(x, 0, z);
                if (town.shrine != null && dist2(c, town.shrine) < 24 * 24) continue;
                if (town.field != null && dist2(c, town.field) < 20 * 20) continue;
                if (town.houses.stream().anyMatch(h -> dist2(c, h) < 18 * 18)) continue;
                boolean open = true;
                int y0 = Ground.surface(level, x, z);
                for (int dx = -HALF; dx <= HALF && open; dx += HALF) {
                    for (int dz = -HALF; dz <= HALF && open; dz += HALF) {
                        if (!Ground.naturalAt(level, x + dx, z + dz) || Math.abs(Ground.surface(level, x + dx, z + dz) - y0) > 3) open = false;
                    }
                }
                if (open) return new BlockPos(x, y0, z);
            }
        }
        return null;
    }

    private static double dist2(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    /** The centre of the town's k-th yard: each new one beside the last, further from the town. */
    private static BlockPos yard(Settlement town, int k) {
        int dx = town.graveyard.getX() - town.centre.getX(), dz = town.graveyard.getZ() - town.centre.getZ();
        int sx = Math.abs(dx) >= Math.abs(dz) ? Integer.signum(dx) : 0, sz = sx == 0 ? Integer.signum(dz) : 0;
        if (sx == 0 && sz == 0) sx = -1;
        return town.graveyard.offset(sx * (2 * HALF + 1) * k, 0, sz * (2 * HALF + 1) * k);
    }

    /** The head of the next grave in the town's burying place (walling a new yard when one is begun), or null if it is full. */
    @Nullable
    private static BlockPos nextPlot(ServerLevel level, Settlement town, MortalEra era) {
        for (int tries = 0; tries < PER_YARD && town.graves < PER_YARD * YARDS; tries++) {
            int n = town.graves++;
            int k = n / PER_YARD, slot = n % PER_YARD;
            BlockPos c = yard(town, k);
            if (slot == 0) {
                c = level(level, c);
                wall(level, town, c, era);
            }
            int x = c.getX() - 5 + (slot % 6) * 2, z = c.getZ() - 5 + (slot / 6) * 3;
            if (!Ground.naturalAt(level, x, z) || !Ground.naturalAt(level, x, z + 1)) continue;   // something stands there
            return new BlockPos(x, Ground.surface(level, x, z), z);
        }
        return null;
    }

    /** Levels a yard's ground to the height of its centre (only earth and plants are moved), so its rows lie even. */
    private static BlockPos level(ServerLevel level, BlockPos c) {
        int y0 = Ground.surface(level, c.getX(), c.getZ());
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                int x = c.getX() + dx, z = c.getZ() + dz;
                if (!Ground.naturalAt(level, x, z)) continue;
                int top = Ground.surface(level, x, z);
                if (Math.abs(top - y0) > 4) continue;
                for (int y = y0; y < top; y++) {   // cut down
                    BlockPos at = new BlockPos(x, y, z);
                    BlockState st = level.getBlockState(at);
                    if (Ground.natural(st) || st.canBeReplaced()) level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                for (int y = top - 1; y < y0 - 1; y++) level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);   // fill up
                level.setBlock(new BlockPos(x, y0 - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                BlockPos above = new BlockPos(x, y0, z);
                if (level.getBlockState(above).canBeReplaced() && !level.getBlockState(above).isAir()) level.removeBlock(above, false);
            }
        }
        return new BlockPos(c.getX(), y0, c.getZ());
    }

    /** The wall or fence of a yard, as its age makes them, with a gap toward the town. */
    private static void wall(ServerLevel level, Settlement town, BlockPos c, MortalEra era) {
        Block fence = switch (era) {
            case HUNTER_GATHERER -> null;
            case AGRARIAN -> Blocks.OAK_FENCE;
            case CLASSICAL -> Blocks.STONE_BRICK_WALL;
            case MEDIEVAL -> Blocks.MOSSY_COBBLESTONE_WALL;
            case INDUSTRIAL -> Blocks.IRON_BARS;
            case SPACEFARING -> Blocks.WHITE_STAINED_GLASS_PANE;
        };
        if (fence == null) return;
        Direction toTown = Direction.getNearest(town.centre.getX() - c.getX(), 0, town.centre.getZ() - c.getZ());
        List<BlockPos> placed = new ArrayList<>();
        for (int i = -HALF; i <= HALF; i++) {
            for (int side = 0; side < 4; side++) {
                int x = side < 2 ? i : side == 2 ? -HALF : HALF, z = side < 2 ? (side == 0 ? -HALF : HALF) : i;
                if (side >= 2 && Math.abs(i) == HALF) continue;   // corners once
                Direction face = side == 0 ? Direction.NORTH : side == 1 ? Direction.SOUTH : side == 2 ? Direction.WEST : Direction.EAST;
                if (face == toTown && Math.abs(i) <= 1) continue;   // the gate
                int wx = c.getX() + x, wz = c.getZ() + z;
                if (!Ground.naturalAt(level, wx, wz)) continue;
                BlockPos at = new BlockPos(wx, Ground.surface(level, wx, wz), wz);
                level.setBlock(at, fence.defaultBlockState(), Block.UPDATE_CLIENTS);
                placed.add(at);
            }
        }
        for (BlockPos at : placed) {
            BlockState s = Block.updateFromNeighbourShapes(level.getBlockState(at), level, at);
            level.setBlock(at, s, Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ the grave

    /** The marker at the head (and over the body for the great), with the inscription on its face toward the north. */
    private static void marker(ServerLevel level, BlockPos head, MortalEra era, boolean great, boolean church, String[] words) {
        BlockPos body = head.south();
        level.setBlock(body.below(), Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL);   // the mound
        level.setBlock(head.below(), Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL);
        BlockState stone, top = null, lid = null;
        switch (era) {
            case HUNTER_GATHERER -> {
                stone = Blocks.COBBLESTONE.defaultBlockState();   // a heap of stones
                if (great) top = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            }
            case AGRARIAN -> {
                stone = Blocks.STONE.defaultBlockState();   // a standing stone
                if (great) top = Blocks.STONE.defaultBlockState();
            }
            case CLASSICAL -> {
                stone = (great ? Blocks.CHISELED_STONE_BRICKS : Blocks.SMOOTH_STONE).defaultBlockState();
                lid = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
            }
            case MEDIEVAL -> {
                stone = (great ? Blocks.STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS).defaultBlockState();
                top = (church ? Blocks.STONE_BRICK_WALL : Blocks.MOSSY_STONE_BRICK_WALL).defaultBlockState();
                if (great) lid = Blocks.POLISHED_ANDESITE.defaultBlockState();
            }
            case INDUSTRIAL -> {
                stone = Blocks.POLISHED_ANDESITE.defaultBlockState();
                if (great) top = Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState();
                lid = Blocks.POTTED_POPPY.defaultBlockState();
            }
            default -> {
                stone = Blocks.SMOOTH_QUARTZ.defaultBlockState();
                if (great) top = Blocks.QUARTZ_PILLAR.defaultBlockState();
                lid = Blocks.POTTED_LILY_OF_THE_VALLEY.defaultBlockState();
            }
        }
        level.setBlock(head, stone, Block.UPDATE_ALL);
        level.setBlock(head.above(), top != null ? top : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (top != null) level.setBlock(head.above(), Block.updateFromNeighbourShapes(top, level, head.above()), Block.UPDATE_ALL);
        level.setBlock(body, lid != null ? lid : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        // before men write, a grave has no name on it
        if (era != MortalEra.HUNTER_GATHERER && words.length > 0) {
            BlockPos at = head.north();
            BlockState board = Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH);
            if (!level.getBlockState(at).canBeReplaced() && level.getBlockState(head.above()).isAir()) {
                at = head.above();   // no room before the stone: the words stand on top of it
                board = Blocks.SPRUCE_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION, 8);
            }
            if (level.getBlockState(at).canBeReplaced()) {
                level.setBlock(at, board, Block.UPDATE_ALL);
                if (level.getBlockEntity(at) instanceof SignBlockEntity sign) {
                    SignText text = new SignText();
                    for (int i = 0; i < Math.min(4, words.length); i++) text = text.setMessage(i, Component.literal(words[i]));
                    sign.setText(text, true);
                    sign.setWaxed(true);
                    sign.setChanged();
                    level.sendBlockUpdated(at, level.getBlockState(at), level.getBlockState(at), Block.UPDATE_ALL);
                }
            }
        }
        level.sendParticles(ParticleTypes.WHITE_ASH, head.getX() + 0.5, head.getY() + 0.5, head.getZ() + 1, 20, 0.6, 0.3, 0.8, 0.01);
    }

    /** Name, years, and a word about the one who lies there, each line short enough for a stone. */
    private static String[] inscription(ServerLevel level, MortalEntity dead, boolean saved, boolean church, @Nullable Settlement town) {
        String name = dead.getName().getString();
        String first = (church ? "✝ " : "") + name;
        int years = (int) dead.ageYears();
        String age = years < 2 ? "an infant" : "aged " + years;
        String[] epitaph;
        if (dead.isFirstParent()) epitaph = dead.isMale() ? new String[]{"Father of all", "the living"} : new String[]{"Mother of all", "living"};
        else if (prophet(level, dead)) epitaph = new String[]{"A prophet", "of the Most High"};
        else if (ruler(level, dead)) epitaph = new String[]{"Who ruled", town == null || town.name.isEmpty() ? "the people" : town.name};
        else if (years < 12) epitaph = new String[]{"Of such is the", "kingdom"};
        else if (!saved) epitaph = new String[]{"Dust to dust", ""};
        else if (church) epitaph = new String[]{"Asleep in", "the Lord"};
        else epitaph = level.random.nextBoolean() ? new String[]{"Rest in", "peace"} : new String[]{"Dust thou art", ""};
        return new String[]{trim(first), age, trim(epitaph[0]), trim(epitaph[1])};
    }

    private static String trim(String s) {
        return s.length() <= 15 ? s : s.substring(0, 15);
    }

    private static boolean prophet(ServerLevel level, MortalEntity m) {
        return com.josephison.fiatlux.prophecy.Prophets.called(level.getServer(), m.getUUID());
    }

    private static boolean ruler(ServerLevel level, MortalEntity m) {
        Polity g = Justice.polityOf(level, m);
        return g != null && m.getUUID().equals(g.ruler);
    }

    /** Of the Son's church (or a church that came of it)? */
    private static boolean church(ServerLevel level, MortalEntity m) {
        if (m.religion() == null) return false;
        Generations gens = Generations.get(level.getServer());
        java.util.UUID son = com.josephison.fiatlux.son.SonData.get(level.getServer()).church;
        for (Religion r = gens.religion(m.religion()); r != null; r = r.parent == null ? null : gens.religion(r.parent)) {
            if (r.id.equals(son)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ the mourners

    /** Their family and neighbours come to the grave; the eldest of them speaks over it. */
    private static void funeral(ServerLevel level, MortalEntity dead, BlockPos head, boolean church) {
        java.util.UUID id = dead.getUUID();
        List<MortalEntity> near = new ArrayList<>(level.getEntitiesOfClass(MortalEntity.class, dead.getBoundingBox().inflate(48),
                m -> m != dead && m.isAlive() && !m.isPossessed() && m.sentence() == Justice.Sentence.NONE));
        near.sort(Comparator.<MortalEntity>comparingInt(m -> id.equals(m.spouseId()) || id.equals(m.fatherId()) || id.equals(m.motherId())
                || m.getUUID().equals(dead.fatherId()) || m.getUUID().equals(dead.motherId()) ? 0 : 1).thenComparingDouble(m -> m.distanceToSqr(dead)));
        List<MortalEntity> mourners = near.subList(0, Math.min(6, near.size()));
        if (mourners.isEmpty()) return;
        for (int i = 0; i < mourners.size(); i++) {
            MortalEntity m = mourners.get(i);
            m.getNavigation().moveTo(head.getX() + 0.5 + (i - mourners.size() / 2.0) * 1.2, head.getY(), head.getZ() - 2.5, 0.7);
        }
        MortalEntity eldest = mourners.stream().filter(m -> !m.isChild()).max(Comparator.comparingDouble(MortalEntity::ageYears)).orElse(mourners.getFirst());
        String words = church ? "I am the resurrection, and the life: he that believeth in me, though he were dead, yet shall he live."
                : level.random.nextBoolean() ? "Dust thou art, and unto dust shalt thou return."
                : "The Creator gave, and the Creator hath taken away; blessed be the name of the Creator.";
        Cinema.later(100, () -> {
            for (MortalEntity m : mourners) {
                if (!m.isAlive()) continue;
                m.getLookControl().setLookAt(head.getX() + 0.5, head.getY() + 0.5, head.getZ() + 0.5);
                m.beginAct(MortalEntity.BOW, 80);
                if (m != eldest && level.random.nextInt(3) == 0) m.say(MortalSpeech.Topic.MOURNING);
            }
            if (eldest.isAlive()) eldest.say(words);
            level.sendParticles(ParticleTypes.WHITE_ASH, head.getX() + 0.5, head.getY() + 1, head.getZ() + 1, 30, 1.2, 0.5, 1.2, 0.01);
        });
    }

    // ------------------------------------------------------------------ the grave opened

    /**
     * "Lazarus, come forth" (John 11:43): the grave of one raised from the dead is opened, its stone
     * taken away. Returns where they stand when they come out of it, or null if they had no grave here.
     */
    @Nullable
    public static BlockPos open(ServerLevel level, Person p) {
        BlockPos head = p.grave;
        if (head == null || !level.isLoaded(head)) return null;
        level.removeBlock(head.north(), false);
        level.removeBlock(head.above(), false);
        level.removeBlock(head, false);
        level.removeBlock(head.south(), false);
        level.setBlock(head.south().below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);   // the empty grave
        level.sendParticles(ParticleTypes.CLOUD, head.getX() + 0.5, head.getY() + 0.5, head.getZ() + 1, 30, 0.5, 0.5, 0.8, 0.02);
        p.grave = null;
        p.buried = "";
        return head.north(2);
    }
}
