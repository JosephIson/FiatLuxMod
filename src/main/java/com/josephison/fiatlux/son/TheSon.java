package com.josephison.fiatlux.son;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.civilization.Faiths;
import com.josephison.fiatlux.civilization.Generations;
import com.josephison.fiatlux.civilization.Justice;
import com.josephison.fiatlux.civilization.MortalPopulation;
import com.josephison.fiatlux.civilization.Person;
import com.josephison.fiatlux.civilization.Polities;
import com.josephison.fiatlux.civilization.Polity;
import com.josephison.fiatlux.civilization.Profession;
import com.josephison.fiatlux.civilization.Religion;
import com.josephison.fiatlux.civilization.Settlement;
import com.josephison.fiatlux.civilization.SoulRealms;
import com.josephison.fiatlux.civilization.Trait;
import com.josephison.fiatlux.civilization.Weddings;
import com.josephison.fiatlux.config.FiatLuxConfig;
import com.josephison.fiatlux.divinity.Alignment;
import com.josephison.fiatlux.divinity.Divinity;
import com.josephison.fiatlux.entity.HeavenlyBeing;
import com.josephison.fiatlux.entity.MortalEntity;
import com.josephison.fiatlux.entity.SoulEntity;
import com.josephison.fiatlux.flood.BirdEntity;
import com.josephison.fiatlux.network.Fx;
import com.josephison.fiatlux.network.FxKind;
import com.josephison.fiatlux.prophecy.Book;
import com.josephison.fiatlux.prophecy.ProphecyData;
import com.josephison.fiatlux.prophecy.Prophet;
import com.josephison.fiatlux.prophecy.Prophets;
import com.josephison.fiatlux.realm.RealmPool;
import com.josephison.fiatlux.registry.ModEntities;
import com.josephison.fiatlux.world.CosmicExpanseSavedData;
import com.josephison.fiatlux.world.CosmosData;
import com.josephison.fiatlux.world.FiatLuxDimensions;
import com.josephison.fiatlux.world.cosmos.MortalEra;
import com.josephison.fiatlux.world.cosmos.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * "For God so loved the world, that he gave his only begotten Son" (John 3:16). On a world the Creator
 * chooses, in a town and to a mother the Creator chooses, the Son is born, named as the Creator names
 * him: Gabriel comes to his mother (Luke 1), a star stands over the town at his birth, angels sing to
 * the shepherds and wise men come from the other towns (Matthew 2, Luke 2). He grows, is baptised by
 * the prophet of his day while the dove descends and the Creator speaks from heaven, and is tempted in
 * the wilderness by the Devil (Matthew 3-4). He calls twelve, goes from town to town teaching in
 * parables, heals, feeds the multitude, calms the storm, walks upon the water, casts out devils and
 * raises the dead, and is transfigured on a mountain. He rides into the chief city to cries of Hosanna,
 * eats the last supper, is betrayed in the garden, tried before the priest and the ruler, and
 * crucified between two thieves under a darkened sky; he goes down to the spirits in prison, rises on
 * the third day, and is taken up into heaven before his disciples. At Pentecost tongues of fire rest on
 * them, the church is born, the apostles go out to every world, and four of them write the Gospels.
 */
@EventBusSubscriber(modid = FiatLux.MODID)
public final class TheSon {
    private TheSon() {}

    /** How long the parts of the story take, in seconds (scaled by sonPace). */
    static double secs(double s) {
        return s / Math.max(0.05, FiatLuxConfig.COMMON.sonPace.get()) * 20;
    }

    // ================================================================== who he is

    public static boolean isSon(@Nullable MortalEntity m) {
        if (m == null || m.getServer() == null) return false;
        SonData d = SonData.get(m.getServer());
        return d.son != null && d.son.equals(m.getUUID()) && d.stage != SonData.Stage.NONE;
    }

    /** Whether this one walks with the Son (his disciples, while he is on the earth). */
    public static boolean disciple(@Nullable MortalEntity m) {
        if (m == null || m.getServer() == null) return false;
        SonData d = SonData.get(m.getServer());
        return d.active() && d.disciples.contains(m.getUUID());
    }

    /** No hand can touch him before his hour is come (John 7:30). */
    public static boolean shielded(MortalEntity m) {
        if (!isSon(m)) return false;
        SonData.Stage s = SonData.get(m.getServer()).stage;
        return s != SonData.Stage.CROSS;
    }

    public static String titleOf(MortalEntity m) {
        if (!isSon(m)) {
            if (disciple(m)) return "Disciple";
            return "";
        }
        SonData d = SonData.get(m.getServer());
        return switch (d.stage) {
            case NATIVITY, CHILDHOOD -> "";
            case RISEN -> "the Risen";
            default -> "Son of " + d.creatorName;
        };
    }

    // ================================================================== the beginning

    /**
     * The Creator sends forth the Son: `name`, of look `variant`, to be born in `town` of `mother` (or
     * of the one the Creator's providence chooses). Returns an error, or null if it is begun.
     */
    @Nullable
    public static String begin(ServerPlayer creator, String name, int variant, @Nullable BlockPos townAt, @Nullable UUID motherId) {
        MinecraftServer server = creator.server;
        SonData d = SonData.get(server);
        if (d.active()) return d.name + " is already sent; his story is in its " + d.stage.name().toLowerCase() + ".";
        ServerLevel level = creator.serverLevel();
        CosmicExpanseSavedData cosmos = CosmicExpanseSavedData.get(server);
        int slot = RealmPool.slotOf(level.dimension());
        Planet world = slot >= 0 && slot < cosmos.planets.size() ? cosmos.planets.get(slot) : null;
        if (world == null || !world.hasLife()) return "Stand on a living world you have made.";
        if (!cosmos.fallOfMan) return "Not before the Fall: they have no need of a Saviour yet.";
        List<Settlement> towns = cosmos.townsOf(world.id);
        if (towns.isEmpty()) return world.name + " has no town yet.";
        Settlement town = townAt == null ? towns.getFirst() : towns.stream().min(Comparator.comparingDouble(t -> t.centre.distSqr(townAt))).orElse(towns.getFirst());
        if (!level.isLoaded(town.centre)) return "Go near " + (town.name.isEmpty() ? "the town" : town.name) + " first.";
        Religion faith = Generations.get(server).faithOf(creator.getUUID(), creator.getGameProfile().getName(), world.name, level.getGameTime());
        MortalEntity mother = motherId != null && level.getEntity(motherId) instanceof MortalEntity m ? m : chooseMother(level, town, faith);
        if (mother == null) return "There is no woman of age in " + (town.name.isEmpty() ? "the town" : town.name) + " to be his mother.";
        start(level, town.centre, town.name.isEmpty() ? world.name : town.name, world.name, creator.getUUID(), creator.getGameProfile().getName(),
                mother, faith.id, name, variant);
        return null;
    }

    /** Sets the story going at `town` (also used by the tests, where there is no created world). */
    public static void start(ServerLevel level, BlockPos townAt, String townName, String worldName, UUID creatorId, String creatorName,
                             MortalEntity mother, UUID faithId, String name, int variant) {
        MinecraftServer server = level.getServer();
        SonData d = SonData.get(server);
        name = name.trim().isEmpty() ? "Immanuel" : name.trim();
        d.stage = SonData.Stage.ANNUNCIATION;
        d.since = level.getGameTime();
        d.step = 0;
        d.name = name;
        d.variant = Math.floorMod(variant, MortalEntity.VARIANTS);
        d.creator = creatorId;
        d.creatorName = creatorName;
        d.world = worldName;
        d.dim = level.dimension().location().toString();
        d.town = townAt;
        d.townName = townName;
        d.mother = mother.getUUID();
        d.motherName = mother.getName().getString();
        d.faith = faithId;
        d.disciples.clear();
        d.discipleNames.clear();
        d.sayings.clear();
        d.deeds.clear();
        d.miracles = d.parables = d.raised = d.fed = 0;
        d.son = null;
        d.father = null;
        d.fatherName = "";
        d.born = -1;
        d.church = null;
        d.betrayer = null;
        d.betrayerName = "";
        d.baptist = null;
        d.baptistName = "";
        d.cross = d.tomb = d.capital = d.visiting = null;
        d.transfigured = d.walkedOnWater = d.wine = false;
        d.setDirty();
        CosmosData.get(server).record(server, d.creatorName, "fulness", "in the fulness of the time sent forth the Son, to be born in "
                + d.townName + " on " + worldName + ", of " + d.motherName + ", and called his name " + name + ".");
    }

    /** A young woman of the faith, unwed if there is one, of the line of Seth or Shem if there is one. */
    @Nullable
    static MortalEntity chooseMother(ServerLevel level, Settlement town, Religion faith) {
        Generations g = Generations.get(level.getServer());
        MortalEntity chosen = level.getEntitiesOfClass(MortalEntity.class, new AABB(town.centre).inflate(64),
                        m -> !m.isMale() && !m.isChild() && m.ageYears() < 50 && !m.isPossessed() && m.sentence() == Justice.Sentence.NONE)
                .stream().max(Comparator.comparingDouble(m -> {
                    Person p = g.person(m.getUUID());
                    return (faith.id.equals(m.religion()) ? 4 : 0) + (m.spouseId() == null ? 3 : 0) + (m.hasTrait(Trait.PIOUS) ? 2 : 0)
                            + (p != null && !p.line.isEmpty() && !p.line.equals("cain") ? 2 : 0) + level.random.nextDouble();
                })).orElse(null);
        if (chosen != null) return chosen;
        // no woman of age in the town: providence brings one forth, a young woman of the faith
        Planet world = world(level);
        BlockPos at = MortalPopulation.safeGround(level, town.centre.getX() + 3, town.centre.getZ() + 4, 6);
        return MortalPopulation.bringForth(level, at != null ? at : town.centre, town.centre, world != null ? world.era : MortalEra.AGRARIAN,
                null, false, 17, null, null, faith.id);
    }

    // ================================================================== the story, a second at a time

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        SonData d = SonData.get(server);
        if (!d.active()) return;
        ServerLevel level = level(server, d);
        if (level == null) return;
        if (server.getTickCount() % 20 == 11) tick(level, d, false);
        Motion.tick(level, d);   // the walking on the sea and the ascension move every tick
    }

    @Nullable
    static ServerLevel level(MinecraftServer server, SonData d) {
        if (d.dim.isEmpty()) return null;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(d.dim));
        return server.getLevel(key);
    }

    @Nullable
    static MortalEntity mortal(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof MortalEntity m && m.isAlive() ? m : null;
    }

    /** One step of the story (every second; `force` hastens its waits, for tests and commands). */
    /** True while the story is being hastened (waits on motion and on the Creator's voice are skipped). */
    static boolean forcing;

    public static void tick(ServerLevel level, SonData d, boolean force) {
        if (d.town != null && !level.isLoaded(d.town) && !force) return;   // the story waits while no one is near
        forcing = force;
        try {
            step(level, d, force);
        } finally {
            forcing = false;
        }
    }

    private static void step(ServerLevel level, SonData d, boolean force) {
        long now = level.getGameTime();
        long t = force ? Long.MAX_VALUE / 4 : now - d.since;
        switch (d.stage) {
            case ANNUNCIATION -> Nativity.annunciation(level, d, t, now);
            case NATIVITY -> Nativity.nativity(level, d, t, now);
            case CHILDHOOD -> Nativity.childhood(level, d, t, now, force);
            case BAPTISM -> Ministry.baptism(level, d, t, now);
            case TEMPTATION -> Ministry.temptation(level, d, t, now);
            case MINISTRY -> Ministry.ministry(level, d, t, now, force);
            case ENTRY -> Passion.entry(level, d, t, now);
            case SUPPER -> Passion.supper(level, d, t, now);
            case GETHSEMANE -> Passion.gethsemane(level, d, t, now);
            case TRIAL -> Passion.trial(level, d, t, now);
            case CROSS -> Passion.cross(level, d, t, now);
            case TOMB -> Passion.tomb(level, d, t, now);
            case RISEN -> Glory.risen(level, d, t, now);
            case ASCENSION -> Glory.ascension(level, d, t, now);
            case PENTECOST -> Glory.pentecost(level, d, t, now);
            default -> {
            }
        }
    }

    /** Runs the story on to its end at once (for tests and the impatient). */
    public static void hasten(ServerLevel level, SonData d, SonData.Stage until) {
        for (int i = 0; i < 400 && d.active() && d.stage.ordinal() < until.ordinal(); i++) tick(level, d, true);
    }

    /** How long since the present part of a stage began (as good as for ever while the story is hastened). */
    static long since(SonData d, long now) {
        return forcing ? Long.MAX_VALUE / 4 : now - d.since;
    }

    /** `step` is reached once `t` has passed `at` seconds; advances to it and returns true the first time. */
    static boolean at(SonData d, long t, int step, double seconds) {
        if (d.step != step || t < secs(seconds)) return false;
        d.step++;
        d.setDirty();
        return true;
    }

    // ================================================================== shared helpers

    static void narrate(ServerLevel level, @Nullable BlockPos near, String text, String ref) {
        Component line = Component.literal(text).withStyle(s -> s.withColor(0xF5D77A).withItalic(true))
                .append(Component.literal("  (" + ref + ")").withStyle(s -> s.withColor(0x9A8A6A).withItalic(false)));
        for (ServerPlayer p : level.players()) if (near == null || p.blockPosition().distSqr(near) < 200 * 200) p.sendSystemMessage(line);
    }

    /** One who has no voice of its own in the world (an angel, the Devil, the Creator) speaks, in chat. */
    static void voice(ServerLevel level, @Nullable BlockPos near, String who, int color, String words) {
        Component line = Component.literal(who + ": ").withStyle(s -> s.withColor(color).withBold(true))
                .append(Component.literal(words).withStyle(s -> s.withColor(0xF5E6B8).withItalic(true).withBold(false)));
        for (ServerPlayer p : level.players()) if (near == null || p.blockPosition().distSqr(near) < 200 * 200) p.sendSystemMessage(line);
    }

    /** The Son speaks: in his bubble, and in full to those near. */
    static void speak(ServerLevel level, MortalEntity son, SonData d, String words, boolean record) {
        son.say(words);
        son.beginAct(MortalEntity.TALK, 70);
        Component line = Component.literal(d.name + ": ").withStyle(s -> s.withColor(0xFFE9A8).withBold(true))
                .append(Component.literal(words).withStyle(s -> s.withColor(0xFFF6E0).withBold(false)));
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(son) < 64 * 64) p.sendSystemMessage(line);
        if (record) d.saying(words);
    }

    static void tellCreator(MinecraftServer server, SonData d, String text) {
        ServerPlayer p = d.creator == null ? null : server.getPlayerList().getPlayer(d.creator);
        if (p != null) p.sendSystemMessage(Component.literal(text).withStyle(s -> s.withColor(0xE8C890)));
    }

    /** Walks one to a place; if no one watches and it is far, it is simply there. Returns whether it has come. */
    static boolean go(ServerLevel level, MortalEntity m, BlockPos to, double near, boolean hurry) {
        Vec3 target = Vec3.atBottomCenterOf(to);
        double d2 = m.distanceToSqr(target);
        if (d2 < near * near) return true;
        boolean watched = level.getNearestPlayer(m, 40) != null;
        if (!watched && d2 > 30 * 30 || hurry || forcing) {
            BlockPos g = MortalPopulation.safeGround(level, to.getX(), to.getZ(), 4);
            BlockPos at = g != null ? g : to;
            m.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            return true;
        }
        if (m.act() == MortalEntity.WORK || m.act() == MortalEntity.EAT || m.act() == MortalEntity.TALK) m.beginAct(MortalEntity.NONE, 1);
        m.walkTo(to, 0.8);
        return false;
    }

    static List<MortalEntity> disciples(ServerLevel level, SonData d) {
        List<MortalEntity> out = new ArrayList<>();
        for (UUID id : d.disciples) {
            MortalEntity m = mortal(level, id);
            if (m != null) out.add(m);
        }
        return out;
    }

    /** The disciples keep near him. */
    static void follow(ServerLevel level, SonData d, MortalEntity son) {
        int i = 0;
        for (MortalEntity m : disciples(level, d)) {
            if (m.getUUID().equals(d.betrayer) && d.stage.ordinal() >= SonData.Stage.GETHSEMANE.ordinal()) continue;
            double a = Math.PI * 2 * i++ / Math.max(1, d.disciples.size());
            BlockPos spot = son.blockPosition().offset((int) Math.round(Math.cos(a) * 3), 0, (int) Math.round(Math.sin(a) * 3));
            if (m.distanceToSqr(son) > 7 * 7) go(level, m, spot, 2.5, m.distanceToSqr(son) > 48 * 48);
            m.setHome(son.blockPosition());
        }
    }

    /** People of the world, gathered round. */
    static List<MortalEntity> crowd(ServerLevel level, BlockPos at, double r) {
        return level.getEntitiesOfClass(MortalEntity.class, new AABB(at).inflate(r), m -> !isSon(m));
    }

    @Nullable
    static Planet world(ServerLevel level) {
        CosmicExpanseSavedData c = CosmicExpanseSavedData.get(level.getServer());
        int slot = RealmPool.slotOf(level.dimension());
        return slot >= 0 && slot < c.planets.size() ? c.planets.get(slot) : null;
    }

    /** A sign of glory about him: light, and motes rising. */
    static void glory(ServerLevel level, MortalEntity son, int strength) {
        level.sendParticles(com.josephison.fiatlux.registry.ModParticles.LIGHT_MOTE.get(), son.getX(), son.getY() + 1.2, son.getZ(),
                4 * strength, 0.4, 0.8, 0.4, 0.01);
        if (strength > 1) Fx.send(level, FxKind.BLESSING, son.position(), son.getBbHeight(), 0xFFFFF4D6, 20);
    }

    /** A heavenly being for a moment of the story: still, and gone when it is done. */
    @Nullable
    static net.minecraft.world.entity.Mob herald(ServerLevel level, EntityType<? extends net.minecraft.world.entity.Mob> type, Vec3 at, String name) {
        net.minecraft.world.entity.Mob b = type.create(level);
        if (b == null) return null;
        b.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360f, 0);
        b.setNoAi(true);
        b.setNoGravity(true);
        b.addTag("fiatlux_son_story");
        if (!name.isEmpty()) b.setCustomName(Component.literal(name));
        level.addFreshEntity(b);
        Fx.send(level, FxKind.HOST_ARRIVAL, b.position(), 2.5f, 0xFFFFF4D6, 40);
        return b;
    }

    static void dismiss(ServerLevel level, BlockPos around) {
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(around).inflate(160),
                e -> e.getTags().contains("fiatlux_son_story"))) {
            level.sendParticles(com.josephison.fiatlux.registry.ModParticles.HOLY_SPARK.get(), e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 0.8, 0.3, 0.08);
            e.discard();
        }
    }

    // ================================================================== the voice from heaven

    /** While the story waits on the Creator's words (at the baptism, the transfiguration), what the Creator says. */
    static String awaitingVoice = "";
    static long awaitingUntil;
    static String spoken = "";

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        if (awaitingVoice.isEmpty()) return;
        ServerPlayer player = event.getPlayer();
        SonData d = SonData.get(player.server);
        if (!player.getUUID().equals(d.creator)) return;
        String said = event.getRawText().toLowerCase();
        if (said.contains("beloved") || said.contains("my son") || said.contains("hear him") || said.contains("well pleased")) {
            spoken = event.getRawText();
        }
    }

    // ================================================================== the Son made new, for the record

    /** The Gospels, written by four of the twelve once the church is born. */
    static void gospels(ServerLevel level, SonData d, Religion church) {
        ProphecyData pd = ProphecyData.get(level.getServer());
        List<String> writers = new ArrayList<>();
        for (int i = 0; i < d.disciples.size() && writers.size() < 4; i++) {
            if (!d.disciples.get(i).equals(d.betrayer)) writers.add(d.discipleNames.get(i));
        }
        if (writers.isEmpty()) writers.add("the disciples");
        for (int k = 0; k < writers.size(); k++) {
            String w = writers.get(k);
            Book b = new Book("The Gospel according to " + w, Book.Kind.GOSPEL, w, level.getGameTime() + k, church.id);
            b.chapter("The Generation of " + d.name);
            b.verse(genealogy(level, d));
            if (k != 1) {   // one of them begins at the baptism, as Mark does
                b.chapter("The Birth of " + d.name);
                b.verse("The angel Gabriel was sent from " + d.creatorName + " unto " + d.townName + ", to " + d.motherName
                        + ", and said, Thou shalt bring forth a son, and shalt call his name " + d.name + ".");
                b.verse("And she brought forth her firstborn son in " + d.townName + ", and a star stood over the place where the young child was.");
            }
            b.chapter("The Baptism and the Temptation");
            if (!d.baptistName.isEmpty()) b.verse("And " + d.name + " was baptised of " + d.baptistName + ".");
            b.verse("And lo a voice from heaven, saying, This is my beloved Son, in whom I am well pleased.");
            b.verse("Then was " + d.name + " led up of the Spirit into the wilderness to be tempted of the devil; and the devil leaveth him.");
            b.chapter("The Teaching of " + d.name);
            for (int i = k % 2; i < d.sayings.size(); i += (k == 3 ? 1 : 2)) b.verse(d.sayings.get(i));
            b.chapter("The Works of " + d.name);
            for (int i = k % 2; i < d.deeds.size(); i += (k == 3 ? 1 : 2)) b.verse(d.deeds.get(i));
            b.chapter("The Passion");
            b.verse("And " + d.name + " rode into " + d.capitalName + ", and they cried, Hosanna; blessed is he that cometh in the name of " + d.creatorName + ".");
            b.verse("And as they did eat, he took bread, and blessed it, and brake it, and said, Take, eat; this is my body.");
            if (!d.betrayerName.isEmpty()) b.verse("And " + d.betrayerName + ", one of the twelve, betrayed him with a kiss.");
            b.verse("And they crucified him, and set up over his head his accusation written, " + d.name.toUpperCase()
                    + " THE KING OF " + d.world.toUpperCase() + ".");
            b.verse("And " + d.name + " cried with a loud voice, It is finished: and he gave up the ghost.");
            b.chapter("The Resurrection");
            b.verse("He is not here: for he is risen, as he said.");
            b.verse("And he was taken up; and a cloud received him out of their sight.");
            if (k == 2) b.verse("And many other signs truly did " + d.name + " which are not written in this book: but these are written, that ye might believe.");
            pd.books.add(b);
        }
        Book acts = new Book("The Acts of the Apostles", Book.Kind.GOSPEL, writers.getFirst(), level.getGameTime() + 9, church.id);
        acts.chapter("Pentecost");
        acts.verse("And when the day of Pentecost was fully come, they were all with one accord in one place.");
        acts.verse("And there appeared unto them cloven tongues like as of fire, and it sat upon each of them.");
        acts.verse("And they went forth, and preached every where, " + d.creatorName + " working with them.");
        pd.books.add(acts);
        pd.setDirty();
    }

    /** "Which was the son of ..., which was the son of Adam, which was the son of God" (Luke 3:38). */
    static String genealogy(ServerLevel level, SonData d) {
        Generations g = Generations.get(level.getServer());
        StringBuilder b = new StringBuilder(d.name + ", born of " + d.motherName);
        Person p = g.person(d.mother);
        int n = 0;
        while (p != null && n++ < 40) {
            Person parent = g.person(p.father != null ? p.father : p.mother);
            if (parent == null) break;
            b.append(", which was the ").append(p.male ? "son" : "daughter").append(" of ").append(parent.name);
            p = parent;
        }
        b.append(", which was of ").append(d.creatorName).append(".");
        return b.toString();
    }

    /** Ends the story where it stands (the Son is taken away), so that it can be begun again. */
    public static String reset(MinecraftServer server) {
        SonData d = SonData.get(server);
        if (d.stage == SonData.Stage.NONE) return "The Son has not been sent.";
        ServerLevel level = level(server, d);
        if (level != null) {
            MortalEntity son = mortal(level, d.son);
            if (son != null) {
                son.release();
                son.discard();
            }
            if (d.town != null) dismiss(level, d.town);
            if (d.tomb != null) Passion.seal(level, d.tomb, false);
        }
        String name = d.name;
        d.to(SonData.Stage.NONE, 0);
        d.son = null;
        awaitingVoice = "";
        return "The story of " + name + " is set aside; /fiatlux son to send the Son again.";
    }

    // ================================================================== reports

    public static String describe(MinecraftServer server) {
        SonData d = SonData.get(server);
        if (d.stage == SonData.Stage.NONE) return "The Son has not been sent. Use /fiatlux son on a living world to choose his name, his look, his town and his mother.";
        String where = d.stage.name().toLowerCase().replace('_', ' ');
        return d.name + ", Son of " + d.creatorName + ", born of " + d.motherName + " in " + d.townName + " on " + d.world + ": "
                + (d.stage == SonData.Stage.DONE ? "his story is fulfilled" : "now at his " + where) + ". " + d.disciples.size() + " disciples, "
                + d.miracles + " miracles, " + d.parables + " parables, " + d.raised + " raised from the dead.";
    }

    static ItemStack gift(net.minecraft.world.item.Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(st -> st.withItalic(false).withColor(0xF5D77A)));
        return s;
    }

    static void drop(ServerLevel level, Vec3 at, ItemStack stack) {
        ItemEntity e = new ItemEntity(level, at.x, at.y + 0.5, at.z, stack);
        e.setPickUpDelay(40);
        level.addFreshEntity(e);
    }

    /** The nearest water's surface within `r` of `around`, if any. */
    @Nullable
    static BlockPos water(ServerLevel level, BlockPos around, int r) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (int dx = -r; dx <= r; dx += 2) for (int dz = -r; dz <= r; dz += 2) {
            int x = around.getX() + dx, z = around.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            BlockPos p = new BlockPos(x, y, z);
            if (!level.getFluidState(p).is(FluidTags.WATER) || !level.getFluidState(p.below()).is(FluidTags.WATER)) continue;
            double dd = p.distSqr(around);
            if (dd < bd) {
                bd = dd;
                best = p;
            }
        }
        return best;
    }

    static void sound(ServerLevel level, BlockPos at, net.minecraft.sounds.SoundEvent s, float vol, float pitch) {
        level.playSound(null, at, s, SoundSource.AMBIENT, vol, pitch);
    }

    // used by the parts of the story
    static final net.minecraft.util.RandomSource R = net.minecraft.util.RandomSource.create();
}
