package com.josephison.fiatlux.civilization;

import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.realm.RealmPool;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.cosmos.MortalEra;
import com.josephison.fiatlux.world.cosmos.Planet;
import com.josephison.fiatlux.world.cosmos.PlanetType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Embodies the simulated civilisation of every living world. While anyone walks a world, the
 * mortals its population would show are kept living around their settlement - a hearth and a
 * shrine with dwellings for their age - and the creatures that world brings forth "after their
 * kind" (Genesis 1:24) roam near them. The very first life breathed in the cosmos brings forth the
 * first man and woman, Adam and Eve (Genesis 2:7, 3:20).
 */
public final class MortalPopulation {
    private static final int INTERVAL = 100;
    private static final double RANGE = 80.0;

    private MortalPopulation() {}

    /** How many mortals a world of this population shows in its settlement. */
    public static int embodied(double population) {
        if (population < 1.0) return 0;
        return Math.max(2, Math.min(18, (int) (2 + Math.log10(population + 1) * 2.5)));
    }

    /** The creatures a world of this type brings forth (empty where nothing could live). */
    public static List<EntityType<? extends Animal>> fauna(PlanetType type) {
        return switch (type) {
            case EARTHLIKE -> List.of(EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN, EntityType.HORSE,
                    EntityType.RABBIT, EntityType.GOAT);
            case CRYSTALLINE -> List.of(EntityType.GOAT, EntityType.RABBIT, EntityType.FOX, EntityType.SHEEP);
            default -> List.of();
        };
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % INTERVAL != 0) return;
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        for (int i = 0; i < Math.min(RealmPool.SIZE, cosmos.planets.size()); i++) {
            Planet p = cosmos.planets.get(i);
            if (!p.hasLife()) continue;
            ServerLevel level = server.getLevel(RealmPool.dimension(i));
            if (level == null || level.players().isEmpty()) continue;
            boolean inEden = p.id.equals(cosmos.edenWorld) && !cosmos.fallOfMan;
            if (com.josephison.fiatlux.flood.FloodData.get(server).upon(p.id)) continue;   // the waters are upon the earth: no town is kept
            if (inEden) continue;   // the first pair live in the garden; towns come after the expulsion
            if (cosmos.townsOf(p.id).isEmpty()) {
                if (p.id.equals(cosmos.edenWorld) && cosmos.eden != com.josephison.fiatlux.eden.EdenState.EXILED
                        && cosmos.eden != com.josephison.fiatlux.eden.EdenState.NONE) continue;   // not until they are sent out
                Settlement first = found(level, p);
                if (first == null) continue;
                first.name = p.name;
                first.founded = level.getGameTime();
                cosmos.addTown(p.id, first);
            }
            List<Settlement> towns = cosmos.townsOf(p.id);
            for (int t = 0; t < towns.size(); t++) {
                Settlement town = towns.get(t);
                if (level.isLoaded(town.centre)) populate(level, cosmos, p, town, t == 0, towns.size());
            }
            expand(level, cosmos, p, towns);
            Government.tick(level, cosmos, p, cosmos.townsOf(p.id));   // their rulers and their laws
            Churches.tick(level, cosmos, p);                            // their faiths: leaders, branches and the faithful who leave
            com.josephison.fiatlux.prophecy.Prophets.worldTick(level, cosmos, p);    // prophets raised, judgments come due
            com.josephison.fiatlux.prophecy.Scripture.worldTick(level, cosmos, p);   // the books they write
        }
    }

    /** Names for the towns a people founds as it spreads, after the cities of Genesis and the land of Israel. */
    private static final String[] TOWN_NAMES = {"Enoch", "Nod", "Irad", "Havilah", "Shinar", "Erech", "Accad", "Calneh", "Nineveh", "Rehoboth",
            "Calah", "Resen", "Ur", "Haran", "Salem", "Zoar", "Gerar", "Beersheba", "Bethel", "Hebron", "Shechem", "Mamre", "Peniel",
            "Succoth", "Ephrath", "Jericho", "Gilgal", "Shiloh", "Ramah", "Mizpah", "Bethlehem", "Cana", "Bethany", "Emmaus", "Joppa"};

    /**
     * "Be fruitful, and multiply, and replenish the earth" (Gen 1:28): when a town is full, or the
     * people have grown beyond what their towns can hold, young families go out and found another a
     * little way off, and it grows in its turn.
     */
    private static void expand(ServerLevel level, CosmicExpanseSavedData cosmos, Planet p, List<Settlement> towns) {
        int max = com.josephison.fiatlux.config.FiatLuxConfig.COMMON.maxTownsPerWorld.get();
        if (towns.size() >= max) return;
        long year = com.josephison.fiatlux.config.FiatLuxConfig.COMMON.mortalYearTicks.get();
        long newest = towns.stream().mapToLong(t -> t.founded).max().orElse(0);
        if (level.getGameTime() - newest < 3 * year) return;
        int perTown = com.josephison.fiatlux.config.FiatLuxConfig.COMMON.maxMortalsPerSettlement.get();
        // the most crowded loaded town sends out its young
        Settlement from = null;
        List<MortalEntity> fromPeople = List.of();
        for (Settlement t : towns) {
            if (!level.isLoaded(t.centre)) continue;
            var people = level.getEntitiesOfClass(MortalEntity.class, new AABB(t.centre).inflate(RANGE));
            if (people.size() > fromPeople.size()) {
                from = t;
                fromPeople = people;
            }
        }
        boolean crowded = from != null && fromPeople.size() >= perTown - 4;
        boolean numerous = Math.ceil(p.population / Math.max(1, p.era.perTown)) > towns.size();
        if (from == null || !(crowded || numerous) || fromPeople.size() < 8) return;
        List<MortalEntity> young = new java.util.ArrayList<>(fromPeople.stream()
                .filter(m -> !m.isChild() && m.ageYears() < 36 && m.isAlive() && !m.isLeaving()
                        && !com.josephison.fiatlux.son.TheSon.isSon(m) && !com.josephison.fiatlux.son.TheSon.disciple(m)
                        && m.sentence() == com.josephison.fiatlux.civilization.Justice.Sentence.NONE).toList());
        if (young.size() < 2) return;
        // a site some way off, clear of every other town
        BlockPos site = null;
        for (int tries = 0; tries < 12 && site == null; tries++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = 90 + level.random.nextInt(90);
            int x = from.centre.getX() + (int) (Math.cos(a) * r), z = from.centre.getZ() + (int) (Math.sin(a) * r);
            BlockPos g = safeGround(level, x, z, 24);
            if (g == null) continue;
            boolean clear = towns.stream().allMatch(t -> t.centre.distSqr(g) > 70 * 70);
            if (clear) site = g;
        }
        if (site == null) return;
        Settlement town = new Settlement(site);
        java.util.Set<String> used = new java.util.HashSet<>();
        towns.forEach(t -> used.add(t.name));
        town.name = java.util.Arrays.stream(TOWN_NAMES).filter(n -> !used.contains(n)).findFirst().orElse(p.name + " " + (towns.size() + 1));
        town.founded = level.getGameTime();
        build(level, town, p.era);
        // a young couple first if there is one, then others of an age to go
        young.sort(java.util.Comparator.comparingDouble(MortalEntity::ageYears));
        List<MortalEntity> going = new java.util.ArrayList<>();
        for (MortalEntity m : young) {
            if (going.size() >= 4) break;
            if (going.contains(m)) continue;
            going.add(m);
            MortalEntity spouse = m.spouseEntity();
            if (spouse != null && young.contains(spouse) && !going.contains(spouse) && going.size() < 4) going.add(spouse);
        }
        for (MortalEntity m : going) m.leaveFor(site);
        town.founders = going.stream().map(m -> m.getName().getString()).collect(java.util.stream.Collectors.joining(", "));
        cosmos.addTown(p.id, town);
        com.josephison.fiatlux.world.CosmosData.get(level.getServer()).record(level.getServer(), town.founders, "town",
                "went out from " + from.name + " and founded " + town.name + " on " + p.name + ".");
    }

    /** Founds a settlement on safe ground near the landing place, and builds it for the people's age. */
    @Nullable
    public static Settlement found(ServerLevel level, Planet p) {
        BlockPos ground = safeGround(level, 14, 14, 40);
        if (ground == null) return null;
        Settlement s = new Settlement(ground);
        build(level, s, p.era);
        return s;
    }

    /**
     * Keeps a living settlement peopled. Its own families grow it by birth; newcomers arrive only to
     * found it and to keep it from dwindling below what its world's population would show.
     */
    private static void populate(ServerLevel level, CosmicExpanseSavedData cosmos, Planet p, Settlement settlement, boolean first, int townCount) {
        BlockPos centre = settlement.centre;
        AABB area = new AABB(centre).inflate(RANGE);
        var people = level.getEntitiesOfClass(MortalEntity.class, area);
        Faiths.expirePetitions(level, settlement, people);
        Faiths.crownShrine(level, settlement, people);
        int have = people.size();
        // newcomers only found the first town and keep a town from dying out: beyond that, it grows by its own births
        int want = first ? (int) Math.min(6, Math.floor(p.population)) : have > 0 ? 0 : 2;
        var giver = cosmos.lifeGiverOf(p);
        if (giver == null && !level.players().isEmpty()) {   // a world given life before its giver was recorded worships the first Creator it sees
            var seer = level.players().getFirst();
            giver = com.mojang.datafixers.util.Pair.of(seer.getUUID(), seer.getGameProfile().getName());
            cosmos.lifeGivers.put(p.id, giver);
            cosmos.setDirty();
        }
        java.util.UUID faith = giver == null ? null
                : Generations.get(level.getServer()).faithOf(giver.getFirst(), giver.getSecond(), p.name, level.getGameTime()).id;
        for (int n = 0; n < Math.min(2, want - have); n++) {
            String name = null;
            Boolean male = null;
            if (!cosmos.firstPairBorn && first) {   // "Adam called his wife's name Eve; because she was the mother of all living" (Gen 3:20)
                name = n == 0 ? "Adam" : "Eve";
                male = n == 0;
                if (n == 1) {
                    cosmos.firstPairBorn = true;
                    cosmos.setDirty();
                }
            }
            BlockPos at = safeGround(level, centre.getX() + level.random.nextInt(13) - 6, centre.getZ() + level.random.nextInt(13) - 6, 8);
            if (at != null) bringForth(level, at, centre, p.era, name, male, name != null ? 24 : 18 + level.random.nextInt(30), null, null, faith);
        }
        var kinds = fauna(p.type);
        if (kinds.isEmpty()) return;
        int animals = level.getEntitiesOfClass(Animal.class, area).size();
        if (animals < 10) {
            var type = kinds.get(level.random.nextInt(kinds.size()));
            BlockPos at = safeGround(level, centre.getX() + level.random.nextInt(41) - 20, centre.getZ() + level.random.nextInt(41) - 20, 12);
            if (at != null) {
                for (int k = 0; k < 2; k++) {   // "two of every sort" - they come in pairs
                    Mob a = type.create(level);
                    if (a == null) continue;
                    a.moveTo(at.getX() + 0.5 + k, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360f, 0);
                    a.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null);
                    level.addFreshEntity(a);
                }
            }
        }
    }

    /** "And the LORD God formed man of the dust of the ground" (Gen 2:7): a mortal rises at pos. */
    @Nullable
    public static MortalEntity bringForth(ServerLevel level, BlockPos pos, @Nullable BlockPos home, MortalEra era, @Nullable String name,
                                          @Nullable Boolean male, double age, @Nullable MortalEntity father, @Nullable MortalEntity mother,
                                          @Nullable java.util.UUID faith) {
        MortalEntity m = ModEntities.MORTAL.get().create(level);
        if (m == null) return null;
        m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0);
        m.setEra(era);
        m.setOrigin(age, father, mother, faith);
        m.becomeLivingSoul(name, male);
        m.setHome(home != null ? home : pos);
        level.addFreshEntity(m);
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                Blocks.DIRT.defaultBlockState()), m.getX(), m.getY() + 0.8, m.getZ(), 40, 0.3, 0.6, 0.3, 0.05);
        Fx.send(level, FxKind.BLESSING, m.position(), 2.4f, 0xFF9CE38C, 30);
        return m;
    }

    /** Solid, dry ground near (x, z): searches outward up to `radius`. */
    @Nullable
    public static BlockPos safeGround(ServerLevel level, int x, int z, int radius) {
        for (int r = 0; r <= radius; r += 3) {
            int steps = r == 0 ? 1 : 12;
            for (int k = 0; k < steps; k++) {
                double a = Math.PI * 2 * k / steps;
                int px = x + (int) Math.round(Math.cos(a) * r), pz = z + (int) Math.round(Math.sin(a) * r);
                level.getChunk(px >> 4, pz >> 4);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
                BlockPos ground = new BlockPos(px, y - 1, pz);
                BlockState s = level.getBlockState(ground);
                if (y > level.getMinBuildHeight() + 1 && s.getFluidState().isEmpty() && s.isFaceSturdy(level, ground, Direction.UP)
                        && !s.is(Blocks.MAGMA_BLOCK)) {
                    return ground.above();
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ the settlement

    /**
     * A hearth ringed with stones at the centre, a shrine of stone with an altar and its light, and
     * four dwellings - hide tents for hunter-gatherers, timber houses with a field of wheat once the
     * people farm.
     */
    public static void build(ServerLevel level, Settlement settlement, MortalEra era) {
        BlockPos c = settlement.centre;
        // the hearth
        set(level, c.below(), Blocks.COBBLESTONE.defaultBlockState());
        set(level, c, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) continue;
            set(level, c.offset(dx * 2, -1, dz * 2), Blocks.COBBLESTONE.defaultBlockState());
            set(level, c.offset(dx * 2, 0, dz * 2), Blocks.COBBLESTONE_SLAB.defaultBlockState());
        }
        // the shrine: "an altar unto the LORD" (Gen 8:20)
        BlockPos sh = safeGround(level, c.getX(), c.getZ() - 8, 3);
        settlement.shrine = sh != null ? sh : c.offset(0, 0, -8);
        BlockPos s = settlement.shrine;
        foundation(level, s, 2);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) set(level, s.offset(dx, -1, dz), Blocks.STONE_BRICKS.defaultBlockState());
        set(level, s, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        set(level, s.above(), com.josephison.fiatlux.registry.ModBlocks.LUX_BLOCK.get().defaultBlockState());
        for (int side = -1; side <= 1; side += 2) {
            for (int y = 0; y <= 3; y++) set(level, s.offset(side * 2, y, -2), Blocks.STONE_BRICK_WALL.defaultBlockState());
            set(level, s.offset(side * 2, 4, -2), Blocks.LANTERN.defaultBlockState());
        }
        settlement.layout = 1;
        int style = Architecture.templeStyle(era);
        if (style > 0) {
            Architecture.temple(level, settlement.shrine, settlement.centre, style);
            settlement.templeStyle = style;
        }
        // the first dwellings
        for (int i = 0; i < 3; i++) raiseHouse(level, settlement, era);
        if (era.ordinal() >= MortalEra.AGRARIAN.ordinal()) settlement.field = field(level, c.offset(0, 0, 18));
    }

    /** Builders raise a new dwelling for the age at the next free spot, with a bed for its sleepers. */
    public static void raiseHouse(ServerLevel level, Settlement settlement, MortalEra era) {
        BlockPos spot = settlement.houseSpot(settlement.houses.size());
        BlockPos g = safeGround(level, spot.getX(), spot.getZ(), 4);
        if (g == null) g = spot;
        BlockPos bed = Architecture.house(level, g, settlement.centre, era);
        settlement.houses.add(g);
        settlement.beds.add(bed);
        settlement.houseStyles.add(era.ordinal());
        if (settlement.field == null && era.ordinal() >= MortalEra.AGRARIAN.ordinal()) {
            settlement.field = field(level, settlement.centre.offset(0, 0, 18));
        }
    }

    /**
     * The builders build anew what is behind the age: a town laid out in the old way is laid out again
     * with room for greater houses; the house of their god is raised for the age; then, one at a time,
     * the houses of an older age are rebuilt. Returns what was done ("" if nothing).
     */
    public static String renew(ServerLevel level, Settlement s, MortalEra era) {
        if (s.layout < 1) {
            for (BlockPos old : s.houses) Architecture.clearOld(level, old);
            int n = Math.max(3, s.houses.size());
            s.houses.clear();
            s.beds.clear();
            s.houseStyles.clear();
            s.layout = 1;
            for (int i = 0; i < n; i++) raiseHouse(level, s, era);
            return "laid out the town anew, with " + n + " houses of the " + era.getSerializedName().replace('_', '-') + " age";
        }
        int style = Architecture.templeStyle(era);
        if (style > s.templeStyle) {
            Architecture.temple(level, s.shrine, s.centre, style);
            s.templeStyle = style;
            return switch (style) {
                case 1 -> "raised a high place of standing stones about the altar";
                case 2 -> "built a temple of columns about the altar";
                case 3 -> "built a church of stone, with a bell tower, about the altar";
                default -> "raised a dome of glass about the altar";
            };
        }
        for (int i = 0; i < s.houses.size(); i++) {
            if (s.houseStyles.get(i) >= era.ordinal()) continue;
            BlockPos g = s.houses.get(i);
            s.beds.set(i, Architecture.house(level, g, s.centre, era));
            s.houseStyles.set(i, era.ordinal());
            return "rebuilt a house in the manner of the " + era.getSerializedName().replace('_', '-') + " age";
        }
        return "";
    }

    /** Builders lay a path of trodden earth from the hearth to a house, and set a light at its door. */
    public static void layPath(ServerLevel level, Settlement settlement) {
        if (settlement.houses.isEmpty()) return;
        BlockPos to = settlement.houses.get(settlement.works % settlement.houses.size());
        settlement.works++;
        BlockPos from = settlement.centre;
        int steps = (int) Math.sqrt(from.distSqr(to));
        for (int i = 2; i < steps - 2; i++) {
            int x = from.getX() + (to.getX() - from.getX()) * i / steps, z = from.getZ() + (to.getZ() - from.getZ()) * i / steps;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos p = new BlockPos(x, y, z);
            if (level.getBlockState(p).is(Blocks.GRASS_BLOCK) || level.getBlockState(p).is(Blocks.DIRT)) {
                set(level, p, Blocks.DIRT_PATH.defaultBlockState());
                if (level.getBlockState(p.above()).canBeReplaced()) set(level, p.above(), Blocks.AIR.defaultBlockState());
            }
        }
        Direction front = Architecture.facing(to, settlement.centre);
        BlockPos post = to.relative(front, 7).relative(front.getClockWise(), 2);
        BlockPos postGround = safeGround(level, post.getX(), post.getZ(), 0);
        if (postGround != null) post = postGround;
        if (level.getBlockState(post).canBeReplaced()) {
            set(level, post, Blocks.OAK_FENCE.defaultBlockState());
            set(level, post.above(), Blocks.OAK_FENCE.defaultBlockState());
            set(level, post.above(2), Blocks.LANTERN.defaultBlockState());
        }
    }

    /** A farmer's harvest: ripe wheat is cut and resown; what is cut goes to the store. */
    public static int harvest(ServerLevel level, BlockPos field) {
        int got = 0;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            BlockPos p = field.offset(dx, 0, dz);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) {
                if (crop.isMaxAge(s)) {
                    set(level, p, crop.getStateForAge(0));
                    got++;
                } else if (level.random.nextInt(3) == 0) {
                    set(level, p, crop.getStateForAge(Math.min(crop.getMaxAge(), crop.getAge(s) + 1)));   // tended crops grow
                }
            } else if (s.isAir() && level.getBlockState(p.below()).is(Blocks.FARMLAND)) {
                set(level, p, Blocks.WHEAT.defaultBlockState());   // resow
            }
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, field.getX() + 0.5, field.getY() + 1, field.getZ() + 0.5, 6, 2, 0.3, 2, 0);
        return Math.max(1, got * 2);
    }

    /** "And the earth brought forth grass, and herb yielding seed" (Gen 1:12): a watered field of wheat. */
    @Nullable
    private static BlockPos field(ServerLevel level, BlockPos around) {
        BlockPos g = safeGround(level, around.getX(), around.getZ(), 6);
        if (g == null) return null;
        foundation(level, g, 3);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            BlockPos p = g.offset(dx, -1, dz);
            if (dx == 0 && dz == 0) {
                set(level, p, Blocks.WATER.defaultBlockState());
                continue;
            }
            set(level, p, Blocks.FARMLAND.defaultBlockState());
            set(level, p.above(), Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, level.random.nextInt(8)));
        }
        return g;
    }

    /** Levels the ground under a square: fills below with dirt, clears above. */
    private static void foundation(ServerLevel level, BlockPos c, int r) {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            for (int y = -4; y <= -2; y++) {
                if (level.getBlockState(c.offset(dx, y, dz)).isAir()) set(level, c.offset(dx, y, dz), Blocks.DIRT.defaultBlockState());
            }
            for (int y = 0; y <= 5; y++) {
                BlockState s = level.getBlockState(c.offset(dx, y, dz));
                if (!s.isAir() && !s.is(Blocks.CAMPFIRE)) set(level, c.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void set(ServerLevel level, BlockPos p, BlockState s) {
        level.setBlock(p, s, net.minecraft.world.level.block.Block.UPDATE_ALL);
    }
}
