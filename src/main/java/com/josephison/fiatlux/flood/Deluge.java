package com.josephison.fiatlux.flood;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.civilization.Faiths;
import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.MortalPopulation;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Polities;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.civilization.SoulRealms;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.divinity.Alignment;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.realm.RealmPool;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Flood (Genesis 6:9-9:17), played out on a world whose wickedness is great, when its Creator
 * says so - "The end of all flesh is come before me ... Make thee an ark of gopher wood" (6:13-14).
 *
 * The most righteous man there - Noah himself, if the line of Seth has come down to him - builds the
 * ark to its measure while he preaches and his neighbours mock. When it is done, he and his wife, his
 * sons and their wives go in, and the beasts come "two and two unto Noah into the ark", the clean by
 * sevens (7:2-9); after seven days the LORD shuts him in (7:16). It rains forty days and forty nights,
 * the fountains of the deep are broken up, and the waters rise over every town until they stand fifteen
 * cubits above the mountains, bearing the ark up. All flesh outside dies (7:21-23); the Watchers are
 * bound. The waters prevail a hundred and fifty days while the ark drifts to the highest mountain, then
 * abate; the ark rests "upon the mountains of Ararat" (8:4). Noah sends out the raven and the dove
 * (8:6-12), and when the earth is dry they all go forth; he builds an altar, and the bow is set in the
 * cloud as the token of the covenant that the waters shall no more become a flood (9:8-17).
 *
 * The flood is a year and ten days of the ark's calendar; each day lasts {@code floodDayTicks}.
 */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class Deluge {
    /** The beasts that go in: the clean by sevens (seven pairs), the unclean by two (7:2-3), each kind to its own stall. */
    private record Kind(EntityType<? extends Mob> type, int pairs, int stall) {}

    private static final List<Kind> KINDS = List.of(
            new Kind(EntityType.COW, 7, 0), new Kind(EntityType.SHEEP, 7, 1), new Kind(EntityType.GOAT, 7, 2),
            new Kind(EntityType.CHICKEN, 7, 29),   // "of fowls also of the air by sevens" - on the second deck
            new Kind(EntityType.PIG, 1, 3), new Kind(EntityType.HORSE, 1, 4), new Kind(EntityType.DONKEY, 1, 5), new Kind(EntityType.CAMEL, 1, 6),
            new Kind(EntityType.RABBIT, 1, 7), new Kind(EntityType.LLAMA, 1, 8), new Kind(EntityType.PANDA, 1, 9),
            new Kind(EntityType.ARMADILLO, 1, 10), new Kind(EntityType.PARROT, 1, 30));
    private static final int PAIRS = KINDS.stream().mapToInt(Kind::pairs).sum();
    private static final int PAIR_TICKS = 24;
    private static final String ARK_TAG = "fiatlux_ark";
    private static final BlockState WATER = Blocks.WATER.defaultBlockState(), AIR = Blocks.AIR.defaultBlockState();
    /** Beasts on their way in: entity -> (stall, when sent). */
    private static final Map<UUID, long[]> WALKING = new HashMap<>();

    private Deluge() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 == 13) Nephilim.tick(server, false);
        FloodData d = FloodData.get(server);
        if (d.active()) tick(server, false);
        else if (d.rainbowUntil > 0 && server.getTickCount() % 40 == 0) rainbow(server, d);
    }

    @Nullable
    public static ServerLevel level(MinecraftServer server, FloodData d) {
        if (d.dim.isEmpty()) return null;
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(d.dim)));
    }

    // ================================================================== the word

    /**
     * "And God said unto Noah, The end of all flesh is come before me ... Make thee an ark" (6:13-14):
     * finds the righteous man near `near` and his house, chooses ground for the ark, and sets him to
     * build. Returns what to tell the Creator.
     */
    public static Component begin(ServerLevel level, BlockPos near) {
        MinecraftServer server = level.getServer();
        FloodData d = FloodData.get(server);
        if (d.covenant) return Component.translatable("message.fiatlux.flood.covenant");
        if (d.active()) return Component.translatable("message.fiatlux.flood.already", d.noahName, d.worldName);
        MortalEntity noah = righteous(level, near);
        if (noah == null) return Component.translatable("message.fiatlux.flood.none_righteous");
        Planet p = planet(level);
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        List<BlockPos> avoid = new ArrayList<>();
        List<Settlement> towns = p != null ? cosmos.townsOf(p.id) : List.of();
        for (Settlement t : towns) {
            avoid.add(t.centre);
            avoid.add(t.shrine);
            avoid.addAll(t.houses);
            if (t.field != null) avoid.add(t.field);
        }
        BlockPos home = towns.isEmpty() ? noah.blockPosition() : cosmos.townNear(p.id, noah.blockPosition()).centre;
        avoid.add(home);
        BlockPos origin = site(level, home, avoid);
        if (origin == null) return Component.translatable("message.fiatlux.flood.no_ground");
        d.stage = FloodData.Stage.BUILDING;
        d.since = level.getGameTime();
        d.dim = level.dimension().location().toString();
        d.world = p != null ? p.id : null;
        d.worldName = p != null ? p.name : noah.worldName();
        d.noah = noah.getUUID();
        d.noahName = noah.getName().getString();
        d.family.clear();
        household(level, noah).forEach(m -> d.family.add(m.getUUID()));
        d.ark = origin;
        d.arkHome = origin;
        d.arkProgress = 0;
        d.doorShut = false;
        d.pairs = d.birds = d.beats = 0;
        d.rested = false;
        d.centres.clear();
        d.radii.clear();
        int reach = FiatLuxConfig.COMMON.floodRadius.get();
        if (towns.isEmpty()) {
            d.centres.add(noah.blockPosition());
            d.radii.add(reach);
        }
        for (Settlement t : towns) {
            d.centres.add(t.centre);
            d.radii.add(reach);
        }
        d.centres.add(Ark.centre(origin));
        d.radii.add(Math.max(reach, Ark.LENGTH / 2 + 8));
        d.resetWaters();
        d.forced.clear();
        for (long c : d.waters().chunks()) {
            level.setChunkForced(ChunkPos.getX(c), ChunkPos.getZ(c), true);
            d.forced.add(c);
        }
        Ark.prepareSite(level, origin);
        d.setDirty();
        narrate(level, d, "But Noah found grace in the eyes of the LORD. ... Noah was a just man and perfect in his generations, and Noah walked with God.",
                "Genesis 6:8-9");
        narrate(level, d, "And God said unto Noah, The end of all flesh is come before me; for the earth is filled with violence through them; "
                + "and, behold, I will destroy them with the earth. Make thee an ark of gopher wood; rooms shalt thou make in the ark, and shalt "
                + "pitch it within and without with pitch.", "Genesis 6:13-14");
        noah.say("I hear. I will build it to the cubit, as You have said.");
        CosmosData.get(server).record(server, d.noahName, "noah",
                "found grace in the eyes of the LORD, and was told to make an ark of gopher wood, three hundred cubits long, on " + d.worldName + ".");
        return Component.translatable("message.fiatlux.flood.begun", d.noahName, d.worldName);
    }

    /** "Noah was a just man and perfect in his generations" (6:9): Noah of the line of Seth, or the most righteous man there is. */
    @Nullable
    public static MortalEntity righteous(ServerLevel level, BlockPos near) {
        List<MortalEntity> all = level.getEntitiesOfClass(MortalEntity.class, new AABB(near).inflate(160),
                m -> m.isAlive() && !m.isChild() && !m.isNephil() && m.watcherHusband() == null && !m.isMarked()
                        && m.sentence() == com.josephison.fiatlux.civilization.Justice.Sentence.NONE);
        Generations g = Generations.get(level.getServer());
        for (MortalEntity m : all) {
            Person p = g.person(m.getUUID());
            if ("Noah".equals(m.getName().getString()) && p != null && "seth".equals(p.line)) return m;
        }
        Comparator<MortalEntity> worth = Comparator.comparingDouble(m -> {
            Religion r = Faiths.of(m);
            Alignment a = Divinity.alignment(m);
            return (r != null && r.deity.true_ ? 1000 : 0) + a.virtue() - 2.0 * a.sin() + (m.spouseEntity() != null ? 5 : 0);
        });
        return all.stream().filter(MortalEntity::isMale).max(worth).orElse(all.stream().max(worth).orElse(null));
    }

    /** "Thou, and thy sons, and thy wife, and thy sons' wives with thee" (6:18). */
    static List<MortalEntity> household(ServerLevel level, MortalEntity noah) {
        List<MortalEntity> out = new ArrayList<>();
        out.add(noah);
        MortalEntity wife = noah.spouseEntity();
        if (wife != null) out.add(wife);
        Generations g = Generations.get(level.getServer());
        List<Person> children = g.childrenOf(noah.getUUID()).stream().filter(Person::living).toList();
        List<Person> sons = children.stream().filter(c -> c.male).toList();
        for (Person c : (sons.isEmpty() ? children : sons)) {
            if (out.size() >= 8) break;
            if (level.getEntity(c.id) instanceof MortalEntity m && m.isAlive()) {
                out.add(m);
                MortalEntity w = m.spouseEntity();
                if (w != null && out.size() < 8) out.add(w);
            }
        }
        return out;
    }

    /** Level, dry ground for the ark a little way from the town, clear of its houses. */
    @Nullable
    static BlockPos site(ServerLevel level, BlockPos home, List<BlockPos> avoid) {
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int r : new int[]{45, 65, 85}) {
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                int cx = home.getX() + (int) (Math.cos(a) * r), cz = home.getZ() + (int) (Math.sin(a) * r);
                int x0 = cx - Ark.LENGTH / 2, z0 = cz - Ark.MID;
                boolean clear = avoid.stream().allMatch(t -> t.getX() < x0 - 12 || t.getX() > x0 + Ark.LENGTH + 12
                        || t.getZ() < z0 - 12 || t.getZ() > z0 + Ark.BREADTH + 12);
                if (!clear) continue;
                List<Integer> hs = new ArrayList<>();
                boolean wet = false;
                for (int sx = 0; sx <= 8; sx++) {
                    for (int sz = 0; sz <= 2; sz++) {
                        int x = x0 + sx * (Ark.LENGTH - 1) / 8, z = z0 + sz * (Ark.BREADTH - 1) / 2;
                        level.getChunk(x >> 4, z >> 4);
                        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                        if (!level.getFluidState(new BlockPos(x, y, z)).isEmpty()) wet = true;
                        hs.add(y);
                    }
                }
                hs.sort(Integer::compare);
                int score = hs.getLast() - hs.getFirst() + (wet ? 200 : 0) + r / 10;
                if (score < bestScore) {
                    bestScore = score;
                    best = new BlockPos(x0, hs.get(hs.size() / 2) + 1, z0);
                }
            }
        }
        return best;
    }

    // ================================================================== the story

    /** One tick of the Flood. force: skip every wait and do each stage's work at once (tests, commands). */
    public static void tick(MinecraftServer server, boolean force) {
        FloodData d = FloodData.get(server);
        ServerLevel level = level(server, d);
        if (level == null || !d.active()) return;
        long now = level.getGameTime();
        switch (d.stage) {
            case BUILDING -> building(level, d, now, force);
            case BOARDING -> boarding(level, d, now, force);
            case RAIN, PREVAIL, ABATE, DRY -> waters(level, d, now, force);
            default -> {
            }
        }
        d.setDirty();
    }

    /** Noah and his sons build, Noah preaches, his neighbours mock (Gen 6:22; 2 Pet 2:5; Matt 24:38). */
    private static void building(ServerLevel level, FloodData d, long now, boolean force) {
        int per = Math.max(1, (int) Math.ceil(Ark.total() / (double) FiatLuxConfig.COMMON.arkBuildTicks.get()));
        d.arkProgress = Ark.build(level, d.ark, d.arkProgress, force ? Ark.total() : per);
        if (now % 20 == 0) {
            BlockPos door = Ark.door(d.ark);
            for (MortalEntity m : family(level, d)) {
                if (m.isChild()) continue;
                if (m.distanceToSqr(Vec3.atBottomCenterOf(door)) > 30 * 30) m.walkTo(door.offset(level.random.nextInt(30) - 15, 0, -3), 0.7);
                else if (m.act() == MortalEntity.NONE && level.random.nextInt(3) == 0) m.beginAct(MortalEntity.WORK, 100);
            }
            MortalEntity noah = noah(level, d);
            if (noah != null && now % 300 == 0) {
                String[] preach = {"Turn from your violence! The waters are coming.", "The Lord has spoken: a flood upon the earth. Come in with us!",
                        "There is room in the ark for any who will turn.", "Repent, while the door stands open."};
                noah.say(preach[level.random.nextInt(preach.length)]);
                MortalEntity mocker = level.getEntitiesOfClass(MortalEntity.class, noah.getBoundingBox().inflate(48),
                        m -> !d.family.contains(m.getUUID()) && !m.isChild() && m.isAlive()).stream().findAny().orElse(null);
                if (mocker != null) {
                    String[] mock = {"Water from the sky? There goes up a mist from the earth, as always.", "A boat? On dry land? Noah has lost his wits.",
                            "Eat, drink, be merry - and let the old man build his box.", "Rain? What is rain?"};
                    mocker.say(mock[level.random.nextInt(mock.length)]);
                }
            }
        }
        if (d.arkProgress >= Ark.total()) {
            narrate(level, d, "Thus did Noah; according to all that God commanded him, so did he.", "Genesis 6:22");
            CosmosData.get(level.getServer()).record(level.getServer(), d.noahName, "ark", "finished the ark: three hundred cubits long, "
                    + "fifty broad and thirty high, of gopher wood pitched within and without, with three stories, a window and a door in the side.");
            next(d, FloodData.Stage.BOARDING, now);
        }
    }

    /** "Come thou and all thy house into the ark" (7:1); the beasts go in two and two (7:8-9); after seven days, the door is shut. */
    private static void boarding(ServerLevel level, FloodData d, long now, boolean force) {
        long elapsed = now - d.since;
        if (d.beats == 0) {
            narrate(level, d, "Come thou and all thy house into the ark; for thee have I seen righteous before me in this generation. Of every clean "
                    + "beast thou shalt take to thee by sevens, the male and his female: and of beasts that are not clean by two. ... For yet seven "
                    + "days, and I will cause it to rain upon the earth forty days and forty nights.", "Genesis 7:1-4");
            d.beats = 1;
        }
        List<MortalEntity> fam = family(level, d);
        for (int i = 0; i < fam.size(); i++) {
            MortalEntity m = fam.get(i);
            BlockPos room = Ark.quarters(d.ark, i);
            if (!aboard(d, m)) {
                if (force || elapsed > 200 || m.distanceToSqr(Vec3.atBottomCenterOf(Ark.door(d.ark))) < 16) {
                    m.teleportTo(room.getX() + 0.5, room.getY(), room.getZ() + 0.5);
                } else if (now % 20 == 0) {
                    m.walkTo(Ark.door(d.ark), 0.8);
                }
            }
            m.setHome(room);
        }
        int due = force ? PAIRS : (int) Math.min(PAIRS, elapsed / PAIR_TICKS);
        while (d.pairs < due) sendPair(level, d, d.pairs++, force, now);
        walkIn(level, d, now, force);
        long days = 7L * FiatLuxConfig.COMMON.floodDayTicks.get();
        if (force || elapsed >= Math.max(days, (long) PAIRS * PAIR_TICKS + 200)) {
            walkIn(level, d, Long.MAX_VALUE / 2, true);
            for (int i = 0; i < fam.size(); i++) {
                BlockPos room = Ark.quarters(d.ark, i);
                if (!aboard(d, fam.get(i))) fam.get(i).teleportTo(room.getX() + 0.5, room.getY(), room.getZ() + 0.5);
            }
            shut(level, d, now);
        }
    }

    private static void sendPair(ServerLevel level, FloodData d, int index, boolean force, long now) {
        Kind kind = null;
        int n = index;
        for (Kind k : KINDS) {
            if (n < k.pairs()) {
                kind = k;
                break;
            }
            n -= k.pairs();
        }
        if (kind == null) return;
        BlockPos stall = Ark.stall(d.ark, kind.stall());
        BlockPos door = Ark.door(d.ark);
        BlockPos from = force ? stall : MortalPopulation.safeGround(level, door.getX(), door.getZ() - 18, 8);
        if (from == null) from = stall;
        for (int k = 0; k < 2; k++) {   // "the male and his female"
            Mob a = kind.type().create(level);
            if (a == null) continue;
            a.moveTo(from.getX() + 0.5 + k * 1.2, from.getY(), from.getZ() + 0.5, level.random.nextFloat() * 360f, 0);
            a.finalizeSpawn(level, level.getCurrentDifficultyAt(from), MobSpawnType.EVENT, null);
            a.setPersistenceRequired();
            a.addTag(ARK_TAG);
            level.addFreshEntity(a);
            if (from != stall) {
                a.getNavigation().moveTo(d.ark.getX() + 68.5, d.ark.getY() + 1, d.ark.getZ() + 3.5, 1.1);
                WALKING.put(a.getUUID(), new long[]{kind.stall(), now});
            }
        }
        if (index % 7 == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, from.getX() + 0.5, from.getY() + 1, from.getZ() + 0.5, 8, 1, 0.5, 1, 0);
        }
    }

    /** Beasts that reach the door (or have been long on the way) go to their stall. */
    private static void walkIn(ServerLevel level, FloodData d, long now, boolean all) {
        var it = WALKING.entrySet().iterator();
        Vec3 door = Vec3.atBottomCenterOf(Ark.door(d.ark));
        while (it.hasNext()) {
            var e = it.next();
            Entity a = level.getEntity(e.getKey());
            if (a == null) {
                it.remove();
                continue;
            }
            if (all || a.distanceToSqr(door) < 5 * 5 || aboard(d, a) || now - e.getValue()[1] > 200) {
                BlockPos stall = Ark.stall(d.ark, (int) e.getValue()[0]);
                a.teleportTo(stall.getX() + 0.5 + level.random.nextInt(5) - 2, stall.getY(), stall.getZ() + 0.5 + level.random.nextInt(5) - 2);
                it.remove();
            } else if (a instanceof Mob mob && mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(d.ark.getX() + 68.5, d.ark.getY() + 1, d.ark.getZ() + 3.5, 1.1);
            }
        }
    }

    /** "And they that went in ... as God had commanded him: and the LORD shut him in" (7:16). The windows of heaven are opened. */
    private static void shut(ServerLevel level, FloodData d, long now) {
        Ark.setDoor(level, d.ark, true);
        d.doorShut = true;
        narrate(level, d, "In the selfsame day entered Noah, and Shem, and Ham, and Japheth, the sons of Noah, and Noah's wife, and the three wives "
                + "of his sons with them, into the ark ... And they that went in, went in male and female of all flesh, as God had commanded him: "
                + "and the LORD shut him in.", "Genesis 7:13-16");
        level.playSound(null, Ark.door(d.ark), SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 4f, 0.4f);
        // the reach of the waters: from the lowest ground to fifteen cubits over the highest
        int[] relief = d.waters().relief(level, d.ark);
        d.startY = relief[0] + 1;
        d.peakY = Math.max(Math.min(relief[1] + 7, d.startY + FiatLuxConfig.COMMON.floodMaxRise.get()), d.ark.getY() + Ark.DRAFT + 2);
        d.ararat = new BlockPos(relief[2], relief[1], relief[3]);
        d.waterY = d.startY - 1;
        d.cursor = 0;
        d.floodStart = now;
        next(d, FloodData.Stage.RAIN, now);
        narrate(level, d, "In the six hundredth year of Noah's life, in the second month, the seventeenth day of the month, the same day were all "
                + "the fountains of the great deep broken up, and the windows of heaven were opened. And the rain was upon the earth forty days and "
                + "forty nights.", "Genesis 7:11-12");
        Nephilim.bindAll(level);
        for (ServerPlayer p : level.players()) Fx.send(level, FxKind.DELUGE, p.position(), 30f, 0xFF4F8CFF, 200);
        int cried = 0;
        for (MortalEntity m : level.getEntitiesOfClass(MortalEntity.class, Ark.bounds(d.ark).inflate(160), m -> !d.family.contains(m.getUUID()))) {
            if (cried++ > 10) break;
            String[] cry = {"The fountains of the deep are broken up!", "Noah! Open the door!", "To the high places, quickly!",
                    "Rain... so this is rain.", "He warned us. He warned us every day."};
            m.say(cry[level.random.nextInt(cry.length)]);
        }
        CosmosData.get(level.getServer()).record(level.getServer(), "The LORD", "flood",
                "shut Noah into the ark; the fountains of the great deep were broken up over " + d.worldName + ", and the windows of heaven were opened.");
    }

    /** The year of the waters: rain, prevailing, abating, drying (7:17 - 8:14). */
    private static void waters(ServerLevel level, FloodData d, long now, boolean force) {
        double dayTicks = FiatLuxConfig.COMMON.floodDayTicks.get();
        double day = force ? switch (d.stage) {
            case RAIN -> 40;
            case PREVAIL -> 150;
            case ABATE -> 314;
            default -> 370;
        } : (now - d.floodStart) / dayTicks;
        int layers = d.peakY - (d.startY - 1);
        int target = switch (d.stage) {
            case RAIN -> d.startY - 1 + (int) Math.round(layers * Math.min(1, day / 40));
            case PREVAIL -> d.peakY;
            case ABATE -> d.peakY - (int) Math.round(layers * Math.min(1, Math.max(0, day - 150) / 150));
            default -> d.startY - 1;
        };
        work(level, d, target, force);
        if (now % 40 == 0 || force) keepAboard(level, d);
        if (now % 20 == 0 || force) sea(level, d);
        switch (d.stage) {
            case RAIN -> {
                if (now % 10 == 0) storm(level, d, now);
                if (day >= 40 && d.waterY >= d.peakY) {
                    allFlesh(level, d, now);
                    calm(level);
                    next(d, FloodData.Stage.PREVAIL, now);
                }
            }
            case PREVAIL -> {
                if (force || now % 10 == 0) drift(level, d, force);
                if (day >= 150) {
                    narrate(level, d, "And God remembered Noah ... and God made a wind to pass over the earth, and the waters asswaged; the fountains "
                            + "also of the deep and the windows of heaven were stopped, and the rain from heaven was restrained.", "Genesis 8:1-2");
                    for (ServerPlayer p : level.players()) level.sendParticles(p, ParticleTypes.CLOUD, true, p.getX(), p.getY() + 6, p.getZ(), 120, 30, 4, 30, 0.6);
                    next(d, FloodData.Stage.ABATE, now);
                }
            }
            case ABATE -> {
                if (!d.rested && d.ark.getY() <= Ark.restingY(level, d.ark, d.ark) && d.waterY < d.peakY) {
                    d.rested = true;
                    narrate(level, d, "And the ark rested in the seventh month, on the seventeenth day of the month, upon the mountains of Ararat. "
                            + "And the waters decreased continually until the tenth month: ... were the tops of the mountains seen.", "Genesis 8:4-5");
                    CosmosData.get(level.getServer()).record(level.getServer(), "The ark", "ararat", "came to rest upon the mountains of Ararat on "
                            + d.worldName + ".");
                }
                birds(level, d, day, force);
                if (d.waterY <= d.startY - 1 && day >= 314) {
                    d.rested = true;
                    birds(level, d, 999, true);
                    Ark.setDoor(level, d.ark, false);
                    d.doorShut = false;
                    narrate(level, d, "And Noah removed the covering of the ark, and looked, and, behold, the face of the ground was dry.", "Genesis 8:13");
                    next(d, FloodData.Stage.DRY, now);
                }
            }
            case DRY -> {
                if (day >= 370) goForth(level, d, now);
            }
            default -> {
            }
        }
    }

    /** Raises or lowers the waters toward `target`, a layer at a time, floating the ark on them. */
    private static void work(ServerLevel level, FloodData d, int target, boolean force) {
        FloodWaters w = d.waters();
        int budget = force ? Integer.MAX_VALUE / 2 : FiatLuxConfig.COMMON.floodBlocksPerTick.get();
        for (int guard = 0; guard < 4096; guard++) {
            if (d.waterY < target) {
                int y = d.waterY + 1;
                d.cursor = w.raise(level, y, d.cursor, budget, d.ark);
                if (d.cursor >= w.size()) {
                    d.cursor = 0;
                    d.waterY = y;
                    floatArk(level, d);
                }
            } else if (d.waterY > target) {
                int y = d.waterY;
                d.cursor = w.lower(level, y, d.cursor, budget, d.ark);
                if (d.cursor >= w.size()) {
                    d.cursor = 0;
                    d.waterY = y - 1;
                    floatArk(level, d);
                }
            } else {
                return;
            }
            if (!force) return;
        }
    }

    /** "The waters increased, and bare up the ark, and it was lift up above the earth" (7:17) - and later let it down. */
    private static void floatArk(ServerLevel level, FloodData d) {
        int rest = Ark.restingY(level, d.ark, d.ark);
        int want = Math.max(rest, d.waterY - Ark.DRAFT + 1);
        if (want == d.ark.getY()) return;
        if (d.stage == FloodData.Stage.RAIN && want > d.ark.getY() && d.ark.getY() == d.arkHome.getY()) {
            narrate(level, d, "And the waters increased, and bare up the ark, and it was lift up above the earth.", "Genesis 7:17");
        }
        moveArk(level, d, new BlockPos(d.ark.getX(), want, d.ark.getZ()));
    }

    private static void moveArk(ServerLevel level, FloodData d, BlockPos to) {
        FloodWaters w = d.waters();
        int top = d.waterY;
        Ark.move(level, d.ark, to, d.doorShut, y -> y <= top ? WATER : AIR, w::covers, w::wetted);
        d.ark = to;
    }

    /** While the waters prevail, the ark is carried toward the highest mountain, where it will rest. */
    private static void drift(ServerLevel level, FloodData d, boolean force) {
        BlockPos goal = new BlockPos(d.ararat.getX() - Ark.LENGTH / 2, d.ark.getY(), d.ararat.getZ() - Ark.MID);
        if (goal.equals(d.ark)) return;
        if (force) {
            if (Ark.restingY(level, goal, d.ark) <= d.ark.getY() && d.waters().covers(goal.getX() + Ark.LENGTH / 2, goal.getZ() + Ark.MID)) moveArk(level, d, goal);
            return;
        }
        int dx = Integer.signum(goal.getX() - d.ark.getX()), dz = Integer.signum(goal.getZ() - d.ark.getZ());
        for (BlockPos to : new BlockPos[]{d.ark.offset(dx, 0, 0), d.ark.offset(0, 0, dz)}) {
            if (to.equals(d.ark)) continue;
            if (Ark.restingY(level, to, d.ark) <= d.ark.getY()) {
                moveArk(level, d, to);
                return;
            }
        }
    }

    /** "And all flesh died that moved upon the earth ... and Noah only remained alive, and they that were with him in the ark" (7:21-23). */
    private static void allFlesh(ServerLevel level, FloodData d, long now) {
        MinecraftServer server = level.getServer();
        Set<UUID> kept = new HashSet<>(d.family);
        AABB ark = Ark.bounds(d.ark).inflate(1, 0, 1).expandTowards(0, 3, 0);
        FloodWaters w = d.waters();
        int died = 0;
        for (MortalEntity m : level.getEntities(ModEntities.MORTAL.get(), m -> m.isAlive())) {
            if (kept.contains(m.getUUID()) || ark.contains(m.position())) continue;
            if (d.world == null && !w.covers(m.getBlockX(), m.getBlockZ())) continue;
            m.hurt(level.damageSources().drown(), Float.MAX_VALUE);
            died++;
        }
        for (Animal a : level.getEntities(EntityTypeTest.forClass(Animal.class), a -> a.isAlive() && w.covers(a.getBlockX(), a.getBlockZ())
                && !ark.contains(a.position()))) {
            a.hurt(level.damageSources().drown(), Float.MAX_VALUE);
        }
        for (Monster a : level.getEntities(EntityTypeTest.forClass(Monster.class), a -> a.isAlive() && w.covers(a.getBlockX(), a.getBlockZ())
                && !(a instanceof com.josephison.fiatlux.entity.FallenEntity) && !ark.contains(a.position()))) {
            a.hurt(level.damageSources().drown(), Float.MAX_VALUE);
        }
        Planet p = planet(level);
        if (p != null) {   // and those of that world who were nowhere near
            Generations g = Generations.get(server);
            CosmicExpanseSavedData c = CosmicExpanseSavedData.get(server);
            long year = FiatLuxConfig.COMMON.mortalYearTicks.get();
            int shown = 0;
            for (Person x : g.people.values()) {
                if (!x.living() || !p.name.equals(x.world) || kept.contains(x.id) || level.getEntity(x.id) != null) continue;
                x.died = now;
                x.ageAtDeath = (int) ((now - x.born) / year);
                died++;
                if (x.nephil) {
                    x.soul = "spirit";
                    d.giantSpirits++;
                    continue;
                }
                boolean heaven = level.random.nextDouble() < 0.1;   // "every imagination of the thoughts of his heart was only evil continually"
                x.soul = heaven ? "heaven" : "underworld";
                if (heaven) c.heavenSouls += 1;
                else c.underworld.souls += 1;
                if (shown++ < 12) SoulRealms.onDeath(server, x, heaven);
            }
            g.setDirty();
            long alive = d.family.stream().filter(id -> g.person(id) != null && g.person(id).living()).count();
            p.population = Math.max(2, alive);
            p.sin = 0;
            p.faith = 1;
            c.setDirty();
        }
        narrate(level, d, "And all flesh died that moved upon the earth, both of fowl, and of cattle, and of beast, and of every creeping thing ... "
                + "and every man ... and Noah only remained alive, and they that were with him in the ark. And the waters prevailed upon the earth "
                + "an hundred and fifty days.", "Genesis 7:21-24");
        CosmosData.get(server).record(server, d.worldName, "all_flesh",
                "was covered by the waters fifteen cubits above the mountains; " + died + " souls perished, and only " + d.family.size()
                        + " lived, with Noah in the ark.");
    }

    /** The raven, and the dove three times (8:6-12). */
    private static void birds(ServerLevel level, FloodData d, double day, boolean force) {
        double[] when = {264, 271, 278, 285};
        while (d.birds < 4 && day >= when[d.birds]) {
            int i = d.birds++;
            Vec3 window = Vec3.atCenterOf(d.ark.offset(Ark.LENGTH / 2, 13, -1));
            Vec3 far = window.add(level.random.nextInt(41) - 20, -4, -45);
            if (!force || i == 3) {
                BirdEntity b = ModEntities.BIRD.get().create(level);
                if (b != null) {
                    b.send(i == 0 ? BirdEntity.RAVEN : BirdEntity.DOVE, window, far, i == 0 || i == 3 ? BirdEntity.AWAY : BirdEntity.RETURNS, i == 2);
                    level.addFreshEntity(b);
                }
            }
            switch (i) {
                case 0 -> narrate(level, d, "And it came to pass at the end of forty days, that Noah opened the window of the ark which he had made: "
                        + "And he sent forth a raven, which went forth to and fro, until the waters were dried up from off the earth.", "Genesis 8:6-7");
                case 1 -> narrate(level, d, "Also he sent forth a dove from him ... But the dove found no rest for the sole of her foot, and she "
                        + "returned unto him into the ark.", "Genesis 8:8-9");
                case 2 -> narrate(level, d, "And he stayed yet other seven days; and again he sent forth the dove out of the ark; And the dove came "
                        + "in to him in the evening; and, lo, in her mouth was an olive leaf pluckt off.", "Genesis 8:10-11");
                default -> narrate(level, d, "And he stayed yet other seven days; and sent forth the dove; which returned not again unto him any more.",
                        "Genesis 8:12");
            }
        }
    }

    /**
     * "Go forth of the ark" (8:16): the household and every beast go out onto the dry ground, Noah
     * builds an altar and offers on it (8:20), and the bow is set in the cloud (9:13). The world begins
     * again from the eight.
     */
    private static void goForth(ServerLevel level, FloodData d, long now) {
        MinecraftServer server = level.getServer();
        narrate(level, d, "Go forth of the ark, thou, and thy wife, and thy sons, and thy sons' wives with thee. Bring forth with thee every living "
                + "thing that is with thee ... that they may breed abundantly in the earth, and be fruitful, and multiply upon the earth.", "Genesis 8:16-17");
        BlockPos land = landing(level, d);
        List<MortalEntity> fam = family(level, d);
        for (int i = 0; i < fam.size(); i++) {
            MortalEntity m = fam.get(i);
            m.teleportTo(land.getX() + 0.5 + (i % 4) * 1.5 - 2, land.getY(), land.getZ() + 0.5 + (i / 4) * 1.5);
            m.setHome(land);
        }
        for (Entity a : level.getEntitiesOfClass(Entity.class, Ark.bounds(d.ark).inflate(1), e -> e.getTags().contains(ARK_TAG))) {
            BlockPos g = MortalPopulation.safeGround(level, land.getX() + level.random.nextInt(49) - 24, land.getZ() + level.random.nextInt(49) - 24, 16);
            if (g != null) a.teleportTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
            a.removeTag(ARK_TAG);
        }
        // "And Noah builded an altar unto the LORD; and took of every clean beast ... and offered burnt offerings on the altar" (8:20)
        BlockPos altar = land.offset(4, 0, 4);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            level.setBlock(altar.offset(dx, -1, dz), Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(altar.offset(dx, 0, dz), (dx == 0 && dz == 0 ? Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
                    .setValue(CampfireBlock.SIGNAL_FIRE, true) : Blocks.COBBLESTONE_SLAB.defaultBlockState()), Block.UPDATE_ALL);
        }
        MortalEntity noah = noah(level, d);
        if (noah != null) {
            noah.teleportTo(altar.getX() + 0.5, altar.getY(), altar.getZ() - 1.5);
            noah.beginAct(MortalEntity.PRAY, 400);
            noah.say("We are alive. Blessed be the One who remembered us.");
        }
        narrate(level, d, "And the LORD smelled a sweet savour; and the LORD said in his heart, I will not again curse the ground any more for man's "
                + "sake ... While the earth remaineth, seedtime and harvest, and cold and heat, and summer and winter, and day and night shall not "
                + "cease.", "Genesis 8:21-22");
        narrate(level, d, "And God said, This is the token of the covenant which I make between me and you and every living creature that is with "
                + "you, for perpetual generations: I do set my bow in the cloud ... and the waters shall no more become a flood to destroy all flesh.",
                "Genesis 9:12-15");
        d.rainbowUntil = now + 2400;
        d.rainbow = Ark.centre(d.ark).atY(land.getY());
        // the world begins again from the eight
        Planet p = planet(level);
        if (p != null) {
            CosmicExpanseSavedData c = CosmicExpanseSavedData.get(server);
            BlockPos site = MortalPopulation.safeGround(level, land.getX() - 30, land.getZ() - 20, 30);
            Settlement town = new Settlement(site != null ? site : land);
            town.name = "Ararat";
            town.founded = now;
            town.founders = d.noahName;
            MortalPopulation.build(level, town, p.era);
            c.towns.put(p.id, new ArrayList<>(List.of(town)));
            Polities.get(server).byWorld.remove(p.id);
            Polities.get(server).setDirty();
            for (MortalEntity m : fam) m.setHome(town.centre);
            Generations g = Generations.get(server);
            p.population = Math.max(2, d.family.stream().filter(id -> g.person(id) != null && g.person(id).living()).count());
            c.setDirty();
        }
        for (long ch : d.forced) level.setChunkForced(ChunkPos.getX(ch), ChunkPos.getZ(ch), false);
        d.forced.clear();
        d.covenant = true;
        d.floods++;
        d.centres.clear();
        d.radii.clear();
        d.resetWaters();
        next(d, FloodData.Stage.NONE, now);
        sea(level, d);
        CosmosData.get(server).record(server, d.noahName, "covenant", "went forth of the ark with all his house and every living thing, built an "
                + "altar unto the LORD, and received the covenant of the bow: the waters shall no more become a flood to destroy all flesh.");
    }

    /** Dry ground beside the ark for those who come out of it. */
    private static BlockPos landing(ServerLevel level, FloodData d) {
        BlockPos door = Ark.door(d.ark);
        for (int r : new int[]{6, 14, 24, 40}) {
            BlockPos g = MortalPopulation.safeGround(level, door.getX(), door.getZ() - r, r / 2 + 4);
            if (g != null && !Ark.contains(d.ark, g.getX(), g.getY() - 1, g.getZ())) return g;
        }
        BlockPos g = MortalPopulation.safeGround(level, door.getX(), door.getZ() - 8, 60);
        return g != null ? g : door;
    }

    // ================================================================== those aboard, and the sky

    /** Those in the ark live their days there: praying, working, eating, sleeping in the upper deck. */
    private static void keepAboard(ServerLevel level, FloodData d) {
        if (!d.doorShut) return;
        List<MortalEntity> fam = family(level, d);
        for (int i = 0; i < fam.size(); i++) {
            MortalEntity m = fam.get(i);
            BlockPos room = Ark.quarters(d.ark, i);
            m.setHome(room);
            if (!aboard(d, m)) m.teleportTo(room.getX() + 0.5, room.getY(), room.getZ() + 0.5);
            if (m.act() == MortalEntity.NONE && level.random.nextInt(3) == 0) {
                int[] acts = {MortalEntity.PRAY, MortalEntity.WORK, MortalEntity.EAT};
                m.beginAct(acts[level.random.nextInt(acts.length)], 120 + level.random.nextInt(160));
            }
        }
    }

    /** The windows of heaven opened: driving rain and thunder over the world, and the deep breaking up. */
    private static void storm(ServerLevel level, FloodData d, long now) {
        for (ServerPlayer p : level.players()) {
            p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1f));
            p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 1f));
            for (int k = 0; k < 3; k++) {   // "all the fountains of the great deep broken up"
                int x = p.getBlockX() + level.random.nextInt(81) - 40, z = p.getBlockZ() + level.random.nextInt(81) - 40;
                if (!d.waters().covers(x, z)) continue;
                double y = Math.max(d.waterY + 1, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z));
                level.sendParticles(p, ParticleTypes.SPLASH, true, x + 0.5, y, z + 0.5, 40, 0.6, 0.4, 0.6, 0.3);
                level.sendParticles(p, ParticleTypes.BUBBLE_COLUMN_UP, true, x + 0.5, y - 1, z + 0.5, 20, 0.4, 0.8, 0.4, 0.1);
                level.sendParticles(p, ParticleTypes.CLOUD, true, x + 0.5, y + 1, z + 0.5, 6, 0.4, 1.5, 0.4, 0.08);
            }
            if (level.random.nextInt(8) == 0) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    int x = p.getBlockX() + level.random.nextInt(121) - 60, z = p.getBlockZ() + level.random.nextInt(121) - 60;
                    bolt.moveTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z + 0.5);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
            }
        }
    }

    /** Tells those on the world where the waters stand (so their surface reaches the horizon), and where the bow is. */
    private static void sea(ServerLevel level, FloodData d) {
        boolean wet = d.active() && d.waterY >= d.startY && !d.centres.isEmpty();
        int bow = (int) Math.max(0, d.rainbowUntil - level.getGameTime());
        com.josephison.fiatlux.network.FloodPayload p = wet || bow > 0 ? new com.josephison.fiatlux.network.FloodPayload(d.dim, wet, d.waterY,
                d.centres.stream().mapToLong(BlockPos::asLong).toArray(), d.radii.stream().mapToInt(Integer::intValue).toArray(),
                d.rainbow.asLong(), bow) : com.josephison.fiatlux.network.FloodPayload.NONE;
        for (ServerPlayer pl : level.players()) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(pl, p);
    }

    /** The rain from heaven is restrained (8:2): the sky goes back to its own weather. */
    private static void calm(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1f)));
            p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1f)));
        }
    }

    /** Sets a rainbow in the cloud over a world for a while (the bow stands 90 blocks south of `at`). */
    public static void setBow(ServerLevel level, BlockPos at, int ticks) {
        FloodData d = FloodData.get(level.getServer());
        if (!d.active()) d.dim = level.dimension().location().toString();
        d.rainbow = at;
        d.rainbowUntil = level.getGameTime() + ticks;
        d.setDirty();
        sea(level, d);
    }

    /** "I do set my bow in the cloud" (9:13): a rainbow arching over the land beyond the ark, drawn by those who see it. */
    private static void rainbow(MinecraftServer server, FloodData d) {
        ServerLevel level = level(server, d);
        if (level == null) return;
        if (level.getGameTime() > d.rainbowUntil) {
            d.rainbowUntil = 0;
            d.setDirty();
        }
        sea(level, d);
    }

    // ================================================================== the Creator's part, and reports

    /** "Make thee an ark" - or "The end of all flesh is come before me" - spoken by a Creator on a world. */
    public static void onChat(ServerPlayer player, String said) {
        boolean decree = said.contains("make thee an ark") || said.contains("make an ark") || said.contains("build an ark")
                || said.contains("the end of all flesh") || said.contains("i will destroy man");
        if (!decree) return;
        player.server.execute(() -> {
            if (Divinity.get(player).pilgrim()) return;
            player.sendSystemMessage(begin(player.serverLevel(), player.blockPosition()).copy().withStyle(s -> s.withColor(0x9AB8E8)));
        });
    }

    /** A short account of the Flood and the Watchers, for reports. */
    public static String describe(MinecraftServer server) {
        FloodData d = FloodData.get(server);
        long now = server.overworld().getGameTime();
        ServerLevel level = level(server, d);
        if (level != null) now = level.getGameTime();
        long day = d.floodStart > 0 ? (now - d.floodStart) / Math.max(1, FiatLuxConfig.COMMON.floodDayTicks.get()) : 0;
        String watchers = d.watchedWorlds.isEmpty() ? "" : " Watchers came down to " + d.watchedWorlds.size() + (d.watchedWorlds.size() == 1 ? " world" : " worlds")
                + "; " + d.giantsBorn + " giants were born of them" + (d.giantSpirits > 0 ? ", and " + d.giantSpirits + " giant spirits wander the earth" : "")
                + (d.watchersBound > 0 ? "; " + d.watchersBound + " Watchers are bound in chains of darkness." : ".");
        String flood = switch (d.stage) {
            case NONE -> d.covenant ? "The Flood is past; the bow is set in the cloud, and the waters shall no more destroy all flesh."
                    : "There has been no Flood.";
            case BUILDING -> d.noahName + " is building the ark on " + d.worldName + " (" + (100L * d.arkProgress / Ark.total()) + "% done).";
            case BOARDING -> "The beasts are going into the ark two and two on " + d.worldName + ".";
            case RAIN -> "It is raining on " + d.worldName + ": day " + day + " of forty; the waters stand at y=" + d.waterY + ", rising to y=" + d.peakY + ".";
            case PREVAIL -> "The waters prevail over " + d.worldName + " (day " + day + " of 150); the ark drifts on them.";
            case ABATE -> "The waters abate on " + d.worldName + " (day " + day + "); they stand at y=" + d.waterY + "."
                    + (d.rested ? " The ark rests upon the mountains of Ararat." : "");
            case DRY -> "The ground is drying on " + d.worldName + " (day " + day + " of 370); soon they go forth of the ark.";
        };
        String where = d.active() ? " The ark stands at " + Ark.centre(d.ark).toShortString() + "." : "";
        return flood + where + watchers;
    }

    // ================================================================== helpers

    private static void next(FloodData d, FloodData.Stage stage, long now) {
        d.stage = stage;
        d.since = now;
        d.setDirty();
    }

    static boolean aboard(FloodData d, Entity e) {
        return Ark.bounds(d.ark).contains(e.position());
    }

    static List<MortalEntity> family(ServerLevel level, FloodData d) {
        List<MortalEntity> out = new ArrayList<>();
        for (UUID id : d.family) if (level.getEntity(id) instanceof MortalEntity m && m.isAlive()) out.add(m);
        return out;
    }

    @Nullable
    static MortalEntity noah(ServerLevel level, FloodData d) {
        return d.noah != null && level.getEntity(d.noah) instanceof MortalEntity m && m.isAlive() ? m : null;
    }

    @Nullable
    static Planet planet(ServerLevel level) {
        int slot = RealmPool.slotOf(level.dimension());
        var planets = CosmicExpanseSavedData.get(level.getServer()).planets;
        return slot >= 0 && slot < planets.size() ? planets.get(slot) : null;
    }

    private static void narrate(ServerLevel level, FloodData d, String text, String ref) {
        Nephilim.narrate(level, Ark.centre(d.ark), text, ref);
    }
}
